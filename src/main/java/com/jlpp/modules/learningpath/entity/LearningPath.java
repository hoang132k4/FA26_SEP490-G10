package com.jlpp.modules.learningpath.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "learning_paths")
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_path_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "daily_hours_setup", nullable = false)
    private BigDecimal dailyHoursSetup;

    @Column(name = "target_level", nullable = false, length = 10)
    private String targetLevel;

    @Column(name = "current_level", length = 10)
    private String currentLevel;

    @Column(name = "path_status", nullable = false, length = 30)
    @Builder.Default
    private String pathStatus = "ACTIVE"; // ACTIVE, COMPLETED, PAUSED

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lesson_progress", columnDefinition = "jsonb")
    private String lessonProgress;
}
