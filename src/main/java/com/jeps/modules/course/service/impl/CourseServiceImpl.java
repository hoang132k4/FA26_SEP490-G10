package com.jeps.modules.course.service.impl;

import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.course.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    @Cacheable(value = "courseDetails", key = "#id")
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khóa học với mã ID: " + id));
    }

    @Override
    @Cacheable(value = "courses", key = "'other_' + #excludedId + '_' + (#level ?: 'ALL') + '_' + #limit")
    public List<Course> getOtherCourses(Long excludedId, String level, int limit) {
        List<Course> list = courseRepository.findTop3ByLevelAndStatusAndIdNot(level, "PUBLISHED", excludedId);
        if (list.size() < limit) {
            List<Course> all = courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
            for (Course c : all) {
                if (!c.getId().equals(excludedId) && !list.contains(c)) {
                    list.add(c);
                    if (list.size() >= limit) break;
                }
            }
        }
        return list;
    }

    @Override
    @Cacheable(value = "lessons", key = "#courseId")
    public List<Lesson> getCourseLessons(Long courseId) {
        if (lessonRepository == null || courseId == null) {
            return List.of();
        }
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Override
    public Map<String, List<Lesson>> groupLessonsBySection(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return Map.of();
        }
        return lessons.stream()
                .collect(Collectors.groupingBy(
                        l -> (l.getSectionName() != null && !l.getSectionName().isBlank())
                                ? l.getSectionName()
                                : "Chương 1: Bài học căn bản",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
