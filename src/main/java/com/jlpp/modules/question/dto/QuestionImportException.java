package com.jlpp.modules.question.dto;

import java.util.List;

public class QuestionImportException extends RuntimeException {

    private final List<String> errors;

    public QuestionImportException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() { return errors; }
}
