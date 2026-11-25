package com.jeps.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamPrerequisiteResult {
    private boolean unlocked;
    private int totalRequiredLessons;
    private int completedRequiredLessons;
    private String message;
}
