package com.jlpp.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ManagerQuestionForm {

    private Long id;

    @NotBlank(message = "Loại câu hỏi không được để trống.")
    private String questionType = "MULTI_CHOICE";

    @NotBlank(message = "Nội dung câu hỏi không được để trống.")
    @Size(max = 10000, message = "Nội dung câu hỏi không được vượt quá 10000 ký tự.")
    private String content;

    @Size(max = 1000, message = "Audio URL không được vượt quá 1000 ký tự.")
    private String audioUrl;

    @Size(max = 10000, message = "Giải thích không được vượt quá 10000 ký tự.")
    private String explanation;

    private String jlptLevel;
    private String skillType;
    private String status = "DRAFT";

    @Size(max = 2000, message = "Đáp án A không được vượt quá 2000 ký tự.")
    private String optionA;
    @Size(max = 2000, message = "Đáp án B không được vượt quá 2000 ký tự.")
    private String optionB;
    @Size(max = 2000, message = "Đáp án C không được vượt quá 2000 ký tự.")
    private String optionC;
    @Size(max = 2000, message = "Đáp án D không được vượt quá 2000 ký tự.")
    private String optionD;

    private List<String> answerKeys;

    private Long parentId;
}
