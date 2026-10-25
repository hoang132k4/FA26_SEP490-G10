package com.jlpp.modules.manager.courses.dto;

/** Course data needed by manager pages and the lesson workspace. */
public record CourseOption(long id, String title, String level, String status, int lessonCount) {
}
