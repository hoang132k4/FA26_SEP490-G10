package com.jlpp.modules.quiz.entity;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseQuestionId implements Serializable {
    private Long exerciseId;
    private Long questionId;
}
