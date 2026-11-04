package com.jlpp.modules.question.dto;

public record QuestionListRow(
        Long id,
        String content,
        String questionType,
        String jlptLevel,
        String skillType,
        String difficulty,
        String status,
        long examCount,
        long quizCount
) {
    public long totalUsages() { return examCount + quizCount; }
}
