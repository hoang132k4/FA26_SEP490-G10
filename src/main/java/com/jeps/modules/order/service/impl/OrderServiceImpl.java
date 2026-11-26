package com.jeps.modules.order.service.impl;

import com.jeps.modules.order.dto.OrderDetailResponse;
import com.jeps.modules.order.dto.OrderHistoryItemResponse;
import com.jeps.modules.order.dto.OrderItemDetailResponse;
import com.jeps.modules.order.entity.Order;
import com.jeps.modules.order.entity.OrderItem;
import com.jeps.modules.order.entity.OrderStatus;
import com.jeps.modules.order.repository.OrderRepository;
import com.jeps.modules.order.service.OrderService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.jeps.modules.course.repository.CourseRegistrationRepository registrationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    @Override
    @Transactional
    public List<OrderHistoryItemResponse> getOrderHistory(String userIdentifier, String statusFilter) {
        User user = findUserByIdentifier(userIdentifier);
        syncOrdersFromRegistrations(user);

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

    private void syncOrdersFromRegistrations(User user) {
        if (registrationRepository == null || user == null) {
            return;
        }
        try {
            var registrations = registrationRepository.findByUserIdWithCourse(user.getId());
            if (registrations == null || registrations.isEmpty()) {
                return;
            }

            var existingOrders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());
            java.util.Set<Long> orderedCourseIds = new java.util.HashSet<>();
            if (existingOrders != null) {
                for (Order o : existingOrders) {
                    if (o.getItems() != null) {
                        for (OrderItem item : o.getItems()) {
                            if (item.getCourseId() != null) {
                                orderedCourseIds.add(item.getCourseId());
                            }
                        }
                    }
                }
            }

            for (var reg : registrations) {
                if (reg.getCourse() != null && !orderedCourseIds.contains(reg.getCourse().getId())) {
                    java.math.BigDecimal amount = java.math.BigDecimal.valueOf(reg.getPaidAmount() != null ? reg.getPaidAmount() : 0.0);
                    String orderNumber = "ORD-" + (reg.getRegisteredAt() != null ? reg.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "2026")
                            + "-" + reg.getId() + String.format("%03d", (int)(Math.random() * 900 + 100));

                    Order order = Order.builder()
                            .orderNumber(orderNumber)
                            .user(user)
                            .totalAmount(amount)
                            .orderStatus(OrderStatus.COMPLETED)
                            .paymentMethod(reg.getPaymentMethod() != null ? reg.getPaymentMethod() : "FREE")
                            .paymentStatus("PAID")
                            .transactionReference("TXN-" + reg.getId())
                            .build();

                    OrderItem item = OrderItem.builder()
                            .order(order)
                            .courseId(reg.getCourse().getId())
                            .itemName(reg.getCourse().getTitle())
                            .price(amount)
                            .quantity(1)
                            .subtotal(amount)
                            .build();

                    order.addItem(item);
                    if (reg.getRegisteredAt() != null) {
                        order.setCreatedAt(reg.getRegisteredAt());
                    }
                    orderRepository.save(order);
                    orderedCourseIds.add(reg.getCourse().getId());
                    log.info("Synced missing order {} for user {} and registered course {}", orderNumber, user.getId(), reg.getCourse().getId());
                }
            }
        } catch (Exception ex) {
            log.warn("Could not sync orders from course registrations: {}", ex.getMessage());
        }
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

        String paymentMethod = (order.getPaymentMethod() != null && !order.getPaymentMethod().isBlank())
                ? order.getPaymentMethod()
                : "Chưa có thông tin";

        String paymentStatus = (order.getPaymentStatus() != null && !order.getPaymentStatus().isBlank())
                ? order.getPaymentStatus()
                : "Chưa có thông tin";

        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDateFormatted(dateTimeFormatted)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
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
