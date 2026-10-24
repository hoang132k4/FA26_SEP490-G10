package com.jlpp.modules.course.service;

import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.lesson.dto.LessonPreview;
import com.jlpp.modules.lesson.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PublicCatalogService {

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final LessonRepository lessonRepository;

    public PublicCatalogService(CourseRepository courseRepository, ExamRepository examRepository,
                                LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.examRepository = examRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<Course> findCourses(String level, String keyword) {
        String normalizedLevel = normalize(level);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return courseRepository.findAll().stream()
                .filter(course -> normalizedLevel.isEmpty()
                        || normalizedLevel.equals(normalize(course.getJlptLevel())))
                .filter(course -> normalizedKeyword.isEmpty()
                        || contains(course.getCourseTitle(), normalizedKeyword)
                        || contains(course.getDescription(), normalizedKeyword)
                        || contains(course.getJlptLevel(), normalizedKeyword))
                .toList();
    }

    public List<Course> getFeaturedCourses() {
        return courseRepository.findByFeaturedTrueOrderByCourseIdAsc().stream()
                .limit(3)
                .toList();
    }

    public Optional<Course> findCourse(Long id) {
        return courseRepository.findById(id);
    }

    public List<LessonPreview> findLessonPreviews(Long courseId) {
        return lessonRepository.findPreviewsByCourseId(courseId);
    }

    public List<Exam> getJlptMockExams() {
        return examRepository.findByExamTypeIgnoreCase("JLPT_MOCK");
    }

    public Optional<Exam> findJlptMockExam(Long id) {
        return examRepository.findById(id)
                .filter(exam -> "JLPT_MOCK".equalsIgnoreCase(exam.getExamType()));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
