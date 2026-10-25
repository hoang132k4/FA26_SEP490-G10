package com.jlpp.modules.manager.course.service;

import com.jlpp.modules.manager.course.dto.CourseOption;
import com.jlpp.modules.manager.course.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseOption> findCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CourseOption> findCourse(long courseId) {
        return courseRepository.findById(courseId);
    }

    @Transactional
    public long createCourse(String title, String level, String status) {
        return courseRepository.insert(title, level, status);
    }

    @Transactional
    public void updateCourse(long courseId, String title, String level, String status) {
        if (courseRepository.update(courseId, title, level, status) == 0) {
            throw new IllegalArgumentException("Không tìm thấy khóa học cần sửa.");
        }
    }
}
