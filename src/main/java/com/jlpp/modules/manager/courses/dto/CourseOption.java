package com.jlpp.modules.manager.courses.dto;

/** Course data needed by manager pages and the lesson workspace. */
public record CourseOption(long id, String title, String code, String level, String status, int lessonCount,
                           String shortDescription, String description, String thumbnailUrl, Double price,
                           Double discountPrice, Integer durationMinutes, String coveredSkills, Boolean featured) {
}
