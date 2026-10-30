package com.jlpp.modules.exam;

import com.jlpp.modules.exam.controller.ExamController;
import com.jlpp.modules.exam.dto.ExamCardDto;
import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.ExamTakeResponse;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExamController.class)
@AutoConfigureMockMvc
public class ExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamService examService;

    @MockBean
    private com.jlpp.common.util.CurrentUserSession currentUserSession;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .username("learner1")
                .email("learner1@test.com")
                .build();
        user.setId(1L);

        when(userRepository.findByEmailIgnoreCase("learner1")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("learner1")).thenReturn(Optional.of(user));
    }


    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("GET /exams (Authenticated) - Trả về view pages/exam/center (Đánh giá năng lực) với 3 nhóm bài thi")
    void testExamCenterPage_Authenticated() throws Exception {
        mockMvc.perform(get("/exams"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/center"))
                .andExpect(model().attribute("pageTitle", "Đánh giá năng lực - Kotoba JEPS"))
                .andExpect(model().attribute("activeMenu", "exams"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("GET /exams/mock (Authenticated) - Trả về view pages/exam/list với danh sách đề thi thử JLPT")
    void testExamListPage_Authenticated() throws Exception {
        ExamCardDto card = ExamCardDto.builder()
                .examId(8L)
                .examTitle("JLPT N4 Mock Exam")
                .jlptLevel("N4")
                .durationMinutes(50)
                .totalQuestions(5)
                .ready(true)
                .build();

        when(examService.getLearnerExams("ALL")).thenReturn(List.of(card));

        mockMvc.perform(get("/exams/mock"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/list"))
                .andExpect(model().attributeExists("exams"))
                .andExpect(model().attribute("activeLevel", "ALL"))
                .andExpect(model().attribute("activeMenu", "exams"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("GET /exams/milestones (Authenticated) - Trả về view pages/exam/milestone-list với danh sách bài kiểm tra cột mốc")
    void testMilestoneListPage_Authenticated() throws Exception {
        ExamCardDto card = ExamCardDto.builder()
                .examId(10L)
                .examTitle("Milestone Exam Course 2")
                .jlptLevel("N4")
                .durationMinutes(60)
                .totalQuestions(20)
                .ready(true)
                .build();

        when(examService.getMilestoneExams(null)).thenReturn(List.of(card));

        mockMvc.perform(get("/exams/milestones"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/milestone-list"))
                .andExpect(model().attributeExists("exams"))
                .andExpect(model().attribute("activeMenu", "exams"));
    }

    @Test
    @DisplayName("GET /exams (Unauthenticated) - Chuyển hướng về trang đăng nhập")
    void testExamListPage_Unauthenticated() throws Exception {
        mockMvc.perform(get("/exams"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 1A: GET /exams/placement - Vào màn hình thông tin giới thiệu đánh giá đầu vào")
    void testStartPlacementExam_Success() throws Exception {
        ExamTakeResponse placementTake = ExamTakeResponse.builder()
                .examId(7L)
                .examTitle("Placement Assessment")
                .examType("PLACEMENT_TEST")
                .durationMinutes(30)
                .totalQuestions(4)
                .questions(Collections.emptyList())
                .build();

        when(examService.getPlacementExamForTaking()).thenReturn(placementTake);

        mockMvc.perform(get("/exams/placement"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/placement-intro"))
                .andExpect(model().attributeExists("exam"))
                .andExpect(model().attribute("activeMenu", "placement"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 1B: GET /exams/placement/take - Vào phòng thi làm bài đánh giá đầu vào")
    void testTakePlacementExam_Success() throws Exception {
        ExamTakeResponse placementTake = ExamTakeResponse.builder()
                .examId(7L)
                .examTitle("Placement Assessment")
                .examType("PLACEMENT_TEST")
                .durationMinutes(30)
                .totalQuestions(4)
                .questions(Collections.emptyList())
                .build();

        when(examService.getPlacementExamForTaking()).thenReturn(placementTake);
        when(examService.getPlacementExamForTaking(any())).thenReturn(placementTake);

        mockMvc.perform(get("/exams/placement/take")
                        .param("targetLevel", "N4")
                        .param("priorStudy", "SELF_STUDY"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/take-exam"))
                .andExpect(model().attributeExists("exam"))
                .andExpect(model().attribute("activeMenu", "placement"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 1 Alias: GET /assessment/placement-test - Đồng bộ route đánh giá đầu vào")
    void testAssessmentPlacementAlias_Success() throws Exception {
        ExamTakeResponse placementTake = ExamTakeResponse.builder()
                .examId(7L)
                .examTitle("Placement Assessment")
                .examType("PLACEMENT_TEST")
                .durationMinutes(30)
                .totalQuestions(4)
                .questions(Collections.emptyList())
                .build();

        when(examService.getPlacementExamForTaking()).thenReturn(placementTake);

        mockMvc.perform(get("/assessment/placement-test"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/placement-intro"))
                .andExpect(model().attributeExists("exam"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 3: GET /exams/milestone/{id} - Vào màn hình làm bài kiểm tra cột mốc")
    void testStartMilestoneExam_Success() throws Exception {
        ExamTakeResponse milestoneTake = ExamTakeResponse.builder()
                .examId(10L)
                .examTitle("Milestone Exam Course 2")
                .examType("MILESTONE")
                .jlptLevel("N4")
                .durationMinutes(60)
                .totalQuestions(2)
                .questions(Collections.emptyList())
                .build();

        when(examService.getMilestoneExamForTaking(10L)).thenReturn(milestoneTake);

        mockMvc.perform(get("/exams/milestone/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/take-exam"))
                .andExpect(model().attributeExists("exam"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("GET /exams/{id}/start - Vào màn hình làm bài take-exam")
    void testStartExam_Success() throws Exception {
        ExamTakeResponse examTake = ExamTakeResponse.builder()
                .examId(8L)
                .examTitle("JLPT N4 Mock Exam")
                .jlptLevel("N4")
                .durationMinutes(50)
                .totalQuestions(5)
                .questions(Collections.emptyList())
                .build();

        when(examService.getExamForTaking(8L)).thenReturn(examTake);

        mockMvc.perform(get("/exams/8/start"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/take-exam"))
                .andExpect(model().attributeExists("exam"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 2: POST /exams/{id}/submit - Nộp bài thi và chuyển hướng về /exams/result/{id}?resultId={resultId}")
    void testSubmitExam_Success() throws Exception {
        ExamResultResponse result = ExamResultResponse.builder()
                .resultId(55L)
                .examId(8L)
                .score(45)
                .totalPossibleScore(60)
                .percentage(new BigDecimal("75.0"))
                .passed(true)
                .persistentSaved(true)
                .build();

        when(examService.evaluateAndSaveSubmission(eq(8L), any(), any(), any())).thenReturn(result);

        mockMvc.perform(post("/exams/8/submit")
                        .with(csrf())
                        .param("timeSpentSeconds", "150")
                        .param("answer_16", "61"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/exams/result/8?resultId=55"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 2: GET /exams/result/{id} - Hiển thị màn hình kết quả thi")
    void testExamResultPage_Success() throws Exception {
        ExamResultResponse result = ExamResultResponse.builder()
                .examId(8L)
                .score(45)
                .totalPossibleScore(60)
                .percentage(new BigDecimal("75.0"))
                .passed(true)
                .build();

        when(examService.evaluateSubmission(eq(8L), any(), any())).thenReturn(result);

        mockMvc.perform(get("/exams/result/8"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/result"))
                .andExpect(model().attributeExists("result"));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 4: GET /exams/history - Hiển thị danh sách lịch sử thi thực tế từ Supabase")
    void testExamHistoryPage_Success() throws Exception {
        ExamHistoryDto historyItem = ExamHistoryDto.builder()
                .resultId(55L)
                .examId(8L)
                .examTitle("JLPT N4 Mock Exam")
                .examType("JLPT_MOCK")
                .totalScore(45)
                .passedStatus(true)
                .attemptNumber(1)
                .formattedDate("26/10/2026 14:00")
                .build();

        when(examService.getLearnerExamHistory(any())).thenReturn(List.of(historyItem));

        mockMvc.perform(get("/exams/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/history-list"))
                .andExpect(model().attributeExists("historyList"))
                .andExpect(model().attribute("activeMenu", "history"));

    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 5: GET /exams/history/{resultId} - Xem chi tiết bài thi lịch sử hợp lệ")
    void testExamHistoryDetailPage_Success() throws Exception {
        ExamResultResponse result = ExamResultResponse.builder()
                .resultId(55L)
                .examId(8L)
                .examTitle("JLPT N4 Mock Exam")
                .score(45)
                .passed(true)
                .persistentSaved(true)
                .build();

        when(examService.getHistoricalResult(eq(55L), any())).thenReturn(result);

        mockMvc.perform(get("/exams/history/55"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("isHistoricalView", true));
    }

    @Test
    @WithMockUser(username = "learner1", roles = {"LEARNER"})
    @DisplayName("SCREEN 5 Security: GET /exams/history/{resultId} - Truy cập bài thi của người khác bị chặn 403 Forbidden")
    void testExamHistoryDetailPage_Forbidden() throws Exception {
        when(examService.getHistoricalResult(eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập kết quả bài thi này."));

        mockMvc.perform(get("/exams/history/99"))
                .andExpect(status().isForbidden());
    }
}

