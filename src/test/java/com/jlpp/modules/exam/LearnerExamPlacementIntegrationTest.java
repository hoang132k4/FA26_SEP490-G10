package com.jlpp.modules.exam;

import com.jlpp.common.util.CurrentUserSession;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExamController.class)
@AutoConfigureMockMvc
@DisplayName("Integration Test: Exam & Placement Screens (Screens 4, 8, 9, 10, 11, 12, 17, 18)")
class LearnerExamPlacementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamService examService;

    @MockBean
    private CurrentUserSession currentUserSession;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.jlpp.modules.notification.service.NotificationService notificationService;

    @MockBean
    private com.jlpp.modules.course.repository.CourseRegistrationRepository courseRegistrationRepository;

    @MockBean
    private com.jlpp.modules.course.repository.CourseRepository courseRepository;

    @MockBean
    private com.jlpp.modules.course.service.CourseRecommendationService courseRecommendationService;

    private static final String TEST_EMAIL = "learner_exam@jeps.edu.vn";

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .username("learner_exam")
                .email(TEST_EMAIL)
                .currentJlptLevel("N4")
                .targetJlptLevel("N3")
                .build();
        user.setId(10L);

        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(currentUserSession.isAuthenticated(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Screen 4: Chưa đăng nhập truy cập /exams/placement chuyển hướng về login")
    void testGetPlacementOverviewUnauthenticatedRedirect() throws Exception {
        when(currentUserSession.isAuthenticated(any())).thenReturn(false);
        mockMvc.perform(get("/exams/placement"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 4: GET /exams/placement hiển thị trang Tổng quan đánh giá đầu vào (Placement Overview)")
    void testGetPlacementOverviewPage() throws Exception {
        ExamTakeResponse examData = ExamTakeResponse.builder()
                .examId(1L)
                .examTitle("Bài kiểm tra Đánh giá Năng lực Đầu vào JLPT N4")
                .jlptLevel("N4")
                .durationMinutes(45)
                .totalQuestions(15)
                .build();
        when(examService.getPlacementExamForTaking()).thenReturn(examData);

        mockMvc.perform(get("/exams/placement"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/placement-intro"))
                .andExpect(model().attributeExists("exam"));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 9: GET /exams/mock hiển thị danh sách đề thi thử JLPT Mock")
    void testGetMockExamListPage() throws Exception {
        ExamCardDto card = ExamCardDto.builder()
                .examId(101L)
                .examTitle("Đề thi thử JLPT N3 - Lần 1")
                .jlptLevel("N3")
                .durationMinutes(140)
                .totalQuestions(60)
                .build();
        when(examService.getLearnerExams("ALL")).thenReturn(List.of(card));

        mockMvc.perform(get("/exams/mock"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/list"))
                .andExpect(model().attributeExists("exams"))
                .andExpect(model().attribute("activeLevel", "ALL"));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 10: GET /exams/milestones hiển thị danh sách bài thi cột mốc Milestone")
    void testGetMilestoneExamListPage() throws Exception {
        when(examService.getMilestoneExams(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/exams/milestones"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/milestone-list"))
                .andExpect(model().attribute("isMilestone", true));
    }

    @Test
    @DisplayName("Screen 11: Chưa đăng nhập truy cập /exams/history chuyển hướng đăng nhập")
    void testExamHistoryUnauthenticatedRedirect() throws Exception {
        when(currentUserSession.isAuthenticated(any())).thenReturn(false);
        mockMvc.perform(get("/exams/history"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 11: GET /exams/history hiển thị lịch sử các lần thi của học viên")
    void testGetExamHistoryPage() throws Exception {
        ExamHistoryDto history = ExamHistoryDto.builder()
                .resultId(501L)
                .examId(1L)
                .examTitle("Bài kiểm tra đầu vào N4")
                .jlptLevel("N4")
                .totalScore(120)
                .percentage(new BigDecimal("66.7"))
                .passedStatus(true)
                .resultStatus("PASSED")
                .formattedDate("10/02/2026 15:30")
                .build();
        when(examService.getLearnerExamHistory(any())).thenReturn(List.of(history));

        mockMvc.perform(get("/exams/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/history-list"))
                .andExpect(model().attributeExists("historyList"));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 12 & 8: GET /exams/history/{resultId} hiển thị chi tiết kết quả lịch sử thi")
    void testGetHistoricalResultDetailPage() throws Exception {
        ExamResultResponse res = ExamResultResponse.builder()
                .resultId(501L)
                .examId(1L)
                .examTitle("Bài kiểm tra đầu vào N4")
                .jlptLevel("N4")
                .score(120)
                .totalPossibleScore(180)
                .percentage(new BigDecimal("66.7"))
                .passed(true)
                .build();
        when(examService.getHistoricalResult(501L, 10L)).thenReturn(res);

        mockMvc.perform(get("/exams/history/501"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("isHistoricalView", true));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 17 & 18: GET /exams/placement/take với khảo sát targetLevel=N3 và hasStudied=NO khởi tạo bài thi")
    void testTakePlacementExamWithSurvey() throws Exception {
        ExamTakeResponse examData = ExamTakeResponse.builder()
                .examId(2L)
                .examTitle("Bài kiểm tra nền tảng JLPT N4")
                .jlptLevel("N4")
                .durationMinutes(45)
                .totalQuestions(15)
                .build();
        when(examService.getPlacementExamForTaking("N4")).thenReturn(examData);

        mockMvc.perform(get("/exams/placement/take")
                        .param("targetLevel", "N3")
                        .param("hasStudied", "NO")
                        .param("assignedLevel", "N4"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/take-exam"))
                .andExpect(model().attributeExists("exam"));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = {"LEARNER"})
    @DisplayName("Screen 17 & 18: GET /exams/placement/take với khảo sát targetLevel=N3 và hasStudied=YES khởi tạo bài thi đúng cấp độ N3")
    void testTakePlacementExamWithSurveyExactLevel() throws Exception {
        ExamTakeResponse examData = ExamTakeResponse.builder()
                .examId(3L)
                .examTitle("Bài kiểm tra đánh giá năng lực JLPT N3")
                .jlptLevel("N3")
                .durationMinutes(60)
                .totalQuestions(20)
                .build();
        when(examService.getPlacementExamForTaking("N3")).thenReturn(examData);

        mockMvc.perform(get("/exams/placement/take")
                        .param("targetLevel", "N3")
                        .param("hasStudied", "YES")
                        .param("assignedLevel", "N3"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/take-exam"))
                .andExpect(model().attributeExists("exam"));
    }
}
