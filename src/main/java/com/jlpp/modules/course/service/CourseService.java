package com.jlpp.modules.course.service;

import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.Lesson;

import java.util.List;
import java.util.Map;

public interface CourseService {
    Course getCourseById(Long id);
    List<Course> getOtherCourses(Long excludedId, String level, int limit);
    List<Lesson> getCourseLessons(Long courseId);
    Map<String, List<Lesson>> groupLessonsBySection(List<Lesson> lessons);
}
