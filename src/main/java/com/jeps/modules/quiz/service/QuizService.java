package com.jeps.modules.quiz.service;

import com.jeps.modules.quiz.dto.QuizQuestionDto;
import com.jeps.modules.quiz.dto.QuizResultDto;
import com.jeps.modules.quiz.entity.Exercise;

import java.util.List;
import java.util.Map;

public interface QuizService {

    Exercise getOrCreateExerciseForLesson(Long lessonId);

    Exercise getExercise(Long exerciseId);

    List<QuizQuestionDto> getQuestionsForExercise(Long exerciseId);

    QuizResultDto submitQuiz(Long exerciseId, Long userId, Map<Long, String> answers);

    QuizResultDto getResult(Long resultId);

    void seedQuizQuestionsForLesson(Long lessonId);
}
