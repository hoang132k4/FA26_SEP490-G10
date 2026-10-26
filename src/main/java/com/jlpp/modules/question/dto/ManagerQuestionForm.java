package com.jlpp.modules.question.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManagerQuestionForm {

    private Long id;

    @NotBlank(message = "Nội dung câu hỏi không được để trống.")
    @Size(max = 10000, message = "Nội dung câu hỏi không được vượt quá 10000 ký tự.")
    private String content;

    @Size(max = 255, message = "Chủ đề không được vượt quá 255 ký tự.")
    private String category;

    @Size(max = 1000, message = "Audio URL không được vượt quá 1000 ký tự.")
    private String audioUrl;

    @Size(max = 10000, message = "Giải thích không được vượt quá 10000 ký tự.")
    private String explanation;

    @NotNull(message = "Thứ tự câu hỏi không được để trống.")
    @Min(value = 1, message = "Thứ tự phải lớn hơn 0.")
    private Integer orderIndex;

    @NotNull(message = "Điểm câu hỏi không được để trống.")
    @Min(value = 0, message = "Điểm không được âm.")
    @Max(value = 1000, message = "Điểm không được vượt quá 1000.")
    private Integer points;

    private Long examId;
}
