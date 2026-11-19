package com.jlpp.modules.payment.controller;

import com.jlpp.modules.payment.dto.SePayWebhookResult;
import com.jlpp.modules.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "SEPAY_WEBHOOK_SECRET=test-only-controller-secret",
        "BANK_ACCOUNT_NUMBER=123456789"
})
class SePayWebhookSecurityIntegrationTest {

    private static final String TEST_SECRET = "test-only-controller-secret";
    private static final String RAW_BODY = """
            {
              "id": 92704,
              "gateway": "Vietcombank",
              "transactionDate": "2026-11-15 02:00:00",
              "accountNumber": "123456789",
              "content": "JLPP-TEST-CONTENT",
              "transferType": "in",
              "transferAmount": 250000,
              "referenceCode": "FT24012345678"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @BeforeEach
    void returnProcessedByService() {
        when(paymentService.processSePayWebhook(anyMap()))
                .thenReturn(new SePayWebhookResult(SePayWebhookResult.Outcome.PROCESSED));
    }

    @Test
    void anonymousWebhookWithoutCsrfTokenIsAcceptedWithValidSignature() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, RAW_BODY))
                        .content(RAW_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(paymentService).processSePayWebhook(anyMap());
    }

    @Test
    void invalidSignatureIsRejectedBeforeCallingPaymentService() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, "{}"))
                        .content(RAW_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(paymentService);
    }

    @Test
    void missingSignatureIsRejected() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .content(RAW_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void missingTimestampIsRejected() throws Exception {
        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Signature", "sha256=" + "0".repeat(64))
                        .content(RAW_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void duplicateIsAcknowledgedAsSuccessfulWithoutReportingNewProcessing() throws Exception {
        when(paymentService.processSePayWebhook(anyMap()))
                .thenReturn(new SePayWebhookResult(SePayWebhookResult.Outcome.DUPLICATE));
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, RAW_BODY))
                        .content(RAW_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void rejectedTransactionIsNotReportedAsSuccess() throws Exception {
        when(paymentService.processSePayWebhook(anyMap()))
                .thenReturn(new SePayWebhookResult(SePayWebhookResult.Outcome.AMOUNT_MISMATCH));
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, RAW_BODY))
                        .content(RAW_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
    }

    @Test
    void missingPaymentIsReportedAsNotFound() throws Exception {
        when(paymentService.processSePayWebhook(anyMap()))
                .thenReturn(new SePayWebhookResult(SePayWebhookResult.Outcome.PAYMENT_NOT_FOUND));
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, RAW_BODY))
                        .content(RAW_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    void unexpectedServiceFailureReturnsServerErrorWithoutExceptionDetails() throws Exception {
        when(paymentService.processSePayWebhook(anyMap()))
                .thenThrow(new IllegalStateException("private diagnostic detail"));
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, RAW_BODY))
                        .content(RAW_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    private String sign(String timestamp, String rawBody) throws Exception {
        byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
        byte[] body = rawBody.getBytes(StandardCharsets.UTF_8);
        byte[] signedMessage = new byte[prefix.length + body.length];
        System.arraycopy(prefix, 0, signedMessage, 0, prefix.length);
        System.arraycopy(body, 0, signedMessage, prefix.length, body.length);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(signedMessage));
    }
}
