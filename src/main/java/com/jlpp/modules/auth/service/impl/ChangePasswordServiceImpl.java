package com.jlpp.modules.auth.service.impl;

import com.jlpp.modules.auth.dto.ChangePasswordRequest;
import com.jlpp.modules.auth.service.ChangePasswordService;
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
public class ChangePasswordServiceImpl implements ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(String identifier, ChangePasswordRequest request) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Không xác định được danh tính người dùng.");
        }

        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu đổi mật khẩu không hợp lệ.");
        }

        String cleanIdentifier = identifier.trim();
        User user = userRepository.findByEmailIgnoreCase(cleanIdentifier)
                .or(() -> userRepository.findByUsername(cleanIdentifier))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tài khoản người dùng."));

        if ("DISABLED".equalsIgnoreCase(user.getStatus()) || "LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }

        // 1. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        // 2. Verify new password is not empty and meets length requirements (GB-03)
        String newPassword = request.getNewPassword();
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s]).{8,64}$")) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8-64 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        // 3. Verify confirmation matches new password
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không trùng khớp với mật khẩu mới.");
        }

        // 4. Verify new password differs from current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        // 5. Hash and update password
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);

        // 6. Invalidate any active reset tokens for this user
        user.setResetTokenHash(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", user.getId());
    }
}
