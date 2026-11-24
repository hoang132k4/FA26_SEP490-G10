package com.jeps.modules.dashboard.dto;

import java.util.List;
import java.util.Map;

public record ManagerDashboardData(
        long totalCourses,
        long publishedCourses,
        long draftCourses,
        long totalLessons,
        long activeLessons,
        long totalQuestions,
        long totalExams,
        long publishedExams,
        long totalQuizzes,
        long totalFlashcards,
        long totalEnrollments,
        long totalAttempts,
        long passedAttempts,
        double overallPassRate,
        Map<String, Long> coursesByLevel,
        Map<String, Long> questionsByLevel,
        List<RecentCourse> recentCourses,
        List<RecentExamResult> recentExamResults,
        List<RecentEnrollment> recentEnrollments
) {
    public record RecentCourse(
            Long id,
            String title,
            String code,
            String level,
            String status,
            int lessonCount,
            String thumbnailUrl
    ) {}

    public record RecentExamResult(
            String userId,
            String learnerName,
            String examTitle,
            String level,
            String score,
            String percentage,
            boolean passed,
            String completedAt
    ) {}

    public record RecentEnrollment(
            String userId,
            String learnerName,
            String courseTitle,
            String level,
            String progress,
            String status,
            String registeredAt
    ) {}
}

