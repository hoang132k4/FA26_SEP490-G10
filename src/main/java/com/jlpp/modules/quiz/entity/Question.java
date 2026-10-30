package com.jlpp.modules.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "category")
    private String category;

    @Column(name = "jlpt_level")
    private String jlptLevel;

    @Column(name = "skill_type")
    private String skillType;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "status")
    private String status;

    @Column(name = "audio_url")
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private String options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_key", columnDefinition = "jsonb")
    private String answerKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
        if (questionType == null) questionType = "SINGLE_CHOICE";
        if (options == null) options = "[{\"key\": \"A\", \"text\": \"\"}, {\"key\": \"B\", \"text\": \"\"}, {\"key\": \"C\", \"text\": \"\"}, {\"key\": \"D\", \"text\": \"\"}]";
        if (answerKey == null) answerKey = "[\"A\"]";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
