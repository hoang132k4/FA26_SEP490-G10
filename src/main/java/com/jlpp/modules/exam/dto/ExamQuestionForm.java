package com.jlpp.modules.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExamQuestionForm {

    @NotNull(message = "Vui lòng chọn ít nhất một câu hỏi.")
    private List<Long> questionIds;

    private String section = "LANGUAGE";

    @Min(value = 1, message = "Điểm phải lớn hơn 0.")
    private Integer points = 1;
}
