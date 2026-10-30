package com.jlpp.modules.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_result_details")
@IdClass(ExerciseResultDetailId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseResultDetail {

    @Id
    @Column(name = "exercise_result_id")
    private Long exerciseResultId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "selected_option")
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}
