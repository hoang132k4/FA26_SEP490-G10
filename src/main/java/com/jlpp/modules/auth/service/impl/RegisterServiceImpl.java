package com.jlpp.modules.auth.service.impl;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.dto.RegisterRequest;
import com.jlpp.modules.auth.exception.DuplicateEmailException;
import com.jlpp.modules.auth.exception.DuplicatePhoneException;
import com.jlpp.modules.auth.service.RegisterService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(phone)
                .username(username)
                .password(hashedPassword)
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build();

        log.info("Creating new Learner account with username '{}' and email '{}'", username, email);
        return userRepository.save(user);
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
