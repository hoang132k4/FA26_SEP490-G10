package com.jlpp.modules.quiz.service;

import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.dto.QuizResultDto;
import com.jlpp.modules.quiz.entity.Exercise;

import java.util.List;
import java.util.Map;

public interface QuizService {

    Exercise getOrCreateExerciseForLesson(Long lessonId);

    List<QuizQuestionDto> getQuestionsForExercise(Long exerciseId);

    QuizResultDto submitQuiz(Long exerciseId, Long userId, Map<Long, String> answers);

    QuizResultDto getResult(Long resultId);

    void seedQuizQuestionsForLesson(Long lessonId);
}
