package com.jlpp.modules.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "exercise_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_result_id")
    private Long exerciseResultId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "score")
    private Integer score;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "result_status")
    private String resultStatus;

    @Column(name = "submitted_date")
    private ZonedDateTime submittedDate;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @PrePersist
    protected void onCreate() {
        if (submittedDate == null) submittedDate = ZonedDateTime.now();
        if (attemptNumber == null) attemptNumber = 1;
    }
}
