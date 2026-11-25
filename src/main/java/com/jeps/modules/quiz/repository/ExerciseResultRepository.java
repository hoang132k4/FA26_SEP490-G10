package com.jeps.modules.quiz.repository;

import com.jeps.modules.quiz.entity.ExerciseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, Long> {
    List<ExerciseResult> findByUserIdAndExerciseIdOrderBySubmittedDateDesc(Long userId, Long exerciseId);
    long countByExerciseIdAndUserId(Long exerciseId, Long userId);
}
