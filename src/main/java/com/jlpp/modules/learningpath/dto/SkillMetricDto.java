package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMetricDto {
    private String skillKey;           // VOCABULARY, GRAMMAR, KANJI, READING, LISTENING
    private String skillName;          // e.g. "Từ vựng (Vocabulary)", "Ngữ pháp (Grammar)"
    private int totalQuestions;
    private int correctCount;
    private BigDecimal accuracyPercentage;
    private SkillProficiency proficiency;
    private boolean evidenceAvailable;
}

