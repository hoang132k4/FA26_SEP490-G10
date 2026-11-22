package com.jeps.modules.order;

import com.jeps.modules.order.dto.OrderDetailResponse;
import com.jeps.modules.order.dto.OrderHistoryItemResponse;
import com.jeps.modules.order.entity.Order;
import com.jeps.modules.order.entity.OrderItem;
import com.jeps.modules.order.entity.OrderStatus;
import com.jeps.modules.order.repository.OrderRepository;
import com.jeps.modules.order.service.impl.OrderServiceImpl;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
    @DisplayName("Boundary: Học viên chưa có đơn hàng nào trả về danh sách rỗng (Screen 16)")
    void getOrderHistory_emptyHistory_returnsEmptyList() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(20L)).thenReturn(Collections.emptyList());

        List<OrderHistoryItemResponse> list = orderService.getOrderHistory("learner@jeps.edu.vn", "ALL");

        assertNotNull(list);
        assertTrue(list.isEmpty());
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
    @DisplayName("Abnormal: Lấy lịch sử đơn hàng với identifier rỗng ném ra IllegalArgumentException")
    void getOrderHistory_blankIdentifier_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderHistory("", null));
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderHistory(null, null));
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

    @Test
    @DisplayName("Normal: Lấy chi tiết đơn hàng theo order ID thành công")
    void getOrderDetailById_success() {
        User user = createSampleUser();
        Order order = createSampleOrder(user, "ORD-20260210-001", OrderStatus.COMPLETED);

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserIdWithItems(101L, 20L)).thenReturn(Optional.of(order));

        OrderDetailResponse res = orderService.getOrderDetailById("learner@jeps.edu.vn", 101L);

        assertNotNull(res);
        assertEquals(101L, res.getId());
        assertEquals("ORD-20260210-001", res.getOrderNumber());
    }

    @Test
    @DisplayName("Abnormal: getOrderDetailById khi mã ID không tồn tại ném ra IllegalArgumentException")
    void getOrderDetailById_notFound() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserIdWithItems(999L, 20L)).thenReturn(Optional.empty());
        when(orderRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.getOrderDetailById("learner@jeps.edu.vn", 999L));
    }

    @Test
    @DisplayName("Abnormal: getOrderDetailById khi đơn hàng thuộc người khác ném ra SecurityException")
    void getOrderDetailById_foreignOrder_throwsSecurityException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserIdWithItems(888L, 20L)).thenReturn(Optional.empty());
        when(orderRepository.existsById(888L)).thenReturn(true);

        assertThrows(SecurityException.class,
                () -> orderService.getOrderDetailById("learner@jeps.edu.vn", 888L));
    }

    @Test
    @DisplayName("Normal: Đơn hàng có nhiều hơn 1 mặt hàng hiển thị tóm tắt (+N khóa học khác)")
    void mapToHistoryItem_multipleItems_summarizedCorrectly() {
        User user = createSampleUser();
        Order order = createSampleOrder(user, "ORD-MULTI-001", OrderStatus.COMPLETED);

        OrderItem item2 = OrderItem.builder()
                .order(order)
                .courseId(4L)
                .itemName("Khóa học Nghe Hiểu Choukai N3")
                .price(new BigDecimal("499000"))
                .quantity(1)
                .subtotal(new BigDecimal("499000"))
                .build();
        order.addItem(item2);

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(20L)).thenReturn(List.of(order));

        List<OrderHistoryItemResponse> list = orderService.getOrderHistory("learner@jeps.edu.vn", null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Khóa học Chinh Phục JLPT N3 (+1 khóa học khác)", list.get(0).getItemSummary());
        assertEquals(2, list.get(0).getItemCount());
    }

    @Test
    @DisplayName("Boundary: Đơn hàng thiếu thông tin phương thức hoặc trạng thái thanh toán hiển thị mặc định")
    void mapToDetailResponse_emptyPaymentInfo_defaultsCorrectly() {
        User user = createSampleUser();
        Order order = createSampleOrder(user, "ORD-EMPTY-PAY", OrderStatus.PROCESSING);
        order.setPaymentMethod(null);
        order.setPaymentStatus("");
        order.setCreatedAt(null);

        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserIdWithItems("ORD-EMPTY-PAY", 20L)).thenReturn(Optional.of(order));

        OrderDetailResponse res = orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-EMPTY-PAY");

        assertNotNull(res);
        assertEquals("Chưa có thông tin", res.getPaymentMethod());
        assertEquals("Chưa có thông tin", res.getPaymentStatus());
        assertEquals("N/A", res.getOrderDateFormatted());
    }
}
