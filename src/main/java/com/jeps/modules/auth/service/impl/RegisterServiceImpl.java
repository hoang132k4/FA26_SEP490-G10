package com.jeps.modules.auth.service.impl;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.auth.dto.RegisterRequest;
import com.jeps.modules.auth.exception.DuplicateEmailException;
import com.jeps.modules.auth.exception.DuplicatePhoneException;
import com.jeps.modules.auth.service.EmailService;
import com.jeps.modules.auth.service.RegisterService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl = "http://localhost:8081";

    @Value("${app.email-verification.token-validity-minutes:15}")
    private int tokenValidityMinutes = 15;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ.");
        }

        // A2: Validate name length after trimming
        if (request.getFullName() == null || request.getFullName().trim().length() < 2 || request.getFullName().trim().length() > 100) {
            throw new IllegalArgumentException("Họ và tên phải từ 2 đến 100 ký tự và không được chỉ chứa khoảng trắng.");
        }

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        // A7: Check duplicate email
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration rejected: Duplicate email '{}'", email);
            throw new DuplicateEmailException("Email này đã được đăng ký tài khoản.");
        }

        // A7: Check duplicate phone
        if (userRepository.existsByPhone(phone)) {
            log.warn("Registration rejected: Duplicate phone number '{}'", phone);
            throw new DuplicatePhoneException("Số điện thoại này đã được sử dụng.");
        }

        // A6: Check password match
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        // Generate unique username according to Rule: Unique Username
        String baseUsername = extractBaseUsername(email);
        String username = generateUniqueUsername(baseUsername);

        // Hash password according to Rule: Password Security (BCrypt)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Generate cryptographically secure email verification token (32 bytes = 256 bits)
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(phone)
                .username(username)
                .password(hashedPassword)
                .role(RoleEnum.LEARNER)
                .status("UNVERIFIED")
                .emailVerificationTokenHash(tokenHash)
                .emailVerificationTokenExpiry(LocalDateTime.now().plusMinutes(tokenValidityMinutes))
                .build();

        log.info("Creating new unverified Learner account with username '{}' and email '{}'", username, email);
        User savedUser = userRepository.save(user);

        String cleanBaseUrl = (baseUrl != null && baseUrl.endsWith("/"))
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : (baseUrl != null ? baseUrl : "http://localhost:8081");
        String verificationLink = cleanBaseUrl + "/auth/verify-email?token=" + rawToken;
        emailService.sendRegistrationVerificationEmail(email, savedUser.getFullName(), verificationLink);
        log.info("Registration verification email dispatched for user ID: {} to email: {}", savedUser.getId(), email);

        return savedUser;
    }

    @Override
    @Transactional
    public User verifyRegistrationEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Liên kết xác nhận tài khoản không hợp lệ.");
        }

        String tokenHash = hashToken(token.trim());
        User user = userRepository.findByEmailVerificationTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết xác nhận tài khoản không hợp lệ hoặc đã được sử dụng."));

        if (user.getEmailVerificationTokenExpiry() == null
                || user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Liên kết xác nhận tài khoản đã hết hạn. Vui lòng đăng ký lại hoặc liên hệ hỗ trợ.");
        }

        user.setStatus("ACTIVE");
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiry(null);
        User activatedUser = userRepository.save(user);
        log.info("Account email verified and activated for user ID: {}, email: {}", activatedUser.getId(), activatedUser.getEmail());
        return activatedUser;
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

    private String extractBaseUsername(String email) {
        int atIndex = email.indexOf('@');
        String prefix = atIndex > 0 ? email.substring(0, atIndex) : email;
        // Keep alphanumeric and underscores, lowercase
        String sanitized = prefix.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        if (sanitized.isBlank()) {
            sanitized = "learner";
        }
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40);
        }
        return sanitized;
    }

    private String generateUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = baseUsername + counter;
            counter++;
        }
        return candidate;
    }
}
