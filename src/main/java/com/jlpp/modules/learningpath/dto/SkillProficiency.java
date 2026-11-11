package com.jlpp.modules.learningpath.dto;

public enum SkillProficiency {
    WEAK("Cần cải thiện", "badge-weak", "bg-danger text-white"),
    MEDIUM("Đạt yêu cầu", "badge-medium", "bg-warning text-dark"),
    STRONG("Điểm mạnh", "badge-strong", "bg-success text-white"),
    UNKNOWN("Chưa có dữ liệu", "badge-unknown", "bg-secondary text-white");

    private final String label;
    private final String cssClass;
    private final String badgeBootstrapClass;

    SkillProficiency(String label, String cssClass, String badgeBootstrapClass) {
        this.label = label;
        this.cssClass = cssClass;
        this.badgeBootstrapClass = badgeBootstrapClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getBadgeBootstrapClass() {
        return badgeBootstrapClass;
    }
}

