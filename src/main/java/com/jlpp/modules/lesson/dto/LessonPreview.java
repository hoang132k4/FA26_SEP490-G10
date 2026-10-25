package com.jlpp.modules.lesson.dto;

public record LessonPreview(
        Long lessonId,
        String title,
        Integer order,
        String skillType,
        String estimatedDuration,
        Integer durationMinutes,
        String lessonType,
        String sectionName
) {
    public LessonPreview(Long lessonId, String title, Integer order, String skillType,
                         String estimatedDuration, Integer durationMinutes,
                         String lessonType, String sectionName) {
        this.lessonId = lessonId;
        this.title = title;
        this.order = order;
        this.skillType = skillType;
        this.estimatedDuration = estimatedDuration;
        this.durationMinutes = durationMinutes;
        this.lessonType = lessonType;
        this.sectionName = sectionName;
    }
}
