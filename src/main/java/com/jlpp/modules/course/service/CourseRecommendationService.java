package com.jlpp.modules.course.service;

import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;

import java.util.List;

public interface CourseRecommendationService {
    List<RecommendedCourseItem> getRecommendationsForLearner(Long userId);
    List<Course> getOtherAvailableCourses(Long userId);
}
