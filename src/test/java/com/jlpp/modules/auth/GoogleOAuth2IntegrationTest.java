package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.model.CustomOAuth2User;
import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class GoogleOAuth2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User sampleLearner;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmailIgnoreCase("google_learner@jeps.edu.vn")) {
            sampleLearner = User.builder()
                    .fullName("Google Test Learner")
                    .username("google_learner")
                    .email("google_learner@jeps.edu.vn")
                    .password(passwordEncoder.encode("Password123@"))
                    .role(RoleEnum.LEARNER)
                    .status("ACTIVE")
                    .authProvider("GOOGLE")
                    .providerId("google_sub_sample_123")
                    .phone("0987654321")
                    .build();
            sampleLearner = userRepository.save(sampleLearner);
        } else {
            sampleLearner = userRepository.findByEmailIgnoreCase("google_learner@jeps.edu.vn").orElseThrow();
        }
    }

    @Test
    @DisplayName("UI 1: Login page contains Google Sign-in button with /oauth2/authorization/google")
    void testLoginPage_ContainsGoogleButton() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hoặc tiếp tục với")))
                .andExpect(content().string(containsString("Tiếp tục với Google")))
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
    }

    @Test
    @DisplayName("UI 2: Register page contains Google Sign-up button with /oauth2/authorization/google")
    void testRegisterPage_ContainsGoogleButton() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hoặc đăng ký nhanh bằng")))
                .andExpect(content().string(containsString("Đăng ký với Google")))
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
    }

    @Test
    @DisplayName("UI 3: Login page displays clear Vietnamese message on email conflict")
    void testLoginPage_EmailConflictError() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "email_conflict"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email này đã được đăng ký bằng phương thức mật khẩu. Vui lòng đăng nhập bằng mật khẩu.")));
    }

    @Test
    @DisplayName("UI 4: Login page displays message when user cancels Google authorization")
    void testLoginPage_OAuthCancelError() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "oauth_cancel"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn đã hủy quá trình xác thực với Google.")));
    }

    @Test
    @DisplayName("UI 5: Login page displays message when Google OAuth client is unconfigured")
    void testLoginPage_OAuthConfigError() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "oauth_config"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tính năng Đăng nhập với Google chưa được cấu hình Client ID")));
    }

    @Test
    @DisplayName("Auth 1: Existing email/password form login continues to work perfectly")
    void testExistingFormLogin_Success() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "google_learner@jeps.edu.vn")
                        .param("password", "Password123@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));
    }

    @Test
    @DisplayName("Profile 1: Google user missing phone number can access /auth/complete-profile")
    void testCompleteProfile_GetPage() throws Exception {
        User incompleteUser = User.builder()
                .fullName("OAuth Incomplete")
                .username("oauth_incomplete")
                .email("incomplete@gmail.com")
                .password(passwordEncoder.encode("Secret123"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .authProvider("GOOGLE")
                .providerId("sub_incomplete_999")
                .phone(null)
                .build();
        userRepository.save(incompleteUser);

        CustomUserDetails principal = new CustomUserDetails(incompleteUser);

        mockMvc.perform(get("/auth/complete-profile")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                principal, "n/a", principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hoàn tất hồ sơ")))
                .andExpect(content().string(containsString("incomplete@gmail.com")));
    }

    @Test
    @DisplayName("Profile 2: Google user submits phone number and completes profile to reach dashboard")
    void testCompleteProfile_SubmitPhone_Success() throws Exception {
        User incompleteUser = User.builder()
                .fullName("OAuth Need Phone")
                .username("oauth_need_phone")
                .email("needphone@gmail.com")
                .password(passwordEncoder.encode("Secret123"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .authProvider("GOOGLE")
                .providerId("sub_phone_888")
                .phone(null)
                .build();
        userRepository.save(incompleteUser);

        CustomUserDetails principal = new CustomUserDetails(incompleteUser);

        mockMvc.perform(post("/auth/complete-profile")
                        .with(csrf())
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                principal, "n/a", principal.getAuthorities())))
                        .param("fullName", "OAuth Need Phone")
                        .param("phone", "0934567890"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));

        User updated = userRepository.findByEmailIgnoreCase("needphone@gmail.com").orElseThrow();
        assertThat(updated.getPhone()).isEqualTo("0934567890");
    }
}
