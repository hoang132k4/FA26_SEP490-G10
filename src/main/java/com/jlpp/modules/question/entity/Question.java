package com.jlpp.modules.question.entity;

import com.jlpp.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "questions")
public class Question extends BaseEntity {

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 255)
    private String category;

    @Column(name = "audio_url", length = 1000)
    private String audioUrl;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "question_type", nullable = false, length = 20)
    @Builder.Default
    private String questionType = "SINGLE_CHOICE";

    @Column(name = "jlpt_level", length = 5)
    private String jlptLevel;

    @Column(name = "skill_type", length = 20)
    private String skillType;

    @Column(length = 10)
    private String difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_key", columnDefinition = "jsonb")
    private String answerKey;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";
}
