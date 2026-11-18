package com.jeps.modules.course.service;

import com.jeps.modules.course.dto.ExamRecommendationDto;
import com.jeps.modules.course.dto.RecommendedCourseItem;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.exam.dto.ExamResultResponse;

import java.util.List;

public interface CourseRecommendationService {
    List<RecommendedCourseItem> getRecommendationsForLearner(Long userId);
    List<Course> getOtherAvailableCourses(Long userId);
    ExamRecommendationDto getRecommendationsFromExamResult(ExamResultResponse examResult, Long userId, String targetLevel, String hasStudied, String assignedLevel);
}
