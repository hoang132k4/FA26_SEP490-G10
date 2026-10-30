package com.jlpp.modules.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ExamQuestionId implements Serializable {

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "question_id")
    private Long questionId;
}
