package com.jlpp.modules.flashcard.service;

import com.jlpp.modules.flashcard.dto.FlashcardDeckDto;
import com.jlpp.modules.flashcard.dto.FlashcardRequest;
import com.jlpp.modules.flashcard.entity.Flashcard;

import java.util.List;
import java.util.Map;

public interface FlashcardService {

    List<Flashcard> getFlashcards(String keyword, String level, Long lessonId, String reviewStatus);

    Flashcard getFlashcardById(Long id);

    Flashcard createFlashcard(FlashcardRequest request);

    Flashcard createFlashcard(FlashcardRequest request, Long creatorId);

    Flashcard updateFlashcard(Long id, FlashcardRequest request);

    void deleteFlashcard(Long id);

    List<Flashcard> getPracticeFlashcards(String level, Long lessonId, Integer limit);

    List<Flashcard> getPracticeFlashcards(String level, Long lessonId, String deckName, Integer limit);

    Flashcard updateReviewStatus(Long id, String status);

    Map<String, Long> getStatistics();

    List<FlashcardDeckDto> getFlashcardDecks(String keyword, String level);

    List<FlashcardDeckDto> getSystemFlashcardDecks(String keyword, String level);

    List<FlashcardDeckDto> getUserFlashcardDecks(String keyword, String level, Long currentUserId);

    /**
     * Rename or re-level all flashcards that belong to a user deck (identified by deckName + creatorId).
     */
    int updateDeckInfo(String oldDeckName, String newDeckName, String newJlptLevel, Long creatorId);
}
