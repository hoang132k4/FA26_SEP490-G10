package com.jlpp.modules.payment.dto;

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
public class InvoiceResponseDTO {
    private Long paymentId;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private Long courseId;
    private String courseTitle;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String bankName;
    private String transferContent;
    private LocalDateTime paidAt;
    private String status;
    private String transactionId;
}
