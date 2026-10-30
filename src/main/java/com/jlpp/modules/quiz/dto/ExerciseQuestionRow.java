package com.jlpp.modules.quiz.dto;

public record ExerciseQuestionRow(long questionId, String content, String questionType,
                                  String jlptLevel, String skillType, String difficulty) {}
