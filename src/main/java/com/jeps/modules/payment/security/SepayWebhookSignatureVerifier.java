package com.jeps.modules.payment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;

@Component
public class SepayWebhookSignatureVerifier {

    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300;
    private static final String SIGNATURE_PREFIX = "sha256=";
    private final String secret;

    public SepayWebhookSignatureVerifier(@Value("${SEPAY_WEBHOOK_SECRET:}") String secret) {
        this.secret = secret;
    }

    public boolean isConfigured() {
        return secret != null && !secret.isBlank();
    }

    public VerificationResult verify(byte[] rawBody, String timestampHeader, String signatureHeader) {
        if (!isConfigured()) {
            return VerificationResult.SECRET_NOT_CONFIGURED;
        }
        if (rawBody == null || timestampHeader == null || signatureHeader == null
                || !timestampHeader.matches("[0-9]{1,19}")
                || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return VerificationResult.INVALID;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException exception) {
            return VerificationResult.INVALID;
        }

        long now = Instant.now().getEpochSecond();
        if (timestamp < now - MAX_TIMESTAMP_AGE_SECONDS
                || timestamp > now + MAX_TIMESTAMP_AGE_SECONDS) {
            return VerificationResult.INVALID;
        }

        String signatureHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
        if (!signatureHex.matches("(?i)[0-9a-f]{64}")) {
            return VerificationResult.INVALID;
        }

        byte[] prefix = (timestampHeader + ".").getBytes(StandardCharsets.UTF_8);
        byte[] signedMessage = Arrays.copyOf(prefix, prefix.length + rawBody.length);
        System.arraycopy(rawBody, 0, signedMessage, prefix.length, rawBody.length);

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(signedMessage);
            byte[] provided = HexFormat.of().parseHex(signatureHex);
            return MessageDigest.isEqual(expected, provided)
                    ? VerificationResult.VALID
                    : VerificationResult.INVALID;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable.", exception);
        }
    }

    public enum VerificationResult {
        VALID,
        INVALID,
        SECRET_NOT_CONFIGURED
    }
}
