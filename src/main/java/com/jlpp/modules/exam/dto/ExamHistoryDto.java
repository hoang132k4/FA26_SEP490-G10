package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamHistoryDto {
    private Long resultId;
    private Long examId;
    private String examTitle;
    private String examType;
    private String examTypeLabel;
    private String jlptLevel;
    private Integer totalScore;
    private BigDecimal percentage;
    private Boolean passedStatus;
    private String resultStatus;
    private String formattedDate;
    private Integer attemptNumber;
}
