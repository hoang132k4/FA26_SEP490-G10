package com.jlpp.modules.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManagerQuizForm {

    private Long id;

    @NotNull(message = "Vui lòng chọn bài học.")
    private Long lessonId;

    @NotBlank(message = "Tên quiz không được để trống.")
    @Size(max = 255, message = "Tên quiz không được vượt quá 255 ký tự.")
    private String title;

    @NotNull(message = "Tổng số câu hỏi không được để trống.")
    @Min(value = 1, message = "Tổng số câu hỏi phải lớn hơn 0.")
    @Max(value = 500, message = "Tổng số câu hỏi không được vượt quá 500.")
    private Integer totalQuestions;

    @NotNull(message = "Điểm đạt không được để trống.")
    @Min(value = 0, message = "Điểm đạt không được âm.")
    @Max(value = 100, message = "Điểm đạt tối đa là 100%.")
    private Integer passingScore;

    @NotBlank(message = "Vui lòng chọn trạng thái.")
    private String status;
}
