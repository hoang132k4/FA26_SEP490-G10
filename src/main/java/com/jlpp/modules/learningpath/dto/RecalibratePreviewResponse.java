package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalibratePreviewResponse {

    private boolean hasPath;
    private String courseTitle;
    private Integer originalWeeklyHours;
    private Integer newWeeklyHours;
    private String originalCompletionDate;
    private String newCompletionDate;
    private String daysDifferenceText;
    private Integer remainingHours;
    private Integer weeksNeeded;
    private String paceAnalysisText;

    @Builder.Default
    private List<LearningPathTimelineItemDto> rescheduledItems = new ArrayList<>();

    private String message;
}
