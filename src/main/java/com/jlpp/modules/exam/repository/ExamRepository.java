package com.jlpp.modules.exam.repository;

import com.jlpp.modules.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByExamTypeIgnoreCase(String examType);

    @Query("""
            select e from Exam e
            where (:search = '' or lower(e.examTitle) like lower(concat('%', :search, '%')))
              and (:examType = '' or upper(e.examType) = upper(:examType))
              and (:jlptLevel = '' or upper(e.jlptLevel) = upper(:jlptLevel))
              and (:status = '' or upper(e.status) = upper(:status))
            order by e.examId desc
            """)
    List<Exam> searchForManager(@Param("search") String search,
                                @Param("examType") String examType,
                                @Param("jlptLevel") String jlptLevel,
                                @Param("status") String status);

    long countByExamTypeIgnoreCase(String examType);
}
