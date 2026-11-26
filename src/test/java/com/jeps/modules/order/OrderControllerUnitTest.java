package com.jeps.modules.order;

import com.jeps.modules.order.controller.OrderController;
import com.jeps.modules.order.dto.OrderDetailResponse;
import com.jeps.modules.order.dto.OrderHistoryItemResponse;
import com.jeps.modules.order.entity.OrderStatus;
import com.jeps.modules.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: OrderController (Order History, Order Detail, Legacy Redirect)")
class OrderControllerUnitTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;

    private final Principal learnerPrincipal = () -> "learner@jeps.edu.vn";

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderService);
    }

    // ==================== viewOrderHistory ====================

    @Test
    @DisplayName("Abnormal: GET /orders khi chưa đăng nhập redirect về /auth/login")
    void viewOrderHistory_unauthenticated_redirectsToLogin() {
        String view = orderController.viewOrderHistory("ALL", new ConcurrentModel(), null);

        assertEquals("redirect:/auth/login", view);
        verify(orderService, never()).getOrderHistory(any(), any());
    }

    @Test
    @DisplayName("Normal: GET /orders với danh sách đơn hàng trả về pages/order/history và hasOrders=true")
    void viewOrderHistory_withOrders_returnsHistoryView() {
        OrderHistoryItemResponse item = OrderHistoryItemResponse.builder()
                .orderNumber("ORD-20261125-001")
                .orderStatus(OrderStatus.COMPLETED)
                .build();
        when(orderService.getOrderHistory("learner@jeps.edu.vn", "completed")).thenReturn(List.of(item));
        Model model = new ConcurrentModel();

        String view = orderController.viewOrderHistory("completed", model, learnerPrincipal);

        assertEquals("pages/order/history", view);
        assertEquals("COMPLETED", model.getAttribute("currentStatus"));
        assertEquals(true, model.getAttribute("hasOrders"));
        assertEquals(1, ((List<?>) model.getAttribute("orders")).size());
    }

    @Test
    @DisplayName("Boundary: GET /orders với status=null và danh sách rỗng mặc định currentStatus=ALL và hasOrders=false")
    void viewOrderHistory_nullStatusAndEmptyList_setsDefaults() {
        when(orderService.getOrderHistory("learner@jeps.edu.vn", null)).thenReturn(Collections.emptyList());
        Model model = new ConcurrentModel();

        String view = orderController.viewOrderHistory(null, model, learnerPrincipal);

        assertEquals("pages/order/history", view);
        assertEquals("ALL", model.getAttribute("currentStatus"));
        assertEquals(false, model.getAttribute("hasOrders"));
    }

    // ==================== viewOrderDetail ====================

    @Test
    @DisplayName("Abnormal: GET /orders/{orderNumber} chưa đăng nhập redirect về /auth/login")
    void viewOrderDetail_unauthenticated_redirectsToLogin() {
        String view = orderController.viewOrderDetail("ORD-001", new ConcurrentModel(), null, new RedirectAttributesModelMap());

        assertEquals("redirect:/auth/login", view);
        verify(orderService, never()).getOrderDetailByOrderNumber(any(), any());
    }

    @Test
    @DisplayName("Normal: GET /orders/{orderNumber} hợp lệ trả về pages/order/detail với model order")
    void viewOrderDetail_valid_returnsDetailView() {
        OrderDetailResponse detail = OrderDetailResponse.builder()
                .orderNumber("ORD-20261125-001")
                .orderStatus(OrderStatus.COMPLETED)
                .build();
        when(orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-20261125-001")).thenReturn(detail);
        Model model = new ConcurrentModel();

        String view = orderController.viewOrderDetail("ORD-20261125-001", model, learnerPrincipal, new RedirectAttributesModelMap());

        assertEquals("pages/order/detail", view);
        assertSame(detail, model.getAttribute("order"));
    }

    @Test
    @DisplayName("Abnormal: GET /orders/{orderNumber} truy cập trái phép (IDOR SecurityException) redirect /orders kèm errorMessage")
    void viewOrderDetail_securityException_redirectsWithError() {
        when(orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-OTHER"))
                .thenThrow(new SecurityException("Bạn không có quyền truy cập thông tin đơn hàng này."));
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = orderController.viewOrderDetail("ORD-OTHER", new ConcurrentModel(), learnerPrincipal, ra);

        assertEquals("redirect:/orders", view);
        assertEquals("Bạn không có quyền truy cập thông tin đơn hàng này.", ra.getFlashAttributes().get("errorMessage"));
    }

    @Test
    @DisplayName("Abnormal: GET /orders/{orderNumber} không tồn tại (IllegalArgumentException) redirect /orders kèm errorMessage")
    void viewOrderDetail_notFound_redirectsWithError() {
        when(orderService.getOrderDetailByOrderNumber("learner@jeps.edu.vn", "ORD-404"))
                .thenThrow(new IllegalArgumentException("Không tìm thấy đơn hàng: ORD-404"));
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = orderController.viewOrderDetail("ORD-404", new ConcurrentModel(), learnerPrincipal, ra);

        assertEquals("redirect:/orders", view);
        assertEquals("Không tìm thấy đơn hàng: ORD-404", ra.getFlashAttributes().get("errorMessage"));
    }

    // ==================== legacyOrderHistoryRedirect ====================

    @Test
    @DisplayName("Normal: GET /order/history chuyển hướng sang /orders")
    void legacyOrderHistoryRedirect_redirectsToOrders() {
        assertEquals("redirect:/orders", orderController.legacyOrderHistoryRedirect());
    }
}
