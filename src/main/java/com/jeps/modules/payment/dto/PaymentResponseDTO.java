package com.jeps.modules.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Long id;
    private String orderCode;
    private Long courseId;
    private Long registrationId;
    private String courseTitle;
    private String courseThumbnail;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String transferContent;
    private LocalDateTime expiresAt;
    private String qrCodeUrl;
    private String bankCode;
    private String bankAccountNumber;
    private String bankAccountName;
}
