package com.jlpp.modules.learningpath.service;

import com.jlpp.modules.learningpath.dto.SkillProfileDto;

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
}

