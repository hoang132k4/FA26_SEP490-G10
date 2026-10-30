package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private Long id;
    private Integer points;
    private String category;
    private String skillGroup;      // VOCAB_GRAMMAR, READING, LISTENING
    private String skillGroupLabel; // e.g. "Từ vựng • Ngữ pháp • Kanji", "Đọc hiểu", "Nghe hiểu"
    private String content;
    private String audioUrl;
    private String explanation;
    private Integer orderIndex;
    private Long userAnswerId;
    private Boolean isUserCorrect;

    @Builder.Default
    private List<OptionDto> options = new ArrayList<>();
}
