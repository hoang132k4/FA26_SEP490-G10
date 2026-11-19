package com.jlpp.modules.learningpath.service;

import com.jlpp.modules.learningpath.dto.LearningPathDtos.*;


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
     * Lấy thông tin lộ trình học tập thực tế của học viên cho một khóa học cụ thể mà học viên đã đăng ký.
     *
     * @param username Email hoặc username của học viên đã xác thực
     * @param courseId ID khóa học cần xem lộ trình (phải là khóa học người dùng đã đăng ký)
     * @return DTO lộ trình học tập thực tế
     */
    LearningPathResponse getLearningPath(String username, Long courseId);

    /**
     * Tính toán mô phỏng điều chỉnh lịch học (UC-28) ở chế độ PREVIEW / SIMULATION.
     * Tuyệt đối không ghi đè, không lưu (INSERT/UPDATE) vào cơ sở dữ liệu.
     *
     * @param username Email hoặc username của học viên đã xác thực
     * @param request Tham số điều chỉnh lịch học (số giờ học/tuần)
     * @return DTO kết quả mô phỏng điều chỉnh lịch học
     */
    RecalibratePreviewResponse getRecalibratePreview(String username, RecalibrateRequest request);

    /**
     * Xác nhận áp dụng và lưu lịch trình lộ trình học tập mới vào cơ sở dữ liệu.
     * Cập nhật số giờ học mỗi ngày (daily_hours_setup) và ngày bắt đầu (scheduled_date).
     *
     * @param username Email hoặc username của học viên đã xác thực
     * @param request Tham số điều chỉnh lịch học mới đã được xác nhận
     */
    void applyRecalibratedSchedule(String username, RecalibrateRequest request);

    /**
     * Ghi nhận học viên báo nghỉ học cho một khoảng thời gian (startDate đến endDate).
     * Tự động dời các bài học tiếp theo, bảo lưu chuỗi streak và giữ nguyên Baseline Plan làm mốc so sánh.
     */
    void submitLeaveOfAbsence(String username, SubmitLeaveRequest request);

    /**
     * Hủy bỏ một đợt báo nghỉ đã đăng ký trước đó.
     * Tự động tính toán lại lịch học theo trạng thái thông thường.
     */
    void cancelLeaveOfAbsence(String username, Long courseId, String leaveId);

    /**
     * Bật hoặc tắt Chế độ Bù bài (Catch-up Mode).
     * Khi bật, áp dụng tăng thời lượng hàng ngày hoặc học thêm cuối tuần để đẩy nhanh tiến độ bắt kịp Baseline Plan.
     */
    void toggleCatchUpMode(String username, ToggleCatchUpRequest request);
}

