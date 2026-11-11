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
    private String section;
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

    public String getUserAnswerDisplay() {
        if (userAnswerId == null) {
            return "(Chưa chọn)";
        }
        if (options != null) {
            for (OptionDto opt : options) {
                if (opt.getId() != null && opt.getId().equals(userAnswerId)) {
                    String prefix = opt.getDisplayLabel();
                    if (!prefix.isEmpty()) prefix += " ";
                    return prefix + opt.getCleanText();
                }
            }
        }
        return "(Chưa chọn)";
    }

    public String getCorrectAnswerDisplay() {
        if (options != null) {
            for (OptionDto opt : options) {
                if (Boolean.TRUE.equals(opt.getCorrect())) {
                    String prefix = opt.getDisplayLabel();
                    if (!prefix.isEmpty()) prefix += " ";
                    return prefix + opt.getCleanText();
                }
            }
        }
        return "";
    }
}
