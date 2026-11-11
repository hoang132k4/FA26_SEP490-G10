package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressItemDto {
    private String status;      // NOT_STARTED, IN_PROGRESS, COMPLETED
    private String startedAt;   // ISO-8601
    private String completedAt; // ISO-8601
}

