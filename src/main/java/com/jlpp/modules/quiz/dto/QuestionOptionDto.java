package com.jlpp.modules.quiz.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOptionDto {
    private Long id;
    private String label;
    private String optionText;
    private Boolean isCorrect;
}
