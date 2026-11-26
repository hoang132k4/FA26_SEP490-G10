package com.jeps.modules.exam;

import com.jeps.common.util.CurrentUserSession;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.service.CourseRecommendationService;
import com.jeps.modules.exam.controller.ExamController;
import com.jeps.modules.exam.dto.ExamCardDto;
import com.jeps.modules.exam.dto.ExamHistoryDto;
import com.jeps.modules.exam.dto.ExamResultResponse;
import com.jeps.modules.exam.dto.ExamTakeResponse;
import com.jeps.modules.exam.repository.ExamRepository;
import com.jeps.modules.exam.service.ExamService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ExamController (Center, Mock List, Placement, Milestone, Start, Submit, Result, History)")
class ExamControllerUnitTest {

    @Mock
    private ExamService examService;

    @Mock
    private CurrentUserSession currentUserSession;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @Mock
    private CourseRecommendationService courseRecommendationService;

    @Mock
    private ExamRepository examRepository;

    private ExamController examController;

    private final Principal learnerPrincipal = () -> "learner@jeps.edu.vn";

    @BeforeEach
    void setUp() {
        examController = new ExamController(
                examService,
                currentUserSession,
                userRepository,
                courseRepository,
                courseRegistrationRepository,
                courseRecommendationService,
                examRepository
        );
    }

    private User sampleUser() {
        User u = User.builder().email("learner@jeps.edu.vn").username("learner").targetJlptLevel("N3").currentJlptLevel("N4").build();
        u.setId(10L);
        return u;
    }

    // ==================== examCenterPage ====================

    @Test
    @DisplayName("Abnormal: GET /exams chưa đăng nhập redirect /auth/login?continue=/exams")
    void examCenterPage_unauthenticated_redirectsToLogin() {
        MockHttpSession session = new MockHttpSession();
        when(currentUserSession.isAuthenticated(session)).thenReturn(false);

        String view = examController.examCenterPage(session, new ConcurrentModel(), null);

        assertEquals("redirect:/auth/login?continue=/exams", view);
    }

    @Test
    @DisplayName("Normal: GET /exams đã đăng nhập trả về pages/exam/center")
    void examCenterPage_authenticated_returnsCenterView() {
        Model model = new ConcurrentModel();
        String view = examController.examCenterPage(new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/center", view);
        assertEquals("exams", model.getAttribute("activeMenu"));
    }

    // ==================== jlptMockListPage ====================

    @Test
    @DisplayName("Normal: GET /exams/mock?level=n3 chuẩn hóa level=N3 và nạp danh sách đề thi")
    void jlptMockListPage_withLevel_normalizesAndReturnsList() {
        when(examService.getLearnerExams("N3")).thenReturn(List.of(ExamCardDto.builder().examId(1L).examTitle("Mock N3").build()));
        Model model = new ConcurrentModel();

        String view = examController.jlptMockListPage(" n3 ", new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/list", view);
        assertEquals("N3", model.getAttribute("activeLevel"));
        assertEquals(1, ((List<?>) model.getAttribute("exams")).size());
    }

    @Test
    @DisplayName("Boundary: GET /exams/mock không truyền level mặc định activeLevel=ALL")
    void jlptMockListPage_nullLevel_defaultsToAll() {
        when(examService.getLearnerExams("ALL")).thenReturn(Collections.emptyList());
        Model model = new ConcurrentModel();

        String view = examController.jlptMockListPage(null, new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/list", view);
        assertEquals("ALL", model.getAttribute("activeLevel"));
    }

    // ==================== Placement & Milestone & Start ====================

    @Test
    @DisplayName("Normal: GET /exams/placement trả về pages/exam/placement-intro")
    void startPlacementExam_authenticated_returnsIntro() {
        ExamTakeResponse take = ExamTakeResponse.builder().examId(5L).examTitle("Placement").build();
        when(examService.getPlacementExamForTaking()).thenReturn(take);
        Model model = new ConcurrentModel();

        String view = examController.startPlacementExam(new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/placement-intro", view);
        assertSame(take, model.getAttribute("exam"));
    }

    @Test
    @DisplayName("Normal: GET /exams/placement/take lưu thông tin khảo sát vào session và trả về take-exam")
    void takePlacementExam_storesSurveyAttributesAndReturnsTakeView() {
        MockHttpSession session = new MockHttpSession();
        ExamTakeResponse take = ExamTakeResponse.builder().examId(6L).jlptLevel("N4").build();
        when(examService.getPlacementExamForTaking("N4")).thenReturn(take);
        Model model = new ConcurrentModel();

        String view = examController.takePlacementExam("N3", "NO", null, "N4", session, model, learnerPrincipal);

        assertEquals("pages/exam/take-exam", view);
        assertEquals("N3", session.getAttribute("placement_target_level"));
        assertEquals("NO", session.getAttribute("placement_has_studied"));
        assertEquals("N4", session.getAttribute("placement_assigned_level"));
        assertSame(take, model.getAttribute("exam"));
    }

    @Test
    @DisplayName("Normal: GET /exams/milestone/{id} trả về take-exam")
    void startMilestoneExam_authenticated_returnsTakeView() {
        ExamTakeResponse take = ExamTakeResponse.builder().examId(12L).examType("MILESTONE").build();
        when(examService.getMilestoneExamForTaking(12L)).thenReturn(take);
        Model model = new ConcurrentModel();

        String view = examController.startMilestoneExam(12L, new MockHttpSession(), model, learnerPrincipal, new RedirectAttributesModelMap());

        assertEquals("pages/exam/take-exam", view);
        assertSame(take, model.getAttribute("exam"));
    }

    @Test
    @DisplayName("Normal: GET /exams/milestones trả về pages/exam/milestone-list")
    void milestoneListPage_authenticated_returnsMilestoneList() {
        when(examService.getMilestoneExams(3L)).thenReturn(List.of(ExamCardDto.builder().examId(12L).build()));
        Model model = new ConcurrentModel();

        String view = examController.milestoneListPage(3L, new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/milestone-list", view);
        assertEquals(true, model.getAttribute("isMilestone"));
    }

    @Test
    @DisplayName("Normal: GET /exams/{id}/start trả về pages/exam/take-exam")
    void startExam_authenticated_returnsTakeView() {
        ExamTakeResponse take = ExamTakeResponse.builder().examId(8L).build();
        when(examService.getExamForTaking(8L)).thenReturn(take);
        Model model = new ConcurrentModel();

        String view = examController.startExam(8L, new MockHttpSession(), model, learnerPrincipal, new RedirectAttributesModelMap());

        assertEquals("pages/exam/take-exam", view);
        assertSame(take, model.getAttribute("exam"));
    }

    // ==================== submitExam ====================

    @Test
    @DisplayName("Abnormal: POST /exams/{id}/submit chưa đăng nhập redirect /auth/login")
    void submitExam_unauthenticated_redirectsToLogin() {
        MockHttpSession session = new MockHttpSession();
        when(currentUserSession.isAuthenticated(session)).thenReturn(false);

        String view = examController.submitExam(8L, Map.of(), 100, session, null);

        assertEquals("redirect:/auth/login", view);
        verify(examService, never()).evaluateAndSaveSubmission(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Normal: POST /exams/{id}/submit có resultId lưu vào session và redirect kèm ?resultId=")
    void submitExam_withResultId_savesToSessionAndRedirectsWithQueryParam() {
        User user = sampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        ExamResultResponse res = ExamResultResponse.builder().resultId(301L).score(140).passed(true).build();
        when(examService.evaluateAndSaveSubmission(eq(8L), eq(10L), anyMap(), eq(1200))).thenReturn(res);
        MockHttpSession session = new MockHttpSession();

        String view = examController.submitExam(8L, Map.of("q_1", "10"), 1200, session, learnerPrincipal);

        assertEquals("redirect:/exams/result/8?resultId=301", view);
        assertSame(res, session.getAttribute("EXAM_RESULT_8"));
    }

    @Test
    @DisplayName("Boundary: POST /exams/{id}/submit với resultId=null redirect /exams/result/{id} không có query param")
    void submitExam_nullResultId_redirectsWithoutQueryParam() {
        User user = sampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        ExamResultResponse res = ExamResultResponse.builder().resultId(null).score(100).build();
        when(examService.evaluateAndSaveSubmission(eq(8L), eq(10L), anyMap(), eq(60))).thenReturn(res);
        MockHttpSession session = new MockHttpSession();

        String view = examController.submitExam(8L, Map.of(), 60, session, learnerPrincipal);

        assertEquals("redirect:/exams/result/8", view);
    }

    @Test
    @DisplayName("Boundary: POST /exams/{id}/submit khi hết giờ tự động khóa và giới hạn timeSpent tối đa bằng thời lượng bài thi")
    void submitExam_timeExpired_clampsTimeSpentToMaxDuration() {
        User user = sampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        ExamResultResponse res = ExamResultResponse.builder().resultId(305L).score(90).build();
        when(examService.evaluateAndSaveSubmission(eq(8L), eq(10L), anyMap(), eq(1800))).thenReturn(res);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("EXAM_START_TIME_8", System.currentTimeMillis() - 1900_000L);
        session.setAttribute("EXAM_DURATION_SECONDS_8", 1800);

        String view = examController.submitExam(8L, Map.of("q_1", "10", "autoSubmitted", "true"), 1950, session, learnerPrincipal);

        assertEquals("redirect:/exams/result/8?resultId=305", view);
        assertNull(session.getAttribute("EXAM_START_TIME_8"));
        assertNull(session.getAttribute("EXAM_DURATION_SECONDS_8"));
    }

    // ==================== examResultPage ====================

    @Test
    @DisplayName("Abnormal: GET /exams/result/{id} không có kết quả trong session và không có resultId ném 404 NOT_FOUND")
    void examResultPage_noSessionAndNoResultId_throws404() {
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(sampleUser()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examController.examResultPage(8L, null, new ConcurrentModel(), new MockHttpSession(), learnerPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Normal: GET /exams/result/{id} đọc kết quả JLPT_MOCK từ session và trả về pages/exam/result")
    void examResultPage_mockResultInSession_returnsResultView() {
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(sampleUser()));
        MockHttpSession session = new MockHttpSession();
        ExamResultResponse res = ExamResultResponse.builder().examType("JLPT_MOCK").jlptLevel("N3").score(120).passed(true).build();
        session.setAttribute("EXAM_RESULT_8", res);
        Model model = new ConcurrentModel();

        String view = examController.examResultPage(8L, null, model, session, learnerPrincipal);

        assertEquals("pages/exam/result", view);
        assertSame(res, model.getAttribute("result"));
        assertEquals("exams", model.getAttribute("activeMenu"));
    }

    @Test
    @DisplayName("Normal: GET /exams/result/{id} cho bài PLACEMENT_TEST tính toán khuyến nghị khóa học nền tảng khi dưới 60%")
    void examResultPage_placementResultBelow60_recommendsFoundationCourse() {
        User user = sampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        Course n4Course = Course.builder().title("JLPT N4").level("N4").status("PUBLISHED").build();
        n4Course.setId(40L);
        Course n3Course = Course.builder().title("JLPT N3").level("N3").status("PUBLISHED").build();
        n3Course.setId(30L);
        when(courseRepository.findByLevelAndStatus("N3", "PUBLISHED")).thenReturn(List.of(n3Course));
        when(courseRepository.findByLevelAndStatus("N4", "PUBLISHED")).thenReturn(List.of(n4Course));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("placement_target_level", "N3");
        session.setAttribute("placement_has_studied", "NO");
        session.setAttribute("placement_assigned_level", "N4");
        ExamResultResponse res = ExamResultResponse.builder()
                .examType("PLACEMENT_TEST")
                .jlptLevel("N4")
                .percentage(BigDecimal.valueOf(45.0))
                .build();
        session.setAttribute("EXAM_RESULT_5", res);

        Model model = new ConcurrentModel();
        String view = examController.examResultPage(5L, null, model, session, learnerPrincipal);

        assertEquals("pages/exam/result", view);
        assertEquals("placement", model.getAttribute("activeMenu"));
        assertEquals(false, model.getAttribute("isAssessmentOk"));
        assertSame(n4Course, model.getAttribute("recommendedCourse"));
    }

    // ==================== examHistoryPage & examHistoryDetailPage ====================

    @Test
    @DisplayName("Normal: GET /exams/history trả về danh sách lịch sử thi của học viên")
    void examHistoryPage_authenticated_returnsHistoryList() {
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(sampleUser()));
        when(examService.getLearnerExamHistory(10L)).thenReturn(List.of(ExamHistoryDto.builder().resultId(1L).build()));
        Model model = new ConcurrentModel();

        String view = examController.examHistoryPage(new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/history-list", view);
        assertEquals("history", model.getAttribute("activeMenu"));
        assertEquals(1, ((List<?>) model.getAttribute("historyList")).size());
    }

    @Test
    @DisplayName("Abnormal: GET /exams/history/{resultId} không xác định được userId ném 401 UNAUTHORIZED")
    void examHistoryDetailPage_nullUserId_throws401() {
        MockHttpSession session = new MockHttpSession();
        when(currentUserSession.isAuthenticated(session)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examController.examHistoryDetailPage(301L, session, new ConcurrentModel(), null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Normal: GET /exams/history/{resultId} hợp lệ trả về pages/exam/result với isHistoricalView=true")
    void examHistoryDetailPage_valid_returnsHistoricalResult() {
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(sampleUser()));
        ExamResultResponse res = ExamResultResponse.builder().resultId(301L).score(150).build();
        when(examService.getHistoricalResult(301L, 10L)).thenReturn(res);
        Model model = new ConcurrentModel();

        String view = examController.examHistoryDetailPage(301L, new MockHttpSession(), model, learnerPrincipal);

        assertEquals("pages/exam/result", view);
        assertEquals(true, model.getAttribute("isHistoricalView"));
        assertSame(res, model.getAttribute("result"));
    }

    @Test
    @DisplayName("Normal: GET /admin/exams trả về pages/admin/exam/list")
    void adminExamList_returnsAdminView() {
        assertEquals("pages/admin/exam/list", examController.adminExamList());
    }
}
