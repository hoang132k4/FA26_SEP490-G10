package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamTakeResponse {
    private Long examId;
    private String examTitle;
    private String description;
    private String examType;
    private String jlptLevel;
    private Integer durationMinutes;
    private Integer totalPoints;
    private Integer passingScore;
    private int totalQuestions;
    private String submitUrl;

    @Builder.Default
    private List<QuestionDto> questions = new ArrayList<>();

    @Builder.Default
    private List<QuestionDto> vocabGrammarQuestions = new ArrayList<>();

    @Builder.Default
    private List<QuestionDto> readingQuestions = new ArrayList<>();

    @Builder.Default
    private List<QuestionDto> listeningQuestions = new ArrayList<>();
}
