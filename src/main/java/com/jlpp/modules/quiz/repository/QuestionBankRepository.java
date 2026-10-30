package com.jlpp.modules.quiz.repository;

import com.jlpp.modules.quiz.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    List<QuestionBank> findByQuestionIdIn(List<Long> questionIds);
}
