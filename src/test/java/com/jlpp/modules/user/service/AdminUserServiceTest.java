package com.jlpp.modules.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.systemlog.service.SystemLogService;
import com.jlpp.modules.user.model.AdminUserForm;
import com.jlpp.modules.user.model.AdminUserStatus;
import com.jlpp.modules.user.repository.InMemoryAdminUserRepository;

class AdminUserServiceTest {

    private AdminUserRepositoryFixture fixture;

    @BeforeEach
    void setUp() {
        SystemLogService logService = new SystemLogService();
        fixture = new AdminUserRepositoryFixture(new AdminUserService(
                new InMemoryAdminUserRepository(), logService), logService);
    }

    @Test
    void returnsSeededDemoUsers() {
        assertEquals(3, fixture.userService().findAll().size());
        assertEquals(3, fixture.userService().count());
    }

    @Test
    void updatesUserAndRecordsAuditEvent() {
        AdminUserForm form = new AdminUserForm();
        form.setFullName("  Updated Name  ");
        form.setEmail("UPDATED@example.test");
        form.setPhone("0900000000");
        form.setRole(RoleEnum.MANAGER);
        form.setStatus(AdminUserStatus.INACTIVE);
        long logCountBefore = fixture.logService().count();

        var saved = fixture.userService().update(1L, form);

        assertEquals("Updated Name", saved.fullName());
        assertEquals("updated@example.test", saved.email());
        assertEquals(RoleEnum.MANAGER, saved.role());
        assertEquals(AdminUserStatus.INACTIVE, saved.status());
        assertNotNull(saved.createdAt());
        assertEquals(logCountBefore + 1, fixture.logService().count());
    }

    @Test
    void rejectsDuplicateEmail() {
        AdminUserForm form = new AdminUserForm();
        form.setFullName("Updated Name");
        form.setEmail("THUHA@example.test");
        form.setPhone("");
        form.setRole(RoleEnum.LEARNER);
        form.setStatus(AdminUserStatus.ACTIVE);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.userService().update(1L, form));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void rejectsUnknownUser() {
        AdminUserForm form = new AdminUserForm();
        form.setFullName("Missing User");
        form.setEmail("missing@example.test");
        form.setPhone("");
        form.setRole(RoleEnum.LEARNER);
        form.setStatus(AdminUserStatus.ACTIVE);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.userService().update(999L, form));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private record AdminUserRepositoryFixture(AdminUserService userService, SystemLogService logService) {
    }
}
