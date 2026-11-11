package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultResponse {
    private Long examId;
    private String examTitle;
    private String jlptLevel;
    private Integer score;
    private Integer totalPossibleScore;
    private BigDecimal percentage;
    private Boolean passed;
    private Integer passingScore;
    private Integer timeSpentSeconds;
    private String formattedTimeSpent;
    private int correctCount;
    private int incorrectCount;
    private int unansweredCount;
    private int totalQuestions;

    private String strongestSkill;
    private String weakestSkill;
    private String aiFeedback;
    private String recommendation;
    private boolean persistentSaved;
    private Long resultId;
    private String examType;
    private String completionDate;
    private boolean detailsAvailable;
    private String statusNotice;

    @Builder.Default
    private List<SkillScoreDto> skillBreakdown = new ArrayList<>();

    @Builder.Default
    private List<SkillScoreDto> canonicalSkills = new ArrayList<>();

    @Builder.Default
    private List<QuestionDto> questions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillScoreDto {
        private String skillGroup;      // e.g. KANJI, VOCABULARY, GRAMMAR, READING, LISTENING, VOCAB_GRAMMAR
        private String skillName;       // e.g. "Chữ Hán (Kanji)", "Từ vựng (Vocabulary)", "Ngữ pháp (Grammar)"
        private Integer earnedPoints;
        private Integer totalPoints;
        private int correctCount;
        private int totalQuestions;
        private BigDecimal percentage;
        @Builder.Default
        private String status = "ASSESSED"; // "ASSESSED" or "NOT_ASSESSED"
    }
}
