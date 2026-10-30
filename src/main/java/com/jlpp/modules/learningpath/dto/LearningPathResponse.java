package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {

    private boolean hasPath;
    private Long learningPathId;
    private Long courseId;
    private String courseTitle;
    private String courseLevel;
    private String targetLevel;
    private String currentLevel;
    private BigDecimal dailyHours;
    private Integer weeklyHours;
    private String startDateFormatted;
    private String projectedEndDateFormatted;
    private Integer progressPercent;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer remainingLessons;
    private Integer totalHours;
    private String pathStatus;
    private String statusBadgeClass;

    @Builder.Default
    private List<LearningPathTimelineItemDto> items = new ArrayList<>();

    private String emptyStateMessage;
}
