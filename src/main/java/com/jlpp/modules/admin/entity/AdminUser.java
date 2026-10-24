package com.jlpp.modules.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
public class AdminUser {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "account_status")
    private String accountStatus;

    @Column(name = "status")
    private String legacyStatus;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "current_jlpt_level")
    private String currentJlptLevel;

    @Column(name = "target_jlpt_level")
    private String targetJlptLevel;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getDisplayEmail() {
        return emailAddress == null || emailAddress.isBlank() ? email : emailAddress;
    }

    public String getDisplayStatus() {
        return accountStatus == null || accountStatus.isBlank() ? legacyStatus : accountStatus;
    }
}
