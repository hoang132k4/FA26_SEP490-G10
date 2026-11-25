package com.jeps.modules.course.controller;

import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.exam.dto.ExamPrerequisiteResult;
import com.jeps.modules.exam.entity.Exam;
import com.jeps.modules.exam.service.ExamService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared view-model helpers for learner and public course controllers.
 */
final class CourseViewHelper {

    private CourseViewHelper() {
    }

    static void populateExamPrerequisites(
            ExamService examService,
            Long userId,
            List<Lesson> lessons,
            Exam midtermExam,
            Exam finalExam,
            Set<Long> unlockedExamIds,
            Map<Long, ExamPrerequisiteResult> examPrereqMap) {
        if (userId == null || examService == null) {
            return;
        }
        for (Lesson lesson : lessons) {
            if (lesson.isExam() && lesson.getExamId() != null) {
                recordPrerequisite(examService, lesson.getExamId(), userId, unlockedExamIds, examPrereqMap);
            }
        }
        if (midtermExam != null) {
            recordPrerequisite(examService, midtermExam.getExamId(), userId, unlockedExamIds, examPrereqMap);
        }
        if (finalExam != null) {
            recordPrerequisite(examService, finalExam.getExamId(), userId, unlockedExamIds, examPrereqMap);
        }
    }

    private static void recordPrerequisite(
            ExamService examService,
            Long examId,
            Long userId,
            Set<Long> unlockedExamIds,
            Map<Long, ExamPrerequisiteResult> examPrereqMap) {
        ExamPrerequisiteResult res = examService.checkExamPrerequisites(examId, userId);
        examPrereqMap.put(examId, res);
        if (res != null && res.isUnlocked()) {
            unlockedExamIds.add(examId);
        }
    }
}
