package com.jlpp.modules.auth;

import com.jlpp.modules.auth.dto.ChangePasswordRequest;
import com.jlpp.modules.auth.service.impl.ChangePasswordServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ChangePasswordService (Screen 7 - Password Change)")
class ChangePasswordServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ChangePasswordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChangePasswordServiceImpl(userRepository, passwordEncoder);
    }

    private User createActiveUser() {
        User user = User.builder()
                .email("learner@jeps.edu.vn")
                .username("learner")
                .password("old_encoded_hash")
                .status("ACTIVE")
                .build();
        user.setId(100L);
        return user;
    }

    private ChangePasswordRequest createValidRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass@123");
        request.setNewPassword("NewPass@456");
        request.setConfirmPassword("NewPass@456");
        return request;
    }

    @Test
    @DisplayName("Normal: Đổi mật khẩu thành công khi thông tin hợp lệ")
    void changePassword_success() {
        User user = createActiveUser();
        ChangePasswordRequest request = createValidRequest();

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "old_encoded_hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("new_encoded_hash");

        service.changePassword("learner@jeps.edu.vn", request);

        assertEquals("new_encoded_hash", user.getPassword());
        assertNull(user.getResetTokenHash());
        assertNull(user.getResetTokenExpiry());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Abnormal: Identifier null hoặc rỗng ném ra IllegalArgumentException")
    void changePassword_blankIdentifier() {
        ChangePasswordRequest request = createValidRequest();
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("", request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Request null ném ra IllegalArgumentException")
    void changePassword_nullRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("learner@jeps.edu.vn", null));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Không tìm thấy người dùng ném ra IllegalStateException")
    void changePassword_userNotFound() {
        ChangePasswordRequest request = createValidRequest();
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.changePassword("unknown@jeps.edu.vn", request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Tài khoản bị khóa (DISABLED/LOCKED) ném ra IllegalStateException")
    void changePassword_accountLocked() {
        User user = createActiveUser();
        user.setStatus("LOCKED");
        ChangePasswordRequest request = createValidRequest();

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.changePassword("learner@jeps.edu.vn", request));
        assertTrue(ex.getMessage().contains("khóa hoặc vô hiệu hóa"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Mật khẩu hiện tại sai ném ra IllegalArgumentException")
    void changePassword_wrongCurrentPassword() {
        User user = createActiveUser();
        ChangePasswordRequest request = createValidRequest();

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("learner@jeps.edu.vn", request));
        assertTrue(ex.getMessage().contains("không chính xác"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Boundary: Mật khẩu mới quá yếu không thỏa mãn regex ném ra IllegalArgumentException")
    void changePassword_weakNewPassword() {
        User user = createActiveUser();
        ChangePasswordRequest request = createValidRequest();
        request.setNewPassword("weak123");
        request.setConfirmPassword("weak123");

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.changePassword("learner@jeps.edu.vn", request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Mật khẩu xác nhận không khớp ném ra IllegalArgumentException")
    void changePassword_confirmMismatch() {
        User user = createActiveUser();
        ChangePasswordRequest request = createValidRequest();
        request.setConfirmPassword("DifferentPass@789");

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.changePassword("learner@jeps.edu.vn", request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Mật khẩu mới trùng với mật khẩu cũ ném ra IllegalArgumentException")
    void changePassword_sameAsCurrentPassword() {
        User user = createActiveUser();
        ChangePasswordRequest request = createValidRequest();

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "old_encoded_hash")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("learner@jeps.edu.vn", request));
        assertTrue(ex.getMessage().contains("không được trùng"));
        verify(userRepository, never()).save(any());
    }
}
