package com.jlpp.modules.order.dto;

import com.jlpp.modules.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryItemResponse {

    private Long id;
    private String orderNumber;
    private String orderDateFormatted;
    private BigDecimal totalAmount;
    private String totalAmountFormatted;
    private OrderStatus orderStatus;
    private String statusVietnamese;
    private String statusEnglish;
    private String statusBadgeClass;
    private String itemSummary;
    private int itemCount;
}
