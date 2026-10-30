package com.jlpp.modules.manager.flashcards;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
class ManagerFlashcardPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void createPageRendersQuizletStyleSetForm() throws Exception {
        mockMvc.perform(get("/manager/flashcards/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo bộ Flashcard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thuật ngữ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Định nghĩa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thêm thẻ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập nhanh bằng văn bản")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dùng danh sách này")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phân cách bằng Tab")));
    }

    @Test
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void invalidSetSubmissionReturnsFormWithValidationErrors() throws Exception {
        mockMvc.perform(post("/manager/flashcards/sets")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo bộ Flashcard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng nhập tên bộ thẻ.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng chọn bài học.")));
    }

    @Test
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void missingDatabaseTableIsShownAsReadFailureNotAnEmptyFlashcardList() throws Exception {
        mockMvc.perform(get("/manager/flashcards"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh sách không thể hiển thị")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Tổng: 0 flashcard"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Chưa có flashcard trong bảng flashcards."))));
    }
}
