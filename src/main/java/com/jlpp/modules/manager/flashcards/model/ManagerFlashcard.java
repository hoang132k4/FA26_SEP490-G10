package com.jlpp.modules.manager.flashcards.model;

public class ManagerFlashcard {

    private Long flashcardId;
    private Long lessonId;
    private String frontContent;
    private String backContent;
    private String vocabulary;
    private String reading;
    private String meaning;
    private String exampleSentence;
    private String jlptLevel;
    private String reviewStatus;
    private Long creatorId;
    private String customDeckName;
    private boolean custom;

    public Long getFlashcardId() { return flashcardId; }
    public void setFlashcardId(Long flashcardId) { this.flashcardId = flashcardId; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public String getFrontContent() { return frontContent; }
    public void setFrontContent(String frontContent) { this.frontContent = frontContent; }
    public String getBackContent() { return backContent; }
    public void setBackContent(String backContent) { this.backContent = backContent; }
    public String getVocabulary() { return vocabulary; }
    public void setVocabulary(String vocabulary) { this.vocabulary = vocabulary; }
    public String getReading() { return reading; }
    public void setReading(String reading) { this.reading = reading; }
    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }
    public String getJlptLevel() { return jlptLevel; }
    public void setJlptLevel(String jlptLevel) { this.jlptLevel = jlptLevel; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getCustomDeckName() { return customDeckName; }
    public void setCustomDeckName(String customDeckName) { this.customDeckName = customDeckName; }
    public boolean isCustom() { return custom; }
    public void setCustom(boolean custom) { this.custom = custom; }
}
