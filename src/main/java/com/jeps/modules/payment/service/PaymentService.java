package com.jeps.modules.payment.service;

import com.jeps.modules.payment.dto.InvoiceResponseDTO;
import com.jeps.modules.payment.dto.PaymentRequestDTO;
import com.jeps.modules.payment.dto.PaymentResponseDTO;
import com.jeps.modules.payment.dto.PaymentStatusDTO;
import com.jeps.modules.payment.dto.SePayWebhookResult;

import java.util.Map;

public interface PaymentService {
    PaymentResponseDTO createPayment(Long userId, PaymentRequestDTO request);
    PaymentResponseDTO getPaymentForUser(Long userId, Long paymentId);
    PaymentStatusDTO getPaymentStatus(Long userId, Long paymentId);
    InvoiceResponseDTO getInvoice(Long userId, Long paymentId);
    SePayWebhookResult processSePayWebhook(Map<String, Object> payload);
}
