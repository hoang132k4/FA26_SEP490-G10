package com.jlpp.modules.quiz.dto;

public record QuizOption(long id, Long lessonId, String lessonTitle, String title,
                         Integer totalQuestions, Integer passingScore, String status,
                         long linkedQuestionCount) { }
