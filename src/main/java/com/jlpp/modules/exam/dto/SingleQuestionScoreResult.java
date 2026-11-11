package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of scoring a single question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleQuestionScoreResult {
    private Long questionId;
    private Long selectedOptionId;
    private boolean correct;
    private boolean unanswered;
    private int weight;
    private int earnedScore;
    private String canonicalSkill;
}

