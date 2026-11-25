package com.jeps.modules.payment.controller;

import com.jeps.modules.payment.dto.SePayWebhookResult;
import com.jeps.modules.payment.security.SepayWebhookRequestSizeFilter;
import com.jeps.modules.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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

    @Autowired
    private SepayWebhookRequestSizeFilter requestSizeFilter;

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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"success\":true}", true));

        verify(paymentService).processSePayWebhook(anyMap());
    }

    @Test
    void formWebhookVerifiesSignatureAgainstTheOriginalRawBody() throws Exception {
        String rawForm = "id=92704&gateway=Vietcombank&transactionDate=2026-11-15+02%3A00%3A00"
                + "&accountNumber=123456789&content=JLPP-TEST-CONTENT&transferType=in"
                + "&transferAmount=250000&referenceCode=FT24012345678";
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, rawForm))
                        .content(rawForm))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"success\":true}", true));

        verify(paymentService).processSePayWebhook(anyMap());
    }

    @Test
    void acceptsPayloadBelowLimitAndVerifiesTheUnmodifiedRawBody() throws Exception {
        String rawBody = padToRequestLength(
                RAW_BODY, SepayWebhookRequestSizeFilter.MAX_REQUEST_BODY_BYTES - 1);
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        assertEquals(SepayWebhookRequestSizeFilter.MAX_REQUEST_BODY_BYTES - 1,
                rawBody.getBytes(StandardCharsets.UTF_8).length);

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, rawBody))
                        .content(rawBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"success\":true}", true));

        verify(paymentService).processSePayWebhook(anyMap());
    }

    @Test
    void rejectsOversizedPayloadWithPayloadTooLarge() throws Exception {
        String rawBody = padToRequestLength(
                RAW_BODY, SepayWebhookRequestSizeFilter.MAX_REQUEST_BODY_BYTES + 1);
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, rawBody))
                        .content(rawBody))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void requestSizeLimitDoesNotTrustDeclaredContentLength() throws Exception {
        byte[] rawBody = padToRequestLength(
                RAW_BODY, SepayWebhookRequestSizeFilter.MAX_REQUEST_BODY_BYTES + 1)
                .getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/webhooks/sepay");
        servletRequest.setServletPath("/api/webhooks/sepay");
        servletRequest.setContent(rawBody);
        HttpServletRequest requestWithIncorrectLength = new HttpServletRequestWrapper(servletRequest) {
            @Override
            public int getContentLength() {
                return 1;
            }

            @Override
            public long getContentLengthLong() {
                return 1;
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();

        requestSizeFilter.doFilter(requestWithIncorrectLength, response, new MockFilterChain());

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("PAYLOAD_TOO_LARGE"));
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"success\":true}", true));
    }

    @Test
    void reconciliationRequiredTransactionIsAcknowledgedWithExactSuccessResponse() throws Exception {
        when(paymentService.processSePayWebhook(anyMap()))
                .thenReturn(new SePayWebhookResult(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED));
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SePay-Timestamp", timestamp)
                        .header("X-SePay-Signature", sign(timestamp, RAW_BODY))
                        .content(RAW_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"success\":true}", true));
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

    @Test
    void invoiceApiReturnsConflictWhenPaymentIsNotPaid() throws Exception {
        Long learnerId = 987654L;
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("CURRENT_LEARNER_ID", learnerId);
        when(paymentService.getInvoice(learnerId, 123L))
                .thenThrow(new IllegalStateException("Invoice is only available after payment."));

        mockMvc.perform(get("/api/payments/123/invoice")
                        .with(user("payment-test").roles("LEARNER"))
                        .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVOICE_NOT_AVAILABLE"));
    }

    @Test
    void invoicePageRedirectsToPaymentStatusWhenNotPaid() throws Exception {
        Long learnerId = 987654L;
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("CURRENT_LEARNER_ID", learnerId);
        when(paymentService.getInvoice(learnerId, 123L))
                .thenThrow(new IllegalStateException("Invoice is only available after payment."));

        mockMvc.perform(get("/invoice/123")
                        .with(user("payment-test").roles("LEARNER"))
                        .session(session))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/payment/123"))
                .andExpect(flash().attributeExists("paymentNotice"));
    }

    @Test
    void invoicePageReturnsNotFoundForMissingOrUnownedPayment() throws Exception {
        Long learnerId = 987654L;
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("CURRENT_LEARNER_ID", learnerId);
        when(paymentService.getInvoice(learnerId, 123L))
                .thenThrow(new SecurityException("Not found or not owned."));

        mockMvc.perform(get("/invoice/123")
                        .with(user("payment-test").roles("LEARNER"))
                        .session(session))
                .andExpect(status().isNotFound());
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

    private String padToRequestLength(String rawBody, int targetLength) {
        int currentLength = rawBody.getBytes(StandardCharsets.UTF_8).length;
        return rawBody + " ".repeat(targetLength - currentLength);
    }
}
