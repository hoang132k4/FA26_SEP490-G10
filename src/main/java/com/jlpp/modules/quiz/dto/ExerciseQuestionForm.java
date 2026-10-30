package com.jlpp.modules.quiz.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExerciseQuestionForm {
    private List<Long> questionIds;
}
