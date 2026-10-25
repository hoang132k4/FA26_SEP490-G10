package com.jlpp.modules.user.entity;

import com.jlpp.common.base.BaseEntity;
import com.jlpp.common.constants.RoleEnum;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private RoleEnum role = RoleEnum.LEARNER;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "phone_number", length = 20)
    private String phone;

    @Column(name = "current_jlpt_level", length = 20)
    private String currentJlptLevel;

    @Column(name = "target_jlpt_level", length = 20)
    private String targetJlptLevel;

    @Column(name = "auth_provider", length = 30)
    @Builder.Default
    private String authProvider = "LOCAL";

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
