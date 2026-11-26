package com.jeps.modules.exam.repository;

import com.jeps.modules.exam.entity.ExamQuestion;
import com.jeps.modules.exam.entity.ExamQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestionId> {

    @Query("""
            select eq from ExamQuestion eq
            join fetch eq.question
            where eq.id.examId = :examId
            order by eq.section, eq.orderIndex
            """)
    List<ExamQuestion> findByExamIdWithQuestion(@Param("examId") Long examId);

    @Query("select count(eq) from ExamQuestion eq where eq.id.examId = :examId")
    long countByExamId(@Param("examId") Long examId);

    @Query("select count(eq) from ExamQuestion eq where eq.id.examId = :examId and eq.section = :section")
    long countByExamIdAndSection(@Param("examId") Long examId, @Param("section") String section);

    @Query("select eq.section, count(eq) from ExamQuestion eq where eq.id.examId = :examId group by eq.section")
    List<Object[]> countPerSection(@Param("examId") Long examId);

    @Query("select coalesce(max(eq.orderIndex), 0) from ExamQuestion eq where eq.id.examId = :examId and eq.section = :section")
    int maxOrderIndex(@Param("examId") Long examId, @Param("section") String section);

    void deleteAllByIdExamId(Long examId);

    @Query("select eq.id.questionId from ExamQuestion eq where eq.id.examId = :examId and eq.id.questionId in :ids")
    List<Long> findLinkedQuestionIds(@Param("examId") Long examId, @Param("ids") Collection<Long> ids);

    @Query("select eq.id.questionId from ExamQuestion eq where eq.id.examId = :examId and eq.question.status <> 'ACTIVE' order by eq.id.questionId")
    List<Long> findInactiveQuestionIds(@Param("examId") Long examId);

    @Query("""
            select eq from ExamQuestion eq
            where eq.id.examId = :examId and eq.section = :section
            order by eq.orderIndex
            """)
    List<ExamQuestion> findByExamIdAndSectionOrdered(@Param("examId") Long examId, @Param("section") String section);
}
