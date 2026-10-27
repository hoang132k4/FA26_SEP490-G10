package com.jlpp.modules.order;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.order.dto.OrderDetailResponse;
import com.jlpp.modules.order.dto.OrderHistoryItemResponse;
import com.jlpp.modules.order.entity.Order;
import com.jlpp.modules.order.entity.OrderItem;
import com.jlpp.modules.order.entity.OrderStatus;
import com.jlpp.modules.order.repository.OrderItemRepository;
import com.jlpp.modules.order.repository.OrderRepository;
import com.jlpp.modules.order.service.OrderService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderService orderService;

    private static final String LEARNER_A_EMAIL = "learner_a_order@jeps.edu.vn";
    private static final String LEARNER_B_EMAIL = "learner_b_order@jeps.edu.vn";

    private User learnerA;
    private User learnerB;
    private Order orderA1;
    private Order orderA2;
    private Order orderB1;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        userRepository.findByEmailIgnoreCase(LEARNER_A_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(LEARNER_B_EMAIL).ifPresent(userRepository::delete);

        learnerA = userRepository.save(User.builder()
                .email(LEARNER_A_EMAIL)
                .username("learner_a_order")
                .fullName("Nguyễn Học Viên A")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .phone("0987654321")
                .build());

        learnerB = userRepository.save(User.builder()
                .email(LEARNER_B_EMAIL)
                .username("learner_b_order")
                .fullName("Trần Học Viên B")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .phone("0912345678")
                .build());

        // Order 1 for Learner A (Completed, 2 items)
        orderA1 = Order.builder()
                .orderNumber("ORD-20261009-001")
                .user(learnerA)
                .totalAmount(new BigDecimal("2500000"))
                .orderStatus(OrderStatus.COMPLETED)
                .paymentMethod("VNPay QR")
                .paymentStatus("SUCCESS")
                .transactionReference("VNP-1001")
                .build();
        orderA1.setCreatedAt(LocalDateTime.of(2026, 10, 9, 14, 25));

        OrderItem item1 = OrderItem.builder()
                .courseId(101L)
                .itemName("JLPT N4 Full Course")
                .price(new BigDecimal("2000000"))
                .quantity(1)
                .subtotal(new BigDecimal("2000000"))
                .build();
        orderA1.addItem(item1);

        OrderItem item2 = OrderItem.builder()
                .courseId(102L)
                .itemName("Flashcard Kanji N4 Cấp tốc")
                .price(new BigDecimal("500000"))
                .quantity(1)
                .subtotal(new BigDecimal("500000"))
                .build();
        orderA1.addItem(item2);
        orderRepository.save(orderA1);

        // Order 2 for Learner A (Cancelled)
        orderA2 = Order.builder()
                .orderNumber("ORD-20260801-003")
                .user(learnerA)
                .totalAmount(new BigDecimal("3000000"))
                .orderStatus(OrderStatus.CANCELLED)
                .paymentMethod("Chuyển khoản Ngân hàng (VietQR)")
                .paymentStatus("CANCELLED")
                .transactionReference("VQR-8003")
                .build();
        orderA2.setCreatedAt(LocalDateTime.of(2026, 8, 1, 9, 0));

        OrderItem item3 = OrderItem.builder()
                .courseId(103L)
                .itemName("Combo Toàn diện JLPT N5 - N3")
                .price(new BigDecimal("3000000"))
                .quantity(1)
                .subtotal(new BigDecimal("3000000"))
                .build();
        orderA2.addItem(item3);
        orderRepository.save(orderA2);

        // Order for Learner B (Processing)
        orderB1 = Order.builder()
                .orderNumber("ORD-20261015-099")
                .user(learnerB)
                .totalAmount(new BigDecimal("1500000"))
                .orderStatus(OrderStatus.PROCESSING)
                .paymentMethod("VNPay QR")
                .paymentStatus("PENDING")
                .build();
        orderB1.setCreatedAt(LocalDateTime.of(2026, 10, 15, 11, 0));

        OrderItem itemB = OrderItem.builder()
                .courseId(104L)
                .itemName("Khóa học Ngữ pháp N3")
                .price(new BigDecimal("1500000"))
                .quantity(1)
                .subtotal(new BigDecimal("1500000"))
                .build();
        orderB1.addItem(itemB);
        orderRepository.save(orderB1);
    }

    // ==========================================
    // TRACK ORDER HISTORY (UC-23) TESTS
    // ==========================================

    @Test
    @DisplayName("OH-01: Anonymous user viewing /orders redirects to /auth/login")
    void orderHistory_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("OH-02: Authenticated Learner views own order history successfully")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderHistory_authenticatedLearner_displaysOwnOrders() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/order/history"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("hasOrders", true))
                .andExpect(content().string(containsString("ORD-20261009-001")))
                .andExpect(content().string(containsString("ORD-20260801-003")))
                .andExpect(content().string(containsString("2,500,000 VND")))
                .andExpect(content().string(containsString("Completed")))
                .andExpect(content().string(containsString("Cancelled")))
                // Ensure Learner B's order is strictly hidden
                .andExpect(content().string(not(containsString("ORD-20261015-099"))));
    }

    @Test
    @DisplayName("OH-03: Filter orders by status 'COMPLETED' displays only completed orders")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderHistory_filterCompleted_displaysCompletedOnly() throws Exception {
        mockMvc.perform(get("/orders").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-20261009-001")))
                .andExpect(content().string(not(containsString("ORD-20260801-003"))));
    }

    @Test
    @DisplayName("OH-04: Filter orders by status 'CANCELLED' displays only cancelled orders")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderHistory_filterCancelled_displaysCancelledOnly() throws Exception {
        mockMvc.perform(get("/orders").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-20260801-003")))
                .andExpect(content().string(not(containsString("ORD-20261009-001"))));
    }

    @Test
    @DisplayName("OH-05: Empty state displayed when user has no orders for a filter")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderHistory_noOrdersForStatus_displaysEmptyState() throws Exception {
        mockMvc.perform(get("/orders").param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasOrders", false))
                .andExpect(content().string(containsString("Không tìm thấy đơn hàng")));
    }

    @Test
    @DisplayName("OH-06: Legacy /order/history route gracefully redirects to /orders")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void legacyOrderHistory_redirectsToOrders() throws Exception {
        mockMvc.perform(get("/order/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
    }

    // ==========================================
    // VIEW ORDERS DETAIL (UC-24) TESTS
    // ==========================================

    @Test
    @DisplayName("OD-01: Anonymous user viewing /orders/{orderNumber} redirects to /auth/login")
    void orderDetail_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/orders/ORD-20261009-001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("OD-02: Authenticated Learner views own order details successfully with items and payment info")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderDetail_authenticatedLearner_displaysCompleteDetails() throws Exception {
        mockMvc.perform(get("/orders/ORD-20261009-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/order/detail"))
                .andExpect(model().attributeExists("order"))
                .andExpect(content().string(containsString("ORD-20261009-001")))
                .andExpect(content().string(containsString("VNPay QR")))
                .andExpect(content().string(containsString("Completed")))
                .andExpect(content().string(containsString("JLPT N4 Full Course")))
                .andExpect(content().string(containsString("Flashcard Kanji N4 Cấp tốc")))
                .andExpect(content().string(containsString("Total: 2,500,000 VND")));
    }

    @Test
    @DisplayName("OD-03: Learner A cannot view Learner B's order (GB-27 Ownership Security)")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderDetail_crossUserAccess_deniedWithoutDataLeakage() throws Exception {
        // orderB1 belongs to Learner B
        mockMvc.perform(get("/orders/ORD-20261015-099"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", containsString("Bạn không có quyền truy cập thông tin đơn hàng này.")));
    }

    @Test
    @DisplayName("OD-04: Non-existent order number redirects to /orders with error message")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderDetail_nonExistentOrder_redirectsWithError() throws Exception {
        mockMvc.perform(get("/orders/ORD-NONEXISTENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", containsString("Không tìm thấy đơn hàng")));
    }

    // ==========================================
    // SERVICE LAYER DIRECT UNIT / INTEGRATION TESTS
    // ==========================================

    @Test
    @DisplayName("SRV-01: OrderService.getOrderHistory maps all fields and formats currency correctly")
    void orderService_getOrderHistory_success() {
        List<OrderHistoryItemResponse> history = orderService.getOrderHistory(LEARNER_A_EMAIL, "ALL");

        assertThat(history).hasSize(2);
        assertThat(history).extracting(OrderHistoryItemResponse::getOrderNumber)
                .containsExactlyInAnyOrder("ORD-20261009-001", "ORD-20260801-003");

        OrderHistoryItemResponse completedOrder = history.stream()
                .filter(o -> o.getOrderNumber().equals("ORD-20261009-001"))
                .findFirst().orElseThrow();
        assertThat(completedOrder.getTotalAmountFormatted()).isEqualTo("2,500,000 VND");
        assertThat(completedOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completedOrder.getItemSummary()).contains("JLPT N4 Full Course");
    }

    @Test
    @DisplayName("SRV-02: OrderService.getOrderDetailByOrderNumber throws SecurityException on cross-user access")
    void orderService_crossUserDetail_throwsSecurityException() {
        assertThatThrownBy(() -> orderService.getOrderDetailByOrderNumber(LEARNER_A_EMAIL, "ORD-20261015-099"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Bạn không có quyền truy cập thông tin đơn hàng này.");
    }

    @Test
    @DisplayName("SRV-03: OrderService.getOrderDetailByOrderNumber throws IllegalArgumentException for missing order")
    void orderService_missingOrder_throwsException() {
        assertThatThrownBy(() -> orderService.getOrderDetailByOrderNumber(LEARNER_A_EMAIL, "ORD-999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy đơn hàng");
    }

    @Test
    @DisplayName("OD-05: Fallback displays 'Chưa có thông tin' when paymentMethod and paymentStatus are null")
    @WithMockUser(username = LEARNER_A_EMAIL, roles = {"LEARNER"})
    void orderDetail_nullPaymentInfo_displaysNeutralFallbackLabel() throws Exception {
        Order nullPaymentOrder = Order.builder()
                .orderNumber("ORD-20261027-NULL")
                .user(learnerA)
                .totalAmount(new BigDecimal("990000"))
                .orderStatus(OrderStatus.PROCESSING)
                .paymentMethod(null)
                .paymentStatus(null)
                .build();
        nullPaymentOrder.setCreatedAt(LocalDateTime.of(2026, 10, 27, 9, 30));

        OrderItem item = OrderItem.builder()
                .courseId(201L)
                .itemName("Luyện thi N3 cấp tốc")
                .price(new BigDecimal("990000"))
                .quantity(1)
                .subtotal(new BigDecimal("990000"))
                .build();
        nullPaymentOrder.addItem(item);
        orderRepository.save(nullPaymentOrder);

        mockMvc.perform(get("/orders/ORD-20261027-NULL"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/order/detail"))
                .andExpect(model().attributeExists("order"))
                .andExpect(content().string(containsString("ORD-20261027-NULL")))
                .andExpect(content().string(containsString("Chưa có thông tin")))
                .andExpect(content().string(not(containsString("VNPay QR"))));

        // Direct Service assertion
        OrderDetailResponse detail = orderService.getOrderDetailByOrderNumber(LEARNER_A_EMAIL, "ORD-20261027-NULL");
        assertThat(detail.getPaymentMethod()).isEqualTo("Chưa có thông tin");
        assertThat(detail.getPaymentStatus()).isEqualTo("Chưa có thông tin");
    }

    @Test
    @DisplayName("OD-06: Fallback displays 'Chưa có thông tin' when paymentMethod and paymentStatus are empty or blank")
    void orderDetail_blankPaymentInfo_displaysNeutralFallbackLabel() {
        Order blankPaymentOrder = Order.builder()
                .orderNumber("ORD-20261027-BLANK")
                .user(learnerA)
                .totalAmount(new BigDecimal("500000"))
                .orderStatus(OrderStatus.PROCESSING)
                .paymentMethod("   ")
                .paymentStatus("")
                .build();
        blankPaymentOrder.setCreatedAt(LocalDateTime.of(2026, 10, 27, 10, 0));
        orderRepository.save(blankPaymentOrder);

        OrderDetailResponse detail = orderService.getOrderDetailByOrderNumber(LEARNER_A_EMAIL, "ORD-20261027-BLANK");
        assertThat(detail.getPaymentMethod()).isEqualTo("Chưa có thông tin");
        assertThat(detail.getPaymentStatus()).isEqualTo("Chưa có thông tin");
    }

    @Test
    @DisplayName("SRV-04: OrderRepository eager fetches items with @EntityGraph avoiding N+1 queries without duplicates")
    void orderRepository_entityGraph_eagerFetchesItemsWithoutDuplicates() {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(learnerA.getId());
        assertThat(orders).hasSize(2);

        // Verify items collection is fully loaded and initialized without lazy loading exception
        for (Order order : orders) {
            assertThat(order.getItems()).isNotNull();
            assertThat(order.getItems().size()).isGreaterThan(0);
        }

        // Verify with status filter
        List<Order> completedOrders = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDescIdDesc(
                learnerA.getId(), OrderStatus.COMPLETED);
        assertThat(completedOrders).hasSize(1);
        assertThat(completedOrders.get(0).getItems()).hasSize(2);
    }
}
