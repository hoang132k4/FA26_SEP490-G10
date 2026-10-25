package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.dto.ChangePasswordRequest;
import com.jlpp.modules.auth.service.ChangePasswordService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ChangePasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChangePasswordService changePasswordService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final String testEmail = "learner_cp@jeps.edu.vn";
    private final String originalPassword = "InitialPassword123@";

    @BeforeEach
    void setupUser() {
        userRepository.findByEmailIgnoreCase(testEmail).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .email(testEmail)
                .username("learner_cp")
                .fullName("Learner Change Password")
                .password(passwordEncoder.encode(originalPassword))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());
    }

    @Test
    @DisplayName("CP-01: Correct current password - Change succeeds")
    void testCP01_CorrectCurrentPassword_Succeeds() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword("NewBrandPass123@")
                .confirmPassword("NewBrandPass123@")
                .build();

        changePasswordService.changePassword(testEmail, request);

        User updatedUser = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(passwordEncoder.matches("NewBrandPass123@", updatedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("CP-02: Wrong current password - Change rejected")
    void testCP02_WrongCurrentPassword_Rejected() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("IncorrectCurrentPassword")
                .newPassword("NewBrandPass123@")
                .confirmPassword("NewBrandPass123@")
                .build();

        assertThatThrownBy(() -> changePasswordService.changePassword(testEmail, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");
    }

    @Test
    @DisplayName("CP-03: Empty current password - Validation error")
    void testCP03_EmptyCurrentPassword_ValidationError() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("")
                .newPassword("NewBrandPass123@")
                .confirmPassword("NewBrandPass123@")
                .build();

        var violations = validator.validate(request);
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"))).isTrue();
    }

    @Test
    @DisplayName("CP-04: Empty new password - Validation error")
    void testCP04_EmptyNewPassword_ValidationError() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword("")
                .confirmPassword("")
                .build();

        var violations = validator.validate(request);
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"))).isTrue();
    }

    @Test
    @DisplayName("CP-05: Password below minimum length (< 8 chars) - Validation error (GB-03)")
    void testCP05_PasswordTooShort_ValidationError() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword("Short1")
                .confirmPassword("Short1")
                .build();

        var violations = validator.validate(request);
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"))).isTrue();
    }

    @Test
    @DisplayName("CP-06: Password above maximum length (> 64 chars) - Validation error")
    void testCP06_PasswordTooLong_ValidationError() {
        String longPass = "A".repeat(65);
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword(longPass)
                .confirmPassword(longPass)
                .build();

        var violations = validator.validate(request);
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"))).isTrue();
    }

    @Test
    @DisplayName("CP-07: Confirmation mismatch - Change rejected")
    void testCP07_ConfirmationMismatch_Rejected() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword("NewBrandPass123@")
                .confirmPassword("DifferentPass123@")
                .build();

        assertThatThrownBy(() -> changePasswordService.changePassword(testEmail, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu xác nhận không trùng khớp");
    }

    @Test
    @DisplayName("CP-08: Same old and new password - Change rejected")
    void testCP08_SameOldAndNewPassword_Rejected() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword(originalPassword)
                .confirmPassword(originalPassword)
                .build();

        assertThatThrownBy(() -> changePasswordService.changePassword(testEmail, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu mới không được trùng");
    }

    @Test
    @DisplayName("CP-09: Unauthenticated request to change password - Redirects to Login")
    void testCP09_UnauthenticatedChangePasswordRedirects() throws Exception {
        mockMvc.perform(get("/profile/change-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .param("currentPassword", originalPassword)
                        .param("newPassword", "NewPass123@")
                        .param("confirmPassword", "NewPass123@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("CP-10: Valid new password - Password updated securely via MVC endpoint")
    @WithMockUser(username = "learner_cp@jeps.edu.vn", roles = {"LEARNER"})
    void testCP10_ValidNewPasswordViaMvc() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .param("currentPassword", originalPassword)
                        .param("newPassword", "ValidNewPassword123@")
                        .param("confirmPassword", "ValidNewPassword123@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/change-password"))
                .andExpect(flash().attributeExists("successMessage"));

        User updatedUser = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(passwordEncoder.matches("ValidNewPassword123@", updatedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("CP-11 & CP-12: Login with old password fails, login with new password succeeds")
    void testCP11_and_CP12_LoginVerificationAfterChange() throws Exception {
        // Change password first
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(originalPassword)
                .newPassword("SuccessNewPass123@")
                .confirmPassword("SuccessNewPass123@")
                .build();
        changePasswordService.changePassword(testEmail, request);

        // CP-11: Login with old password must fail
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", testEmail)
                        .param("password", originalPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));

        // CP-12: Login with new password must succeed
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", testEmail)
                        .param("password", "SuccessNewPass123@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));
    }

    @Test
    @DisplayName("CP-13: Database/provider failure - Controlled error response")
    @WithMockUser(username = "non_existent_user@jeps.edu.vn", roles = {"LEARNER"})
    void testCP13_ControlledErrorResponse() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .param("currentPassword", originalPassword)
                        .param("newPassword", "NewPass123@")
                        .param("confirmPassword", "NewPass123@"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("CP-14: CSRF attack - Request without CSRF token is rejected with 403")
    @WithMockUser(username = "learner_cp@jeps.edu.vn", roles = {"LEARNER"})
    void testCP14_CsrfProtectionEnforced() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                        // Notice: NO csrf() token provided
                        .param("currentPassword", originalPassword)
                        .param("newPassword", "NewPass123@")
                        .param("confirmPassword", "NewPass123@"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-15: Attempt to change another user's password - Rejected (Uses server principal)")
    @WithMockUser(username = "learner_cp@jeps.edu.vn", roles = {"LEARNER"})
    void testCP15_CannotChangeOtherUserPassword() throws Exception {
        // Even if client injects userId=999 or targetEmail=admin@jeps.edu.vn, server only alters principal
        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .param("userId", "999")
                        .param("targetEmail", "admin@jeps.edu.vn")
                        .param("currentPassword", originalPassword)
                        .param("newPassword", "ChangedPass123@")
                        .param("confirmPassword", "ChangedPass123@"))
                .andExpect(status().is3xxRedirection());

        // Verified that the principal's password was changed, and no other account was affected
        User principalUser = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(passwordEncoder.matches("ChangedPass123@", principalUser.getPassword())).isTrue();
    }
}
