package com.jeps.modules.quiz.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class QuizGenerateForm {
    private String jlptLevel;
    /** Số câu cần lấy theo skill_type (VOCABULARY, GRAMMAR, ...). */
    private Map<String, Integer> counts = new LinkedHashMap<>();
}
