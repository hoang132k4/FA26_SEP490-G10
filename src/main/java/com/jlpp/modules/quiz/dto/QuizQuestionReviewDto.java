package com.jlpp.modules.quiz.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionReviewDto {
    private Long questionId;
    private Integer orderIndex;
    private String content;
    private String explanation;
    private String category;
    private String jlptLevel;
    @Builder.Default
    private List<QuestionOptionDto> options = new ArrayList<>();
    private String userSelectedLabel;
    private String correctLabel;
    private boolean isUserCorrect;
}
