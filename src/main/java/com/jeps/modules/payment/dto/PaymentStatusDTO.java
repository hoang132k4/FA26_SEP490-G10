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
public class PaymentStatusDTO {
    private Long paymentId;
    private String orderCode;
    private String status;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private String transferContent;
}
