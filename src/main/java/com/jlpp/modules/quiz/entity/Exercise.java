package com.jlpp.modules.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "exercise_title")
    private String exerciseTitle;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Column(name = "status")
    private String status;
}
