package com.jlpp.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetailResponse {

    private Long id;
    private Long courseId;
    private String itemName;
    private BigDecimal price;
    private String priceFormatted;
    private Integer quantity;
    private BigDecimal subtotal;
    private String subtotalFormatted;
}
