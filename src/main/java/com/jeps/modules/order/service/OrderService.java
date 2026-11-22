package com.jeps.modules.order.service;

import com.jeps.modules.order.dto.OrderDetailResponse;
import com.jeps.modules.order.dto.OrderHistoryItemResponse;

import java.util.List;

public interface OrderService {

    List<OrderHistoryItemResponse> getOrderHistory(String userIdentifier, String statusFilter);

    OrderDetailResponse getOrderDetailByOrderNumber(String userIdentifier, String orderNumber);

    OrderDetailResponse getOrderDetailById(String userIdentifier, Long orderId);
}
