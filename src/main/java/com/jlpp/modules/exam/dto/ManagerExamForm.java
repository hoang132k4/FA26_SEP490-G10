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

    @NotNull(message = "Số câu hỏi không được để trống.")
    @Min(value = 1, message = "Số câu hỏi phải lớn hơn 0.")
    @Max(value = 500, message = "Số câu hỏi không được vượt quá 500.")
    private Integer totalQuestions;

    @NotBlank(message = "Vui lòng chọn trạng thái.")
    private String status;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự.")
    private String description;
}
