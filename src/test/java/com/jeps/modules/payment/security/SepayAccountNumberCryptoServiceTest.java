package com.jeps.modules.payment.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SepayAccountNumberCryptoServiceTest {

    private static final String ACCOUNT_NUMBER = "123456789";
    private static final String ROOT_SECRET = "test-only-sepay-root-secret";

    @Test
    void encryptsAndDecryptsWithVersionedAuthenticatedCiphertext() {
        SepayAccountNumberCryptoService cryptoService = cryptoService(ROOT_SECRET, "k1", "", "");

        String encrypted = cryptoService.encrypt(ACCOUNT_NUMBER);

        assertTrue(encrypted.startsWith("v1.k1."));
        assertFalse(encrypted.contains(ACCOUNT_NUMBER));
        assertEquals(ACCOUNT_NUMBER, cryptoService.decrypt(encrypted));
    }

    @Test
    void usesANewNonceForEachEncryption() {
        SepayAccountNumberCryptoService cryptoService = cryptoService(ROOT_SECRET, "k1", "", "");

        String first = cryptoService.encrypt(ACCOUNT_NUMBER);
        String second = cryptoService.encrypt(ACCOUNT_NUMBER);

        assertNotEquals(first.split("\\.")[2], second.split("\\.")[2]);
        assertNotEquals(first, second);
    }

    @Test
    void rejectsWrongKeyAndModifiedCiphertext() {
        SepayAccountNumberCryptoService cryptoService = cryptoService(ROOT_SECRET, "k1", "", "");
        String encrypted = cryptoService.encrypt(ACCOUNT_NUMBER);

        assertThrows(IllegalArgumentException.class,
                () -> cryptoService("different-test-secret", "k1", "", "").decrypt(encrypted));

        String[] parts = encrypted.split("\\.");
        parts[3] = (parts[3].charAt(0) == 'A' ? "B" : "A") + parts[3].substring(1);
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.decrypt(String.join(".", parts)));
    }

    @Test
    void rejectsUnknownFormatVersion() {
        SepayAccountNumberCryptoService cryptoService = cryptoService(ROOT_SECRET, "k1", "", "");
        String encrypted = cryptoService.encrypt(ACCOUNT_NUMBER);

        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.decrypt(encrypted.replaceFirst("^v1\\.", "v2.")));
    }

    @Test
    void retainsDecryptionForConfiguredPreviousKeyDuringRotation() {
        SepayAccountNumberCryptoService oldCrypto = cryptoService("old-test-secret", "k1", "", "");
        String encryptedWithOldKey = oldCrypto.encrypt(ACCOUNT_NUMBER);
        SepayAccountNumberCryptoService rotatedCrypto = cryptoService(
                "new-test-secret", "k2", "old-test-secret", "k1");

        assertEquals(ACCOUNT_NUMBER, rotatedCrypto.decrypt(encryptedWithOldKey));
        assertTrue(rotatedCrypto.encrypt(ACCOUNT_NUMBER).startsWith("v1.k2."));
    }

    @Test
    void rejectsPreviousCiphertextWhenPreviousKeyIsNotConfigured() {
        String encryptedWithOldKey = cryptoService("old-test-secret", "k1", "", "").encrypt(ACCOUNT_NUMBER);
        SepayAccountNumberCryptoService rotatedCrypto = cryptoService("new-test-secret", "k2", "", "");

        assertThrows(IllegalStateException.class, () -> rotatedCrypto.decrypt(encryptedWithOldKey));
    }

    @Test
    void rejectsPreviousCiphertextWhenConfiguredKeyIdDoesNotMatch() {
        String encryptedWithOldKey = cryptoService("old-test-secret", "k1", "", "").encrypt(ACCOUNT_NUMBER);
        SepayAccountNumberCryptoService rotatedCrypto = cryptoService(
                "new-test-secret", "k2", "old-test-secret", "k3");

        assertThrows(IllegalStateException.class, () -> rotatedCrypto.decrypt(encryptedWithOldKey));
    }

    private SepayAccountNumberCryptoService cryptoService(
            String activeSecret,
            String activeKeyId,
            String previousSecret,
            String previousKeyId) {
        return new SepayAccountNumberCryptoService(
                activeSecret, activeKeyId, previousSecret, previousKeyId);
    }
}
