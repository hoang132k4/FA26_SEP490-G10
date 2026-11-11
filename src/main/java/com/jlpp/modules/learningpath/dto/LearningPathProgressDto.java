package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathProgressDto {
    @Builder.Default
    private int version = 1;

    /**
     * Preserved legacy completion count from course_registrations.progress_percent
     * captured before any JSONB records or at initial migration adoption.
     * Guaranteed never to be inflated by new lesson completions.
     */
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

