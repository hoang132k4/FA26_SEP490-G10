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
}
