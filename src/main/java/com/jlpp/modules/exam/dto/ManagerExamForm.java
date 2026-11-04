package com.jlpp.modules.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManagerExamForm {

    private Long examId;

    @NotBlank(message = "Tên đề thi không được để trống.")
    @Size(max = 255, message = "Tên đề thi không được vượt quá 255 ký tự.")
    private String examTitle;

    @NotBlank(message = "Vui lòng chọn loại đề thi.")
    private String examType;

    @NotBlank(message = "Vui lòng chọn cấp độ JLPT.")
    private String jlptLevel;

    @NotNull(message = "Thời lượng không được để trống.")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0 phút.")
    @Max(value = 600, message = "Thời lượng không được vượt quá 600 phút.")
    private Integer durationMinutes;

    @NotNull(message = "Số câu hỏi không được để trống.")
    @Min(value = 1, message = "Số câu hỏi phải lớn hơn 0.")
    @Max(value = 500, message = "Số câu hỏi không được vượt quá 500.")
    private Integer totalQuestions;

    @NotNull(message = "Điểm đạt không được để trống.")
    @Min(value = 0, message = "Điểm đạt không được âm.")
    @Max(value = 100, message = "Điểm đạt tối đa là 100%.")
    private Integer passingScore;

    @NotBlank(message = "Vui lòng chọn trạng thái.")
    private String status;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự.")
    private String description;
}
