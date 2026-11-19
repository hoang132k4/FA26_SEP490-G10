package com.jlpp.modules.payment.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.payment.dto.InvoiceResponseDTO;
import com.jlpp.modules.payment.dto.PaymentRequestDTO;
import com.jlpp.modules.payment.dto.PaymentResponseDTO;
import com.jlpp.modules.payment.dto.PaymentStatusDTO;
import com.jlpp.modules.payment.dto.SePayWebhookResult;
import com.jlpp.modules.payment.security.SepayWebhookSignatureVerifier;
import com.jlpp.modules.payment.service.CoursePaymentPriceCalculator;
import com.jlpp.modules.payment.service.PaymentService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;
    private final SepayWebhookSignatureVerifier sepayWebhookSignatureVerifier;
    private final CoursePaymentPriceCalculator priceCalculator;

    @GetMapping({"/checkout", "/checkout/{courseId}"})
    public String checkoutPage(@PathVariable(value = "courseId", required = false) Long courseId,
                              @RequestParam(value = "courseId", required = false) Long queryCourseId,
                              Principal principal,
                              HttpSession session,
                              Model model) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/checkout";
        }

        Long resolvedCourseId = courseId != null ? courseId : queryCourseId;
        Course course = null;
        if (resolvedCourseId != null) {
            course = courseRepository.findById(resolvedCourseId).orElse(null);
        }
        User user = userRepository.findById(userId).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("course", course);
        model.addAttribute("checkoutAmount",
                course == null ? null : priceCalculator.calculateFinalPrice(course));
        model.addAttribute("activeMenu", "orders");
        model.addAttribute("pageTitle", "Checkout - JEPS");
        return "pages/payment/checkout";
    }

    @PostMapping({"/checkout", "/checkout/{courseId}"})
    public String createCheckout(@PathVariable(value = "courseId", required = false) Long pathCourseId,
                                 @RequestParam(value = "courseId", required = false) Long courseId,
                                 Principal principal,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/checkout";
        }
        Long resolvedCourseId = pathCourseId != null ? pathCourseId : courseId;
        if (resolvedCourseId == null) {
            return "redirect:/courses";
        }

        PaymentResponseDTO payment = paymentService.createPayment(userId,
                PaymentRequestDTO.builder().courseId(resolvedCourseId).paymentMethod("SEPAY").build());
        return "redirect:/payment/" + payment.getId();
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
        model.addAttribute("activeMenu", "orders");
        model.addAttribute("pageTitle", "Thanh toán - JEPS");
        return "pages/payment/payment";
    }

    @GetMapping("/invoice/{paymentId}")
    public String invoicePage(@PathVariable Long paymentId,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/invoice/" + paymentId;
        }

        InvoiceResponseDTO invoice = paymentService.getInvoice(userId, paymentId);
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
        } catch (Exception ex) {
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
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
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
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
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
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/api/webhooks/sepay", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sepayWebhook(
            @RequestBody byte[] rawBody,
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
            payload = objectMapper.readValue(rawBody, new TypeReference<>() { });
        } catch (IOException ex) {
            log.warn("SePay webhook rejected: invalid JSON payload");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "code", "INVALID_JSON"));
        }
        if (payload == null) {
            log.warn("SePay webhook rejected: empty JSON object");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "code", "INVALID_JSON"));
        }

        try {
            return webhookResponse(paymentService.processSePayWebhook(payload));
        } catch (Exception ex) {
            log.error("SePay webhook processing failed ({})", ex.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "code", "INTERNAL_ERROR"));
        }
    }

    private ResponseEntity<Map<String, Object>> webhookResponse(SePayWebhookResult result) {
        return switch (result.outcome()) {
            case PROCESSED -> ResponseEntity.ok(
                    Map.of("success", true, "message", "Webhook processed"));
            case PROCESSED_LATE -> ResponseEntity.ok(
                    Map.of("success", true, "message", "Late payment processed"));
            case DUPLICATE -> ResponseEntity.ok(
                    Map.of("success", true, "duplicate", true, "message", "Already processed"));
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
