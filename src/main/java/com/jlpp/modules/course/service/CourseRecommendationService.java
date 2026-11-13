package com.jlpp.modules.course.service;

import com.jlpp.modules.course.dto.ExamRecommendationDto;
import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.exam.dto.ExamResultResponse;

import java.util.List;

public interface CourseRecommendationService {
    List<RecommendedCourseItem> getRecommendationsForLearner(Long userId);
    List<Course> getOtherAvailableCourses(Long userId);
    ExamRecommendationDto getRecommendationsFromExamResult(ExamResultResponse examResult, Long userId, String targetLevel, String hasStudied, String assignedLevel);
}
