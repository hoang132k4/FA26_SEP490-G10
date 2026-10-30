package com.jlpp.modules.quiz.repository;

import com.jlpp.modules.quiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByIdIn(List<Long> ids);
    List<Question> findByJlptLevel(String jlptLevel);
    List<Question> findByCategory(String category);
}
