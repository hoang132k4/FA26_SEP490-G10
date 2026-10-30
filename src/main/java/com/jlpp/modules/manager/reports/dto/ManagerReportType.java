package com.jlpp.modules.manager.reports.dto;

import java.util.Locale;
import java.util.List;

public enum ManagerReportType {
    EXAM_RESULTS("exam-results", "Báo cáo kết quả thi",
            List.of("Mã học viên", "Học viên", "Bài thi", "JLPT", "Điểm", "Tỷ lệ (%)", "Kết quả", "Ngày hoàn thành")),
    COURSE_ENROLLMENTS("course-enrollments", "Báo cáo đăng ký khóa học",
            List.of("Mã học viên", "Học viên", "Khóa học", "JLPT", "Tiến độ (%)", "Trạng thái", "Ngày đăng ký"));

    private final String value;
    private final String label;
    private final List<String> headers;

    ManagerReportType(String value, String label, List<String> headers) {
        this.value = value;
        this.label = label;
        this.headers = headers;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public List<String> headers() {
        return headers;
    }

    public static ManagerReportType from(String value) {
        if (value == null || value.isBlank()) {
            return EXAM_RESULTS;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ManagerReportType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Loại báo cáo không hợp lệ.");
    }
}
