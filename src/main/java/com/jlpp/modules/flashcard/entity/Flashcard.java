package com.jlpp.modules.flashcard.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flashcard_id")
    private Long flashcardId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "custom_deck_name")
    private String customDeckName;

    @Column(name = "is_custom")
    @Builder.Default
    private Boolean isCustom = false;

    @Column(name = "front_content", columnDefinition = "TEXT")
    private String frontContent;

    @Column(name = "back_content", columnDefinition = "TEXT")
    private String backContent;

    @Column(name = "vocabulary", nullable = false)
    private String vocabulary;

    @Column(name = "reading")
    private String reading;

    @Column(name = "meaning", nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "jlpt_level")
    private String jlptLevel; // N5, N4, N3, N2, N1

    @Column(name = "review_status")
    @Builder.Default
    private String reviewStatus = "NEW"; // NEW, REVIEWING, MASTERED

    public Long getId() {
        return flashcardId;
    }

    public String getStatusBadgeClass() {
        if (reviewStatus == null) return "bg-info text-dark";
        return switch (reviewStatus.toUpperCase()) {
            case "MASTERED" -> "bg-success text-white";
            case "REVIEWING" -> "bg-warning text-dark";
            case "NEW" -> "bg-info text-dark";
            default -> "bg-secondary text-white";
        };
    }

    public String getStatusBadgeLabel() {
        if (reviewStatus == null) return "Mới";
        return switch (reviewStatus.toUpperCase()) {
            case "MASTERED" -> "Đã thuộc";
            case "REVIEWING" -> "Đang ôn";
            case "NEW" -> "Mới";
            default -> reviewStatus;
        };
    }

    public String getLevelBadgeClass() {
        if (jlptLevel == null) return "bg-primary text-white";
        return switch (jlptLevel.toUpperCase()) {
            case "N1" -> "bg-danger text-white";
            case "N2" -> "bg-warning text-dark";
            case "N3" -> "bg-info text-dark";
            case "N4" -> "bg-primary text-white";
            case "N5" -> "bg-success text-white";
            default -> "bg-secondary text-white";
        };
    }
}
