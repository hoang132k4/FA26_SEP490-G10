package com.jlpp.modules.payment.service;

import com.jlpp.modules.payment.dto.InvoiceResponseDTO;
import com.jlpp.modules.payment.dto.PaymentRequestDTO;
import com.jlpp.modules.payment.dto.PaymentResponseDTO;
import com.jlpp.modules.payment.dto.PaymentStatusDTO;
import com.jlpp.modules.payment.dto.SePayWebhookResult;

import java.util.Map;

public interface PaymentService {
    PaymentResponseDTO createPayment(Long userId, PaymentRequestDTO request);
    PaymentResponseDTO getPaymentForUser(Long userId, Long paymentId);
    PaymentStatusDTO getPaymentStatus(Long userId, Long paymentId);
    InvoiceResponseDTO getInvoice(Long userId, Long paymentId);
    SePayWebhookResult processSePayWebhook(Map<String, Object> payload);
}
