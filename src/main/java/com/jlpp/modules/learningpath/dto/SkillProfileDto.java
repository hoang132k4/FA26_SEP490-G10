package com.jlpp.modules.learningpath.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillProfileDto {
    private Long userId;
    private Long resultId;
    private String assessmentTitle;
    private String jlptLevel;
    private BigDecimal overallAccuracy;
    private String strongestSkillName;
    private String weakestSkillName;
    private boolean assessmentTaken;

    @Builder.Default
    private Map<String, SkillMetricDto> skills = new LinkedHashMap<>();

    @Builder.Default
    private List<String> weakSkills = new ArrayList<>();

    @Builder.Default
    private List<String> strongSkills = new ArrayList<>();

    @Builder.Default
    private List<String> mediumSkills = new ArrayList<>();

    public SkillMetricDto getSkill(String skillKey) {
        if (skillKey == null) return null;
        SkillMetricDto direct = skills.get(skillKey.toUpperCase());
        if (direct != null) return direct;
        if ("VOCABULARY".equalsIgnoreCase(skillKey) || "GRAMMAR".equalsIgnoreCase(skillKey) || "KANJI".equalsIgnoreCase(skillKey)) {
            return skills.get("VOCAB_GRAMMAR");
        }
        return null;
    }

    public SkillProficiency getProficiencyForSkill(String skillKey) {
        if (skillKey == null) return SkillProficiency.UNKNOWN;
        SkillMetricDto metric = skills.get(skillKey.toUpperCase());
        if (metric != null && metric.getProficiency() != SkillProficiency.UNKNOWN) {
            return metric.getProficiency();
        }
        // Fallback for compound VOCAB_GRAMMAR / combined language knowledge
        if ("VOCABULARY".equalsIgnoreCase(skillKey) || "GRAMMAR".equalsIgnoreCase(skillKey) || "KANJI".equalsIgnoreCase(skillKey)) {
            SkillMetricDto compound = skills.get("VOCAB_GRAMMAR");
            if (compound != null && compound.getProficiency() != SkillProficiency.UNKNOWN) {
                return compound.getProficiency();
            }
            // Check combined from display group
            for (SkillMetricDto dg : getDisplaySkillGroups()) {
                if ("LANGUAGE_KNOWLEDGE".equalsIgnoreCase(dg.getSkillKey()) && dg.getProficiency() != SkillProficiency.UNKNOWN) {
                    return dg.getProficiency();
                }
            }
        }
        return metric != null ? metric.getProficiency() : SkillProficiency.UNKNOWN;
    }

    /**
     * Trả về đúng 3 nhóm năng lực JLPT chuẩn:
     * 1. Kiến thức ngôn ngữ (Ngữ pháp, Từ vựng, Chữ Hán)
     * 2. Đọc hiểu (Reading)
     * 3. Nghe hiểu (Listening)
     */
    public List<SkillMetricDto> getDisplaySkillGroups() {
        List<SkillMetricDto> list = new ArrayList<>();

        // Group 1: Kiến thức ngôn ngữ (Ngữ pháp, Từ vựng, Chữ Hán)
        SkillMetricDto kanji = skills.get("KANJI");
        SkillMetricDto vocab = skills.get("VOCABULARY");
        SkillMetricDto grammar = skills.get("GRAMMAR");
        SkillMetricDto vocabGrammar = skills.get("VOCAB_GRAMMAR");

        int totalQuestions = 0;
        int correctCount = 0;
        boolean hasTested = false;

        if (vocabGrammar != null && vocabGrammar.isEvidenceAvailable()) {
            totalQuestions = vocabGrammar.getTotalQuestions();
            correctCount = vocabGrammar.getCorrectCount();
            hasTested = true;
        } else {
            for (SkillMetricDto m : List.of(grammar, vocab, kanji)) {
                if (m != null && m.isEvidenceAvailable()) {
                    totalQuestions += m.getTotalQuestions();
                    correctCount += m.getCorrectCount();
                    hasTested = true;
                }
            }
        }

        SkillProficiency prof;
        BigDecimal pct = null;
        if (hasTested && totalQuestions > 0) {
            pct = BigDecimal.valueOf((correctCount * 100.0) / totalQuestions).setScale(1, RoundingMode.HALF_UP);
            if (pct.doubleValue() < 50.0) {
                prof = SkillProficiency.WEAK;
            } else if (pct.doubleValue() >= 70.0) {
                prof = SkillProficiency.STRONG;
            } else {
                prof = SkillProficiency.MEDIUM;
            }
        } else {
            prof = SkillProficiency.UNKNOWN;
        }

        list.add(SkillMetricDto.builder()
                .skillKey("LANGUAGE_KNOWLEDGE")
                .skillName("Kiến thức ngôn ngữ (Ngữ pháp, Từ vựng, Chữ Hán)")
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .accuracyPercentage(pct)
                .proficiency(prof)
                .evidenceAvailable(hasTested && totalQuestions > 0)
                .build());

        // Group 2: Đọc hiểu (Reading)
        SkillMetricDto reading = skills.get("READING");
        if (reading != null) {
            list.add(reading);
        } else {
            list.add(SkillMetricDto.builder()
                    .skillKey("READING")
                    .skillName("Đọc hiểu (Reading)")
                    .totalQuestions(0)
                    .correctCount(0)
                    .accuracyPercentage(null)
                    .proficiency(SkillProficiency.UNKNOWN)
                    .evidenceAvailable(false)
                    .build());
        }

        // Group 3: Nghe hiểu (Listening)
        SkillMetricDto listening = skills.get("LISTENING");
        if (listening != null) {
            list.add(listening);
        } else {
            list.add(SkillMetricDto.builder()
                    .skillKey("LISTENING")
                    .skillName("Nghe hiểu (Listening)")
                    .totalQuestions(0)
                    .correctCount(0)
                    .accuracyPercentage(null)
                    .proficiency(SkillProficiency.UNKNOWN)
                    .evidenceAvailable(false)
                    .build());
        }

        return list;
    }
}

