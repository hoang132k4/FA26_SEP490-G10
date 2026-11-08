package com.jlpp.modules.notification.entity;

import com.jlpp.common.base.BaseEntity;
import com.jlpp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "type", length = 50)
    @Builder.Default
    private String type = "SYSTEM"; // SYSTEM, COURSE, EXAM, FLASHCARD, REMINDER

    @Column(name = "icon", length = 100)
    @Builder.Default
    private String icon = "bi-bell-fill";

    @Column(name = "icon_color", length = 50)
    @Builder.Default
    private String iconColor = "text-primary";

    @Column(name = "target_url", length = 255)
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}

