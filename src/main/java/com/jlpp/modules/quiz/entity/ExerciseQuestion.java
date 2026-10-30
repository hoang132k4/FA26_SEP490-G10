package com.jlpp.modules.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_questions")
@IdClass(ExerciseQuestionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseQuestion {

    @Id
    @Column(name = "exercise_id")
    private Long exerciseId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "question_order")
    private Integer questionOrder;
}
