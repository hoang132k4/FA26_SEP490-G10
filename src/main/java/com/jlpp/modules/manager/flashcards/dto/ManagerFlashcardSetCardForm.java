package com.jlpp.modules.manager.flashcards.dto;

import jakarta.validation.constraints.NotBlank;

public class ManagerFlashcardSetCardForm {

    @NotBlank(message = "Vui lòng nhập thuật ngữ.")
    private String vocabulary;

    private String reading;

    @NotBlank(message = "Vui lòng nhập định nghĩa.")
    private String meaning;

    private String exampleSentence;

    public String getVocabulary() { return vocabulary; }
    public void setVocabulary(String vocabulary) { this.vocabulary = vocabulary; }
    public String getReading() { return reading; }
    public void setReading(String reading) { this.reading = reading; }
    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }
}
