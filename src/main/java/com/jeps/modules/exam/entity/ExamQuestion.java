package com.jeps.modules.exam.entity;

import com.jeps.modules.question.entity.Question;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exam_questions")
public class ExamQuestion implements Persistable<ExamQuestionId> {

    @EmbeddedId
    private ExamQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("examId")
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String section = "LANGUAGE";

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 1;

    /** The id is assigned, so tell Spring Data a built link is new and skip the SELECT that merge() would run. */
    @Transient
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    public void markNotNew() {
        newEntity = false;
    }
}
