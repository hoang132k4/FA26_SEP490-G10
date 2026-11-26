package com.jeps.modules.exam;

import com.jeps.modules.exam.controller.ManagerExamController;
import com.jeps.modules.exam.service.ManagerExamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(ManagerExamController.class)
@ActiveProfiles("test")
@WithMockUser(username = "manager@test.com", roles = "MANAGER")
class ManagerExamControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManagerExamService managerExamService;
    @MockBean private com.jeps.modules.user.repository.UserRepository userRepository;
    @MockBean private com.jeps.modules.notification.service.NotificationService notificationService;

    @Test
    void bulkStatus_success_redirectsWithCount() throws Exception {
        when(managerExamService.bulkUpdateStatus(List.of(1L, 2L), "ARCHIVED")).thenReturn(2);

        mockMvc.perform(post("/manager/exams/bulk-status").with(csrf()).param("ids", "1", "2").param("status", "ARCHIVED"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examSuccess", "Đã cập nhật trạng thái 2 đề thi sang ARCHIVED."));
    }

    @Test
    void bulkStatus_rejected_redirectsWithError() throws Exception {
        when(managerExamService.bulkUpdateStatus(List.of(1L), "PUBLISHED"))
                .thenThrow(new IllegalArgumentException("Đề thi #1: số câu chưa khớp cấu trúc. Không thể chuyển sang PUBLISHED."));

        mockMvc.perform(post("/manager/exams/bulk-status").with(csrf()).param("ids", "1").param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examError", "Đề thi #1: số câu chưa khớp cấu trúc. Không thể chuyển sang PUBLISHED."))
                .andExpect(flash().attributeCount(1));
    }
}
