package com.jlpp.modules.question.repository;

import com.jlpp.modules.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            select q from Question q
            where (:search = '' or lower(q.content) like lower(concat('%', :search, '%')))
              and (:category = '' or lower(q.category) = lower(:category))
              and (:questionType = '' or q.questionType = :questionType)
              and (:jlptLevel = '' or q.jlptLevel = :jlptLevel)
              and (:skillType = '' or q.skillType = :skillType)
              and (:difficulty = '' or q.difficulty = :difficulty)
              and (:status = '' or q.status = :status)
              and (q.parentId is null)
            order by q.id desc
            """)
    List<Question> searchForManager(@Param("search") String search,
                                    @Param("category") String category,
                                    @Param("questionType") String questionType,
                                    @Param("jlptLevel") String jlptLevel,
                                    @Param("skillType") String skillType,
                                    @Param("difficulty") String difficulty,
                                    @Param("status") String status);

    @Query("select distinct q.category from Question q where q.category is not null and q.category <> '' order by q.category")
    List<String> findDistinctCategories();

    List<Question> findByParentIdOrderById(Long parentId);

    @Query("select count(q) from Question q where q.parentId is null")
    long countTopLevel();

    @Query("""
            select q from Question q
            where q.status = 'ACTIVE'
              and q.questionType <> 'PASSAGE'
              and (:jlptLevel = '' or q.jlptLevel = :jlptLevel)
              and (:skillType = '' or q.skillType = :skillType)
              and q.id not in (select eq.id.questionId from ExamQuestion eq where eq.id.examId = :examId)
            order by q.id desc
            """)
    List<Question> findAvailableForExam(@Param("examId") Long examId,
                                        @Param("jlptLevel") String jlptLevel,
                                        @Param("skillType") String skillType);

    // ── Usage stats ──

    @Query(value = "SELECT COUNT(*) FROM exam_questions WHERE question_id = :questionId", nativeQuery = true)
    long countExamUsages(@Param("questionId") Long questionId);

    @Query(value = "SELECT COUNT(*) FROM exercise_questions WHERE question_id = :questionId", nativeQuery = true)
    long countQuizUsages(@Param("questionId") Long questionId);

    @Query(value = """
            SELECT COUNT(*) FROM exam_questions eq
            JOIN exams e ON e.exam_id = eq.exam_id
            WHERE eq.question_id = :questionId AND UPPER(e.status) IN ('PUBLISHED', 'ACTIVE')
            """, nativeQuery = true)
    long countActiveExamUsages(@Param("questionId") Long questionId);

    @Query(value = """
            SELECT COUNT(*) FROM exercise_questions eq
            JOIN exercises e ON e.exercise_id = eq.exercise_id
            WHERE eq.question_id = :questionId AND UPPER(e.status) = 'PUBLISHED'
            """, nativeQuery = true)
    long countActiveQuizUsages(@Param("questionId") Long questionId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM exam_questions WHERE question_id = :questionId UNION SELECT 1 FROM exercise_questions WHERE question_id = :questionId)", nativeQuery = true)
    boolean isUsedAnywhere(@Param("questionId") Long questionId);
}
