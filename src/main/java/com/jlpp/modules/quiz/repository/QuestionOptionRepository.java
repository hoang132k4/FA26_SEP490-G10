package com.jlpp.modules.quiz.repository;

import com.jlpp.modules.quiz.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestionIdOrderByLabelAsc(Long questionId);
    List<QuestionOption> findByQuestionIdInOrderByQuestionIdAscLabelAsc(List<Long> questionIds);
    List<QuestionOption> findByQuestionIdIn(List<Long> questionIds);
}
