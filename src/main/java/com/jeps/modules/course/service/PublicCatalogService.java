package com.jeps.modules.course.service;

import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.exam.entity.Exam;
import com.jeps.modules.exam.repository.ExamRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PublicCatalogService {

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;

    public PublicCatalogService(CourseRepository courseRepository,
                                ExamRepository examRepository) {
        this.courseRepository = courseRepository;
        this.examRepository = examRepository;
    }

    @Cacheable(value = "courses", key = "(#level ?: '') + '_' + (#keyword ?: '')")
    public List<Course> findCourses(String level, String keyword) {
        String normalizedLevel = normalize(level);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return courseRepository.findAll().stream()
                .filter(this::isPublic)
                .filter(course -> normalizedLevel.isEmpty()
                        || normalizedLevel.equals(normalize(course.getJlptLevel())))
                .filter(course -> normalizedKeyword.isEmpty()
                        || contains(course.getCourseTitle(), normalizedKeyword)
                        || contains(course.getDescription(), normalizedKeyword)
                        || contains(course.getJlptLevel(), normalizedKeyword))
                .toList();
    }

    @Cacheable(value = "courses", key = "'featured'")
    public List<Course> getFeaturedCourses() {
        return courseRepository.findByFeaturedTrueOrderByCourseIdAsc().stream()
                .filter(this::isPublic)
                .limit(6)
                .toList();
    }

    @Cacheable(value = "courseDetails", key = "#id")
    public Optional<Course> findCourse(Long id) {
        return courseRepository.findById(id).filter(this::isPublic);
    }

    public boolean isPublic(Course course) {
        if (course == null || course.getStatus() == null) {
            return false;
        }
        String s = course.getStatus().trim().toUpperCase(Locale.ROOT);
        return "PUBLISHED".equals(s) || "ACTIVE".equals(s) || "PUBLIC".equals(s);
    }

    @Cacheable(value = "exams", key = "'jlpt_mock_all'")
    public List<Exam> getJlptMockExams() {
        return examRepository.findByExamTypeIgnoreCase("JLPT_MOCK").stream()
                .filter(this::isExamPublic)
                .toList();
    }

    @Cacheable(value = "exams", key = "'jlpt_mock_' + #id")
    public Optional<Exam> findJlptMockExam(Long id) {
        return examRepository.findById(id)
                .filter(exam -> "JLPT_MOCK".equalsIgnoreCase(exam.getExamType()))
                .filter(this::isExamPublic);
    }

    private boolean isExamPublic(Exam exam) {
        if (exam == null || exam.getStatus() == null) {
            return false;
        }
        String s = exam.getStatus().trim().toUpperCase(Locale.ROOT);
        return "PUBLISHED".equals(s) || "ACTIVE".equals(s);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
