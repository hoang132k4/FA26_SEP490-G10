package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the overall result of evaluating an assessment submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringEvaluationResult {
    private int earnedScore;
    private int totalPossibleScore;
    private BigDecimal percentage;
    private boolean passed;
    private Integer passingScore;
    private int correctCount;
    private int incorrectCount;
    private int unansweredCount;
    private int totalQuestions;
    private String strongestSkill;
    private String weakestSkill;
    private String aiFeedback;
    private String recommendation;

    @Builder.Default
    private List<ExamResultResponse.SkillScoreDto> skillBreakdown = new ArrayList<>();

    @Builder.Default
    private List<ExamResultResponse.SkillScoreDto> canonicalSkills = new ArrayList<>();

    @Builder.Default
    private List<QuestionDto> scoredQuestions = new ArrayList<>();
}

