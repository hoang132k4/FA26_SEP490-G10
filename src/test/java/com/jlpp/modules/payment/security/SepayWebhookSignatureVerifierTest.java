package com.jlpp.modules.payment.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SepayWebhookSignatureVerifierTest {

    private static final String TEST_SECRET = "test-only-sepay-secret";

    @Test
    void acceptsValidSignatureForExactRawBody() throws Exception {
        SepayWebhookSignatureVerifier verifier = new SepayWebhookSignatureVerifier(TEST_SECRET);
        byte[] rawBody = " { \"id\": 92704 }\n".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        assertEquals(SepayWebhookSignatureVerifier.VerificationResult.VALID,
                verifier.verify(rawBody, timestamp, sign(TEST_SECRET, timestamp, rawBody)));
    }

    @Test
    void rejectsIncorrectSignature() throws Exception {
        SepayWebhookSignatureVerifier verifier = new SepayWebhookSignatureVerifier(TEST_SECRET);
        byte[] rawBody = "{\"id\":92704}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String incorrectSignature = sign(TEST_SECRET, timestamp, "{}".getBytes(StandardCharsets.UTF_8));

        assertEquals(SepayWebhookSignatureVerifier.VerificationResult.INVALID,
                verifier.verify(rawBody, timestamp, incorrectSignature));
    }

    @Test
    void rejectsMissingSignature() {
        SepayWebhookSignatureVerifier verifier = new SepayWebhookSignatureVerifier(TEST_SECRET);
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        assertEquals(SepayWebhookSignatureVerifier.VerificationResult.INVALID,
                verifier.verify("{}".getBytes(StandardCharsets.UTF_8), timestamp, null));
    }

    @Test
    void rejectsMissingTimestamp() {
        SepayWebhookSignatureVerifier verifier = new SepayWebhookSignatureVerifier(TEST_SECRET);

        assertEquals(SepayWebhookSignatureVerifier.VerificationResult.INVALID,
                verifier.verify("{}".getBytes(StandardCharsets.UTF_8), null, "sha256=" + "0".repeat(64)));
    }

    @Test
    void rejectsTimestampOutsideFiveMinuteWindow() throws Exception {
        SepayWebhookSignatureVerifier verifier = new SepayWebhookSignatureVerifier(TEST_SECRET);
        byte[] rawBody = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(Instant.now().minusSeconds(301).getEpochSecond());

        assertEquals(SepayWebhookSignatureVerifier.VerificationResult.INVALID,
                verifier.verify(rawBody, timestamp, sign(TEST_SECRET, timestamp, rawBody)));
    }

    @Test
    void rejectsRequestWhenSecretIsNotConfigured() {
        SepayWebhookSignatureVerifier verifier = new SepayWebhookSignatureVerifier(" ");

        assertEquals(SepayWebhookSignatureVerifier.VerificationResult.SECRET_NOT_CONFIGURED,
                verifier.verify("{}".getBytes(StandardCharsets.UTF_8), "1", "sha256=" + "0".repeat(64)));
    }

    private String sign(String secret, String timestamp, byte[] rawBody) throws Exception {
        byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
        byte[] signedMessage = new byte[prefix.length + rawBody.length];
        System.arraycopy(prefix, 0, signedMessage, 0, prefix.length);
        System.arraycopy(rawBody, 0, signedMessage, prefix.length, rawBody.length);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(signedMessage));
    }
}
