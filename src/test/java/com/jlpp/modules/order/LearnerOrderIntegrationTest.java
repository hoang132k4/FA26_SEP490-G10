package com.jlpp.modules.order;

import com.jlpp.common.constants.RoleEnum;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Test: Learner Orders (Screens 15 & 16 - Order History & Order Details)")
class LearnerOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "learner_order_it@jeps.edu.vn";

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmailIgnoreCase(TEST_EMAIL)) {
            userRepository.save(User.builder()
                    .email(TEST_EMAIL)
                    .username("learner_order_it")
                    .fullName("Nguyen Van Order IT")
                    .phone("0988445566")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(RoleEnum.LEARNER)
                    .status("ACTIVE")
                    .build());
        }
    }

    @Test
    @DisplayName("Screen 16: Chưa đăng nhập truy cập /orders chuyển hướng về đăng nhập")
    void testOrderHistoryUnauthenticatedRedirect() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 16: GET /orders hiển thị danh sách lịch sử đơn hàng của học viên")
    void testGetOrderHistoryPage() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/order/history"))
                .andExpect(content().string(containsString("Lịch sử đơn hàng")));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 15: GET /orders/{orderNumber} chuyển hướng về /orders với thông báo khi đơn hàng không tồn tại")
    void testGetOrderDetailNotFound() throws Exception {
        mockMvc.perform(get("/orders/ORD-NONEXISTENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
