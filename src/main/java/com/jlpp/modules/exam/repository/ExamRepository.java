package com.jlpp.modules.exam.repository;

import com.jlpp.modules.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByExamTypeIgnoreCase(String examType);

    List<Exam> findByExamTypeIgnoreCaseAndStatusIgnoreCase(String examType, String status);

    List<Exam> findByExamTypeIgnoreCaseAndJlptLevelIgnoreCase(String examType, String jlptLevel);

    List<Exam> findByExamTypeIgnoreCaseAndJlptLevelIgnoreCaseAndStatusIgnoreCase(String examType, String jlptLevel, String status);

    List<Exam> findByCourseId(Long courseId);

    List<Exam> findByCourseIdAndExamTypeIgnoreCase(Long courseId, String examType);
}
