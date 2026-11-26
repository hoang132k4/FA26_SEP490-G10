package com.jeps.modules.admin;

import com.jeps.modules.admin.entity.AdminUser;
import com.jeps.modules.admin.entity.AdminSystemLog;
import com.jeps.modules.admin.repository.AdminSystemLogRepository;
import com.jeps.modules.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @WithMockUser(roles = "ADMIN")
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
                        .with(csrf())
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
    @WithMockUser(roles = "ADMIN")
    void duplicateEmailIsRejectedWithoutUpdatingUser() throws Exception {
        AdminUser target = userRepository.save(createUser(91002L, "target-user", "target@example.com"));
        userRepository.save(createUser(91003L, "other-user", "other@example.com"));

        mockMvc.perform(post("/admin/users/{userId}", target.getUserId())
                        .with(csrf())
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void managerCrudActivityIsVisibleInReadOnlySystemLogs() throws Exception {
        AdminUser manager = createUser(91004L, "manager-audit-user", "manager-audit@example.com");
        manager.setRole("MANAGER");
        userRepository.save(manager);

        AdminSystemLog log = new AdminSystemLog();
        log.setUserId(manager.getUserId());
        log.setLogType("MANAGER");
        log.setActionPerformed("MANAGER_CREATE");
        log.setIpAddress("127.0.0.1");
        log.setTimestamp(Instant.parse("2025-01-01T10:00:00Z"));
        log.setDetails("POST /manager/courses (HTTP 302)");
        systemLogRepository.save(log);

        mockMvc.perform(get("/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/logs"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MANAGER")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Manager tạo mới")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/manager/courses")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void userManagementShowsOnlyAccountsAndNotPaymentOrManagerLogs() throws Exception {
        AdminUser user = userRepository.save(createUser(91005L, "regular-user", "regular@example.com"));
        AdminUser paymentTest = createUser(91006L, "payment-test-123", "payment-test-123@example.test");
        paymentTest.setFullName("Payment Test");
        userRepository.save(paymentTest);
        AdminUser invoiceTest = createUser(
                91007L, "payment-invoice-123", "payment-invoice-123@example.test");
        invoiceTest.setFullName("Other Learner");
        userRepository.save(invoiceTest);
        userRepository.save(createUser(91008L, "payment-customer", "payment-customer@example.com"));

        AdminSystemLog paymentLog = new AdminSystemLog();
        paymentLog.setLogType("PAYMENT");
        paymentLog.setActionPerformed("PAYMENT_TEST");
        paymentLog.setTimestamp(Instant.parse("2025-01-01T10:00:00Z"));
        paymentLog.setDetails("payment test record");
        systemLogRepository.save(paymentLog);

        AdminSystemLog managerLog = new AdminSystemLog();
        managerLog.setUserId(user.getUserId());
        managerLog.setLogType("MANAGER");
        managerLog.setActionPerformed("MANAGER_CREATE");
        managerLog.setTimestamp(Instant.parse("2025-01-01T10:01:00Z"));
        managerLog.setDetails("POST /manager/courses (HTTP 302)");
        systemLogRepository.save(managerLog);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeDoesNotExist("logs"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("regular-user")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("payment-customer@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("payment-test-123@example.test"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("payment-invoice-123@example.test"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("payment test record"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/manager/courses"))));
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
