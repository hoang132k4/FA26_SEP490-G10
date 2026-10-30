package com.jlpp.modules.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardRequest {

    private Long flashcardId;

    private Long lessonId;
    private String customDeckName;

    private String frontContent;
    private String backContent;

    @NotBlank(message = "Từ vựng / Chữ Hán không được để trống")
    private String vocabulary;

    private String reading;

    @NotBlank(message = "Ý nghĩa từ vựng không được để trống")
    private String meaning;

    private String exampleSentence;

    private String jlptLevel;

    private String reviewStatus;
}
