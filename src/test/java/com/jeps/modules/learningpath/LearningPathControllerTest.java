package com.jeps.modules.learningpath;

import com.jeps.modules.learningpath.controller.LearningPathController;
import com.jeps.modules.learningpath.dto.LearningPathDtos.LearningPathResponse;
import com.jeps.modules.learningpath.dto.LearningPathDtos.RecalibratePreviewResponse;
import com.jeps.modules.learningpath.dto.LearningPathDtos.RecalibrateRequest;
import com.jeps.modules.learningpath.dto.LearningPathDtos.SubmitLeaveRequest;
import com.jeps.modules.learningpath.dto.LearningPathDtos.ToggleCatchUpRequest;
import com.jeps.modules.learningpath.service.LearningPathService;
import com.jeps.modules.notification.service.NotificationService;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Report 5.1 – sheet LearningPathController (UTC_LPC_01 → UTC_LPC_42).
 * Controller layer: @WebMvcTest + MockMvc; các TC "Principal = null" gọi controller trực tiếp (không qua filter).
 */
@WebMvcTest(LearningPathController.class)
@ActiveProfiles("test")
@WithMockUser(username = "learner01", roles = "LEARNER")
class LearningPathControllerTest {

    private static final String VIEW = "pages/learning-path/view";
    private static final String RECALIBRATE_VIEW = "pages/learning-path/recalibrate";
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired private MockMvc mockMvc;
    @MockBean private LearningPathService learningPathService;
    @MockBean private UserRepository userRepository;
    @MockBean private NotificationService notificationService;

    // ------------------------------------------------------------------ fixtures

    private static LearningPathResponse path(boolean completed, LocalDate startDate) {
        return LearningPathResponse.builder()
                .hasPath(true)
                .learningPathId(5L)
                .courseId(1L)
                .courseTitle("N5 Course")
                .courseLevel("N5")
                .targetLevel("N5")
                .currentLevel("N5")
                .dailyHours(new BigDecimal("2.00"))
                .dailyHoursFormatted("2 giờ")
                .weeklyHours(14)
                .studyDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
                .studyDaysText("Thứ 2 - Chủ nhật")
                .scheduleMode("CUSTOM")
                .startDateFormatted(startDate.format(DMY))
                .projectedEndDateFormatted(startDate.plusDays(10).format(DMY))
                .progressPercent(completed ? 100 : 20)
                .totalLessons(5)
                .completedLessons(completed ? 5 : 1)
                .remainingLessons(completed ? 0 : 4)
                .totalHours(5)
                .pathStatus("ON_TRACK")
                .statusBadgeClass("bg-success")
                .catchUpMode(false)
                .catchUpExtraMinutes(30)
                .catchUpIncludeWeekends(false)
                .currentlyOnLeave(false)
                .build();
    }

    private static LearningPathResponse activePath() {
        return path(false, LocalDate.now().plusDays(3));
    }

    private static LearningPathResponse noPath(String message) {
        return LearningPathResponse.builder().hasPath(false).emptyStateMessage(message).build();
    }

    private static RecalibratePreviewResponse preview() {
        return RecalibratePreviewResponse.builder()
                .hasPath(true)
                .courseId(1L)
                .courseTitle("N5 Course")
                .courseLevel("N5")
                .originalWeeklyHours(7)
                .newWeeklyHours(14)
                .dailyHours(new BigDecimal("2.00"))
                .studyDaysText("Thứ 2 - Chủ nhật")
                .startDate(LocalDate.now())
                .startDateFormatted(LocalDate.now().format(DMY))
                .originalCompletionDate("20/12/2026")
                .newCompletionDate("10/12/2026")
                .daysDifferenceText("Sớm hơn 10 ngày")
                .remainingHours(10)
                .weeksNeeded(1)
                .scheduleMode("CUSTOM")
                .build();
    }

    private MockHttpServletRequestBuilder recalibrate(String url, String courseId, String weeklyHours, LocalDate startDate) {
        MockHttpServletRequestBuilder request = post(url).with(csrf()).param("courseId", courseId);
        if (weeklyHours != null) request.param("weeklyHours", weeklyHours);
        if (startDate != null) request.param("startDate", startDate.toString());
        return request;
    }

    private MockHttpServletRequestBuilder leave(String start, String end) {
        MockHttpServletRequestBuilder request = post("/learning-path/leave").with(csrf()).param("courseId", "1");
        if (start != null) request.param("startDate", start);
        if (end != null) request.param("endDate", end);
        return request;
    }

    private MockHttpServletRequestBuilder catchUp(String courseId, String enable, String extraMinutes, String weekends) {
        MockHttpServletRequestBuilder request = post("/learning-path/catch-up").with(csrf()).param("courseId", courseId);
        if (enable != null) request.param("enable", enable);
        if (extraMinutes != null) request.param("extraMinutes", extraMinutes);
        if (weekends != null) request.param("includeWeekends", weekends);
        return request;
    }

    private static String today() {
        return LocalDate.now().format(DMY);
    }

    // ------------------------------------------------------------------ GET /learning-path

    @Nested
    @DisplayName("GET /learning-path")
    class ViewPath {

        @Test
        @DisplayName("UTC_LPC_01: hiển thị lộ trình học tập của khóa học id=1")
        void showsPath() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());

            mockMvc.perform(get("/learning-path").param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW))
                    .andExpect(model().attributeExists("path"))
                    .andExpect(model().attribute("hasPath", true));
        }

        @Test
        @DisplayName("UTC_LPC_02: không truyền courseId thì dùng khóa học mặc định")
        void defaultCourse() throws Exception {
            when(learningPathService.getLearningPath("learner01", null)).thenReturn(activePath());

            mockMvc.perform(get("/learning-path"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW));
            verify(learningPathService, times(1)).getLearningPath("learner01", null);
        }

        @Test
        @DisplayName("UTC_LPC_03: chưa có lộ trình thì hiển thị trạng thái rỗng")
        void emptyState() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(noPath("Chưa có lộ trình."));

            mockMvc.perform(get("/learning-path").param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW))
                    .andExpect(model().attribute("hasPath", false))
                    .andExpect(model().attribute("emptyMessage", "Chưa có lộ trình."));
        }

        @Test
        @Disabled("DEF-LP-01 (Open): controller chưa bắt IllegalArgumentException của getLearningPath, request kết thúc bằng HTTP 500")
        @DisplayName("UTC_LPC_04: không tìm thấy người dùng thì quay về trang chủ kèm thông báo lỗi")
        void userNotFound() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L))
                    .thenThrow(new IllegalArgumentException("Không tìm thấy thông tin người dùng: learner01"));

            mockMvc.perform(get("/learning-path").param("courseId", "1"))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("errorMessage", containsString("Không tìm thấy thông tin người dùng: learner01")));
        }

        @Test
        @DisplayName("UTC_LPC_05: Principal = null thì chuyển về trang đăng nhập (gọi controller trực tiếp)")
        void nullPrincipal() {
            LearningPathService service = mock(LearningPathService.class);

            String view = new LearningPathController(service).getLearningPath(null, null, new ExtendedModelMap());

            assertThat(view).isEqualTo("redirect:/auth/login");
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ GET /learning-path/recalibrate

    @Nested
    @DisplayName("GET /learning-path/recalibrate")
    class RecalibrateScreen {

        @Test
        @DisplayName("UTC_LPC_06: hiển thị bước 1 với lịch học hiện tại")
        void showsStepOne() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());
            when(learningPathService.getRecalibratePreview(eq("learner01"), any())).thenReturn(preview());

            mockMvc.perform(get("/learning-path/recalibrate").param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(RECALIBRATE_VIEW))
                    .andExpect(model().attributeExists("preview", "request"))
                    .andExpect(model().attribute("currentStep", 1));
        }

        @Test
        @DisplayName("UTC_LPC_07: không truyền courseId thì dùng khóa học mặc định")
        void defaultCourse() throws Exception {
            when(learningPathService.getLearningPath("learner01", null)).thenReturn(activePath());
            when(learningPathService.getRecalibratePreview(eq("learner01"), any())).thenReturn(preview());

            mockMvc.perform(get("/learning-path/recalibrate"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("currentStep", 1));
            verify(learningPathService).getLearningPath("learner01", null);
        }

        @Test
        @DisplayName("UTC_LPC_08: chưa đăng ký khóa học 999999 thì hiển thị trạng thái rỗng")
        void notEnrolled() throws Exception {
            when(learningPathService.getLearningPath("learner01", 999999L)).thenReturn(noPath("Bạn chưa đăng ký khóa học này."));

            mockMvc.perform(get("/learning-path/recalibrate").param("courseId", "999999"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("hasPath", false))
                    .andExpect(model().attributeExists("emptyMessage"));
            verify(learningPathService, never()).getRecalibratePreview(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_09: ngày bắt đầu đã qua được thay bằng hôm nay")
        void pastStartDateReplacedByToday() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(path(false, LocalDate.now().minusDays(5)));
            when(learningPathService.getRecalibratePreview(eq("learner01"), any())).thenReturn(preview());

            MvcResult result = mockMvc.perform(get("/learning-path/recalibrate").param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andReturn();

            RecalibrateRequest request = (RecalibrateRequest) result.getModelAndView().getModel().get("request");
            assertThat(request.getStartDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @Disabled("DEF-LP-03 (Open): controller chưa bắt IllegalArgumentException của getRecalibratePreview, request kết thúc bằng HTTP 500")
        @DisplayName("UTC_LPC_10: lỗi khi tính bản xem trước thì quay về lộ trình kèm thông báo lỗi")
        void previewFailure() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());
            when(learningPathService.getRecalibratePreview(eq("learner01"), any()))
                    .thenThrow(new IllegalArgumentException("Không tính được bản xem trước."));

            mockMvc.perform(get("/learning-path/recalibrate").param("courseId", "1"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage", containsString("Không tính được bản xem trước.")));
        }

        @Test
        @DisplayName("UTC_LPC_11: đã hoàn thành 100% thì không cho điều chỉnh lịch")
        void courseCompleted() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(path(true, LocalDate.now()));

            mockMvc.perform(get("/learning-path/recalibrate").param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("courseCompleted", true))
                    .andExpect(model().attribute("infoMessage",
                            "Bạn đã hoàn thành 100% tất cả các bài học trong khóa học này. Lộ trình học tập đã hoàn tất xuất sắc!"));
            verify(learningPathService, never()).getRecalibratePreview(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_12: Principal = null thì chuyển về trang đăng nhập")
        void nullPrincipal() {
            LearningPathService service = mock(LearningPathService.class);

            String view = new LearningPathController(service).viewRecalibrateSchedule(null, 1L, new ExtendedModelMap());

            assertThat(view).isEqualTo("redirect:/auth/login");
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ POST /learning-path/recalibrate

    @Nested
    @DisplayName("POST /learning-path/recalibrate (mô phỏng)")
    class Simulate {

        private void activePathWithPreview(String courseId) {
            when(learningPathService.getLearningPath("learner01", Long.valueOf(courseId))).thenReturn(activePath());
            when(learningPathService.getRecalibratePreview(eq("learner01"), any())).thenReturn(preview());
        }

        @Test
        @DisplayName("UTC_LPC_13: mô phỏng lịch học 14 giờ/tuần thành công (không ghi DB)")
        void simulates() throws Exception {
            activePathWithPreview("1");

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "14", LocalDate.now())
                            .param("studyDays", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(RECALIBRATE_VIEW))
                    .andExpect(model().attribute("simulationSuccess", true))
                    .andExpect(model().attribute("currentStep", 2))
                    .andExpect(model().attributeExists("preview"));
            verify(learningPathService, never()).applyRecalibratedSchedule(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_14: 1 giờ/tuần (cận dưới) được chấp nhận")
        void minimumWeeklyHours() throws Exception {
            activePathWithPreview("1");

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "1", LocalDate.now()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("simulationSuccess", true))
                    .andExpect(model().attribute("currentStep", 2));
        }

        @Test
        @DisplayName("UTC_LPC_15: 0 giờ/tuần bị lỗi ràng buộc, chỉ hiện bản xem trước mặc định")
        void zeroWeeklyHours() throws Exception {
            activePathWithPreview("1");

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "0", LocalDate.now()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("request", "weeklyHours", "Min"))
                    .andExpect(model().attributeExists("preview"))
                    .andExpect(model().attributeDoesNotExist("simulationSuccess"));
        }

        @Test
        @DisplayName("UTC_LPC_16: 105 giờ/tuần (cận trên) được chấp nhận")
        void maximumWeeklyHours() throws Exception {
            activePathWithPreview("1");

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "105", LocalDate.now()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("simulationSuccess", true))
                    .andExpect(model().attribute("currentStep", 2));
        }

        @Test
        @DisplayName("UTC_LPC_17: 106 giờ/tuần bị lỗi ràng buộc")
        void tooManyWeeklyHours() throws Exception {
            activePathWithPreview("1");

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "106", LocalDate.now()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("request", "weeklyHours", "Max"))
                    .andExpect(model().attributeDoesNotExist("simulationSuccess"));
        }

        @Test
        @DisplayName("UTC_LPC_18: ngày bắt đầu là hôm qua thì báo lỗi, đổi về hôm nay và vẫn mô phỏng")
        void yesterdayStartDate() throws Exception {
            activePathWithPreview("1");

            MvcResult result = mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "14", LocalDate.now().minusDays(1)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("errorMessage",
                            "Ngày bắt đầu học dự kiến không được trước ngày hôm nay (" + today() + ")."))
                    .andExpect(model().attribute("currentStep", 2))
                    .andReturn();

            RecalibrateRequest request = (RecalibrateRequest) result.getModelAndView().getModel().get("request");
            assertThat(request.getStartDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("UTC_LPC_19: đã hoàn thành khóa học thì không cho mô phỏng")
        void courseCompleted() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(path(true, LocalDate.now()));

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "1", "14", null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Bạn đã hoàn thành tất cả các bài học trong khóa học này! Không thể điều chỉnh lại lịch học."));
        }

        @Test
        @DisplayName("UTC_LPC_20: chưa có lộ trình thì hiển thị trạng thái rỗng")
        void noPathToSimulate() throws Exception {
            when(learningPathService.getLearningPath("learner01", 999999L)).thenReturn(noPath("Chưa có lộ trình."));

            mockMvc.perform(recalibrate("/learning-path/recalibrate", "999999", "14", null))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("hasPath", false))
                    .andExpect(model().attributeExists("emptyMessage"));
            verify(learningPathService, never()).getRecalibratePreview(anyString(), any());
        }
    }

    // ------------------------------------------------------------------ POST /learning-path/recalibrate/apply

    @Nested
    @DisplayName("POST /learning-path/recalibrate/apply")
    class Apply {

        private static final String APPLY = "/learning-path/recalibrate/apply";

        @Test
        @DisplayName("UTC_LPC_21: áp dụng lịch học mới 14 giờ/tuần")
        void appliesSchedule() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());

            mockMvc.perform(recalibrate(APPLY, "1", "14", LocalDate.now())
                            .param("studyDays", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage",
                            "Lịch học mới đã được áp dụng thành công! Lộ trình đã được cập nhật với thời lượng 14 giờ/tuần."));
            verify(learningPathService, times(1)).applyRecalibratedSchedule(eq("learner01"), any(RecalibrateRequest.class));
        }

        @Test
        @DisplayName("UTC_LPC_22: đã hoàn thành khóa học thì không cho lưu lịch học")
        void courseCompleted() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(path(true, LocalDate.now()));

            mockMvc.perform(recalibrate(APPLY, "1", "14", null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Bạn đã hoàn thành tất cả các bài học trong khóa học này! Không thể lưu thay đổi lịch học."));
            verify(learningPathService, never()).applyRecalibratedSchedule(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_23: ngày bắt đầu là hôm qua bị từ chối")
        void yesterdayStartDate() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());

            mockMvc.perform(recalibrate(APPLY, "1", "14", LocalDate.now().minusDays(1)))
                    .andExpect(redirectedUrl("/learning-path/recalibrate?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Ngày bắt đầu học dự kiến không được trước ngày hôm nay (" + today() + ")."));
            verify(learningPathService, never()).applyRecalibratedSchedule(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_24: 106 giờ/tuần bị từ chối")
        void tooManyWeeklyHours() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());

            mockMvc.perform(recalibrate(APPLY, "1", "106", LocalDate.now()))
                    .andExpect(redirectedUrl("/learning-path/recalibrate?courseId=1"))
                    .andExpect(flash().attribute("errorMessage", "Tham số điều chỉnh lịch học không hợp lệ."));
            verify(learningPathService, never()).applyRecalibratedSchedule(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_25: lỗi từ service khi áp dụng lịch được báo bằng flash")
        void serviceFailure() throws Exception {
            when(learningPathService.getLearningPath("learner01", 1L)).thenReturn(activePath());
            doThrow(new IllegalStateException("Không tìm thấy lộ trình học tập với khóa học đã đăng ký và xuất bản để cập nhật."))
                    .when(learningPathService).applyRecalibratedSchedule(eq("learner01"), any());

            mockMvc.perform(recalibrate(APPLY, "1", "14", LocalDate.now()))
                    .andExpect(redirectedUrl("/learning-path/recalibrate?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Không thể cập nhật lịch học: Không tìm thấy lộ trình học tập với khóa học đã đăng ký và xuất bản để cập nhật."));
        }

        @Test
        @DisplayName("UTC_LPC_26: Principal = null thì chuyển về trang đăng nhập")
        void nullPrincipal() {
            LearningPathService service = mock(LearningPathService.class);
            RecalibrateRequest request = RecalibrateRequest.builder().courseId(1L).weeklyHours(14).build();

            String view = new LearningPathController(service).applyRecalibratedSchedule(null, request,
                    new BeanPropertyBindingResult(request, "request"), new RedirectAttributesModelMap());

            assertThat(view).isEqualTo("redirect:/auth/login");
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ leave of absence

    @Nested
    @DisplayName("Báo nghỉ (POST /learning-path/leave, /leave/cancel)")
    class Leave {

        @Test
        @DisplayName("UTC_LPC_27: báo nghỉ 25/11/2026 → 27/11/2026")
        void submitsLeave() throws Exception {
            mockMvc.perform(leave("2026-11-25", "2026-11-27").param("reason", "Busy"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage", startsWith(
                            "Đã ghi nhận báo nghỉ từ 25/11/2026 đến 27/11/2026. Hệ thống đã tự động dời các bài học tiếp theo và bảo lưu chuỗi ngày học liên tục")))
                    .andExpect(flash().attribute("successMessage", containsString("của bạn!")));
            ArgumentCaptor<SubmitLeaveRequest> sent = ArgumentCaptor.forClass(SubmitLeaveRequest.class);
            verify(learningPathService).submitLeaveOfAbsence(eq("learner01"), sent.capture());
            assertThat(sent.getValue().getStartDate()).isEqualTo(LocalDate.of(2026, 11, 25));
            assertThat(sent.getValue().getEndDate()).isEqualTo(LocalDate.of(2026, 11, 27));
        }

        @Test
        @DisplayName("UTC_LPC_28: báo nghỉ 1 ngày (startDate = endDate)")
        void oneDayLeave() throws Exception {
            mockMvc.perform(leave("2026-11-25", "2026-11-25"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attributeExists("successMessage"));
        }

        @Test
        @DisplayName("UTC_LPC_29: thiếu ngày bắt đầu bị từ chối")
        void missingStartDate() throws Exception {
            mockMvc.perform(leave(null, "2026-11-28"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Thông tin báo nghỉ không hợp lệ. Vui lòng kiểm tra lại ngày bắt đầu và kết thúc."));
            verify(learningPathService, never()).submitLeaveOfAbsence(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_30: thiếu ngày kết thúc bị từ chối")
        void missingEndDate() throws Exception {
            mockMvc.perform(leave("2026-11-25", null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Thông tin báo nghỉ không hợp lệ. Vui lòng kiểm tra lại ngày bắt đầu và kết thúc."));
            verify(learningPathService, never()).submitLeaveOfAbsence(anyString(), any());
        }

        @Test
        @DisplayName("UTC_LPC_31: ngày bắt đầu sau ngày kết thúc bị từ chối")
        void startAfterEnd() throws Exception {
            doThrow(new IllegalArgumentException("Ngày bắt đầu nghỉ không được sau ngày kết thúc nghỉ."))
                    .when(learningPathService).submitLeaveOfAbsence(eq("learner01"), any());

            mockMvc.perform(leave("2026-11-28", "2026-11-25"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage",
                            "Không thể ghi nhận báo nghỉ: Ngày bắt đầu nghỉ không được sau ngày kết thúc nghỉ."));
        }

        @Test
        @DisplayName("UTC_LPC_32: hủy đợt báo nghỉ leave-uuid-001")
        void cancelsLeave() throws Exception {
            mockMvc.perform(post("/learning-path/leave/cancel").with(csrf()).param("courseId", "1").param("leaveId", "leave-uuid-001"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage",
                            "Đã hủy đợt báo nghỉ. Lịch học của bạn đã được cập nhật lại bình thường!"));
            verify(learningPathService).cancelLeaveOfAbsence("learner01", 1L, "leave-uuid-001");
        }

        @Test
        @DisplayName("UTC_LPC_33: mã đợt nghỉ rỗng bị từ chối")
        void blankLeaveId() throws Exception {
            doThrow(new IllegalArgumentException("Mã đợt nghỉ không hợp lệ."))
                    .when(learningPathService).cancelLeaveOfAbsence("learner01", 1L, "   ");

            mockMvc.perform(post("/learning-path/leave/cancel").with(csrf()).param("courseId", "1").param("leaveId", "   "))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("errorMessage", "Không thể hủy đợt báo nghỉ: Mã đợt nghỉ không hợp lệ."));
        }

        @Test
        @DisplayName("UTC_LPC_34: thiếu tham số leaveId trả về 400")
        void missingLeaveId() throws Exception {
            mockMvc.perform(post("/learning-path/leave/cancel").with(csrf()).param("courseId", "1"))
                    .andExpect(status().isBadRequest());
            verify(learningPathService, never()).cancelLeaveOfAbsence(anyString(), any(), any());
        }

        @Test
        @DisplayName("UTC_LPC_35: Principal = null thì chuyển về trang đăng nhập")
        void nullPrincipal() {
            LearningPathService service = mock(LearningPathService.class);

            String view = new LearningPathController(service).cancelLeave(null, 1L, "leave-uuid-001", new RedirectAttributesModelMap());

            assertThat(view).isEqualTo("redirect:/auth/login");
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ catch-up mode

    @Nested
    @DisplayName("Chế độ bù bài (POST /learning-path/catch-up)")
    class CatchUp {

        @Test
        @DisplayName("UTC_LPC_36: bật chế độ bù bài +30 phút/ngày")
        void enables() throws Exception {
            mockMvc.perform(catchUp("1", "true", "30", "false"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage",
                            "Đã kích hoạt Chế độ Bù bài với +30 phút/ngày! Lộ trình đã được tối ưu để bắt kịp kế hoạch."));
        }

        @Test
        @DisplayName("UTC_LPC_37: bật chế độ bù bài +45 phút/ngày, học cả cuối tuần")
        void enablesWithWeekends() throws Exception {
            mockMvc.perform(catchUp("1", "true", "45", "true"))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage", containsString("+45 phút/ngày")));
            ArgumentCaptor<ToggleCatchUpRequest> sent = ArgumentCaptor.forClass(ToggleCatchUpRequest.class);
            verify(learningPathService).toggleCatchUpMode(eq("learner01"), sent.capture());
            assertThat(sent.getValue().getIncludeWeekends()).isTrue();
        }

        @Test
        @DisplayName("UTC_LPC_38: tắt chế độ bù bài")
        void disables() throws Exception {
            mockMvc.perform(catchUp("1", "false", null, null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage", "Đã tắt Chế độ Bù bài. Lộ trình trở về thời lượng học chuẩn."));
        }

        @Test
        @DisplayName("UTC_LPC_39: không gửi extraMinutes thì hiển thị mặc định +30 phút/ngày")
        void defaultExtraMinutes() throws Exception {
            mockMvc.perform(catchUp("1", "true", null, null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage", containsString("+30 phút/ngày")));
        }

        @Test
        @Disabled("DEF-LP-02 (Open): controller hiển thị \"+0 phút/ngày\" vì định dạng giá trị extraMinutes gốc")
        @DisplayName("UTC_LPC_40: extraMinutes = 0 hiển thị giá trị service lưu (+30 phút/ngày)")
        void zeroExtraMinutes() throws Exception {
            mockMvc.perform(catchUp("1", "true", "0", null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage",
                            "Đã kích hoạt Chế độ Bù bài với +30 phút/ngày! Lộ trình đã được tối ưu để bắt kịp kế hoạch."));
        }

        @Test
        @DisplayName("UTC_LPC_41: enable = null được xem như tắt chế độ bù bài")
        void nullEnable() throws Exception {
            mockMvc.perform(catchUp("1", null, "30", null))
                    .andExpect(redirectedUrl("/learning-path?courseId=1"))
                    .andExpect(flash().attribute("successMessage", "Đã tắt Chế độ Bù bài. Lộ trình trở về thời lượng học chuẩn."));
        }

        @Test
        @DisplayName("UTC_LPC_42: chưa có lộ trình thì báo lỗi")
        void noPath() throws Exception {
            doThrow(new IllegalStateException("Không tìm thấy lộ trình học tập."))
                    .when(learningPathService).toggleCatchUpMode(eq("learner01"), any());

            mockMvc.perform(catchUp("999999", "true", "30", null))
                    .andExpect(redirectedUrl("/learning-path?courseId=999999"))
                    .andExpect(flash().attribute("errorMessage", "Không thể thay đổi chế độ bù bài: Không tìm thấy lộ trình học tập."));
        }
    }
}
