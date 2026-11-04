package com.jlpp.modules.manager.flashcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ManagerFlashcardForm {

    private Long flashcardId;

    @NotNull(message = "Vui lòng chọn bài học.")
    @Positive(message = "Bài học không hợp lệ.")
    private Long lessonId;

    @NotBlank(message = "Vui lòng nhập từ vựng.")
    private String vocabulary;

    private String reading;

    @NotBlank(message = "Vui lòng nhập nghĩa của từ.")
    private String meaning;

    private String frontContent;
    private String backContent;
    private String exampleSentence;
    private String jlptLevel;
    private String reviewStatus;
    private String customDeckName;
    private boolean custom;

    public Long getFlashcardId() { return flashcardId; }
    public void setFlashcardId(Long flashcardId) { this.flashcardId = flashcardId; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public String getVocabulary() { return vocabulary; }
    public void setVocabulary(String vocabulary) { this.vocabulary = vocabulary; }
    public String getReading() { return reading; }
    public void setReading(String reading) { this.reading = reading; }
    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
    public String getFrontContent() { return frontContent; }
    public void setFrontContent(String frontContent) { this.frontContent = frontContent; }
    public String getBackContent() { return backContent; }
    public void setBackContent(String backContent) { this.backContent = backContent; }
    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }
    public String getJlptLevel() { return jlptLevel; }
    public void setJlptLevel(String jlptLevel) { this.jlptLevel = jlptLevel; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getCustomDeckName() { return customDeckName; }
    public void setCustomDeckName(String customDeckName) { this.customDeckName = customDeckName; }
    public boolean isCustom() { return custom; }
    public void setCustom(boolean custom) { this.custom = custom; }
}
