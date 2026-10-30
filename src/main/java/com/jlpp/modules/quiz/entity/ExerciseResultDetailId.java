package com.jlpp.modules.quiz.entity;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResultDetailId implements Serializable {
    private Long exerciseResultId;
    private Long questionId;
}
