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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoginAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void initUsers() {
        userRepository.deleteAll();

        // 1. Learner
        userRepository.save(User.builder()
                .email("learner@jeps.edu.vn")
                .username("learner_user")
                .fullName("Test Learner")
                .password(passwordEncoder.encode("LearnerPass123!"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        // 2. Manager
        userRepository.save(User.builder()
                .email("manager@jeps.edu.vn")
                .username("manager_user")
                .fullName("Test Manager")
                .password(passwordEncoder.encode("ManagerPass123!"))
                .role(RoleEnum.MANAGER)
                .status("ACTIVE")
                .build());

        // 3. Admin
        userRepository.save(User.builder()
                .email("admin@jeps.edu.vn")
                .username("admin_user")
                .fullName("Test Admin")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(RoleEnum.ADMIN)
                .status("ACTIVE")
                .build());

        // 4. Disabled Learner
        userRepository.save(User.builder()
                .email("disabled@jeps.edu.vn")
                .username("disabled_user")
                .fullName("Disabled User")
                .password(passwordEncoder.encode("DisabledPass123!"))
                .role(RoleEnum.LEARNER)
                .status("DISABLED")
                .build());
    }

    // ==================== LOGIN TESTS (LG-01 to LG-12) ====================

    @Test
    @DisplayName("LG-01: Valid Learner credentials - Login succeeds and redirects to Learner Dashboard")
    void testLG01_ValidLearnerLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "learner@jeps.edu.vn")
                        .param("password", "LearnerPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));
    }

    @Test
    @DisplayName("LG-02: Valid Manager credentials - Login succeeds and redirects to Manager Dashboard")
    void testLG02_ValidManagerLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "manager@jeps.edu.vn")
                        .param("password", "ManagerPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));
    }

    @Test
    @DisplayName("LG-03: Valid Admin credentials - Login succeeds and redirects to Admin Dashboard")
    void testLG03_ValidAdminLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "admin@jeps.edu.vn")
                        .param("password", "AdminPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("LG-04: Invalid password - Authentication rejected")
    void testLG04_InvalidPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "learner@jeps.edu.vn")
                        .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @DisplayName("LG-05: Unknown email - Generic authentication error (GB-04)")
    void testLG05_UnknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "unknown@jeps.edu.vn")
                        .param("password", "AnyPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @DisplayName("LG-06: Empty email - Validation error / Rejected")
    void testLG06_EmptyEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "")
                        .param("password", "AnyPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @DisplayName("LG-07: Empty password - Validation error / Rejected")
    void testLG07_EmptyPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "learner@jeps.edu.vn")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @DisplayName("LG-08: Invalid email format - Handled securely")
    void testLG08_InvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "not-an-email")
                        .param("password", "Pass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    @DisplayName("LG-09: Disabled account - Authentication denied (GB-05)")
    void testLG09_DisabledAccount() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "disabled@jeps.edu.vn")
                        .param("password", "DisabledPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?disabled=true"));
    }

    @Test
    @DisplayName("LG-10: Unauthenticated access to protected route redirects to login")
    void testLG10_UnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("LG-11: Logout - Session invalidated and redirected to login with logout message")
    void testLG11_LogoutSucceeds() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "learner@jeps.edu.vn")
                        .param("password", "LearnerPass123!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(get("/auth/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?logout=true"));
    }

    @Test
    @DisplayName("LG-12: Reuse old session after logout - Protected access denied")
    void testLG12_ReuseSessionAfterLogout() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("username", "learner@jeps.edu.vn")
                        .param("password", "LearnerPass123!"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(get("/auth/logout").session(session));

        // Reusing the same session should be rejected / redirected to login
        mockMvc.perform(get("/learning/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    // ==================== AUTHORIZATION TESTS (RB-01 to RB-10) ====================

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("RB-01: Learner accesses Learner Dashboard - Allowed (200)")
    void testRB01_LearnerAccessLearnerDashboard() throws Exception {
        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("RB-02: Learner accesses Manager Dashboard - 403 Forbidden")
    void testRB02_LearnerAccessManagerDashboard() throws Exception {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("RB-03: Learner accesses Admin Dashboard - 403 Forbidden")
    void testRB03_LearnerAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@jeps.edu.vn", roles = {"MANAGER"})
    @DisplayName("RB-04: Manager accesses Manager Dashboard - Allowed (200)")
    void testRB04_ManagerAccessManagerDashboard() throws Exception {
        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager@jeps.edu.vn", roles = {"MANAGER"})
    @DisplayName("RB-05: Manager accesses Admin Dashboard - 403 Forbidden")
    void testRB05_ManagerAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@jeps.edu.vn", roles = {"ADMIN"})
    @DisplayName("RB-06: Admin accesses Admin Dashboard - Allowed (200)")
    void testRB06_AdminAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RB-07: Guest accesses protected route - Redirect to Login")
    void testRB07_GuestAccessProtectedRoute() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("RB-08: Guest accesses public course page - Allowed (200)")
    void testRB08_GuestAccessPublicCourse() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("RB-09: User manipulates URL to privileged endpoint - Authorization enforced (403)")
    void testRB09_PrivilegedUrlEnforced() throws Exception {
        mockMvc.perform(get("/admin/exams"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("RB-10: Server ignores client forged role - Server enforces DB authority")
    void testRB10_ClientForgedRoleIgnored() throws Exception {
        // Attempting to send a parameter ?role=ADMIN should have zero effect on backend authorization
        mockMvc.perform(get("/admin/dashboard").param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }
}
