package com.jlpp.modules.learningpath.service;

import com.jlpp.modules.learningpath.dto.LearningPathDtos.*;

import com.jlpp.modules.course.entity.Lesson;

import java.util.List;
import java.util.Set;

public interface LessonProgressService {

    /**
     * Lấy dữ liệu tiến độ chi tiết từng bài học của học viên trong một khóa học.
     */
    LearningPathProgressDto getLessonProgress(Long userId, Long courseId);

    /**
     * Đánh dấu hoàn thành một bài học cụ thể theo lessonId.
     * Cập nhật vào cột JSONB lesson_progress trên bảng learning_paths và đồng bộ
     * progress_percent lên bảng course_registrations.
     */
    void markLessonCompleted(Long userId, Long courseId, Long lessonId);

    /**
     * Kiểm tra một bài học có trạng thái hoàn thành hay chưa, hỗ trợ tương thích ngược
     * với dữ liệu cũ (dựa trên progress_percent của course_registrations).
     */
    boolean isLessonCompleted(Long userId, Long courseId, Long lessonId, int lessonOrderIndex, int totalCourseLessons, int registrationProgressPercent);

    /**
     * Batch-query trạng thái hoàn thành của toàn bộ bài học trong khóa học trong 1 lần duy nhất,
     * loại bỏ hoàn toàn vấn đề N+1 query lặp qua database.
     */
    Set<Long> getCompletedLessonIds(Long userId, Long courseId, List<Lesson> courseLessons, int progressPercent);

    /**
     * Lấy danh sách ID các bài học đã hoàn thành của người dùng trong một khóa học.
     */
    Set<Long> getCompletedLessonIds(Long userId, Long courseId);
}

