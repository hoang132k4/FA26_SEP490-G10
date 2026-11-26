package com.jeps.modules.exam.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jeps.modules.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

  List<Exam> findByExamTypeIgnoreCase(String examType);

  List<Exam> findByExamTypeIgnoreCaseAndStatusIgnoreCase(String examType, String status);

  List<Exam> findByExamTypeIgnoreCaseAndJlptLevelIgnoreCase(String examType, String jlptLevel);

  List<Exam> findByExamTypeIgnoreCaseAndJlptLevelIgnoreCaseAndStatusIgnoreCase(String examType, String jlptLevel,
      String status);

  List<Exam> findByCourseId(Long courseId);

  List<Exam> findByCourseIdAndExamTypeIgnoreCase(Long courseId, String examType);

    @Query("""
            SELECT e FROM Exam e
            WHERE (:courseId IS NOT NULL AND e.courseId = :courseId)
               OR UPPER(e.examType) = 'MILESTONE'
            ORDER BY e.examId ASC
            """)
    List<Exam> findMilestoneAndCourseExams(@Param("courseId") Long courseId);

    @Query("""
            select e from Exam e
            where (:search = '' or lower(e.examTitle) like lower(concat('%', :search, '%')))
              and (:examType = '' or upper(e.examType) = upper(:examType))
              and (:jlptLevel = '' or upper(e.jlptLevel) = upper(:jlptLevel))
              and (:status = '' or upper(e.status) = upper(:status))
            order by e.examId desc
            """)
    Page<Exam> searchForManager(@Param("search") String search,
                                @Param("examType") String examType,
                                @Param("jlptLevel") String jlptLevel,
                                @Param("status") String status,
                                Pageable pageable);

    @Query("select upper(e.examType), count(e) from Exam e group by upper(e.examType)")
    List<Object[]> countPerType();

    long countByExamTypeIgnoreCase(String examType);
}
