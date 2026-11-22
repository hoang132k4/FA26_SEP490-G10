package com.jeps.modules.course.service;

import com.jeps.modules.course.dto.CourseFilterRequest;
import com.jeps.modules.course.entity.Course;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CourseService {
    Page<Course> getCourses(CourseFilterRequest filterRequest);
    Course getCourseById(Long id);
    List<Course> getFeaturedCourses();
    List<Course> getOtherCourses(Long excludedId, String level, int limit);
}
