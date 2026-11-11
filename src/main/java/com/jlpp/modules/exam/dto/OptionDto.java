package com.jlpp.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionDto {
    private Long id;
    private String label;
    private String text;
    private Boolean correct; // null/false during exam, populated true/false during review

    public String getDisplayLabel() {
        if (label == null || label.isBlank()) {
            return "";
        }
        String trimmed = label.trim();
        return trimmed.endsWith(".") ? trimmed : trimmed + ".";
    }

    public String getCleanText() {
        return text != null ? text.trim() : "";
    }
}
