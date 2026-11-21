package com.jlpp.modules.payment.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.service.CoursePriceCalculator;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.payment.dto.InvoiceResponseDTO;
import com.jlpp.modules.payment.dto.PaymentRequestDTO;
import com.jlpp.modules.payment.dto.PaymentResponseDTO;
import com.jlpp.modules.payment.dto.SePayWebhookResult;
import com.jlpp.modules.payment.exception.PaymentConfigurationException;
import com.jlpp.modules.payment.security.SepayWebhookSignatureVerifier;
import com.jlpp.modules.payment.service.PaymentService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private static final Duration LATE_WEBHOOK_POLL_GRACE = Duration.ofMinutes(10);
    private static final Duration FALLBACK_POLL_WINDOW = Duration.ofMinutes(30);

    private final PaymentService paymentService;
    private final CourseRegistrationService courseRegistrationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SepayWebhookSignatureVerifier sepayWebhookSignatureVerifier;
    private final CoursePriceCalculator priceCalculator;

    @GetMapping({"/checkout", "/checkout/{courseId}"})
    public String checkoutPage(@PathVariable(value = "courseId", required = false) Long courseId,
                              @RequestParam(value = "courseId", required = false) Long queryCourseId,
                              @RequestParam(value = "registrationId", required = false) Long registrationId,
                              Principal principal,
                              HttpSession session,
                              Model model) {
        Long resolvedCourseId = courseId != null ? courseId : queryCourseId;
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/courses";
        }

        if (registrationId == null) {
            if (resolvedCourseId == null) {
                return "redirect:/courses";
            }
            return courseRegistrationService.isUserEnrolled(userId, resolvedCourseId)
                    ? "redirect:/courses/" + resolvedCourseId
                    : "redirect:/learning/courses/" + resolvedCourseId + "/register";
        }
        CourseRegistration registration = courseRegistrationService.getRegistrationById(userId, registrationId);
        if (registration == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Course course = registration.getCourse();
        if (resolvedCourseId != null && !resolvedCourseId.equals(course.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        resolvedCourseId = course.getId();
        if (!CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
            return courseRegistrationService.isUserEnrolled(userId, resolvedCourseId)
                    ? "redirect:/courses/" + resolvedCourseId
                    : "redirect:/learning/courses/" + resolvedCourseId + "/register";
        }

        User user = userRepository.findById(userId).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("course", course);
        model.addAttribute("registration", registration);
        model.addAttribute("checkoutAmount", priceCalculator.calculateFinalPrice(course));
        model.addAttribute("activeMenu", "orders");
        model.addAttribute("pageTitle", "Checkout - JEPS");
        return "pages/payment/checkout";
    }

    @PostMapping({"/checkout", "/checkout/{courseId}"})
    public String createCheckout(@PathVariable(value = "courseId", required = false) Long pathCourseId,
                                 @RequestParam(value = "courseId", required = false) Long courseId,
                                 @RequestParam(value = "registrationId", required = false) Long registrationId,
                                 Principal principal,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long resolvedCourseId = pathCourseId != null ? pathCourseId : courseId;
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/courses";
        }
        if (registrationId == null) {
            if (resolvedCourseId == null) {
                return "redirect:/courses";
            }
            return courseRegistrationService.isUserEnrolled(userId, resolvedCourseId)
                    ? "redirect:/courses/" + resolvedCourseId
                    : "redirect:/learning/courses/" + resolvedCourseId + "/register";
        }

        CourseRegistration registration = courseRegistrationService.getRegistrationById(userId, registrationId);
        if (registration == null
                || (resolvedCourseId != null && !resolvedCourseId.equals(registration.getCourse().getId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        resolvedCourseId = registration.getCourse().getId();

        try {
            PaymentResponseDTO payment = paymentService.createPayment(userId,
                    PaymentRequestDTO.builder()
                            .courseId(resolvedCourseId)
                            .registrationId(registrationId)
                            .paymentMethod("SEPAY")
                            .build());
            return "redirect:/payment/" + payment.getId();
        } catch (PaymentConfigurationException exception) {
            redirectAttributes.addFlashAttribute("paymentError", exception.getMessage());
            return "redirect:/checkout/" + resolvedCourseId + "?registrationId=" + registrationId;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("paymentError", exception.getMessage());
            return "redirect:/checkout/" + resolvedCourseId + "?registrationId=" + registrationId;
        }
    }

    @GetMapping("/payment/{paymentId}")
    public String paymentPage(@PathVariable Long paymentId,
                              Principal principal,
                              HttpSession session,
                              Model model) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/payment/" + paymentId;
        }

        PaymentResponseDTO payment = paymentService.getPaymentForUser(userId, paymentId);
        model.addAttribute("payment", payment);
        model.addAttribute("paymentPollingDeadlineEpochMillis", payment.getExpiresAt() == null
                ? Instant.now().plus(FALLBACK_POLL_WINDOW).toEpochMilli()
                : payment.getExpiresAt().atZone(ZoneId.systemDefault())
                        .plus(LATE_WEBHOOK_POLL_GRACE).toInstant().toEpochMilli());
        model.addAttribute("activeMenu", "orders");
        model.addAttribute("pageTitle", "Thanh toán - JEPS");
        return "pages/payment/payment";
    }

    @GetMapping("/invoice/{paymentId}")
    public String invoicePage(@PathVariable Long paymentId,
                             Principal principal,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/invoice/" + paymentId;
        }

        InvoiceResponseDTO invoice;
        try {
            invoice = paymentService.getInvoice(userId, paymentId);
        } catch (SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("paymentNotice", exception.getMessage());
            return "redirect:/payment/" + paymentId;
        }
        model.addAttribute("invoice", invoice);
        model.addAttribute("activeMenu", "orders");
        model.addAttribute("pageTitle", "Hóa đơn - JEPS");
        return "pages/payment/invoice";
    }

    @PostMapping("/api/payments")
    @ResponseBody
    public ResponseEntity<?> createPaymentApi(@RequestBody(required = false) PaymentRequestDTO request,
                                             @RequestParam(value = "courseId", required = false) Long courseId,
                                             Principal principal,
                                             HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        if (request == null) {
            request = new PaymentRequestDTO();
        }
        if (request.getCourseId() == null && courseId != null) {
            request.setCourseId(courseId);
        }
        try {
            return ResponseEntity.ok(paymentService.createPayment(userId, request));
        } catch (PaymentConfigurationException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "PAYMENT_CONFIGURATION_UNAVAILABLE",
                            "message", exception.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/api/payments/{paymentId}")
    @ResponseBody
    public ResponseEntity<?> getPaymentApi(@PathVariable Long paymentId,
                                          Principal principal,
                                          HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        try {
            return ResponseEntity.ok(paymentService.getPaymentForUser(userId, paymentId));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "PAYMENT_NOT_FOUND"));
        }
    }

    @GetMapping("/api/payments/{paymentId}/status")
    @ResponseBody
    public ResponseEntity<?> getStatusApi(@PathVariable Long paymentId,
                                         Principal principal,
                                         HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        try {
            return ResponseEntity.ok(paymentService.getPaymentStatus(userId, paymentId));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "PAYMENT_NOT_FOUND"));
        }
    }

    @GetMapping("/api/payments/{paymentId}/invoice")
    @ResponseBody
    public ResponseEntity<?> getInvoiceApi(@PathVariable Long paymentId,
                                         Principal principal,
                                         HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        try {
            return ResponseEntity.ok(paymentService.getInvoice(userId, paymentId));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "PAYMENT_NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "INVOICE_NOT_AVAILABLE"));
        }
    }

    @PostMapping(path = "/api/webhooks/sepay", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sepayWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) MediaType contentType,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp) {
        SepayWebhookSignatureVerifier.VerificationResult verification;
        try {
            verification = sepayWebhookSignatureVerifier.verify(rawBody, timestamp, signature);
        } catch (Exception ex) {
            log.error("SePay signature verification failed ({})", ex.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "code", "INTERNAL_ERROR"));
        }

        if (verification == SepayWebhookSignatureVerifier.VerificationResult.SECRET_NOT_CONFIGURED) {
            log.error("SePay webhook rejected: authentication secret is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "code", "WEBHOOK_AUTH_NOT_CONFIGURED"));
        }
        if (verification != SepayWebhookSignatureVerifier.VerificationResult.VALID) {
            log.warn("SePay webhook rejected: invalid authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "code", "WEBHOOK_AUTH_FAILED"));
        }

        Map<String, Object> payload;
        try {
            payload = parseWebhookPayload(rawBody, contentType);
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("SePay webhook rejected: invalid payload");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "code", "INVALID_PAYLOAD"));
        }
        if (payload == null || payload.isEmpty()) {
            log.warn("SePay webhook rejected: empty payload");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "code", "INVALID_PAYLOAD"));
        }

        try {
            return webhookResponse(paymentService.processSePayWebhook(payload));
        } catch (Exception ex) {
            log.error("SePay webhook processing failed ({})", ex.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "code", "INTERNAL_ERROR"));
        }
    }

    private Map<String, Object> parseWebhookPayload(byte[] rawBody, MediaType contentType) throws IOException {
        if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return objectMapper.readValue(rawBody, new TypeReference<>() { });
        }
        if (!MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
            throw new IllegalArgumentException("Unsupported webhook content type.");
        }

        String formBody = new String(rawBody, StandardCharsets.UTF_8);
        Map<String, Object> payload = new LinkedHashMap<>();
        for (String pair : formBody.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] nameValue = pair.split("=", 2);
            String name = URLDecoder.decode(nameValue[0], StandardCharsets.UTF_8);
            String value = nameValue.length == 1
                    ? ""
                    : URLDecoder.decode(nameValue[1], StandardCharsets.UTF_8);
            if (name.isBlank() || payload.containsKey(name)) {
                throw new IllegalArgumentException("Webhook form contains an invalid or duplicate field.");
            }
            payload.put(name, value);
        }
        return payload;
    }

    private ResponseEntity<Map<String, Object>> webhookResponse(SePayWebhookResult result) {
        return switch (result.outcome()) {
            case PROCESSED, PROCESSED_LATE, DUPLICATE, RECONCILIATION_REQUIRED ->
                    ResponseEntity.ok(Map.of("success", true));
            case PAYMENT_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "code", "PAYMENT_NOT_FOUND"));
            case INVALID_TRANSACTION_ID, INVALID_PAYLOAD, INVALID_TRANSACTION_DATE ->
                    ResponseEntity.badRequest().body(Map.of("success", false, "code", result.outcome().name()));
            case AMOUNT_MISMATCH, ACCOUNT_MISMATCH, INVALID_DIRECTION ->
                    ResponseEntity.unprocessableEntity()
                            .body(Map.of("success", false, "code", result.outcome().name()));
            case PAYMENT_STATE_REJECTED -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "code", result.outcome().name()));
        };
    }

    private Long resolveUserId(Principal principal, HttpSession session) {
        if (principal != null && principal.getName() != null) {
            String identifier = principal.getName().trim();
            Optional<User> user = userRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> userRepository.findByUsername(identifier));
            if (user.isPresent()) {
                return user.get().getId();
            }
        }
        if (session != null && session.getAttribute("CURRENT_LEARNER_ID") != null) {
            Object value = session.getAttribute("CURRENT_LEARNER_ID");
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }
}
