package com.jlpp.modules.manager.flashcards.service;

import com.jlpp.modules.lesson.entity.Lesson;
import com.jlpp.modules.lesson.repository.LessonRepository;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardForm;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetCardForm;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetForm;
import com.jlpp.modules.manager.flashcards.model.ManagerFlashcard;
import com.jlpp.modules.manager.flashcards.repository.ManagerFlashcardRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ManagerFlashcardService {

    private final ManagerFlashcardRepository flashcardRepository;
    private final LessonRepository lessonRepository;

    public ManagerFlashcardService(ManagerFlashcardRepository flashcardRepository, LessonRepository lessonRepository) {
        this.flashcardRepository = flashcardRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public List<ManagerFlashcard> findFlashcards() {
        return flashcardRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Lesson> findLessons() {
        return lessonRepository.findAll(Sort.by(Sort.Direction.ASC, "lessonId"));
    }

    @Transactional(readOnly = true)
    public Optional<ManagerFlashcard> findFlashcard(long flashcardId) {
        return flashcardRepository.findById(flashcardId);
    }

    @Transactional
    public List<ManagerFlashcard> createFlashcardSet(ManagerFlashcardSetForm form, long creatorId) {
        requireLesson(form.getLessonId());
        String deckName = required(form.getDeckName(), "Tên bộ thẻ");
        List<ManagerFlashcard> savedCards = new ArrayList<>();
        for (ManagerFlashcardSetCardForm cardForm : form.getCards()) {
            ManagerFlashcard flashcard = new ManagerFlashcard();
            flashcard.setLessonId(form.getLessonId());
            flashcard.setVocabulary(required(cardForm.getVocabulary(), "Thuật ngữ"));
            flashcard.setMeaning(required(cardForm.getMeaning(), "Định nghĩa"));
            flashcard.setReading(optional(cardForm.getReading()));
            flashcard.setExampleSentence(optional(cardForm.getExampleSentence()));
            flashcard.setFrontContent(flashcard.getVocabulary());
            flashcard.setBackContent(flashcard.getMeaning());
            flashcard.setJlptLevel(optional(form.getJlptLevel()));
            flashcard.setReviewStatus("NEW");
            flashcard.setCreatorId(creatorId);
            flashcard.setCustomDeckName(deckName);
            flashcard.setCustom(true);
            savedCards.add(flashcardRepository.insert(flashcard));
        }
        return savedCards;
    }

    @Transactional
    public ManagerFlashcard updateFlashcard(long flashcardId, ManagerFlashcardForm form) {
        ManagerFlashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy flashcard cần cập nhật."));
        requireLesson(form.getLessonId());
        flashcard.setLessonId(form.getLessonId());
        flashcard.setVocabulary(required(form.getVocabulary(), "Từ vựng"));
        flashcard.setMeaning(required(form.getMeaning(), "Nghĩa của từ"));
        flashcard.setFrontContent(optional(form.getFrontContent()));
        flashcard.setBackContent(optional(form.getBackContent()));
        flashcard.setReading(optional(form.getReading()));
        flashcard.setExampleSentence(optional(form.getExampleSentence()));
        flashcard.setJlptLevel(optional(form.getJlptLevel()));
        flashcard.setReviewStatus(optional(form.getReviewStatus()));
        flashcard.setCustomDeckName(optional(form.getCustomDeckName()));
        flashcard.setCustom(form.isCustom());
        if (!flashcardRepository.update(flashcard)) {
            throw new IllegalArgumentException("Flashcard đã bị xóa hoặc không còn tồn tại.");
        }
        return flashcard;
    }

    private void requireLesson(Long lessonId) {
        if (lessonId == null || !lessonRepository.existsById(lessonId)) {
            throw new IllegalArgumentException("Bài học đã chọn không tồn tại. Vui lòng chọn lại.");
        }
    }

    private String required(String value, String fieldName) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        return cleaned;
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
