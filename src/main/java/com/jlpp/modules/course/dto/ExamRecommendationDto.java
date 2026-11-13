package com.jlpp.modules.course.dto;

import com.jlpp.modules.course.entity.Course;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamRecommendationDto {

    private String evaluationTitle;
    private String evaluationSummary;
    private String assessmentBadgeText;
    private String assessmentBadgeClass;

    private String targetLevel;
    private String currentAssessedLevel;
    private String weakestSkill;
    private String strongestSkill;
    private Integer percentage;
    private boolean isAssessmentOk;

    private RecommendedCourseItem primaryCourse;
    private RecommendedCourseItem skillGapCourse;
    private RecommendedCourseItem practiceCourse;

    @Builder.Default
    private List<RecommendedCourseItem> allRecommendations = new ArrayList<>();
}

