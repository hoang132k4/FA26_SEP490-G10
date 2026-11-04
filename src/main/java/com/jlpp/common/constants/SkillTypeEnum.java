package com.jlpp.common.constants;

import java.util.List;
import java.util.Locale;

/**
 * Five fixed lesson skills of the JEPS curriculum. Each lesson belongs to exactly one skill.
 * The enum name is what gets persisted in {@code lessons.skill_type} / {@code lessons.lesson_type}.
 */
public enum SkillTypeEnum {

    VOCABULARY("Từ vựng", "語", "bi-translate"),
    GRAMMAR("Ngữ pháp", "文", "bi-diagram-3"),
    KANJI("Hán tự", "漢", "bi-brush"),
    LISTENING("Nghe", "聴", "bi-headphones"),
    READING("Đọc hiểu", "読", "bi-journal-text");

    private final String label;
    private final String glyph;
    private final String icon;

    SkillTypeEnum(String label, String glyph, String icon) {
        this.label = label;
        this.glyph = glyph;
        this.icon = icon;
    }

    /** Value persisted in the database. */
    public String getValue() {
        return name();
    }

    /** Vietnamese display label. */
    public String getLabel() {
        return label;
    }

    /** Single kanji used as the skill marker in the UI. */
    public String getGlyph() {
        return glyph;
    }

    public String getIcon() {
        return icon;
    }

    /** Skills always render in this order. */
    public static List<SkillTypeEnum> orderedSkills() {
        return List.of(values());
    }

    /** Returns the skill matching the request parameter, or {@code null} when absent/unknown. */
    public static SkillTypeEnum fromParam(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        for (SkillTypeEnum skill : values()) {
            if (skill.name().equals(normalized)) {
                return skill;
            }
        }
        return null;
    }
}
