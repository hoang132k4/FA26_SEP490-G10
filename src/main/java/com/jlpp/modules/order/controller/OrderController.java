package com.jlpp.modules.order.controller;

import com.jlpp.modules.order.dto.OrderDetailResponse;
import com.jlpp.modules.order.dto.OrderHistoryItemResponse;
import com.jlpp.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String viewOrderHistory(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            Model model,
            Principal principal) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        List<OrderHistoryItemResponse> orders = orderService.getOrderHistory(principal.getName(), status);

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("hasOrders", !orders.isEmpty());

        return "pages/order/history";
    }

    @GetMapping("/orders/{orderNumber}")
    public String viewOrderDetail(
            @PathVariable("orderNumber") String orderNumber,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        try {
            OrderDetailResponse order = orderService.getOrderDetailByOrderNumber(principal.getName(), orderNumber);
            model.addAttribute("order", order);
            return "pages/order/detail";
        } catch (SecurityException ex) {
            log.warn("Unauthorized access attempt to order {} by user {}", orderNumber, principal.getName());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders";
        } catch (IllegalArgumentException ex) {
            log.warn("Order not found: {} for user {}", orderNumber, principal.getName());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders";
        }
    }

    @GetMapping("/order/history")
    public String legacyOrderHistoryRedirect() {
        return "redirect:/orders";
    }
}
