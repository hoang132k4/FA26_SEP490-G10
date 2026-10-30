package com.jlpp.modules.quiz.repository;

import com.jlpp.modules.quiz.entity.ExerciseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, Long> {
    List<ExerciseResult> findByUserIdAndExerciseIdOrderBySubmittedDateDesc(Long userId, Long exerciseId);
    Optional<ExerciseResult> findFirstByUserIdAndExerciseIdOrderBySubmittedDateDesc(Long userId, Long exerciseId);
}
