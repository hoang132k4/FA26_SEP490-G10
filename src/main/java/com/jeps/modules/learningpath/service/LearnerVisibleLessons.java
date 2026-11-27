package com.jeps.modules.learningpath.service;

import com.jeps.modules.course.entity.Lesson;

import java.util.List;
import java.util.Locale;

/**
 * Bài học mà học viên được thấy trong lộ trình: bỏ các bài quản lý đang ẩn (status = INACTIVE).
 * Bài mới tạo luôn ở trạng thái INACTIVE cho tới khi quản lý bật hiển thị, nên nếu không lọc thì
 * bài nháp sẽ lộ ra với học viên và bị tính vào tổng số bài (học viên không thể đạt 100%).
 */
public final class LearnerVisibleLessons {

    private LearnerVisibleLessons() {
    }

    public static boolean isVisible(Lesson lesson) {
        if (lesson == null) return false;
        String status = lesson.getStatus();
        return status == null || status.isBlank() || "ACTIVE".equals(status.trim().toUpperCase(Locale.ROOT));
    }

    public static List<Lesson> filter(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) return lessons == null ? List.of() : lessons;
        return lessons.stream().filter(LearnerVisibleLessons::isVisible).toList();
    }
}
