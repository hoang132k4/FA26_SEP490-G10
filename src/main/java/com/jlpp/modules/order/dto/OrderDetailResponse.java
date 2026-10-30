package com.jlpp.modules.order.dto;

import com.jlpp.modules.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private Long id;
    private String orderNumber;
    private String orderDateFormatted;
    private String paymentMethod;
    private String paymentStatus;
    private OrderStatus orderStatus;
    private String statusVietnamese;
    private String statusEnglish;
    private String statusBadgeClass;
    private BigDecimal totalAmount;
    private String totalAmountFormatted;
    @Builder.Default
    private List<OrderItemDetailResponse> items = new ArrayList<>();
}
