package com.jeps.modules.payment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SepayAccountNumberCryptoService {

    private static final String FORMAT_VERSION = "v1";
    private static final String HKDF_INFO_PREFIX = "JLPP/SePay/payment-receiver-account-encryption/v1/";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String activeSecret;
    private final String activeKeyId;
    private final String previousSecret;
    private final String previousKeyId;

    public SepayAccountNumberCryptoService(
            @Value("${SEPAY_WEBHOOK_SECRET:}") String activeSecret,
            @Value("${SEPAY_WEBHOOK_ENCRYPTION_KEY_ID:k1}") String activeKeyId,
            @Value("${SEPAY_WEBHOOK_PREVIOUS_SECRET:}") String previousSecret,
            @Value("${SEPAY_WEBHOOK_PREVIOUS_KEY_ID:}") String previousKeyId) {
        this.activeSecret = activeSecret;
        this.activeKeyId = activeKeyId;
        this.previousSecret = previousSecret;
        this.previousKeyId = previousKeyId;

        if (!isValidKeyId(activeKeyId)) {
            throw new IllegalStateException("The active account encryption key ID is invalid.");
        }
        boolean hasPreviousSecret = previousSecret != null && !previousSecret.isBlank();
        boolean hasPreviousKeyId = previousKeyId != null && !previousKeyId.isBlank();
        if (hasPreviousSecret != hasPreviousKeyId
                || (hasPreviousKeyId && (!isValidKeyId(previousKeyId) || activeKeyId.equals(previousKeyId)))) {
            throw new IllegalStateException("Previous account encryption key configuration is invalid.");
        }
    }

    public String encrypt(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required for encryption.");
        }
        requireSecret(activeSecret);

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(nonce);
        byte[] key = null;
        try {
            key = deriveKey(activeSecret, activeKeyId);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(associatedData(activeKeyId));
            byte[] ciphertext = cipher.doFinal(accountNumber.getBytes(StandardCharsets.UTF_8));

            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return FORMAT_VERSION + "." + activeKeyId + "."
                    + encoder.encodeToString(nonce) + "."
                    + encoder.encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt account number.", exception);
        } finally {
            clear(key);
        }
    }

    public String decrypt(String encryptedAccountNumber) {
        String[] parts = encryptedAccountNumber == null
                ? new String[0]
                : encryptedAccountNumber.split("\\.", -1);
        if (parts.length != 4 || !FORMAT_VERSION.equals(parts[0]) || !isValidKeyId(parts[1])) {
            throw new IllegalArgumentException("Encrypted account number format is invalid.");
        }

        String rootSecret = secretForKeyId(parts[1]);
        byte[] nonce;
        byte[] ciphertext;
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            nonce = decoder.decode(parts[2]);
            ciphertext = decoder.decode(parts[3]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Encrypted account number format is invalid.");
        }
        if (nonce.length != NONCE_LENGTH_BYTES || ciphertext.length <= AUTH_TAG_LENGTH_BITS / 8) {
            throw new IllegalArgumentException("Encrypted account number format is invalid.");
        }

        byte[] key = null;
        byte[] plaintext = null;
        try {
            key = deriveKey(rootSecret, parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(associatedData(parts[1]));
            plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (AEADBadTagException exception) {
            throw new IllegalArgumentException("Encrypted account number could not be authenticated.");
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Encrypted account number could not be decrypted.");
        } finally {
            clear(key);
            clear(plaintext);
        }
    }

    private String secretForKeyId(String keyId) {
        if (activeKeyId.equals(keyId) && activeSecret != null && !activeSecret.isBlank()) {
            return activeSecret;
        }
        if (previousKeyId != null && previousKeyId.equals(keyId)
                && previousSecret != null && !previousSecret.isBlank()) {
            return previousSecret;
        }
        throw new IllegalStateException("Encryption key version is unavailable for account data.");
    }

    private static byte[] deriveKey(String secret, String keyId) throws GeneralSecurityException {
        byte[] inputKeyMaterial = secret.getBytes(StandardCharsets.UTF_8);
        byte[] salt = new byte[32];
        byte[] pseudorandomKey = hmacSha256(salt, inputKeyMaterial);
        byte[] info = (HKDF_INFO_PREFIX + keyId).getBytes(StandardCharsets.UTF_8);
        byte[] expandInput = Arrays.copyOf(info, info.length + 1);
        expandInput[expandInput.length - 1] = 1;
        try {
            return hmacSha256(pseudorandomKey, expandInput);
        } finally {
            clear(inputKeyMaterial);
            clear(salt);
            clear(pseudorandomKey);
            clear(expandInput);
        }
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] associatedData(String keyId) {
        return (FORMAT_VERSION + ":JLPP/SePay/payment-receiver-account:" + keyId)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isValidKeyId(String keyId) {
        return keyId != null && keyId.matches("[A-Za-z0-9_-]{1,32}");
    }

    private static void requireSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Account encryption is not configured.");
        }
    }

    private static void clear(byte[] value) {
        if (value != null) {
            Arrays.fill(value, (byte) 0);
        }
    }
}
