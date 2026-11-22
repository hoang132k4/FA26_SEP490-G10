package com.jeps.modules.quiz.repository;

import com.jeps.modules.quiz.entity.ExerciseQuestion;
import com.jeps.modules.quiz.entity.ExerciseQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseQuestionRepository extends JpaRepository<ExerciseQuestion, ExerciseQuestionId> {
    List<ExerciseQuestion> findByExerciseIdOrderByQuestionOrderAsc(Long exerciseId);
    void deleteByExerciseId(Long exerciseId);
}
