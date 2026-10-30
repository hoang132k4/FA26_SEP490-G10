package com.jlpp.modules.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionBank {

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "category")
    private String category;

    @Column(name = "jlpt_level")
    private String jlptLevel;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "status")
    private String status;
}
