package com.jlpp.modules.manager.flashcards.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class ManagerFlashcardSetForm {

    @NotBlank(message = "Vui lòng nhập tên bộ thẻ.")
    private String deckName;

    @NotNull(message = "Vui lòng chọn bài học.")
    @Positive(message = "Bài học không hợp lệ.")
    private Long lessonId;

    private String jlptLevel;

    @Valid
    @NotEmpty(message = "Bộ thẻ cần có ít nhất một thẻ.")
    @Size(max = 200, message = "Mỗi bộ thẻ tối đa 200 flashcard.")
    private List<ManagerFlashcardSetCardForm> cards = new ArrayList<>(
            List.of(new ManagerFlashcardSetCardForm(), new ManagerFlashcardSetCardForm()));

    public String getDeckName() { return deckName; }
    public void setDeckName(String deckName) { this.deckName = deckName; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public String getJlptLevel() { return jlptLevel; }
    public void setJlptLevel(String jlptLevel) { this.jlptLevel = jlptLevel; }
    public List<ManagerFlashcardSetCardForm> getCards() { return cards; }
    public void setCards(List<ManagerFlashcardSetCardForm> cards) { this.cards = cards; }
}
