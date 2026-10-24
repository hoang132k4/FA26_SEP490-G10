package com.jlpp.modules.admin;

import com.jlpp.modules.admin.entity.AdminUser;
import com.jlpp.modules.admin.repository.AdminSystemLogRepository;
import com.jlpp.modules.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AdminSystemLogRepository systemLogRepository;

    @Test
    void adminPagesRenderAndUserCanBeUpdatedWithAuditLog() throws Exception {
        AdminUser user = createUser(91001L, "admin-test-user", "admin-test@example.com");
        userRepository.save(user);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/dashboard/admin-dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Manager Dashboard"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Quản lý khóa học"))));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-test-user")));

        mockMvc.perform(get("/admin/users")
                        .param("keyword", "admin-test")
                        .param("role", "LEARNER")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-test-user")));

        mockMvc.perform(get("/admin/users/{userId}", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/detail"));

        mockMvc.perform(get("/admin/users/{userId}/edit", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/edit"));

        mockMvc.perform(post("/admin/users/{userId}", user.getUserId())
                        .param("fullName", "Admin Test Updated")
                        .param("username", "admin-test-user")
                        .param("email", "updated@example.com")
                        .param("role", "LEARNER")
                        .param("accountStatus", "INACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + user.getUserId()));

        AdminUser updated = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Admin Test Updated");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getEmailAddress()).isEqualTo("updated@example.com");
        assertThat(updated.getRole()).isEqualTo("LEARNER");
        assertThat(updated.getAccountStatus()).isEqualTo("INACTIVE");
        assertThat(systemLogRepository.findAll()).hasSize(1);

        mockMvc.perform(get("/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/logs"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER_UPDATED")));
    }

    @Test
    void duplicateEmailIsRejectedWithoutUpdatingUser() throws Exception {
        AdminUser target = userRepository.save(createUser(91002L, "target-user", "target@example.com"));
        userRepository.save(createUser(91003L, "other-user", "other@example.com"));

        mockMvc.perform(post("/admin/users/{userId}", target.getUserId())
                        .param("fullName", target.getFullName())
                        .param("username", target.getUsername())
                        .param("email", "other@example.com")
                        .param("role", "LEARNER")
                        .param("accountStatus", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email đã được sử dụng.")));

        assertThat(userRepository.findById(target.getUserId()).orElseThrow().getEmail())
                .isEqualTo("target@example.com");
        assertThat(systemLogRepository.findAll()).isEmpty();
    }

    private AdminUser createUser(Long userId, String username, String email) {
        AdminUser user = new AdminUser();
        user.setUserId(userId);
        user.setFullName("Admin Test");
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailAddress(email);
        user.setRole("LEARNER");
        user.setAccountStatus("ACTIVE");
        return user;
    }
}
