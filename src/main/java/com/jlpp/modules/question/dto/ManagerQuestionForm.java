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
    private String questionType = "SINGLE_CHOICE";

    @NotBlank(message = "Nội dung câu hỏi không được để trống.")
    @Size(max = 10000, message = "Nội dung câu hỏi không được vượt quá 10000 ký tự.")
    private String content;

    @Size(max = 255, message = "Chủ đề không được vượt quá 255 ký tự.")
    private String category;

    @Size(max = 1000, message = "Audio URL không được vượt quá 1000 ký tự.")
    private String audioUrl;

    @Size(max = 10000, message = "Giải thích không được vượt quá 10000 ký tự.")
    private String explanation;

    private String jlptLevel;
    private String skillType;
    private String difficulty;
    private String status = "DRAFT";

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private List<String> answerKeys;

    private Long parentId;
}
