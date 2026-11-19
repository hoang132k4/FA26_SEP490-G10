package com.jlpp.modules.learningpath.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LearningPathDtos {

    private LearningPathDtos() {
        // Khóa khởi tạo instance cho utility/container class
    }

    // ========================================================================
    // NHÓM 1: LỘ TRÌNH HỌC TẬP & DASHBOARD NGƯỜI HỌC (LEARNING PATH DASHBOARD)
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseOptionDto {
        private Long courseId;
        private String courseTitle;
        private String courseLevel;
        private Integer progressPercent;
        private boolean selected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathTimelineItemDto {
        private Long id;
        private String title;
        private String sectionName;
        private String contentType;
        private Integer durationMinutes;
        private String durationFormatted;
        private Integer targetWeek;
        private LocalDate plannedDate;
        private String plannedDateFormatted;
        private String dayOfWeekFormatted;
        private String dateWithDayOfWeek;
        private String status;
        private String statusDisplay;
        private String statusBadgeClass;
        private String actionUrl;
        private String actionText;
        private Integer orderIndex;

        // --- Mở rộng phân tích kỹ năng & đề xuất học tập ---
        private String skillType;
        private String skillDisplayName;
        private SkillProficiency skillProficiency;
        private Integer baseDurationMinutes;
        private Integer recommendedDurationMinutes;
        private String recommendedDurationFormatted;
        private String focusBadgeText;
        private String focusBadgeClass;
        private String recommendationReason;
        private String reviewResourcesSummary;
        private boolean hasReviewResources;
        private boolean isOverflowDay;

        public String getDurationFormatted() {
            if (durationFormatted != null && !durationFormatted.isBlank()) {
                return durationFormatted;
            }
            if (durationMinutes == null || durationMinutes <= 0) {
                return "1 giờ";
            }
            int hours = durationMinutes / 60;
            int remaining = durationMinutes % 60;
            if (hours == 0) {
                return remaining + " phút";
            }
            if (remaining == 0) {
                return hours + " giờ";
            }
            return hours + " giờ " + remaining + " phút";
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathDayDto {
        private int dayIndex;
        private LocalDate date;
        private String dateFormatted;
        private DayOfWeek dayOfWeek;
        private String dayOfWeekFormatted;
        private String fullDateDisplay;
        private Integer totalMinutes;
        private String totalDurationFormatted;
        private Integer totalLessons;
        private Integer completedLessons;
        private String status;
        private String statusDisplay;
        private String statusBadgeClass;
        private boolean isToday;
        private boolean isCurrentDay;

        @Builder.Default
        private List<LearningPathTimelineItemDto> items = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathWeekDto {
        private Integer weekNumber;
        private String weekTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private String startDateFormatted;
        private String endDateFormatted;
        private String dateRangeFormatted;
        private String fullTitle;
        private Integer totalLessons;
        private Integer completedLessons;
        private Integer inProgressLessons;
        private Integer progressPercent;
        private String status;
        private String statusDisplay;
        private String statusBadgeClass;
        private boolean isCurrentWeek;
        private Integer totalMinutes;
        private String totalDurationFormatted;

        @Builder.Default
        private List<LearningPathTimelineItemDto> items = new ArrayList<>();

        @Builder.Default
        private List<LearningPathDayDto> days = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayFocusDto {
        private String todayFormatted;
        private boolean studyDayToday;
        private boolean hasLessonsToday;
        private int totalLessonsToday;
        private int completedLessonsToday;
        private int remainingLessonsToday;
        private int totalMinutesToday;
        private int progressPercent;
        private boolean allCompletedToday;

        private String nextLessonTitle;
        private String nextLessonUrl;
        private String nextLessonBadge;
        private String nextLessonBadgeClass;
        private String actionButtonText;
        private String statusMessage;

        // --- Trạng thái hoàn thành toàn bộ khóa học (100%) ---
        private boolean courseCompleted;
        private String courseCompletedDateFormatted;

        // --- Thông số chuỗi ngày học (Streak) ---
        private int streakDays;
        private boolean streakActiveToday;
        private String streakMessage;
        private String streakBadgeClass;

        // --- Trạng thái báo nghỉ (Leave of Absence) ---
        private boolean onLeaveToday;
        private String leaveReason;
        private String leaveEndDateFormatted;
        private boolean streakFrozen;

        // --- Trạng thái bù bài (Catch-up Mode) ---
        private boolean catchUpActive;
        private int catchUpExtraMinutes;

        @Builder.Default
        private List<LearningPathTimelineItemDto> todayLessons = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathResponse {
        private boolean hasPath;
        private Long learningPathId;
        private Long courseId;
        private String courseTitle;
        private String courseLevel;
        private String targetLevel;
        private String currentLevel;
        private BigDecimal dailyHours;
        private String dailyHoursFormatted;
        private Integer weeklyHours;
        @Builder.Default
        private List<String> studyDays = new ArrayList<>();
        private String studyDaysText;
        private LocalDate targetExamDate;
        private String targetExamDateFormatted;
        private Long daysUntilExam;
        private String examBufferDaysText;
        private String scheduleMode;
        private String startDateFormatted;
        private String projectedEndDateFormatted;

        // --- Kế hoạch ban đầu cố định (Baseline Plan) ---
        private LocalDate baselineStartDate;
        private String baselineStartDateFormatted;
        private LocalDate baselineEndDate;
        private String baselineEndDateFormatted;
        private BigDecimal baselineDailyHours;
        private Integer baselineWeeklyHours;

        // --- Độ lệch giữa Kế hoạch ban đầu & Kế hoạch hiện tại (Variance) ---
        private Long baselineDaysVariance;
        private String baselineVarianceShortText;
        private String baselineVarianceText;
        private String baselineVarianceBadgeClass;
        private String planComparisonStatus;

        private Integer progressPercent;
        private Integer totalLessons;
        private Integer completedLessons;
        private Integer remainingLessons;
        private Integer totalHours;
        private String pathStatus;
        private String statusBadgeClass;

        @Builder.Default
        private List<LearningPathTimelineItemDto> items = new ArrayList<>();

        @Builder.Default
        private List<LearningPathWeekDto> weeks = new ArrayList<>();

        private SkillProfileDto skillProfile;
        private String emptyStateMessage;

        private TodayFocusDto todayFocus;

        // --- Chế độ Bù bài (Catch-up Mode) ---
        private Boolean catchUpMode;
        private Integer catchUpExtraMinutes;
        private Boolean catchUpIncludeWeekends;
        private String catchUpStatusMessage;

        // --- Báo nghỉ học (Leave of Absence) ---
        private Boolean currentlyOnLeave;
        private String currentLeaveReason;
        private String currentLeaveEndDateFormatted;
        @Builder.Default
        private List<LeaveRecordDto> leaveRecords = new ArrayList<>();

        @Builder.Default
        private List<CourseOptionDto> enrolledCourses = new ArrayList<>();

        public String getDailyHoursFormatted() {
            if (dailyHoursFormatted != null && !dailyHoursFormatted.isBlank()) {
                return dailyHoursFormatted;
            }
            if (dailyHours == null) {
                return "1.00 giờ/ngày";
            }
            return String.format(java.util.Locale.US, "%.2f giờ/ngày", dailyHours);
        }

        public boolean isBehindBaselinePlan() {
            if (progressPercent != null && progressPercent >= 100) {
                return false;
            }
            if (completedLessons != null && totalLessons != null && totalLessons > 0 && completedLessons >= totalLessons) {
                return false;
            }
            return (baselineDaysVariance != null && baselineDaysVariance > 0)
                    || "DELAYED".equalsIgnoreCase(planComparisonStatus)
                    || Boolean.TRUE.equals(catchUpMode);
        }

        public boolean isAllLessonsCompleted() {
            if (progressPercent != null && progressPercent >= 100) {
                return true;
            }
            if (completedLessons != null && totalLessons != null && totalLessons > 0 && completedLessons >= totalLessons) {
                return true;
            }
            if (remainingLessons != null && remainingLessons <= 0 && totalLessons != null && totalLessons > 0) {
                return true;
            }
            return todayFocus != null && todayFocus.isCourseCompleted();
        }
    }

    // ========================================================================
    // NHÓM 2: THIẾT LẬP LỊCH HỌC, MÔ PHỎNG 2 BƯỚC, BÁO NGHỈ & BÙ BÀI
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecalibrateRequest {
        @Min(value = 1, message = "Số giờ học tối thiểu mỗi tuần là 1 giờ")
        @Max(value = 105, message = "Số giờ học tối đa mỗi tuần là 105 giờ để tránh quá tải")
        private Integer weeklyHours;

        private BigDecimal dailyHours;

        private List<String> studyDays;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate targetExamDate;

        private String scheduleMode;

        private String jlptPeriod;

        private Long courseId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecalibratePreviewResponse {
        private boolean hasPath;
        private Long courseId;
        private String courseTitle;
        private String courseLevel;

        @Builder.Default
        private List<CourseOptionDto> enrolledCourses = new ArrayList<>();

        private Integer originalWeeklyHours;
        private Integer newWeeklyHours;
        private BigDecimal dailyHours;
        @Builder.Default
        private List<String> studyDays = new ArrayList<>();
        private String studyDaysText;
        private LocalDate startDate;
        private String startDateFormatted;
        private LocalDate targetExamDate;
        private String targetExamDateFormatted;
        private Long daysUntilExam;
        private Long examBufferDays;
        private String examBufferDaysText;
        private String examReadinessStatus;
        private String examReadinessBadgeClass;
        private BigDecimal recommendedDailyHoursForExam;
        private String scheduleMode;

        // --- Kế hoạch ban đầu (Baseline Plan) ---
        private LocalDate baselineStartDate;
        private String baselineStartDateFormatted;
        private LocalDate baselineEndDate;
        private String baselineEndDateFormatted;

        // --- Kế hoạch hiện tại (Current Plan trước khi đổi) ---
        private String originalCompletionDate;

        // --- Kế hoạch đề xuất mới (Proposed Plan sau khi đổi) ---
        private String newCompletionDate;
        private String daysDifferenceText;
        private Long daysDiffFromBaseline;
        private String daysDiffFromBaselineText;
        private Integer remainingHours;
        private Integer weeksNeeded;
        private String paceAnalysisText;

        @Builder.Default
        private List<LearningPathTimelineItemDto> rescheduledItems = new ArrayList<>();

        @Builder.Default
        private List<LearningPathWeekDto> weeks = new ArrayList<>();

        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveRecordDto {
        private String id;
        private LocalDate startDate;
        private LocalDate endDate;
        private String startDateFormatted;
        private String endDateFormatted;
        private String reason;
        private int totalDays;
        private boolean activeNow;
        private boolean upcoming;

        public int getDaysCount() {
            return totalDays;
        }

        public boolean isActive() {
            return activeNow;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitLeaveRequest {
        private Long courseId;

        @NotNull(message = "Ngày bắt đầu nghỉ không được để trống")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;

        @NotNull(message = "Ngày kết thúc nghỉ không được để trống")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;

        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleCatchUpRequest {
        private Long courseId;
        private Boolean enable;
        private Integer extraMinutes;
        private Boolean includeWeekends;
    }

    // ========================================================================
    // NHÓM 3: HỒ SƠ NĂNG LỰC & TRỌNG SỐ KỸ NĂNG (SKILL PROFILE & ADAPTATION)
    // ========================================================================

    public enum SkillProficiency {
        WEAK("Cần cải thiện", "badge-weak", "bg-danger text-white"),
        MEDIUM("Đạt yêu cầu", "badge-medium", "bg-warning text-dark"),
        STRONG("Điểm mạnh", "badge-strong", "bg-success text-white"),
        UNKNOWN("Chưa có dữ liệu", "badge-unknown", "bg-secondary text-white");

        private final String label;
        private final String cssClass;
        private final String badgeBootstrapClass;

        SkillProficiency(String label, String cssClass, String badgeBootstrapClass) {
            this.label = label;
            this.cssClass = cssClass;
            this.badgeBootstrapClass = badgeBootstrapClass;
        }

        public String getLabel() {
            return label;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getBadgeBootstrapClass() {
            return badgeBootstrapClass;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonSkillFocusDto {
        private Long lessonId;
        private String lessonTitle;
        private String rawSkillType;
        private String skillDisplayName;
        private SkillProficiency proficiency;
        private double durationMultiplier;
        private int baseDurationMinutes;
        private int recommendedDurationMinutes;
        private String focusBadgeText;
        private String focusBadgeClass;
        private String recommendationReason;
        private int availableExercisesCount;
        private int availableFlashcardsCount;
        private int availableStudyMaterialsCount;
        private String reviewResourcesSummary;
        private boolean hasReviewResources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMetricDto {
        private String skillKey;
        private String skillName;
        private int totalQuestions;
        private int correctCount;
        private BigDecimal accuracyPercentage;
        private SkillProficiency proficiency;
        private boolean evidenceAvailable;

        private String iconSymbol;
        private String shortSkillName;

        // --- So sánh tiến bộ sau khi làm bài thi Cột mốc ---
        private BigDecimal previousAccuracy;
        private BigDecimal accuracyDelta;
        private boolean improved;

        public String getIconSymbol() {
            if (iconSymbol != null && !iconSymbol.isBlank()) return iconSymbol;
            if (skillKey == null) return "★";
            return switch (skillKey.toUpperCase()) {
                case "VOCABULARY" -> "あ";
                case "GRAMMAR" -> "文";
                case "KANJI" -> "漢";
                case "READING" -> "読";
                case "LISTENING" -> "聴";
                default -> "★";
            };
        }

        public String getShortSkillName() {
            if (shortSkillName != null && !shortSkillName.isBlank()) return shortSkillName;
            if (skillKey == null) return skillName != null ? skillName : "Kỹ năng";
            return switch (skillKey.toUpperCase()) {
                case "VOCABULARY" -> "Từ vựng";
                case "GRAMMAR" -> "Ngữ pháp";
                case "KANJI" -> "Kanji";
                case "READING" -> "Đọc hiểu";
                case "LISTENING" -> "Nghe hiểu";
                case "LANGUAGE_KNOWLEDGE" -> "Kiến thức ngôn ngữ";
                default -> skillName != null ? skillName : skillKey;
            };
        }

        public String getProgressBarColorHex() {
            if (!evidenceAvailable || accuracyPercentage == null) {
                return "#94A3B8";
            }
            double val = accuracyPercentage.doubleValue();
            if (val >= 70.0) {
                return "#16A34A";
            } else if (val >= 50.0) {
                return "#EAB308";
            } else {
                return "#DC2626";
            }
        }

        public String getProgressBarClass() {
            if (!evidenceAvailable || accuracyPercentage == null) {
                return "bg-secondary";
            }
            double val = accuracyPercentage.doubleValue();
            if (val >= 70.0) {
                return "bg-success";
            } else if (val >= 50.0) {
                return "bg-warning";
            } else {
                return "bg-danger";
            }
        }

        public String getDisplayPercentageText() {
            if (!evidenceAvailable || accuracyPercentage == null) {
                return "N/A";
            }
            return accuracyPercentage.stripTrailingZeros().toPlainString() + "%";
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillProfileDto {
        private Long userId;
        private Long resultId;
        private String assessmentTitle;
        private String jlptLevel;
        private BigDecimal overallAccuracy;
        private String strongestSkillName;
        private String weakestSkillName;
        private boolean assessmentTaken;

        // --- Tái hiệu chỉnh năng lực sau bài thi Cột mốc (Milestone Recalibration) ---
        private boolean recalibratedFromMilestone;
        private String milestoneExamTitle;
        private String milestoneCompletionDate;
        private Integer milestoneScore;
        private String recalibrationNote;

        @Builder.Default
        private Map<String, SkillMetricDto> skills = new LinkedHashMap<>();

        @Builder.Default
        private List<String> weakSkills = new ArrayList<>();

        @Builder.Default
        private List<String> strongSkills = new ArrayList<>();

        @Builder.Default
        private List<String> mediumSkills = new ArrayList<>();

        public SkillMetricDto getSkill(String skillKey) {
            if (skillKey == null) return null;
            SkillMetricDto direct = skills.get(skillKey.toUpperCase());
            if (direct != null) return direct;
            if ("VOCABULARY".equalsIgnoreCase(skillKey) || "GRAMMAR".equalsIgnoreCase(skillKey) || "KANJI".equalsIgnoreCase(skillKey)) {
                return skills.get("VOCAB_GRAMMAR");
            }
            return null;
        }

        public SkillProficiency getProficiencyForSkill(String skillKey) {
            if (skillKey == null) return SkillProficiency.UNKNOWN;
            SkillMetricDto metric = skills.get(skillKey.toUpperCase());
            if (metric != null && metric.getProficiency() != SkillProficiency.UNKNOWN) {
                return metric.getProficiency();
            }
            if ("VOCABULARY".equalsIgnoreCase(skillKey) || "GRAMMAR".equalsIgnoreCase(skillKey) || "KANJI".equalsIgnoreCase(skillKey)) {
                SkillMetricDto compound = skills.get("VOCAB_GRAMMAR");
                if (compound != null && compound.getProficiency() != SkillProficiency.UNKNOWN) {
                    return compound.getProficiency();
                }
                for (SkillMetricDto dg : getDisplaySkillGroups()) {
                    if ("LANGUAGE_KNOWLEDGE".equalsIgnoreCase(dg.getSkillKey()) && dg.getProficiency() != SkillProficiency.UNKNOWN) {
                        return dg.getProficiency();
                    }
                }
            }
            return metric != null ? metric.getProficiency() : SkillProficiency.UNKNOWN;
        }

        public List<SkillMetricDto> getDisplaySkillGroups() {
            return getAllGranularSkills();
        }

        public List<SkillMetricDto> getLanguageKnowledgeSkills() {
            List<SkillMetricDto> list = new ArrayList<>();
            list.add(resolveSkillWithFallback("VOCABULARY", "Từ vựng", "あ"));
            list.add(resolveSkillWithFallback("GRAMMAR", "Ngữ pháp", "文"));
            list.add(resolveSkillWithFallback("KANJI", "Kanji", "漢"));
            return list;
        }

        public List<SkillMetricDto> getApplicationSkills() {
            List<SkillMetricDto> list = new ArrayList<>();
            list.add(getListeningSkill());
            list.add(getReadingSkill());
            return list;
        }

        public SkillMetricDto getReadingSkill() {
            return resolveSkillWithFallback("READING", "Đọc hiểu", "📖");
        }

        public SkillMetricDto getListeningSkill() {
            return resolveSkillWithFallback("LISTENING", "Nghe hiểu", "🎧");
        }

        public List<SkillMetricDto> getAllGranularSkills() {
            List<SkillMetricDto> list = new ArrayList<>();
            list.addAll(getLanguageKnowledgeSkills());
            list.addAll(getApplicationSkills());
            return list;
        }

        private SkillMetricDto resolveSkillWithFallback(String key, String displayName, String icon) {
            SkillMetricDto m = (skills != null) ? skills.get(key) : null;
            if (m == null && skills != null && ("VOCABULARY".equals(key) || "GRAMMAR".equals(key) || "KANJI".equals(key))) {
                m = skills.get("VOCAB_GRAMMAR");
            }
            if (m != null) {
                return SkillMetricDto.builder()
                        .skillKey(key)
                        .skillName(displayName)
                        .shortSkillName(displayName)
                        .iconSymbol(icon)
                        .totalQuestions(m.getTotalQuestions())
                        .correctCount(m.getCorrectCount())
                        .accuracyPercentage(m.getAccuracyPercentage())
                        .proficiency(m.getProficiency())
                        .evidenceAvailable(m.isEvidenceAvailable())
                        .previousAccuracy(m.getPreviousAccuracy())
                        .accuracyDelta(m.getAccuracyDelta())
                        .improved(m.isImproved())
                        .build();
            }
            return SkillMetricDto.builder()
                    .skillKey(key)
                    .skillName(displayName)
                    .shortSkillName(displayName)
                    .iconSymbol(icon)
                    .totalQuestions(0)
                    .correctCount(0)
                    .accuracyPercentage(null)
                    .proficiency(SkillProficiency.UNKNOWN)
                    .evidenceAvailable(false)
                    .build();
        }
    }

    // ========================================================================
    // NHÓM 4: QUẢN LÝ TIẾN TRÌNH HỌC TẬP JSONB (LESSON PROGRESS JSONB TRACKING)
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonProgressItemDto {
        private String status;
        private String startedAt;
        private String completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathProgressDto {
        @Builder.Default
        private int version = 1;

        private Integer legacyInferredCount;

        @Builder.Default
        private Map<String, LessonProgressItemDto> lessons = new LinkedHashMap<>();

        public LessonProgressItemDto getLessonProgress(Long lessonId) {
            if (lessonId == null || lessons == null) return null;
            return lessons.get(String.valueOf(lessonId));
        }

        public boolean isLessonCompleted(Long lessonId) {
            LessonProgressItemDto item = getLessonProgress(lessonId);
            return item != null && "COMPLETED".equalsIgnoreCase(item.getStatus());
        }
    }
}
