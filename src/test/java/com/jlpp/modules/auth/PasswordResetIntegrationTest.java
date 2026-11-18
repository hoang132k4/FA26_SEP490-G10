package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.service.EmailService;
import com.jlpp.modules.auth.service.PasswordResetService;
import com.jlpp.modules.auth.service.RateLimiter;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Test: Password Reset Full-Context Flow (FN-02)")
public class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private RateLimiter rateLimiter;

    @MockBean
    private EmailService emailService;

    private static final String REGISTERED_EMAIL = "learner_pwd_reset@jeps.edu.vn";
    private static final String INITIAL_PASSWORD = "OldPassword@123";

    @BeforeEach
    void setUp() {
        rateLimiter.clear();
        userRepository.findByEmailIgnoreCase(REGISTERED_EMAIL).ifPresent(userRepository::delete);

        User user = User.builder()
                .email(REGISTERED_EMAIL)
                .username("learner_pwd_reset")
                .password(passwordEncoder.encode(INITIAL_PASSWORD))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .fullName("Learner Password Reset Test")
                .phone("0918888999")
                .build();
        userRepository.save(user);
    }

    private String extractTokenFromResetLink(String resetLink) {
        assertNotNull(resetLink, "Reset link must not be null");
        int tokenIdx = resetLink.indexOf("token=");
        assertTrue(tokenIdx != -1, "Reset link must contain 'token=' parameter");
        return resetLink.substring(tokenIdx + 6);
    }

    @Test
    @DisplayName("IT_PR_01: Request password reset with valid email generates token, persists hash, and dispatches email")
    void testForgotPassword_ValidEmail_GeneratesAndPersistsToken_DispatchesEmail() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .with(csrf())
                        .param("email", REGISTERED_EMAIL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/forgot-password"))
                .andExpect(flash().attributeExists("successMessage"));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendPasswordResetEmail(eq(REGISTERED_EMAIL), linkCaptor.capture());

        String resetLink = linkCaptor.getValue();
        String rawToken = extractTokenFromResetLink(resetLink);
        assertFalse(rawToken.isBlank());

        User user = userRepository.findByEmailIgnoreCase(REGISTERED_EMAIL).orElseThrow();
        assertNotNull(user.getResetTokenHash(), "Token hash must be persisted in H2 database");
        assertNotNull(user.getResetTokenExpiry(), "Token expiry must be set");
        assertTrue(user.getResetTokenExpiry().isAfter(LocalDateTime.now()), "Token expiry must be in the future");
    }

    @Test
    @DisplayName("IT_PR_02: Request password reset for unregistered email returns generic success (Anti-Enumeration)")
    void testForgotPassword_UnregisteredEmail_NeutralResponse_AntiEnumeration() throws Exception {
        String unregisteredEmail = "nonexistent_reset@jeps.edu.vn";

        mockMvc.perform(post("/auth/forgot-password")
                        .with(csrf())
                        .param("email", unregisteredEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/forgot-password"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("IT_PR_03: Access reset-password page with valid token renders form successfully")
    void testResetPasswordPage_ValidToken_RendersForm() throws Exception {
        passwordResetService.processForgotPassword(REGISTERED_EMAIL);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(REGISTERED_EMAIL), linkCaptor.capture());
        String rawToken = extractTokenFromResetLink(linkCaptor.getValue());

        mockMvc.perform(get("/auth/reset-password")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/reset-password"))
                .andExpect(model().attributeExists("resetPasswordRequest"));
    }

    @Test
    @DisplayName("IT_PR_04: Access reset-password page with invalid token redirects to forgot-password with error")
    void testResetPasswordPage_InvalidToken_RedirectsWithError() throws Exception {
        mockMvc.perform(get("/auth/reset-password")
                        .param("token", "completely_invalid_token_99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/forgot-password"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("IT_PR_05: Access reset-password page with expired token redirects to forgot-password with error")
    void testResetPasswordPage_ExpiredToken_RedirectsWithError() throws Exception {
        passwordResetService.processForgotPassword(REGISTERED_EMAIL);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(REGISTERED_EMAIL), linkCaptor.capture());
        String rawToken = extractTokenFromResetLink(linkCaptor.getValue());

        User user = userRepository.findByEmailIgnoreCase(REGISTERED_EMAIL).orElseThrow();
        user.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));
        userRepository.save(user);

        mockMvc.perform(get("/auth/reset-password")
                        .param("token", rawToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/forgot-password"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("IT_PR_06: Reset password with valid credentials updates password hash in database and clears token")
    void testResetPassword_ValidNewPassword_PersistsEncodedPassword_ClearsToken() throws Exception {
        passwordResetService.processForgotPassword(REGISTERED_EMAIL);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(REGISTERED_EMAIL), linkCaptor.capture());
        String rawToken = extractTokenFromResetLink(linkCaptor.getValue());

        String newPassword = "BrandNewPassword@2026";

        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .param("token", rawToken)
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        User updatedUser = userRepository.findByEmailIgnoreCase(REGISTERED_EMAIL).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()), "New password must match BCrypt hash in database");
        assertFalse(passwordEncoder.matches(INITIAL_PASSWORD, updatedUser.getPassword()), "Old password must no longer match");
        assertNull(updatedUser.getResetTokenHash(), "Reset token hash must be cleared upon successful consumption");
        assertNull(updatedUser.getResetTokenExpiry(), "Reset token expiry must be cleared");
    }

    @Test
    @DisplayName("IT_PR_07: User can successfully authenticate with new password immediately after reset")
    void testLoginWithNewPassword_SucceedsAfterReset() throws Exception {
        passwordResetService.processForgotPassword(REGISTERED_EMAIL);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(REGISTERED_EMAIL), linkCaptor.capture());
        String rawToken = extractTokenFromResetLink(linkCaptor.getValue());

        String newPassword = "ResetLoginPass@2026";

        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .param("token", rawToken)
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", REGISTERED_EMAIL)
                        .param("password", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("IT_PR_08: Consumed reset token cannot be reused to reset password a second time")
    void testResetPassword_RejectTokenReuse() throws Exception {
        passwordResetService.processForgotPassword(REGISTERED_EMAIL);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(REGISTERED_EMAIL), linkCaptor.capture());
        String rawToken = extractTokenFromResetLink(linkCaptor.getValue());

        String firstNewPassword = "FirstNewPass@2026";
        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .param("token", rawToken)
                        .param("newPassword", firstNewPassword)
                        .param("confirmPassword", firstNewPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        String secondNewPassword = "SecondNewPass@2026";
        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .param("token", rawToken)
                        .param("newPassword", secondNewPassword)
                        .param("confirmPassword", secondNewPassword))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/reset-password"))
                .andExpect(model().attributeExists("errorMessage"));

        User userAfterSecondAttempt = userRepository.findByEmailIgnoreCase(REGISTERED_EMAIL).orElseThrow();
        assertTrue(passwordEncoder.matches(firstNewPassword, userAfterSecondAttempt.getPassword()),
                "Password must remain the first new password and not be changed by reused token");
    }

    @Test
    @DisplayName("IT_PR_09: Email service outage is handled gracefully without leaking stack traces")
    void testForgotPassword_EmailServiceFailure_HandlesGracefully() throws Exception {
        doThrow(new RuntimeException("Simulated SMTP Connection Timeout"))
                .when(emailService).sendPasswordResetEmail(any(), any());

        mockMvc.perform(post("/auth/forgot-password")
                        .with(csrf())
                        .param("email", REGISTERED_EMAIL))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/forgot-password"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("IT_PR_10: Rate limiter enforces abuse protection on repeated forgot-password requests")
    void testForgotPassword_RateLimitExceeded_AbuseProtection() throws Exception {
        // Exceed rate limit
        for (int i = 0; i < 5; i++) {
            try {
                passwordResetService.processForgotPassword(REGISTERED_EMAIL);
            } catch (Exception ignored) {
            }
        }

        mockMvc.perform(post("/auth/forgot-password")
                        .with(csrf())
                        .param("email", REGISTERED_EMAIL))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/forgot-password"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", containsString("quá nhiều lần")));
    }
}
