package com.jlpp.modules.flashcard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardReviewRequest {
    private Long flashcardId;
    private String status; // NEW, REVIEWING, MASTERED
}
