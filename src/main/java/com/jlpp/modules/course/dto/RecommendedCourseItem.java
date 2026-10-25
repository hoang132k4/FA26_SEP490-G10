package com.jlpp.modules.course.dto;

import com.jlpp.modules.course.entity.Course;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedCourseItem {
    private Course course;
    private int matchPercentage; // e.g. 98%
    private String matchReason;  // e.g. "Phù hợp trực tiếp với kết quả đánh giá năng lực hiện tại (N5)"
    private String tag;          // "Gợi ý hàng đầu", "Lộ trình tiếp theo"
    private boolean isEnrolled;
}
