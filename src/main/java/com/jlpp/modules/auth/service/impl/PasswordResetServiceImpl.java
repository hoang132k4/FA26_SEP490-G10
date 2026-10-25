package com.jlpp.modules.auth.service.impl;

import com.jlpp.modules.auth.model.PasswordResetToken;
import com.jlpp.modules.auth.repository.PasswordResetTokenStore;
import com.jlpp.modules.auth.service.EmailService;
import com.jlpp.modules.auth.service.PasswordResetService;
import com.jlpp.modules.auth.service.RateLimiter;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenStore tokenStore;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${app.reset-password.token-validity-minutes:15}")
    private int tokenValidityMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final java.util.Map<String, String> devResetLinkStore = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional
    public void processForgotPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Enforce rate limiting per email (abuse prevention)
        if (!rateLimiter.isAllowed(normalizedEmail)) {
            log.warn("Rate limit exceeded for forgot password request: {}", normalizedEmail);
            throw new IllegalArgumentException("Bạn đã gửi yêu cầu quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        // Account lookup
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            // GB-04: Do not disclose whether email exists
            log.info("Password reset requested for non-existing email: {}", normalizedEmail);
            return;
        }

        User user = userOpt.get();

        // Check account status
        if ("DISABLED".equalsIgnoreCase(user.getStatus()) || "LOCKED".equalsIgnoreCase(user.getStatus())) {
            log.warn("Password reset requested for inactive account ID: {}", user.getId());
            return;
        }

        // Retain active tokens within their 15-minute validity window so any recent email link remains usable

        // Generate cryptographically secure random token (32 bytes = 256 bits)
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Hash token with SHA-256 before storage
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .userId(user.getId())
                .email(normalizedEmail)
                .expiryDate(LocalDateTime.now().plusMinutes(tokenValidityMinutes))
                .used(false)
                .build();

        tokenStore.save(resetToken);

        // Construct reset link
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String resetLink = cleanBaseUrl + "/auth/reset-password?token=" + rawToken;

        devResetLinkStore.put(normalizedEmail, resetLink);

        // Send email
        emailService.sendPasswordResetEmail(normalizedEmail, resetLink);
        log.info("Password reset link generated and dispatched for user ID: {}", user.getId());
    }

    @Override
    public String getLatestGeneratedResetLink(String email) {
        if (email == null) return null;
        return devResetLinkStore.get(email.trim().toLowerCase());
    }

    @Override
    public boolean validateResetToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String tokenHash = hashToken(token.trim());
        Optional<PasswordResetToken> tokenOpt = tokenStore.findByTokenHash(tokenHash);

        return tokenOpt.map(PasswordResetToken::isValid).orElse(false);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ.");
        }

        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 64) {
            throw new IllegalArgumentException("Mật khẩu mới phải có độ dài từ 8 đến 64 ký tự.");
        }

        String tokenHash = hashToken(token.trim());
        PasswordResetToken resetToken = tokenStore.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

        if (!resetToken.isValid()) {
            if (resetToken.isUsed()) {
                throw new IllegalArgumentException("Liên kết đặt lại mật khẩu này đã được sử dụng.");
            }
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu liên kết mới.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tài khoản người dùng."));

        if ("DISABLED".equalsIgnoreCase(user.getStatus()) || "LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Tài khoản này đã bị khóa hoặc vô hiệu hóa.");
        }

        // Hash new password using BCrypt
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        userRepository.save(user);

        // Mark token as consumed immediately
        resetToken.setUsed(true);
        tokenStore.save(resetToken);
        tokenStore.invalidateTokensForEmail(user.getEmail());

        log.info("Password updated successfully for user ID: {}", user.getId());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
