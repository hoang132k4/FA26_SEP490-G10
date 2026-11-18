package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.auth.service.impl.CustomUserDetailsService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: CustomUserDetailsService (Screen 3 - Login Authentication)")
class CustomUserDetailsServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    private User createSampleUser(RoleEnum role, String status) {
        User user = User.builder()
                .email("user@jeps.edu.vn")
                .username("jepsuser")
                .fullName("JEPS User")
                .password("encoded_pass_123")
                .role(role)
                .status(status)
                .build();
        user.setId(50L);
        return user;
    }

    @Test
    @DisplayName("Normal: Tải thông tin người dùng thành công bằng Email")
    void loadUserByUsername_byEmail_success() {
        User user = createSampleUser(RoleEnum.LEARNER, "ACTIVE");
        when(userRepository.findByEmailIgnoreCase("user@jeps.edu.vn")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("user@jeps.edu.vn");

        assertNotNull(userDetails);
        assertEquals("user@jeps.edu.vn", userDetails.getUsername());
        assertEquals("encoded_pass_123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LEARNER")));
        verify(userRepository, times(1)).findByEmailIgnoreCase("user@jeps.edu.vn");
    }

    @Test
    @DisplayName("Normal: Tải thông tin người dùng thành công bằng Username khi không khớp Email")
    void loadUserByUsername_byUsername_success() {
        User user = createSampleUser(RoleEnum.MANAGER, "ACTIVE");
        when(userRepository.findByEmailIgnoreCase("jepsuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("jepsuser")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("jepsuser");

        assertNotNull(userDetails);
        assertEquals("user@jeps.edu.vn", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    @DisplayName("Abnormal: Identifier là null ném ra UsernameNotFoundException")
    void loadUserByUsername_nullIdentifier_throwsException() {
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername(null));
        assertEquals("Invalid email or password.", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Abnormal: Identifier rỗng hoặc chỉ có khoảng trắng ném ra UsernameNotFoundException")
    void loadUserByUsername_blankIdentifier_throwsException() {
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("   "));
        assertEquals("Invalid email or password.", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Abnormal: Không tìm thấy người dùng theo cả email và username ném ra UsernameNotFoundException")
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByEmailIgnoreCase("unknown@jeps.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown@jeps.edu.vn")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@jeps.edu.vn"));
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    @DisplayName("Boundary: Tài khoản ở trạng thái DISABLED -> isEnabled() trả về false")
    void loadUserByUsername_disabledAccount_notEnabled() {
        User user = createSampleUser(RoleEnum.LEARNER, "DISABLED");
        when(userRepository.findByEmailIgnoreCase("disabled@jeps.edu.vn")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("disabled@jeps.edu.vn");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
    }

    @Test
    @DisplayName("Boundary: Tài khoản ở trạng thái LOCKED -> isAccountNonLocked() trả về false và isEnabled() trả về false")
    void loadUserByUsername_lockedAccount_lockedAndNotEnabled() {
        User user = createSampleUser(RoleEnum.LEARNER, "LOCKED");
        when(userRepository.findByEmailIgnoreCase("locked@jeps.edu.vn")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("locked@jeps.edu.vn");

        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked());
        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("Boundary: Quyền hạn ADMIN được ánh xạ đúng authority ROLE_ADMIN")
    void loadUserByUsername_adminRole_mappedCorrectly() {
        User user = createSampleUser(RoleEnum.ADMIN, "ACTIVE");
        when(userRepository.findByEmailIgnoreCase("admin@jeps.edu.vn")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("admin@jeps.edu.vn");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
