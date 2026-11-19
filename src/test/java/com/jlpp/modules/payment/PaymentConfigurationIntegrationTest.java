package com.jlpp.modules.payment;

import com.jlpp.modules.payment.dto.PaymentRequestDTO;
import com.jlpp.modules.payment.exception.PaymentConfigurationException;
import com.jlpp.modules.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "BANK_ACCOUNT_NUMBER=",
        "SEPAY_WEBHOOK_SECRET=",
        "payment.expiry-sweep-interval-ms=3600000"
})
class PaymentConfigurationIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    void refusesToCreatePaymentWhenRequiredBankAndWebhookSettingsAreMissing() {
        PaymentConfigurationException exception = assertThrows(PaymentConfigurationException.class,
                () -> paymentService.createPayment(1L,
                        PaymentRequestDTO.builder().courseId(1L).paymentMethod("SEPAY").build()));

        assertTrue(exception.getMessage().contains("BANK_ACCOUNT_NUMBER"));
        assertTrue(exception.getMessage().contains("SEPAY_WEBHOOK_SECRET"));
        assertTrue(!exception.getMessage().contains("="));
    }
}
