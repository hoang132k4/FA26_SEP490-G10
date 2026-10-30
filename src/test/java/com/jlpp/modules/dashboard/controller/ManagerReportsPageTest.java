package com.jlpp.modules.dashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerReportsPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanViewAndFilterReports() throws Exception {
        mockMvc.perform(get("/manager/reports")
                        .param("type", "course-enrollments")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xem báo cáo")))
                .andExpect(content().string(containsString("Xuất báo cáo CSV")))
                .andExpect(content().string(containsString("Báo cáo đăng ký khóa học")));
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void learnerCannotOpenManagerReports() throws Exception {
        mockMvc.perform(get("/manager/reports"))
                .andExpect(status().isForbidden());
    }
}
