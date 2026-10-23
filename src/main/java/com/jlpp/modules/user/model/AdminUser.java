package com.jlpp.modules.user.model;

import java.time.LocalDateTime;

import com.jlpp.common.constants.RoleEnum;

public record AdminUser(
        Long id,
        String fullName,
        String email,
        String phone,
        RoleEnum role,
        AdminUserStatus status,
        LocalDateTime createdAt) {

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public RoleEnum getRole() {
        return role;
    }

    public AdminUserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
