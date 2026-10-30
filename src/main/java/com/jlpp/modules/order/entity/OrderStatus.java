package com.jlpp.modules.order.entity;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PROCESSING("Đang xử lý", "Processing", "badge-processing", "#D97706", "#FEF3C7"),
    COMPLETED("Hoàn thành", "Completed", "badge-completed", "#15803D", "#DCFCE7"),
    CANCELLED("Đã hủy", "Cancelled", "badge-cancelled", "#DC2626", "#FEE2E2");

    private final String vietnameseName;
    private final String englishName;
    private final String badgeClass;
    private final String textColor;
    private final String bgColor;

    OrderStatus(String vietnameseName, String englishName, String badgeClass, String textColor, String bgColor) {
        this.vietnameseName = vietnameseName;
        this.englishName = englishName;
        this.badgeClass = badgeClass;
        this.textColor = textColor;
        this.bgColor = bgColor;
    }

    public static OrderStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (OrderStatus os : values()) {
            if (os.name().equalsIgnoreCase(status.trim())) {
                return os;
            }
        }
        return null;
    }
}
