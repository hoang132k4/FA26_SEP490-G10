package com.jlpp.modules.quiz.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResultDto {
    private Long resultId;
    private Long exerciseId;
    private Long lessonId;
    private String exerciseTitle;
    private String lessonTitle;
    private int totalQuestions;
    private int correctCount;
    private int score;
    private BigDecimal percentage;
    private boolean passed;
    private String resultStatus;
    private String feedback;
    private ZonedDateTime submittedDate;
    @Builder.Default
    private List<QuizQuestionReviewDto> questions = new ArrayList<>();
}
