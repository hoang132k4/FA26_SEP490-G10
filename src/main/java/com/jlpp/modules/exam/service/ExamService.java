package com.jlpp.modules.exam.service;

import com.jlpp.modules.exam.dto.ExamCardDto;
import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.ExamTakeResponse;

import java.util.List;
import java.util.Map;

public interface ExamService {

    /**
     * Lấy danh sách đề thi JLPT Mock từ database cho Learner, hỗ trợ lọc theo cấp độ N5-N1.
     */
    List<ExamCardDto> getLearnerExams(String jlptLevel);

    /**
     * Lấy dữ liệu đề thi và câu hỏi từ database cho màn hình làm bài (không chứa đáp án đúng).
     */
    ExamTakeResponse getExamForTaking(Long examId);

    /**
     * Lấy bài thi đánh giá đầu vào (Placement Assessment) hợp lệ từ database cho Learner.
     */
    ExamTakeResponse getPlacementExamForTaking();

    /**
     * Lấy bài thi đánh giá đầu vào theo cấp độ được chỉ định (assignedLevel: N5, N4, N3, N2, N1).
     */
    ExamTakeResponse getPlacementExamForTaking(String assignedLevel);

    /**
     * Lấy danh sách các bài kiểm tra cột mốc (Milestone Exam) theo khóa học nếu có.
     */
    List<ExamCardDto> getMilestoneExams(Long courseId);

    /**
     * Lấy dữ liệu bài kiểm tra cột mốc (Milestone Exam) để làm bài.
     */
    ExamTakeResponse getMilestoneExamForTaking(Long examId);

    /**
     * Chấm điểm bài thi thực tế từ các câu hỏi và đáp án đúng trong database.
     */
    ExamResultResponse evaluateSubmission(Long examId, Map<String, String> formParams, Integer timeSpentSeconds);

    /**
     * Chấm điểm bài thi và lưu kết quả tổng quan vào bảng exam_results trên Supabase.
     */
    ExamResultResponse evaluateAndSaveSubmission(Long examId, Long userId, Map<String, String> formParams, Integer timeSpentSeconds);

    /**
     * Lấy lịch sử tất cả các lần làm bài thi của học viên đăng nhập từ Supabase.
     */
    List<ExamHistoryDto> getLearnerExamHistory(Long userId);

    /**
     * Lấy chi tiết một lần thi trong lịch sử với kiểm tra quyền sở hữu (IDOR protection).
     */
    ExamResultResponse getHistoricalResult(Long resultId, Long userId);
}

