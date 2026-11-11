package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSkillFocusDto {
    private Long lessonId;
    private String lessonTitle;
    private String rawSkillType;       // e.g. "GRAMMAR", "VOCABULARY", etc.
    private String skillDisplayName;   // e.g. "Ngữ pháp (Grammar)"
    private SkillProficiency proficiency;
    private double durationMultiplier; // 1.5 (WEAK), 1.0 (MEDIUM), 0.75 (STRONG), 1.0 (UNKNOWN)
    private int baseDurationMinutes;
    private int recommendedDurationMinutes;
    private String focusBadgeText;     // e.g. "Trọng tâm cần củng cố", "Nắm vững - Duy trì", "Điểm mạnh - Ôn tập nhanh"
    private String focusBadgeClass;    // CSS class
    private String recommendationReason; // Plain human-readable reason
    private int availableExercisesCount;
    private int availableFlashcardsCount;
    private int availableStudyMaterialsCount;
    private String reviewResourcesSummary;
    private boolean hasReviewResources;
}

