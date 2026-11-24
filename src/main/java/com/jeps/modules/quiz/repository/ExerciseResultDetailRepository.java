package com.jeps.modules.quiz.repository;

import com.jeps.modules.quiz.entity.ExerciseResultDetail;
import com.jeps.modules.quiz.entity.ExerciseResultDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseResultDetailRepository extends JpaRepository<ExerciseResultDetail, ExerciseResultDetailId> {
    List<ExerciseResultDetail> findByExerciseResultId(Long exerciseResultId);
}
