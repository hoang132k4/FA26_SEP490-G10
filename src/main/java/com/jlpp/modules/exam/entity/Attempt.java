package com.jlpp.modules.exam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String answers = "{}";

    @Column(name = "grading_snapshot", columnDefinition = "jsonb")
    private String gradingSnapshot;

    private Integer score;

    @Column(name = "max_score")
    private Integer maxScore;

    private Boolean passed;

    @Column(nullable = false, length = 15)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;
}
