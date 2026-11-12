package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Test: Learner Auth & Profile Screens (Screens 1, 2, 3, 5, 6, 7)")
class LearnerAuthAndProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "learner_it@jeps.edu.vn";

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmailIgnoreCase(TEST_EMAIL)) {
            userRepository.save(User.builder()
                    .email(TEST_EMAIL)
                    .username("learner_it")
                    .fullName("Nguyen Van IT")
                    .phone("0988112233")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(RoleEnum.LEARNER)
                    .status("ACTIVE")
                    .currentJlptLevel("N4")
                    .targetJlptLevel("N3")
                    .build());
        }
    }

    @Test
    @DisplayName("Screen 1: GET /auth/register hiển thị trang Đăng ký tài khoản")
    void testGetRegisterPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(content().string(containsString("Đăng ký")));
    }

    @Test
    @DisplayName("Screen 2: GET /auth/forgot-password hiển thị trang Quên mật khẩu")
    void testGetForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/forgot-password"))
                .andExpect(content().string(containsString("Quên mật khẩu")));
    }

    @Test
    @DisplayName("Screen 3: GET /auth/login hiển thị trang Đăng nhập")
    void testGetLoginPage() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/login"))
                .andExpect(content().string(containsString("Đăng nhập")));
    }

    @Test
    @DisplayName("Screen 3: Chưa đăng nhập truy cập /profile chuyển hướng về login")
    void testUnauthenticatedProfileRedirect() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 5: GET /profile hiển thị thông tin Learner Profile khi đã đăng nhập")
    void testGetProfilePage() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/view"))
                .andExpect(content().string(containsString("Hồ sơ")));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 6: GET /profile/edit hiển thị form chỉnh sửa Profile")
    void testGetEditProfilePage() throws Exception {
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/edit"))
                .andExpect(content().string(containsString("Lưu thay đổi")));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 7: GET /auth/change-password hiển thị trang Đổi mật khẩu")
    void testGetChangePasswordPage() throws Exception {
        mockMvc.perform(get("/auth/change-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/change-password"))
                .andExpect(content().string(containsString("Đổi mật khẩu")));
    }
}
