package com.jlpp.modules.exam.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GeneratePreview(
        long examId,
        String jlptLevel,
        Map<String, List<PreviewQuestion>> sections
) implements Serializable {
    public int totalAdded() {
        return sections.values().stream().mapToInt(List::size).sum();
    }

    public Map<String, List<Long>> toIdMap() {
        Map<String, List<Long>> map = new LinkedHashMap<>();
        sections.forEach((section, questions) ->
                map.put(section, questions.stream().map(PreviewQuestion::questionId).toList()));
        return map;
    }

    public record PreviewQuestion(
            Long questionId,
            String content,
            String questionType,
            String jlptLevel,
            String skillType
    ) implements Serializable {}
}
