package com.jlpp.modules.order.service.impl;

import com.jlpp.modules.order.dto.OrderDetailResponse;
import com.jlpp.modules.order.dto.OrderHistoryItemResponse;
import com.jlpp.modules.order.dto.OrderItemDetailResponse;
import com.jlpp.modules.order.entity.Order;
import com.jlpp.modules.order.entity.OrderItem;
import com.jlpp.modules.order.entity.OrderStatus;
import com.jlpp.modules.order.repository.OrderRepository;
import com.jlpp.modules.order.service.OrderService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryItemResponse> getOrderHistory(String userIdentifier, String statusFilter) {
        User user = findUserByIdentifier(userIdentifier);

        OrderStatus status = OrderStatus.fromString(statusFilter);
        List<Order> orders;

        if (status != null) {
            orders = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDescIdDesc(user.getId(), status);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        }

        return orders.stream()
                .map(this::mapToHistoryItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailByOrderNumber(String userIdentifier, String orderNumber) {
        User user = findUserByIdentifier(userIdentifier);

        Order order = orderRepository.findByOrderNumberAndUserIdWithItems(orderNumber.trim(), user.getId())
                .orElseGet(() -> {
                    // Check if order exists but belongs to another user (Ownership check GB-27)
                    if (orderRepository.existsByOrderNumber(orderNumber.trim())) {
                        log.warn("Security alert: User {} attempted to access foreign order {}", userIdentifier, orderNumber);
                        throw new SecurityException("Bạn không có quyền truy cập thông tin đơn hàng này.");
                    }
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderNumber);
                });

        return mapToDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailById(String userIdentifier, Long orderId) {
        User user = findUserByIdentifier(userIdentifier);

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseGet(() -> {
                    if (orderRepository.existsById(orderId)) {
                        log.warn("Security alert: User {} attempted to access foreign order ID {}", userIdentifier, orderId);
                        throw new SecurityException("Bạn không có quyền truy cập thông tin đơn hàng này.");
                    }
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng với mã ID: " + orderId);
                });

        return mapToDetailResponse(order);
    }

    private User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Thông tin định danh người dùng không hợp lệ.");
        }
        return userRepository.findByEmailIgnoreCase(identifier.trim())
                .or(() -> userRepository.findByUsername(identifier.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản: " + identifier));
    }

    private OrderHistoryItemResponse mapToHistoryItem(Order order) {
        String dateFormatted = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_FORMATTER)
                : "N/A";

        String itemSummary = "Khóa học trực tuyến";
        int itemCount = 0;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            itemCount = order.getItems().size();
            String firstItemName = order.getItems().get(0).getItemName();
            if (itemCount > 1) {
                itemSummary = firstItemName + " (+" + (itemCount - 1) + " khóa học khác)";
            } else {
                itemSummary = firstItemName;
            }
        }

        return OrderHistoryItemResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDateFormatted(dateFormatted)
                .totalAmount(order.getTotalAmount())
                .totalAmountFormatted(formatCurrency(order.getTotalAmount()))
                .orderStatus(order.getOrderStatus())
                .statusVietnamese(order.getOrderStatus().getVietnameseName())
                .statusEnglish(order.getOrderStatus().getEnglishName())
                .statusBadgeClass(order.getOrderStatus().getBadgeClass())
                .itemSummary(itemSummary)
                .itemCount(itemCount)
                .build();
    }

    private OrderDetailResponse mapToDetailResponse(Order order) {
        String dateTimeFormatted = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_TIME_FORMATTER)
                : "N/A";

        List<OrderItemDetailResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::mapToOrderItemDetail).collect(Collectors.toList())
                : Collections.emptyList();

        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDateFormatted(dateTimeFormatted)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "VNPay QR")
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus() : "SUCCESS")
                .orderStatus(order.getOrderStatus())
                .statusVietnamese(order.getOrderStatus().getVietnameseName())
                .statusEnglish(order.getOrderStatus().getEnglishName())
                .statusBadgeClass(order.getOrderStatus().getBadgeClass())
                .totalAmount(order.getTotalAmount())
                .totalAmountFormatted(formatCurrency(order.getTotalAmount()))
                .items(itemResponses)
                .build();
    }

    private OrderItemDetailResponse mapToOrderItemDetail(OrderItem item) {
        return OrderItemDetailResponse.builder()
                .id(item.getId())
                .courseId(item.getCourseId())
                .itemName(item.getItemName())
                .price(item.getPrice())
                .priceFormatted(formatCurrency(item.getPrice()))
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .subtotalFormatted(formatCurrency(item.getSubtotal()))
                .build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " VND";
    }
}
