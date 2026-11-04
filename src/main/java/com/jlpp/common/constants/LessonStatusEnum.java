package com.jlpp.common.constants;

import java.util.List;
import java.util.Locale;

/**
 * Publication state of a lesson. The database enforces a CHECK constraint that only allows
 * {@code ACTIVE} and {@code INACTIVE} (see {@code lessons_status_check}).
 */
public enum LessonStatusEnum {

    ACTIVE("Hiển thị"),
    INACTIVE("Đang ẩn");

    private final String label;

    LessonStatusEnum(String label) {
        this.label = label;
    }

    public String getValue() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public boolean isVisible() {
        return this == ACTIVE;
    }

    public static List<LessonStatusEnum> orderedStatuses() {
        return List.of(values());
    }

    /** Unknown or blank values fall back to {@link #ACTIVE} because the column is NOT NULL. */
    public static LessonStatusEnum fromParam(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ACTIVE;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        for (LessonStatusEnum status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return ACTIVE;
    }

    public LessonStatusEnum toggled() {
        return this == ACTIVE ? INACTIVE : ACTIVE;
    }
}
