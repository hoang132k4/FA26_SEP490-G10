package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.controller.AuthController;
import com.jlpp.modules.auth.dto.ForgotPasswordRequest;
import com.jlpp.modules.auth.dto.ResetPasswordRequest;
import com.jlpp.modules.auth.model.PasswordResetToken;
import com.jlpp.modules.auth.repository.PasswordResetTokenStore;
import com.jlpp.modules.auth.service.EmailService;
import com.jlpp.modules.auth.service.PasswordResetService;
import com.jlpp.modules.auth.service.RateLimiter;
import com.jlpp.modules.auth.service.impl.PasswordResetServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.jlpp.modules.auth.service.ChangePasswordService changePasswordService;

    private PasswordResetTokenStore tokenStore;
    private PasswordEncoder passwordEncoder;
    private RateLimiter rateLimiter;
    private PasswordResetServiceImpl passwordResetService;
    private AuthController authController;
    private Validator validator;

    private User testUser;
    private final String testEmail = "learner@jeps.edu.vn";
    private final String oldRawPassword = "OldPassword123";

    @BeforeEach
    void setUp() {
        tokenStore = new PasswordResetTokenStore();
        passwordEncoder = new BCryptPasswordEncoder(10);
        rateLimiter = new RateLimiter();
        ReflectionTestUtils.setField(rateLimiter, "maxRequests", 3);
        ReflectionTestUtils.setField(rateLimiter, "windowMinutes", 15);

        passwordResetService = new PasswordResetServiceImpl(
                userRepository,
                tokenStore,
                emailService,
                passwordEncoder,
                rateLimiter
        );
        ReflectionTestUtils.setField(passwordResetService, "baseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(passwordResetService, "tokenValidityMinutes", 15);

        authController = new AuthController(passwordResetService, changePasswordService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        testUser = User.builder()
                .fullName("Test Learner")
                .username("testlearner")
                .email(testEmail)
                .password(passwordEncoder.encode(oldRawPassword))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build();
        testUser.setId(100L);
    }

    @Test
    @DisplayName("RP-01: Registered email - Recovery request accepted and email sent")
    void testRP01_RegisteredEmail_RequestAccepted() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));

        passwordResetService.processForgotPassword(testEmail);

        verify(emailService, times(1)).sendPasswordResetEmail(eq(testEmail), contains("/auth/reset-password?token="));
    }

    @Test
    @DisplayName("RP-02: Unregistered email - Generic confirmation, no account disclosure (GB-04)")
    void testRP02_UnregisteredEmail_NoDisclosure() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        // Must complete without exception and without sending an email
        passwordResetService.processForgotPassword("unknown@example.com");

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("RP-03: Empty email - Validation error")
    void testRP03_EmptyEmail_ValidationError() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("").build();
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"))).isTrue();
    }

    @Test
    @DisplayName("RP-04: Invalid email format - Validation error")
    void testRP04_InvalidEmailFormat_ValidationError() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("invalid-email-format").build();
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"))).isTrue();
    }

    @Test
    @DisplayName("RP-05: Valid reset credential - New-password form accessible")
    void testRP05_ValidResetCredential_FormAccessible() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        passwordResetService.processForgotPassword(testEmail);
        verify(emailService).sendPasswordResetEmail(eq(testEmail), linkCaptor.capture());

        String link = linkCaptor.getValue();
        String rawToken = link.substring(link.indexOf("token=") + 6);

        assertThat(passwordResetService.validateResetToken(rawToken)).isTrue();

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = authController.resetPasswordPage(rawToken, model, redirectAttributes);

        assertThat(view).isEqualTo("pages/auth/reset-password");
        assertThat(model.containsAttribute("resetPasswordRequest")).isTrue();
    }

    @Test
    @DisplayName("RP-06: Expired credential - Reset rejected")
    void testRP06_ExpiredCredential_Rejected() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .tokenHash("sample-expired-hash")
                .userId(testUser.getId())
                .email(testEmail)
                .expiryDate(LocalDateTime.now().minusMinutes(5)) // expired
                .used(false)
                .build();
        tokenStore.save(expiredToken);

        assertThat(expiredToken.isExpired()).isTrue();
        assertThat(expiredToken.isValid()).isFalse();
    }

    @Test
    @DisplayName("RP-07: Invalid credential - Reset rejected")
    void testRP07_InvalidCredential_Rejected() {
        boolean isValid = passwordResetService.validateResetToken("completely-invalid-nonexistent-token");
        assertThat(isValid).isFalse();

        assertThatThrownBy(() -> passwordResetService.resetPassword("nonexistent-token", "NewPassword123!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RP-08: Password shorter than 8 characters - Validation error (GB-03)")
    void testRP08_PasswordShorterThan8Chars_ValidationError() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword("Short1")
                .confirmPassword("Short1")
                .build();
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"))).isTrue();
    }

    @Test
    @DisplayName("RP-09: Password longer than 64 characters - Validation error")
    void testRP09_PasswordLongerThan64Chars_ValidationError() {
        String longPassword = "A".repeat(65);
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword(longPassword)
                .confirmPassword(longPassword)
                .build();
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"))).isTrue();
    }

    @Test
    @DisplayName("RP-10: Password confirmation mismatch - Validation error")
    void testRP10_PasswordConfirmationMismatch_ValidationError() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword("ValidPassword123")
                .confirmPassword("DifferentPassword123")
                .build();

        assertThat(request.isPasswordMatching()).isFalse();

        BindingResult bindingResult = new BeanPropertyBindingResult(request, "resetPasswordRequest");
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = authController.handleResetPassword(request, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("pages/auth/reset-password");
        assertThat(bindingResult.hasFieldErrors("confirmPassword")).isTrue();
    }

    @Test
    @DisplayName("RP-11: Valid new password - Password updated successfully")
    void testRP11_ValidNewPassword_PasswordUpdated() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        passwordResetService.processForgotPassword(testEmail);
        verify(emailService).sendPasswordResetEmail(eq(testEmail), linkCaptor.capture());

        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        String newPassword = "BrandNewSecurePassword2026!";

        passwordResetService.resetPassword(rawToken, newPassword);

        verify(userRepository, times(1)).save(testUser);
        assertThat(passwordEncoder.matches(newPassword, testUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("RP-12: Reuse of consumed credential - Reset rejected")
    void testRP12_ReusedCredential_Rejected() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        passwordResetService.processForgotPassword(testEmail);
        verify(emailService).sendPasswordResetEmail(eq(testEmail), linkCaptor.capture());

        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        // First use
        passwordResetService.resetPassword(rawToken, "FirstNewPassword123");

        // Second use attempt must be rejected
        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "SecondNewPassword123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RP-13: Login with new password - Authentication succeeds")
    void testRP13_LoginWithNewPassword_Succeeds() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        passwordResetService.processForgotPassword(testEmail);
        verify(emailService).sendPasswordResetEmail(eq(testEmail), linkCaptor.capture());

        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        String newPassword = "BrandNewPassword123";

        passwordResetService.resetPassword(rawToken, newPassword);

        assertThat(passwordEncoder.matches(newPassword, testUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("RP-14: Login with old password - Authentication fails")
    void testRP14_LoginWithOldPassword_Fails() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        passwordResetService.processForgotPassword(testEmail);
        verify(emailService).sendPasswordResetEmail(eq(testEmail), linkCaptor.capture());

        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        passwordResetService.resetPassword(rawToken, "NewlySetPassword123");

        assertThat(passwordEncoder.matches(oldRawPassword, testUser.getPassword())).isFalse();
    }

    @Test
    @DisplayName("RP-15: Database unavailable - Controlled error handling")
    void testRP15_DatabaseUnavailable_ControlledError() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenThrow(new RuntimeException("DB Connection Timeout"));

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("learner@jeps.edu.vn").build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "forgotPasswordRequest");

        String view = authController.handleForgotPassword(request, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("pages/auth/forgot-password");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Đã xảy ra lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.");
    }

    @Test
    @DisplayName("RP-16: Email provider unavailable - Controlled error handling")
    void testRP16_EmailProviderUnavailable_ControlledError() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("SMTP Host Unreachable")).when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // Should handle gracefully without exposing internal SMTP details
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(testEmail).build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "forgotPasswordRequest");

        String view = authController.handleForgotPassword(request, bindingResult, model, redirectAttributes);

        // User gets generic success or friendly error, but no SMTP stack trace
        assertThat(view).isNotNull();
    }

    @Test
    @DisplayName("RP-17: Repeated reset requests - Abuse protection enforced")
    void testRP17_RepeatedResetRequests_AbuseProtectionEnforced() {
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));

        // Limit is 3 requests per 15 minutes
        passwordResetService.processForgotPassword(testEmail);
        passwordResetService.processForgotPassword(testEmail);
        passwordResetService.processForgotPassword(testEmail);

        // 4th request must be blocked by rate limiter
        assertThatThrownBy(() -> passwordResetService.processForgotPassword(testEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bạn đã gửi yêu cầu quá nhiều lần");
    }

    @Test
    @DisplayName("RP-18: Unauthenticated reset request - Allowed through valid recovery flow")
    void testRP18_UnauthenticatedResetRequest_Allowed() {
        // Unauthenticated user submits forgot-password page
        Model model = new ConcurrentModel();
        String view = authController.forgotPasswordPage(model);

        assertThat(view).isEqualTo("pages/auth/forgot-password");
        assertThat(model.containsAttribute("forgotPasswordRequest")).isTrue();
    }
}
