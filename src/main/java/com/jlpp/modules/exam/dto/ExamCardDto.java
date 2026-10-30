package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCardDto {
    private Long examId;
    private String examTitle;
    private String description;
    private String jlptLevel;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private Integer passingScore;
    private String status;
    private boolean ready; // true if totalQuestions > 0
}
