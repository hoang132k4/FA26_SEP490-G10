package com.jlpp.modules.payment.entity;

import com.jlpp.common.base.BaseEntity;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_user_id", columnList = "user_id"),
        @Index(name = "idx_payments_course_id", columnList = "course_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_order_code", columnList = "order_code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_order_code", columnNames = "order_code"),
        @UniqueConstraint(name = "uk_payments_transfer_content", columnNames = "transfer_content")
})
public class Payment extends BaseEntity {

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "payment_method", length = 50)
    @Builder.Default
    private String paymentMethod = "SEPAY";

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "transfer_content", nullable = false, unique = true, length = 100)
    private String transferContent;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
