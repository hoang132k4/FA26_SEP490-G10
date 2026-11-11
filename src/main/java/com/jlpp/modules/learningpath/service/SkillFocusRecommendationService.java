package com.jlpp.modules.learningpath.service;

import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.learningpath.dto.LessonSkillFocusDto;
import com.jlpp.modules.learningpath.dto.SkillProfileDto;

import java.util.List;

public interface SkillFocusRecommendationService {

    /**
     * Đưa ra gợi ý trọng tâm và thời lượng học tập cá nhân hóa cho một bài học cụ thể
     * dựa trên kỹ năng thực tế của bài học và hồ sơ năng lực của học viên.
     */
    LessonSkillFocusDto recommendFocus(Lesson lesson, SkillProfileDto profile);

    /**
     * Đưa ra gợi ý trọng tâm hàng loạt cho danh sách bài học của một khóa học.
     */
    List<LessonSkillFocusDto> recommendFocusBatch(List<Lesson> lessons, SkillProfileDto profile);
}

