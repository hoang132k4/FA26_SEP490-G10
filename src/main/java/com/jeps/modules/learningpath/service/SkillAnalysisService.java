package com.jeps.modules.learningpath.service;

import com.jeps.modules.learningpath.dto.LearningPathDtos.*;


public interface SkillAnalysisService {

    /**
     * Phân tích hồ sơ năng lực theo 5 kỹ năng (Từ vựng, Ngữ pháp, Hán tự, Đọc hiểu, Nghe hiểu)
     * của học viên dựa trên bài kiểm tra đánh giá năng lực đầu vào (Placement Assessment).
     *
     * @param userId ID người dùng
     * @param resultId ID kết quả bài thi cụ thể (nếu có, hoặc null để tìm bài thi Placement gần nhất)
     * @return SkillProfileDto chứa phân loại WEAK, MEDIUM, STRONG, UNKNOWN và tỷ lệ chính xác từng kỹ năng
     */
    SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId);

    /**
     * Phân tích hồ sơ năng lực theo 5 kỹ năng của học viên dựa trên bài kiểm tra đánh giá năng lực đầu vào
     * phù hợp với khóa học / cấp độ JLPT cụ thể (không lấy nhầm bài test của cấp độ khác).
     *
     * @param userId ID người dùng
     * @param resultId ID kết quả bài thi cụ thể (nếu có, hoặc null để tìm bài thi Placement gần nhất)
     * @param courseId ID khóa học hiện tại để giới hạn đúng cấp độ bài thi
     * @return SkillProfileDto chứa phân loại WEAK, MEDIUM, STRONG, UNKNOWN và tỷ lệ chính xác từng kỹ năng
     */
    default SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId, Long courseId) {
        return analyzeLearnerSkills(userId, resultId, courseId, null);
    }

    /**
     * Phân tích hồ sơ năng lực theo 5 kỹ năng của học viên dựa trên bài kiểm tra đánh giá năng lực đầu vào
     * phù hợp với khóa học / cấp độ JLPT cụ thể (không lấy nhầm bài test của cấp độ khác).
     *
     * @param userId ID người dùng
     * @param resultId ID kết quả bài thi cụ thể (nếu có, hoặc null để tìm bài thi Placement gần nhất)
     * @param courseId ID khóa học hiện tại để giới hạn đúng cấp độ bài thi
     * @param courseLevel Cấp độ JLPT chuẩn của khóa học (N5, N4, N3, N2, N1)
     * @return SkillProfileDto chứa phân loại WEAK, MEDIUM, STRONG, UNKNOWN và tỷ lệ chính xác từng kỹ năng
     */
    SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId, Long courseId, String courseLevel);

    /**
     * Phân tích và Tái hiệu chỉnh năng lực của học viên sau Bài thi Cột mốc (Milestone Exam).
     * Tự động ưu tiên kết quả bài thi Cột mốc mới nhất của khóa học để cập nhật lại độ thành thạo 5 kỹ năng.
     * Nếu học viên chưa thi cột mốc, tự động dùng kết quả bài thi Đánh giá đầu vào (Placement Test).
     *
     * @param userId ID người dùng
     * @param courseId ID khóa học hiện tại
     * @param fallbackResultId ID kết quả bài test đầu vào dự phòng
     * @return SkillProfileDto đã được tái hiệu chỉnh cùng thông tin so sánh tiến bộ
     */
    default SkillProfileDto analyzeAndRecalibrateSkills(Long userId, Long courseId, Long fallbackResultId) {
        return analyzeAndRecalibrateSkills(userId, courseId, null, fallbackResultId);
    }

    /**
     * Phân tích và Tái hiệu chỉnh năng lực của học viên sau Bài thi Cột mốc (Milestone Exam) theo đúng cấp độ khóa học.
     *
     * @param userId ID người dùng
     * @param courseId ID khóa học hiện tại
     * @param courseLevel Cấp độ JLPT chuẩn của khóa học (N5, N4, N3, N2, N1)
     * @param fallbackResultId ID kết quả bài test đầu vào dự phòng
     * @return SkillProfileDto đã được tái hiệu chỉnh cùng thông tin so sánh tiến bộ
     */
    SkillProfileDto analyzeAndRecalibrateSkills(Long userId, Long courseId, String courseLevel, Long fallbackResultId);
}

