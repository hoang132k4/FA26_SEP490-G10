package com.jlpp.modules.order;

import com.jlpp.modules.order.dto.OrderDetailResponse;
import com.jlpp.modules.order.dto.OrderHistoryItemResponse;
import com.jlpp.modules.order.entity.Order;
import com.jlpp.modules.order.entity.OrderItem;
import com.jlpp.modules.order.entity.OrderStatus;
import com.jlpp.modules.order.repository.OrderRepository;
import com.jlpp.modules.order.service.impl.OrderServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: OrderService (Screens 15 & 16 - Order Details & History)")
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, userRepository);
    }

    private User createSampleUser() {
        User user = User.builder()
                .email("learner@jeps.edu.vn")
                .fullName("Nguyen Van Hoc Vien")
                .build();
        user.setId(20L);
        return user;
    }

    private Order createSampleOrder(User user, String orderNumber, OrderStatus status) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .totalAmount(new BigDecimal("999000"))
                .orderStatus(status)
                .paymentMethod("VNPAY")
                .paymentStatus("PAID")
                .build();
        order.setId(101L);
        order.setCreatedAt(LocalDateTime.of(2026, 2, 10, 14, 0));

        OrderItem item = OrderItem.builder()
                .order(order)
                .courseId(3L)
                .itemName("Khóa học Chinh Phục JLPT N3")
                .price(new BigDecimal("999000"))
                .quantity(1)
                .subtotal(new BigDecimal("999000"))
                .build();
        item.setId(201L);

        order.addItem(item);
        return order;
    }

    @Test
    @DisplayName("Normal: Lấy lịch sử đơn hàng của học viên (Screen 16)")
    void getOrderHistory_allOrders_success() {
        User user = createSampleUser();
        Order order1 = createSampleOrder(user, "ORD-20260210-001", OrderStatus.COMPLETED);
        Order order2 = createSampleOrder(user, "ORD-20260210-002", OrderStatus.PROCESSING);

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(20L)).thenReturn(List.of(order1, order2));

        List<OrderHistoryItemResponse> list = orderService.getOrderHistory("learner@jeps.edu.vn", null);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("ORD-20260210-001", list.get(0).getOrderNumber());
        assertEquals("Hoàn thành", list.get(0).getStatusVietnamese());
        assertEquals("ORD-20260210-002", list.get(1).getOrderNumber());
        assertEquals("Đang xử lý", list.get(1).getStatusVietnamese());
    }

    @Test
    @DisplayName("Normal: Lọc lịch sử đơn hàng theo trạng thái COMPLETED (Screen 16)")
    void getOrderHistory_filterCompleted_success() {
        User user = createSampleUser();
        Order order1 = createSampleOrder(user, "ORD-20260210-001", OrderStatus.COMPLETED);

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDescIdDesc(20L, OrderStatus.COMPLETED))
                .thenReturn(List.of(order1));

        List<OrderHistoryItemResponse> list = orderService.getOrderHistory("learner@jeps.edu.vn", "COMPLETED");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("ORD-20260210-001", list.get(0).getOrderNumber());
    }

    @Test
    @DisplayName("Normal: Lấy chi tiết đơn hàng theo orderNumber (Screen 15)")
    void getOrderDetailByOrderNumber_success() {
        User user = createSampleUser();
        Order order = createSampleOrder(user, "ORD-20260210-001", OrderStatus.COMPLETED);

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserIdWithItems("ORD-20260210-001", 20L)).thenReturn(Optional.of(order));

        OrderDetailResponse res = orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-20260210-001");

        assertNotNull(res);
        assertEquals("ORD-20260210-001", res.getOrderNumber());
        assertEquals("Hoàn thành", res.getStatusVietnamese());
        assertEquals(1, res.getItems().size());
        assertEquals("Khóa học Chinh Phục JLPT N3", res.getItems().get(0).getItemName());
    }

    @Test
    @DisplayName("Abnormal: Đơn hàng không tồn tại ném ra IllegalArgumentException (Screen 15)")
    void getOrderDetailByOrderNumber_notFound() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserIdWithItems(anyString(), anyLong())).thenReturn(Optional.empty());
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-INVALID"));
    }

    @Test
    @DisplayName("Abnormal: Học viên không sở hữu đơn hàng (truy cập trái phép) ném ra SecurityException (Screen 15)")
    void getOrderDetailByOrderNumber_unauthorizedUser_throwsSecurityException() {
        User user = createSampleUser(); // ID = 20

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserIdWithItems("ORD-OTHER-001", 20L)).thenReturn(Optional.empty());
        when(orderRepository.existsByOrderNumber("ORD-OTHER-001")).thenReturn(true);

        assertThrows(SecurityException.class,
                () -> orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-OTHER-001"));
    }
}
