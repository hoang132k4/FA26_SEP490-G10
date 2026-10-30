package com.jlpp.modules.exam.dto;

public record ExamQuestionRow(
        Long questionId,
        String content,
        String questionType,
        String jlptLevel,
        String skillType,
        String difficulty,
        String section,
        Integer orderIndex,
        Integer points
) {}
