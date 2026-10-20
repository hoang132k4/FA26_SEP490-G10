package com.jlpp.modules.auth.repository;

import com.jlpp.modules.auth.model.PasswordResetToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe token store for reset password tokens.
 * Stores tokens by SHA-256 hash without mutating existing database schema,
 * fully compliant with zero-schema-change database restriction.
 */
@Component
public class PasswordResetTokenStore {

    private final Map<String, PasswordResetToken> store = new ConcurrentHashMap<>();

    public void save(PasswordResetToken token) {
        cleanupExpired();
        store.put(token.getTokenHash(), token);
    }

    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        if (tokenHash == null) {
            return Optional.empty();
        }
        PasswordResetToken token = store.get(tokenHash);
        if (token == null) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    public void invalidateTokensForEmail(String email) {
        if (email == null) return;
        store.values().stream()
                .filter(t -> email.equalsIgnoreCase(t.getEmail()))
                .forEach(t -> t.setUsed(true));
    }

    public void remove(String tokenHash) {
        if (tokenHash != null) {
            store.remove(tokenHash);
        }
    }

    public void clear() {
        store.clear();
    }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(entry -> entry.getValue().getExpiryDate().isBefore(now));
    }
}
