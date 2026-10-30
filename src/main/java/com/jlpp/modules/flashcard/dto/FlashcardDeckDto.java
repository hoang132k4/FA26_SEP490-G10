package com.jlpp.modules.flashcard.dto;

import com.jlpp.modules.flashcard.entity.Flashcard;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardDeckDto {
    private String deckId;
    private String title;
    private String description;
    private String jlptLevel; // N5, N4, N3, N2, N1
    private Long lessonId;
    private String lessonTitle;
    private String topicName;
    private String iconClass;
    private String badgeColor;
    @Builder.Default
    private String deckType = "SYSTEM"; // "SYSTEM" or "USER"
    private boolean custom;
    private Long creatorId;
    private String creatorName;

    @Builder.Default
    private List<Flashcard> cards = new ArrayList<>();

    private int totalCards;
    private int newCount;
    private int reviewingCount;
    private int masteredCount;
    private int masteryRate; // Phần trăm đã thuộc (0 - 100)

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
