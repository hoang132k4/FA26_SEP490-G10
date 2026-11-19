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

    @Column(name = "study_days", length = 100)
    private String studyDays; // Comma-separated: "MONDAY,WEDNESDAY,FRIDAY" or "2,3,4,5,6,7,1"

    @Column(name = "target_exam_date")
    private LocalDate targetExamDate;

    @Column(name = "schedule_mode", length = 30)
    private String scheduleMode; // "CUSTOM", "JLPT_TARGET"

    // Baseline Plan - Kế hoạch cơ sở ban đầu (không bị thay đổi khi recalibrate hoặc học viên nghỉ)
    @Column(name = "baseline_start_date")
    private LocalDate baselineStartDate;

    @Column(name = "baseline_end_date")
    private LocalDate baselineEndDate;

    @Column(name = "baseline_daily_hours")
    private BigDecimal baselineDailyHours;

    /**
     * Tính toán số giờ học/tuần của Kế hoạch ban đầu (Baseline Weekly Hours) theo công thức:
     * baseline_weekly_hours = (số ngày học/tuần) x baseline_daily_hours
     */
    public Integer calculateBaselineWeeklyHours(int studyDaysPerWeek) {
        if (baselineDailyHours == null) {
            return null;
        }
        int days = studyDaysPerWeek > 0 ? studyDaysPerWeek : 7;
        return Math.max(1, baselineDailyHours.multiply(BigDecimal.valueOf(days)).intValue());
    }

    // Chế độ Bù bài (Catch-up Mode)
    @Column(name = "catch_up_mode")
    @Builder.Default
    private Boolean catchUpMode = false;

    @Column(name = "catch_up_extra_minutes")
    private Integer catchUpExtraMinutes; // e.g. 15, 30, 45, 60

    @Column(name = "catch_up_include_weekends")
    @Builder.Default
    private Boolean catchUpIncludeWeekends = false;

    // Danh sách các đợt Báo nghỉ (Leave of Absence)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "leave_records", columnDefinition = "jsonb")
    private String leaveRecords;
}
