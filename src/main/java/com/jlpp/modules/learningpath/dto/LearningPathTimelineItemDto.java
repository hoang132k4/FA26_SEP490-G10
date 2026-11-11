package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathTimelineItemDto {

    private Long id;
    private String title;
    private String sectionName;
    private String contentType; // LESSON, MILESTONE_EXAM, PRACTICE
    private Integer durationMinutes;
    private String durationFormatted;
    private Integer targetWeek;
    private LocalDate plannedDate;
    private String plannedDateFormatted;
    private String status; // COMPLETED, IN_PROGRESS, UPCOMING, LOCKED
    private String statusDisplay;
    private String statusBadgeClass;
    private String actionUrl;
    private String actionText;
    private Integer orderIndex;

    // Skill Focus & Recommendation Extensions
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
