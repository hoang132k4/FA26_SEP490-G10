package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.dto.RegisterRequest;
import com.jlpp.modules.auth.exception.DuplicateEmailException;
import com.jlpp.modules.auth.exception.DuplicatePhoneException;
import com.jlpp.modules.auth.service.impl.RegisterServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: RegisterService (Screen 1 - Account Registration)")
class RegisterServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterServiceImpl registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterServiceImpl(userRepository, passwordEncoder);
    }

    private RegisterRequest createValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("nguyenvana@example.com");
        request.setPhone("0987654321");
        request.setPassword("Password@123");
        request.setConfirmPassword("Password@123");
        return request;
    }

    @Test
    @DisplayName("Normal: Đăng ký thành công tạo User với mật khẩu được mã hóa và Role LEARNER")
    void register_success() {
        RegisterRequest request = createValidRequest();
        when(userRepository.existsByEmailIgnoreCase("nguyenvana@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0987654321")).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded_hash_123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = registerService.register(request);

        assertNotNull(result);
        assertEquals("Nguyen Van A", result.getFullName());
        assertEquals("nguyenvana@example.com", result.getEmail());
        assertEquals("0987654321", result.getPhone());
        assertEquals("encoded_hash_123", result.getPassword());
        assertEquals(RoleEnum.LEARNER, result.getRole());
        assertEquals("ACTIVE", result.getStatus());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("nguyenvana", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("Abnormal: Request null ném ra IllegalArgumentException")
    void register_nullRequest() {
        assertThrows(IllegalArgumentException.class, () -> registerService.register(null));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Boundary: Họ tên quá ngắn (< 2 ký tự) ném ra IllegalArgumentException")
    void register_shortFullName() {
        RegisterRequest request = createValidRequest();
        request.setFullName("A");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.register(request));
        assertTrue(ex.getMessage().contains("từ 2 đến 100 ký tự"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Boundary: Họ tên chỉ chứa khoảng trắng ném ra IllegalArgumentException")
    void register_blankFullName() {
        RegisterRequest request = createValidRequest();
        request.setFullName("     ");

        assertThrows(IllegalArgumentException.class, () -> registerService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Boundary: Họ tên quá dài (> 100 ký tự) ném ra IllegalArgumentException")
    void register_longFullName() {
        RegisterRequest request = createValidRequest();
        request.setFullName("A".repeat(101));

        assertThrows(IllegalArgumentException.class, () -> registerService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Trùng Email ném ra DuplicateEmailException")
    void register_duplicateEmail() {
        RegisterRequest request = createValidRequest();
        when(userRepository.existsByEmailIgnoreCase("nguyenvana@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> registerService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Trùng Số điện thoại ném ra DuplicatePhoneException")
    void register_duplicatePhone() {
        RegisterRequest request = createValidRequest();
        when(userRepository.existsByEmailIgnoreCase("nguyenvana@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0987654321")).thenReturn(true);

        assertThrows(DuplicatePhoneException.class, () -> registerService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abnormal: Mật khẩu xác nhận không khớp ném ra IllegalArgumentException")
    void register_mismatchedPassword() {
        RegisterRequest request = createValidRequest();
        request.setConfirmPassword("DifferentPassword@123");

        assertThrows(IllegalArgumentException.class, () -> registerService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Normal: Tự động thêm hậu tố số khi username cơ sở đã tồn tại")
    void register_uniqueUsernameGeneration() {
        RegisterRequest request = createValidRequest();
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("nguyenvana")).thenReturn(true);
        when(userRepository.existsByUsername("nguyenvana1")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = registerService.register(request);

        assertNotNull(result);
        assertEquals("nguyenvana1", result.getUsername());
    }
}

