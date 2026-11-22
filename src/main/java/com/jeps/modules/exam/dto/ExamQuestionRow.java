package com.jeps.modules.exam.dto;

public record ExamQuestionRow(
        Long questionId,
        String content,
        String questionType,
        String jlptLevel,
        String skillType,
        String section,
        Integer orderIndex
) {}
