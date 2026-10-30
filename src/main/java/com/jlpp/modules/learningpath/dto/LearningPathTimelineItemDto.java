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
    private Integer targetWeek;
    private LocalDate plannedDate;
    private String plannedDateFormatted;
    private String status; // COMPLETED, IN_PROGRESS, UPCOMING, LOCKED
    private String statusDisplay;
    private String statusBadgeClass;
    private String actionUrl;
    private String actionText;
    private Integer orderIndex;
}
