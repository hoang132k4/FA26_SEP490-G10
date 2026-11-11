package com.jlpp.modules.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExamQuestionForm {

    @NotNull(message = "Vui lòng chọn ít nhất một câu hỏi.")
    private List<Long> questionIds;

    /** Để trống: tự xếp phần theo kỹ năng của từng câu. */
    private String section;
}
