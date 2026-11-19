package com.jlpp.modules.payment.service.impl;

import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.payment.dto.InvoiceResponseDTO;
import com.jlpp.modules.payment.dto.PaymentRequestDTO;
import com.jlpp.modules.payment.dto.PaymentResponseDTO;
import com.jlpp.modules.payment.dto.PaymentStatusDTO;
import com.jlpp.modules.payment.dto.SePayWebhookResult;
import com.jlpp.modules.payment.entity.Payment;
import com.jlpp.modules.payment.entity.PaymentTransaction;
import com.jlpp.modules.payment.entity.PaymentWebhookLog;
import com.jlpp.modules.payment.exception.PaymentConfigurationException;
import com.jlpp.modules.payment.repository.PaymentRepository;
import com.jlpp.modules.payment.repository.PaymentTransactionRepository;
import com.jlpp.modules.payment.repository.PaymentWebhookLogRepository;
import com.jlpp.modules.payment.security.SepayAccountNumberCryptoService;
import com.jlpp.modules.payment.security.SepayWebhookSignatureVerifier;
import com.jlpp.modules.payment.service.CoursePaymentPriceCalculator;
import com.jlpp.modules.payment.service.PaymentOrderCodeGenerator;
import com.jlpp.modules.payment.service.PaymentService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookLogRepository paymentWebhookLogRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseRegistrationService courseRegistrationService;
    private final SepayAccountNumberCryptoService accountNumberCryptoService;
    private final SepayWebhookSignatureVerifier sepayWebhookSignatureVerifier;
    private final CoursePaymentPriceCalculator priceCalculator;
    private final PaymentOrderCodeGenerator orderCodeGenerator;
    private final PlatformTransactionManager transactionManager;

    private static final List<String> ACCOUNT_NUMBER_FIELDS =
            List.of("accountNumber", "bankAccountNumber", "accountNo");
    private static final Set<String> SELLABLE_COURSE_STATUSES = Set.of("PUBLISHED", "ACTIVE", "PUBLIC");
    private static final int ORDER_CODE_ATTEMPTS = 5;

    @Value("${payment.expire-minutes:15}")
    private int expireMinutes;

    @Value("${BANK_ACCOUNT_NUMBER:}")
    private String bankAccountNumber;

    @Value("${BANK_ACCOUNT_NAME:JEPS}")
    private String bankAccountName;

    @Value("${BANK_CODE:MBBank}")
    private String bankCode;

    @Override
    public PaymentResponseDTO createPayment(Long userId, PaymentRequestDTO request) {
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Thiếu mã khóa học để tạo thanh toán.");
        }

        validatePaymentConfiguration();
        for (int attempt = 0; attempt < ORDER_CODE_ATTEMPTS; attempt++) {
            String orderCode = orderCodeGenerator.generateOrderCode();
            String transferContent = orderCodeGenerator.generateTransferContent(orderCode);
            try {
                return requiresNewTransactionTemplate().execute(transactionStatus ->
                        createPaymentInTransaction(userId, request, orderCode, transferContent));
            } catch (DataIntegrityViolationException exception) {
                if (!isUniqueConstraintViolation(exception)
                        || !isOrderCodeCollision(orderCode, transferContent)) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("Không thể tạo mã thanh toán duy nhất. Vui lòng thử lại.");
    }

    private TransactionTemplate requiresNewTransactionTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private boolean isOrderCodeCollision(String orderCode, String transferContent) {
        Boolean collision = requiresNewTransactionTemplate().execute(transactionStatus ->
                paymentRepository.existsByOrderCode(orderCode)
                        || paymentRepository.existsByTransferContent(transferContent));
        return Boolean.TRUE.equals(collision);
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (!(cause instanceof SQLException sqlException)) {
                continue;
            }
            for (SQLException sqlCause = sqlException; sqlCause != null;
                 sqlCause = sqlCause.getNextException()) {
                if ("23505".equals(sqlCause.getSQLState())
                        || ("23000".equals(sqlCause.getSQLState())
                        && (sqlCause.getErrorCode() == 1062
                        || sqlCause.getErrorCode() == 2601
                        || sqlCause.getErrorCode() == 2627))) {
                    return true;
                }
            }
        }
        return false;
    }

    private PaymentResponseDTO createPaymentInTransaction(
            Long userId, PaymentRequestDTO request, String orderCode, String transferContent) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học."));

        String courseStatus = course.getStatus() == null
                ? ""
                : course.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!SELLABLE_COURSE_STATUSES.contains(courseStatus)) {
            throw new IllegalStateException("Khóa học hiện không mở bán.");
        }

        if (course.getPrice() == null || course.getPrice() <= 0) {
            throw new IllegalArgumentException("Khóa học này chưa có giá thanh toán hợp lệ.");
        }
        BigDecimal amount = priceCalculator.calculateFinalPrice(course);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Khóa học này chưa có giá thanh toán hợp lệ.");
        }

        if (courseRegistrationService.isUserEnrolled(userId, course.getId())) {
            throw new IllegalStateException("Bạn đã đăng ký khóa học này rồi.");
        }

        if (paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
                userId, course.getId(), "PAID").isPresent()) {
            throw new IllegalStateException("Khóa học này đã được thanh toán.");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment reusablePendingPayment = null;
        for (Payment existingPayment : paymentRepository
                .findByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
                        userId, course.getId(), "PENDING")) {
            if (existingPayment.getExpiresAt() != null && existingPayment.getExpiresAt().isAfter(now)) {
                if (reusablePendingPayment == null) {
                    reusablePendingPayment = existingPayment;
                }
                continue;
            }
            existingPayment.setStatus("EXPIRED");
            paymentRepository.save(existingPayment);
        }
        if (reusablePendingPayment != null) {
            return toResponse(reusablePendingPayment);
        }

        Payment payment = Payment.builder()
                .orderCode(orderCode)
                .user(user)
                .course(course)
                .amount(amount)
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PENDING")
                .description(request.getDescription() != null && !request.getDescription().isBlank()
                        ? request.getDescription() : "Thanh toán khóa học " + course.getTitle())
                .transferContent(transferContent)
                .expiresAt(now.plusMinutes(expireMinutes))
                .build();

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    private void validatePaymentConfiguration() {
        List<String> invalidSettings = new java.util.ArrayList<>();
        if (bankAccountNumber == null || !bankAccountNumber.trim().matches("[0-9]{6,20}")) {
            invalidSettings.add("BANK_ACCOUNT_NUMBER");
        }
        if (bankCode == null || !bankCode.matches("[A-Za-z0-9_-]{1,32}")) {
            invalidSettings.add("BANK_CODE");
        }
        if (bankAccountName == null || bankAccountName.isBlank()) {
            invalidSettings.add("BANK_ACCOUNT_NAME");
        }
        if (expireMinutes <= 0) {
            invalidSettings.add("payment.expire-minutes");
        }
        if (!sepayWebhookSignatureVerifier.isConfigured()) {
            invalidSettings.add("SEPAY_WEBHOOK_SECRET");
        }
        if (!invalidSettings.isEmpty()) {
            throw new PaymentConfigurationException(
                    "Payment is unavailable. Check required configuration: "
                            + String.join(", ", invalidSettings) + ".");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentForUser(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithCourse(paymentId, userId)
                .orElseThrow(() -> new SecurityException("Bạn không có quyền truy cập giao dịch này."));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusDTO getPaymentStatus(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithCourse(paymentId, userId)
                .orElseThrow(() -> new SecurityException("Bạn không có quyền truy cập giao dịch này."));
        return PaymentStatusDTO.builder()
                .paymentId(payment.getId())
                .orderCode(payment.getOrderCode())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .expiresAt(payment.getExpiresAt())
                .transferContent(payment.getTransferContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoice(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithCourse(paymentId, userId)
                .orElseThrow(() -> new SecurityException("Bạn không có quyền truy cập hóa đơn này."));
        if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Hóa đơn chỉ khả dụng sau khi thanh toán thành công.");
        }

        String transactionId = null;
        Optional<PaymentTransaction> tx =
                paymentTransactionRepository.findFirstByPayment_IdOrderByCreatedAtDesc(payment.getId());
        if (tx.isPresent()) {
            transactionId = tx.get().getGatewayTransactionId();
        }

        return InvoiceResponseDTO.builder()
                .paymentId(payment.getId())
                .orderCode(payment.getOrderCode())
                .customerName(payment.getUser().getFullName())
                .customerEmail(payment.getUser().getEmail())
                .courseId(payment.getCourse().getId())
                .courseTitle(payment.getCourse().getTitle())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .bankName(bankCode)
                .transferContent(payment.getTransferContent())
                .paidAt(payment.getPaidAt())
                .status(payment.getStatus())
                .transactionId(transactionId)
                .build();
    }

    @Override
    @Transactional
    public SePayWebhookResult processSePayWebhook(Map<String, Object> payload) {
        if (payload == null) {
            return new SePayWebhookResult(SePayWebhookResult.Outcome.INVALID_PAYLOAD);
        }

        String gateway = "SEPAY";
        String eventId = payloadString(payload, "id", "eventId");
        String gatewayTransactionId = resolveGatewayTransactionId(payload);
        String content = payloadString(payload, "content", "description", "transferContent");
        String bankAccount = payloadString(payload, "accountNumber", "bankAccountNumber", "accountNo");
        String direction = payloadString(payload, "transferType", "direction", "transactionType");
        BigDecimal amount = toBigDecimal(payloadValue(payload, "transferAmount", "amount"));
        Map<String, Object> logPayload = encryptAccountNumberFields(payload);

        PaymentWebhookLog log = PaymentWebhookLog.builder()
                .gateway(gateway)
                .eventId(eventId)
                .gatewayTransactionId(gatewayTransactionId)
                .processingStatus("RECEIVED")
                .processingMessage("Webhook được nhận")
                .receivedAt(LocalDateTime.now())
                .requestPayload(logPayload)
                .build();
        paymentWebhookLogRepository.save(log);

        if (gatewayTransactionId.isBlank() || !hasValidSePayId(payload)) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Thiếu gateway_transaction_id");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.INVALID_TRANSACTION_ID);
        }

        Long paymentUserId = paymentRepository.findUserIdByTransferContent(content).orElse(null);
        if (paymentUserId == null) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Không tìm thấy payment cho nội dung chuyển khoản");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.PAYMENT_NOT_FOUND);
        }

        userRepository.findByIdForUpdate(paymentUserId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng của payment."));

        Payment payment = paymentRepository.findByTransferContentForUpdate(content)
                .orElse(null);
        if (payment == null) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Không tìm thấy payment cho nội dung chuyển khoản");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.PAYMENT_NOT_FOUND);
        }

        if (paymentTransactionRepository.existsByGatewayTransactionId(gatewayTransactionId)) {
            log.setProcessingStatus("DUPLICATE");
            log.setProcessingMessage("Giao dịch đã được xử lý");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.DUPLICATE);
        }

        if (amount == null || amount.compareTo(payment.getAmount()) != 0) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Số tiền không khớp với payment");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.AMOUNT_MISMATCH);
        }

        if (bankAccountNumber == null || bankAccountNumber.isBlank()
                || bankAccount.isBlank()
                || !bankAccountNumber.trim().equalsIgnoreCase(bankAccount)) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Tài khoản ngân hàng nhận không khớp");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.ACCOUNT_MISMATCH);
        }

        if (!"IN".equalsIgnoreCase(direction) && !"INCOMING".equalsIgnoreCase(direction)) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Giao dịch không phải là chuyển tiền vào");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.INVALID_DIRECTION);
        }

        LocalDateTime transactionDate = parseTransactionDate(payloadString(payload, "transactionDate"));
        if (transactionDate == null) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("transactionDate không hợp lệ");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.INVALID_TRANSACTION_DATE);
        }

        String paymentStatus = payment.getStatus();
        if ("PAID".equalsIgnoreCase(paymentStatus)) {
            log.setProcessingStatus("DUPLICATE");
            log.setProcessingMessage("Payment đã được thanh toán trước đó");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.DUPLICATE);
        }

        if (!"PENDING".equalsIgnoreCase(paymentStatus) && !"EXPIRED".equalsIgnoreCase(paymentStatus)) {
            log.setProcessingStatus("REJECTED");
            log.setProcessingMessage("Trạng thái payment không cho phép xác nhận thanh toán");
            log.setProcessedAt(LocalDateTime.now());
            paymentWebhookLogRepository.save(log);
            return new SePayWebhookResult(SePayWebhookResult.Outcome.PAYMENT_STATE_REJECTED);
        }

        boolean latePayment = "EXPIRED".equalsIgnoreCase(paymentStatus)
                || payment.getExpiresAt() == null
                || !payment.getExpiresAt().isAfter(LocalDateTime.now());
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .payment(payment)
                .gateway(gateway)
                .gatewayTransactionId(gatewayTransactionId)
                .bankBrandName(payloadString(payload, "gateway", "bankBrandName", "bankName"))
                .bankAccountNumber(maskAccountNumber(bankAccount))
                .transactionDate(transactionDate)
                .amountIn(amount)
                .amountOut(BigDecimal.ZERO)
                .transactionContent(content)
                .referenceCode(payloadString(payload, "referenceCode"))
                .webhookPayload(encryptAccountNumberFields(payload))
                .build();
        paymentTransactionRepository.save(paymentTransaction);

        ensureCourseRegistration(payment);

        log.setProcessingStatus(latePayment ? "SUCCESS_LATE" : "SUCCESS");
        log.setProcessingMessage(latePayment
                ? "Đã xác nhận thanh toán đến muộn thành công"
                : "Đã xác nhận thanh toán thành công");
        log.setProcessedAt(LocalDateTime.now());
        paymentWebhookLogRepository.save(log);
        return new SePayWebhookResult(latePayment
                ? SePayWebhookResult.Outcome.PROCESSED_LATE
                : SePayWebhookResult.Outcome.PROCESSED);
    }

    private void ensureCourseRegistration(Payment payment) {
        Long userId = payment.getUser().getId();
        Long courseId = payment.getCourse().getId();
        if (!courseRegistrationService.isUserEnrolled(userId, courseId)) {
            courseRegistrationService.registerCourse(userId, CourseRegistrationRequest.builder()
                    .courseId(courseId)
                    .paymentMethod(payment.getPaymentMethod())
                    .note("Đăng ký tự động từ payment " + payment.getOrderCode())
                    .build());
        }
    }

    private Map<String, Object> encryptAccountNumberFields(Map<String, Object> payload) {
        Map<String, Object> storedPayload = new LinkedHashMap<>(payload);
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (ACCOUNT_NUMBER_FIELDS.stream().noneMatch(entry.getKey()::equalsIgnoreCase)) {
                continue;
            }
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String accountNumber && !accountNumber.isBlank()) {
                storedPayload.put(field, accountNumberCryptoService.encrypt(accountNumber));
            } else if (value instanceof Number accountNumber) {
                storedPayload.put(field, accountNumberCryptoService.encrypt(accountNumber.toString()));
            } else if (value != null && !(value instanceof String)) {
                storedPayload.put(field, "[REDACTED_INVALID_ACCOUNT_NUMBER]");
            }
        }
        return storedPayload;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        String normalized = accountNumber.trim();
        return normalized.length() <= 4
                ? "****"
                : "****" + normalized.substring(normalized.length() - 4);
    }

    private PaymentResponseDTO toResponse(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderCode(payment.getOrderCode())
                .courseId(payment.getCourse() != null ? payment.getCourse().getId() : null)
                .courseTitle(payment.getCourse() != null ? payment.getCourse().getTitle() : null)
                .courseThumbnail(payment.getCourse() != null ? payment.getCourse().getThumbnailUrl() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transferContent(payment.getTransferContent())
                .expiresAt(payment.getExpiresAt())
                .qrCodeUrl(buildQrCodeUrl(payment))
                .bankCode(hasValidBankConfiguration() ? bankCode : null)
                .bankAccountNumber(hasValidBankConfiguration() ? bankAccountNumber.trim() : null)
                .bankAccountName(hasValidBankConfiguration() ? bankAccountName.trim() : null)
                .build();
    }

    private String buildQrCodeUrl(Payment payment) {
        if (!hasValidBankConfiguration()) {
            return null;
        }
        String safeAccount = bankAccountNumber.trim();
        String safeAccountName = bankAccountName.trim();
        String safeAmount = payment.getAmount() == null ? "0" : payment.getAmount().toPlainString();
        String info = payment.getTransferContent() == null ? "" : payment.getTransferContent();

        return "https://img.vietqr.io/image/" + bankCode + "-" + safeAccount + "-compact2.png"
                + "?amount=" + safeAmount
                + "&addInfo=" + URLEncoder.encode(info, StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(safeAccountName, StandardCharsets.UTF_8);
    }

    private boolean hasValidBankConfiguration() {
        return bankAccountNumber != null
                && bankAccountNumber.trim().matches("[0-9]{6,20}")
                && bankCode != null
                && bankCode.matches("[A-Za-z0-9_-]{1,32}")
                && bankAccountName != null
                && !bankAccountName.isBlank()
                && expireMinutes > 0
                && sepayWebhookSignatureVerifier.isConfigured();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.replaceAll(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Object payloadValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private LocalDateTime parseTransactionDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException invalidDate) {
                return null;
            }
        }
    }

    private String payloadString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private String resolveGatewayTransactionId(Map<String, Object> payload) {
        Object sePayId = payload.get("id");
        if (sePayId != null) {
            return sePayId.toString().trim();
        }
        return payloadString(payload, "transactionId", "gatewayTransactionId");
    }

    private boolean hasValidSePayId(Map<String, Object> payload) {
        Object rawId = payload.get("id");
        if (rawId != null) {
            try {
                return Long.parseLong(rawId.toString().trim()) > 0;
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        String fallbackId = payloadString(payload, "transactionId", "gatewayTransactionId");
        if (!fallbackId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")) {
            return false;
        }
        try {
            return new BigInteger(fallbackId).signum() > 0;
        } catch (NumberFormatException exception) {
            return true;
        }
    }

}
