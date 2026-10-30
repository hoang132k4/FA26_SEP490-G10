package com.jlpp.modules.quiz.repository;

import com.jlpp.modules.quiz.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByLessonId(Long lessonId);
    List<Exercise> findAllByLessonId(Long lessonId);
}
