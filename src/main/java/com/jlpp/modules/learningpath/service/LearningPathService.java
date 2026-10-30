package com.jlpp.modules.learningpath.service;

import com.jlpp.modules.learningpath.dto.LearningPathResponse;
import com.jlpp.modules.learningpath.dto.RecalibratePreviewResponse;
import com.jlpp.modules.learningpath.dto.RecalibrateRequest;

public interface LearningPathService {

    /**
     * Lấy thông tin lộ trình học tập thực tế của học viên đang đăng nhập (UC-27).
     * Tuyệt đối không sinh dữ liệu giả lập. Nếu chưa có, trả về hasPath = false.
     *
     * @param username Email hoặc username của học viên đã xác thực
     * @return DTO lộ trình học tập thực tế
     */
    LearningPathResponse getLearningPath(String username);

    /**
     * Tính toán mô phỏng điều chỉnh lịch học (UC-28) ở chế độ PREVIEW / SIMULATION.
     * Tuyệt đối không ghi đè, không lưu (INSERT/UPDATE) vào cơ sở dữ liệu.
     *
     * @param username Email hoặc username của học viên đã xác thực
     * @param request Tham số điều chỉnh lịch học (số giờ học/tuần)
     * @return DTO kết quả mô phỏng điều chỉnh lịch học
     */
    RecalibratePreviewResponse getRecalibratePreview(String username, RecalibrateRequest request);
}
