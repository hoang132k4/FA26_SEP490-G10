package com.jlpp.modules.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
public class AdminSystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "log_type", nullable = false)
    private String logType;

    @Column(name = "action_performed", nullable = false)
    private String actionPerformed;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "details")
    private String details;
}
