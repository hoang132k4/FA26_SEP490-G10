package com.jeps.modules.learningpath;

import com.jeps.modules.learningpath.dto.LearningPathDtos.LearningPathResponse;
import com.jeps.modules.learningpath.dto.LearningPathDtos.LearningPathTimelineItemDto;
import com.jeps.modules.learningpath.dto.LearningPathDtos.RecalibratePreviewResponse;
import com.jeps.modules.learningpath.dto.LearningPathDtos.RecalibrateRequest;
import com.jeps.modules.learningpath.dto.LearningPathDtos.SkillMetricDto;
import com.jeps.modules.learningpath.dto.LearningPathDtos.SkillProficiency;
import com.jeps.modules.learningpath.dto.LearningPathDtos.SkillProfileDto;
import com.jeps.modules.learningpath.service.SkillAnalysisService;
import com.jeps.support.LocalPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Report 5.2 – sheet Learning Path_LongBM (ITC_LP_01 → ITC_LP_18).
 *
 * <p>Chạy toàn bộ luồng Controller → Service → PostgreSQL jeps_test với tài khoản seed learner01@test.local.
 * Chỉ {@link SkillAnalysisService} (phân tích kết quả bài thi) được giả lập để dựng hồ sơ kỹ năng cho ITC_LP_05;
 * ở các TC khác nó trả về null (chưa có hồ sơ kỹ năng). Ngày được tính tương đối theo ngày chạy test.</p>
 */
@WithMockUser(username = "learner01@test.local", roles = "LEARNER")
class LearningPathIntegrationTest extends LocalPostgresIntegrationTest {

    private static final String ALL_DAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @MockBean
    private SkillAnalysisService skillAnalysisService;

    private final LocalDate today = LocalDate.now();
    private long learnerId;
    private long courseId;
    private List<Long> lessonIds;

    @BeforeEach
    void setUpLearner() {
        learnerId = userId("learner01@test.local");
        // Học viên chỉ đăng ký các khóa học do test tạo ra (dữ liệu cũ được xóa trong transaction rồi rollback).
        jdbc.update("DELETE FROM learning_paths WHERE user_id = ?", learnerId);
        jdbc.update("DELETE FROM course_registrations WHERE user_id = ?", learnerId);
        courseId = createCourse("IT LP Course", "N5");
        lessonIds = addLessons(courseId, 5, "VOCABULARY");
        enroll(learnerId, courseId, 0);
    }

    // ------------------------------------------------------------------ fixtures (JDBC trong transaction)

    private long userId(String email) {
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private long createCourse(String title, String level) {
        return jdbc.queryForObject("""
                INSERT INTO courses (code, title, level, price, status, short_description, full_description, objectives,
                                     enrollment_count, lesson_count, duration_hours, is_featured,
                                     created_at, created_date, published_date, updated_at)
                VALUES (?, ?, ?, 0, 'PUBLISHED', 'IT', 'IT', 'IT', 0, 0, 10, false, now(), now(), now(), now())
                RETURNING course_id
                """, Long.class, "IT_LP_" + System.nanoTime(), title, level);
    }

    private long addLesson(long course, int order, String title, String skill, int minutes, String status) {
        return jdbc.queryForObject("""
                INSERT INTO lessons (course_id, order_index, lesson_order, title, lesson_title, skill_type, lesson_type,
                                     section_name, status, is_free_preview, duration_minutes, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'Chapter 1', ?, false, ?, now(), now())
                RETURNING lesson_id
                """, Long.class, course, order, order, title, title, skill, skill, status, minutes);
    }

    private List<Long> addLessons(long course, int count, String skill) {
        int last = jdbc.queryForObject("SELECT COALESCE(MAX(order_index), 0) FROM lessons WHERE course_id = ?", Integer.class, course);
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) ids.add(addLesson(course, last + i, "IT Lesson " + (last + i), skill, 60, "ACTIVE"));
        return ids;
    }

    private void enroll(long user, long course, int progress) {
        jdbc.update("""
                INSERT INTO course_registrations (id, user_id, course_id, registered_at, status, progress_percent,
                                                  paid_amount, payment_method, created_at, updated_at)
                VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM course_registrations), ?, ?, now(), 'ACTIVE', ?, 0, 'FREE', now(), now())
                """, user, course, progress);
    }

    /** Lộ trình học: dailyHours giờ/ngày, các ngày học studyDays, bắt đầu từ hôm nay. */
    private long createPath(long course, String dailyHours, String studyDays) {
        return jdbc.queryForObject("""
                INSERT INTO learning_paths (user_id, course_id, daily_hours_setup, target_level, current_level, path_status,
                                            scheduled_date, study_days, schedule_mode)
                VALUES (?, ?, ?, 'N5', 'N5', 'ACTIVE', ?, ?, 'CUSTOM')
                RETURNING learning_path_id
                """, Long.class, learnerId, course, new BigDecimal(dailyHours), today, studyDays);
    }

    private Map<String, Object> pathRow(long pathId) {
        flushAndClear();
        return jdbc.queryForMap("SELECT * FROM learning_paths WHERE learning_path_id = ?", pathId);
    }

    private LearningPathResponse viewPath(long course) throws Exception {
        MvcResult result = mockMvc.perform(get("/learning-path").param("courseId", String.valueOf(course)))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/view"))
                .andReturn();
        return (LearningPathResponse) result.getModelAndView().getModel().get("path");
    }

    private static List<LocalDate> plannedDates(LearningPathResponse path) {
        return path.getItems().stream().map(LearningPathTimelineItemDto::getPlannedDate).toList();
    }

    private static LocalDate lastPlannedDate(LearningPathResponse path) {
        return path.getItems().get(path.getItems().size() - 1).getPlannedDate();
    }

    private MockHttpServletRequestBuilder recalibrate(String url, String weeklyHours, LocalDate start, String... days) {
        MockHttpServletRequestBuilder request = post(url).with(csrf())
                .param("courseId", String.valueOf(courseId)).param("weeklyHours", weeklyHours)
                .param("startDate", start.toString());
        if (days.length > 0) request.param("studyDays", days);
        return request;
    }

    // ------------------------------------------------------------------ Personalized Learning Path

    @Test
    @DisplayName("ITC_LP_01: xem lộ trình cá nhân hóa (tên khóa học, timeline theo tuần/ngày, % tiến độ, chuỗi ngày học, thẻ bài hôm nay)")
    void viewLearningPath() throws Exception {
        createPath(courseId, "1.0", ALL_DAYS);

        LearningPathResponse path = viewPath(courseId);

        assertThat(path.isHasPath()).isTrue();
        assertThat(path.getCourseTitle()).isEqualTo("IT LP Course");
        assertThat(path.getItems()).hasSize(5);
        assertThat(path.getWeeks()).isNotEmpty();
        assertThat(path.getProgressPercent()).isZero();
        assertThat(path.getTodayFocus()).isNotNull();
        assertThat(path.getTodayFocus().getStreakDays()).isGreaterThanOrEqualTo(0);
        assertThat(path.getTodayFocus().getTodayLessons()).isNotEmpty();
    }

    @Test
    @DisplayName("ITC_LP_02: chuyển khóa học bằng Course Switcher, chỉ hiển thị dữ liệu của khóa học được chọn")
    void switchCourse() throws Exception {
        long secondCourse = createCourse("IT LP Second Course", "N4");
        List<Long> secondLessons = addLessons(secondCourse, 3, "GRAMMAR");
        enroll(learnerId, secondCourse, 0);

        LearningPathResponse path = viewPath(secondCourse);

        assertThat(path.getCourseId()).isEqualTo(secondCourse);
        assertThat(path.getCourseTitle()).isEqualTo("IT LP Second Course");
        assertThat(path.getItems()).extracting(LearningPathTimelineItemDto::getId).containsExactlyElementsOf(secondLessons);
        assertThat(path.getEnrolledCourses()).extracting(c -> c.getCourseId()).contains(courseId, secondCourse);
        assertThat(path.getEnrolledCourses()).filteredOn(c -> c.isSelected()).extracting(c -> c.getCourseId()).containsExactly(secondCourse);
    }

    @Test
    @WithMockUser(username = "learner02@test.local", roles = "LEARNER")
    @DisplayName("ITC_LP_03: học viên chưa đăng ký khóa học nào thấy trạng thái rỗng và nút khám phá khóa học "
            + "(thực tế: nút 'Khám phá khóa học' → /learning/courses, report ghi 'Khám phá khóa học ngay' → /courses)")
    void emptyStateWithoutEnrollment() throws Exception {
        long learner2 = userId("learner02@test.local");
        jdbc.update("DELETE FROM learning_paths WHERE user_id = ?", learner2);
        jdbc.update("DELETE FROM course_registrations WHERE user_id = ?", learner2);

        mockMvc.perform(get("/learning-path"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasPath", false))
                .andExpect(model().attribute("emptyMessage", startsWith("Bạn chưa đăng ký khóa học nào")))
                .andExpect(content().string(containsString("href=\"/learning/courses\"")))
                .andExpect(content().string(containsString("Khám phá khóa học")));
    }

    @Test
    @DisplayName("ITC_LP_04: mở lộ trình của khóa học chưa đăng ký (?courseId=999999) không lộ dữ liệu, hiển thị cảnh báo "
            + "và danh sách khóa học đã đăng ký để chọn")
    void guardUnenrolledCourse() throws Exception {
        long foreign = jdbc.queryForObject("SELECT COALESCE(MAX(course_id), 0) + 1000 FROM courses", Long.class);

        MvcResult result = mockMvc.perform(get("/learning-path").param("courseId", String.valueOf(foreign)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasPath", false))
                .andExpect(model().attribute("emptyMessage", startsWith("Bạn chưa đăng ký khóa học này hoặc khóa học chưa")))
                .andReturn();

        LearningPathResponse path = (LearningPathResponse) result.getModelAndView().getModel().get("path");
        assertThat(path.getItems()).isEmpty();
        assertThat(path.getEnrolledCourses()).extracting(c -> c.getCourseId()).containsExactly(courseId);
    }

    @Test
    @DisplayName("ITC_LP_05: kỹ năng yếu (Kanji) tăng thời lượng 60 → 90 phút, kỹ năng mạnh (Ngữ pháp) giảm 60 → 45 phút "
            + "(badge thực tế: 'Trọng tâm cần củng cố [x1.5]' / 'Nền tảng vững [x0.75]')")
    void skillAwareDurations() throws Exception {
        long skillCourse = createCourse("IT Skill Course", "N5");
        long kanji = addLesson(skillCourse, 1, "IT Kanji lesson", "KANJI", 60, "ACTIVE");
        long grammar = addLesson(skillCourse, 2, "IT Grammar lesson", "GRAMMAR", 60, "ACTIVE");
        enroll(learnerId, skillCourse, 0);
        Map<String, SkillMetricDto> skills = new LinkedHashMap<>();
        skills.put("KANJI", SkillMetricDto.builder().skillKey("KANJI").proficiency(SkillProficiency.WEAK).build());
        skills.put("GRAMMAR", SkillMetricDto.builder().skillKey("GRAMMAR").proficiency(SkillProficiency.STRONG).build());
        when(skillAnalysisService.analyzeAndRecalibrateSkills(anyLong(), anyLong(), any(), any()))
                .thenReturn(SkillProfileDto.builder().userId(learnerId).assessmentTaken(true).skills(skills).build());

        LearningPathResponse path = viewPath(skillCourse);

        LearningPathTimelineItemDto kanjiItem = path.getItems().stream().filter(i -> i.getId().equals(kanji)).findFirst().orElseThrow();
        LearningPathTimelineItemDto grammarItem = path.getItems().stream().filter(i -> i.getId().equals(grammar)).findFirst().orElseThrow();
        assertThat(kanjiItem.getRecommendedDurationMinutes()).isEqualTo(90);
        assertThat(kanjiItem.getFocusBadgeText()).isEqualTo("Trọng tâm cần củng cố [x1.5]");
        assertThat(grammarItem.getRecommendedDurationMinutes()).isEqualTo(45);
        assertThat(grammarItem.getFocusBadgeText()).isEqualTo("Nền tảng vững [x0.75]");
    }

    @Test
    @DisplayName("ITC_LP_06: thẻ so sánh Kế hoạch ban đầu hiển thị ngày baseline, ngày dự kiến và độ lệch (+3 ngày, DELAYED)")
    void baselineComparison() throws Exception {
        long pathId = createPath(courseId, "1.0", ALL_DAYS);
        jdbc.update("UPDATE learning_paths SET baseline_start_date = ?, baseline_end_date = ?, baseline_daily_hours = 1.0 "
                + "WHERE learning_path_id = ?", today, today.plusDays(1), pathId);

        LearningPathResponse path = viewPath(courseId);

        assertThat(path.getBaselineStartDate()).isEqualTo(today);
        assertThat(path.getBaselineEndDate()).isEqualTo(today.plusDays(1));
        assertThat(path.getProjectedEndDateFormatted()).isEqualTo(today.plusDays(4).format(DMY));
        assertThat(path.getBaselineDaysVariance()).isEqualTo(3L);
        assertThat(path.getBaselineVarianceShortText()).isEqualTo("+3 ngày");
        assertThat(path.getPlanComparisonStatus()).isEqualTo("DELAYED");
    }

    @Test
    @DisplayName("ITC_LP_07: hoàn thành bài đang học → bài chuyển COMPLETED, tiến độ tăng, chuỗi ngày học tính hôm nay, bài kế tiếp IN_PROGRESS")
    void completeLessonUpdatesProgress() throws Exception {
        createPath(courseId, "1.0", ALL_DAYS);
        long first = lessonIds.get(0);
        long second = lessonIds.get(1);

        mockMvc.perform(post("/learning/lessons/" + first + "/complete").with(csrf()))
                .andExpect(redirectedUrl("/learning/lessons/" + second));

        LearningPathResponse path = viewPath(courseId);
        assertThat(path.getItems().get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(path.getItems().get(1).getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(path.getCompletedLessons()).isEqualTo(1);
        assertThat(path.getRemainingLessons()).isEqualTo(4);
        flushAndClear();
        assertThat(jdbc.queryForObject("SELECT progress_percent FROM course_registrations WHERE user_id = ? AND course_id = ?",
                Integer.class, learnerId, courseId)).isEqualTo(20);
        assertThat(path.getTodayFocus().getStreakDays()).isGreaterThanOrEqualTo(1);
    }

    // ------------------------------------------------------------------ Learning Schedule Adjustment

    @Test
    @DisplayName("ITC_LP_08: màn hình điều chỉnh lịch (bước 1) điền sẵn ngày học, giờ/ngày, giờ/tuần và ngày bắt đầu từ database")
    void recalibrateScreenPrefilled() throws Exception {
        createPath(courseId, "1.0", ALL_DAYS);

        MvcResult result = mockMvc.perform(get("/learning-path/recalibrate").param("courseId", String.valueOf(courseId)))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/recalibrate"))
                .andExpect(model().attribute("currentStep", 1))
                .andReturn();

        RecalibrateRequest request = (RecalibrateRequest) result.getModelAndView().getModel().get("request");
        assertThat(request.getCourseId()).isEqualTo(courseId);
        assertThat(request.getDailyHours()).isEqualByComparingTo("1.0");
        assertThat(request.getWeeklyHours()).isEqualTo(7);
        assertThat(request.getStudyDays()).containsExactlyInAnyOrder(ALL_DAYS.split(","));
        assertThat(request.getStartDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("ITC_LP_09: mô phỏng 14 giờ/tuần hiển thị bước 2 với ngày hoàn thành sớm hơn, không ghi gì vào learning_paths")
    void simulateWithoutWriting() throws Exception {
        long pathId = createPath(courseId, "1.0", ALL_DAYS);
        viewPath(courseId); // khởi tạo baseline trước, để bước mô phỏng không còn gì cần ghi
        Map<String, Object> before = pathRow(pathId);

        MvcResult result = mockMvc.perform(recalibrate("/learning-path/recalibrate", "14", today, ALL_DAYS.split(",")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("simulationSuccess", true))
                .andExpect(model().attribute("currentStep", 2))
                .andReturn();

        RecalibratePreviewResponse preview = (RecalibratePreviewResponse) result.getModelAndView().getModel().get("preview");
        assertThat(preview.getNewWeeklyHours()).isEqualTo(14);
        assertThat(preview.getNewCompletionDate()).isNotEqualTo(preview.getOriginalCompletionDate());
        assertThat(pathRow(pathId)).isEqualTo(before);
    }

    @Test
    @DisplayName("ITC_LP_10: áp dụng lịch mới 14 giờ/tuần, learning_paths cập nhật daily_hours_setup / study_days và timeline tính lại")
    void applySchedule() throws Exception {
        long pathId = createPath(courseId, "1.0", ALL_DAYS);

        mockMvc.perform(recalibrate("/learning-path/recalibrate/apply", "14", today, ALL_DAYS.split(",")))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("successMessage",
                        "Lịch học mới đã được áp dụng thành công! Lộ trình đã được cập nhật với thời lượng 14 giờ/tuần."));

        Map<String, Object> row = pathRow(pathId);
        assertThat((BigDecimal) row.get("daily_hours_setup")).isEqualByComparingTo("2.00");
        assertThat(row.get("study_days")).isEqualTo(ALL_DAYS);
        LearningPathResponse path = viewPath(courseId);
        assertThat(path.getWeeklyHours()).isEqualTo(14);
        assertThat(plannedDates(path)).containsExactly(today, today, today.plusDays(1), today.plusDays(1), today.plusDays(2));
    }

    @Test
    @DisplayName("ITC_LP_11: đã hoàn thành 100% bài học thì không cho lưu lịch mới, learning_paths giữ nguyên "
            + "(thông báo thực tế: 'Bạn đã hoàn thành tất cả... Không thể lưu thay đổi lịch học.')")
    void preventRecalibrationWhenCompleted() throws Exception {
        long pathId = createPath(courseId, "1.0", ALL_DAYS);
        jdbc.update("UPDATE course_registrations SET progress_percent = 100 WHERE user_id = ? AND course_id = ?", learnerId, courseId);

        mockMvc.perform(recalibrate("/learning-path/recalibrate/apply", "14", today, ALL_DAYS.split(",")))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("errorMessage",
                        "Bạn đã hoàn thành tất cả các bài học trong khóa học này! Không thể lưu thay đổi lịch học."));

        Map<String, Object> row = pathRow(pathId);
        assertThat((BigDecimal) row.get("daily_hours_setup")).isEqualByComparingTo("1.0");
        assertThat(row.get("study_days")).isEqualTo(ALL_DAYS);
    }

    @Test
    @DisplayName("ITC_LP_12: ngày bắt đầu là hôm qua → báo lỗi, ngày bắt đầu được đổi về hôm nay "
            + "(thực tế bản xem trước vẫn được tính từ hôm nay, report ghi 'không mô phỏng')")
    void pastStartDateCorrected() throws Exception {
        createPath(courseId, "1.0", ALL_DAYS);

        MvcResult result = mockMvc.perform(recalibrate("/learning-path/recalibrate", "14", today.minusDays(1), ALL_DAYS.split(",")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage",
                        "Ngày bắt đầu học dự kiến không được trước ngày hôm nay (" + today.format(DMY) + ")."))
                .andReturn();

        RecalibrateRequest request = (RecalibrateRequest) result.getModelAndView().getModel().get("request");
        assertThat(request.getStartDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("ITC_LP_13: báo nghỉ 5 ngày (tương đương 01–05/12/2026 trong report) → leave_records có bản ghi mới, "
            + "các bài trong khoảng nghỉ được dời ra sau 5 ngày")
    void submitLeaveShiftsTimeline() throws Exception {
        addLessons(courseId, 5, "VOCABULARY");
        long pathId = createPath(courseId, "1.0", ALL_DAYS);
        LocalDate start = today.plusDays(3);
        LocalDate end = today.plusDays(7);
        LocalDate lastBefore = lastPlannedDate(viewPath(courseId));

        mockMvc.perform(post("/learning-path/leave").with(csrf()).param("courseId", String.valueOf(courseId))
                        .param("startDate", start.toString()).param("endDate", end.toString()).param("reason", "Bận công tác"))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("successMessage", startsWith(
                        "Đã ghi nhận báo nghỉ từ " + start.format(DMY) + " đến " + end.format(DMY) + ".")));

        assertThat(String.valueOf(pathRow(pathId).get("leave_records"))).contains("Bận công tác", start.toString(), end.toString());
        LearningPathResponse path = viewPath(courseId);
        assertThat(plannedDates(path)).noneMatch(d -> !d.isBefore(start) && !d.isAfter(end));
        assertThat(lastPlannedDate(path)).isEqualTo(lastBefore.plusDays(5));
    }

    @Test
    @DisplayName("ITC_LP_14: ngày kết thúc trước ngày bắt đầu bị từ chối, leave_records không đổi "
            + "(thông báo thực tế có tiền tố 'Không thể ghi nhận báo nghỉ: ')")
    void rejectInvalidLeave() throws Exception {
        long pathId = createPath(courseId, "1.0", ALL_DAYS);

        mockMvc.perform(post("/learning-path/leave").with(csrf()).param("courseId", String.valueOf(courseId))
                        .param("startDate", today.plusDays(13).toString()).param("endDate", today.plusDays(8).toString()))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("errorMessage",
                        "Không thể ghi nhận báo nghỉ: Ngày bắt đầu nghỉ không được sau ngày kết thúc nghỉ."));

        assertThat(pathRow(pathId).get("leave_records")).isNull();
    }

    @Test
    @DisplayName("ITC_LP_15: hủy đợt nghỉ leave-uuid-001 → bản ghi bị xóa khỏi leave_records, ngày hoàn thành quay về sớm hơn")
    void cancelLeaveRestoresTimeline() throws Exception {
        long pathId = createPath(courseId, "1.0", ALL_DAYS);
        jdbc.update("UPDATE learning_paths SET leave_records = CAST(? AS jsonb) WHERE learning_path_id = ?",
                "[{\"id\":\"leave-uuid-001\",\"startDate\":\"" + today.plusDays(1) + "\",\"endDate\":\"" + today.plusDays(2)
                        + "\",\"reason\":\"Ốm\"}]", pathId);
        LocalDate lastWithLeave = lastPlannedDate(viewPath(courseId));

        mockMvc.perform(post("/learning-path/leave/cancel").with(csrf())
                        .param("courseId", String.valueOf(courseId)).param("leaveId", "leave-uuid-001"))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("successMessage",
                        "Đã hủy đợt báo nghỉ. Lịch học của bạn đã được cập nhật lại bình thường!"));

        assertThat(String.valueOf(pathRow(pathId).get("leave_records"))).doesNotContain("leave-uuid-001");
        LearningPathResponse path = viewPath(courseId);
        assertThat(lastPlannedDate(path)).isEqualTo(lastWithLeave.minusDays(2));
    }

    @Test
    @DisplayName("ITC_LP_16: bật bù bài +30 phút/ngày kèm cuối tuần → database cập nhật, ngày hoàn thành được kéo sớm hơn")
    void enableCatchUp() throws Exception {
        addLessons(courseId, 5, "VOCABULARY");
        long pathId = createPath(courseId, "1.0", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        jdbc.update("UPDATE learning_paths SET baseline_start_date = ?, baseline_end_date = ? WHERE learning_path_id = ?",
                today.minusDays(14), today, pathId);
        LocalDate before = lastPlannedDate(viewPath(courseId));

        mockMvc.perform(post("/learning-path/catch-up").with(csrf()).param("courseId", String.valueOf(courseId))
                        .param("enable", "true").param("extraMinutes", "30").param("includeWeekends", "true"))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("successMessage",
                        "Đã kích hoạt Chế độ Bù bài với +30 phút/ngày! Lộ trình đã được tối ưu để bắt kịp kế hoạch."));

        Map<String, Object> row = pathRow(pathId);
        assertThat(row.get("catch_up_mode")).isEqualTo(true);
        assertThat(row.get("catch_up_extra_minutes")).isEqualTo(30);
        assertThat(row.get("catch_up_include_weekends")).isEqualTo(true);
        assertThat(lastPlannedDate(viewPath(courseId))).isBefore(before);
    }

    @Test
    @DisplayName("ITC_LP_17: tắt bù bài → catch_up_mode = false, lịch quay về nhịp học chuẩn (không học cuối tuần)")
    void disableCatchUp() throws Exception {
        addLessons(courseId, 5, "VOCABULARY");
        long pathId = createPath(courseId, "1.0", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        jdbc.update("UPDATE learning_paths SET catch_up_mode = true, catch_up_extra_minutes = 30, "
                + "catch_up_include_weekends = true WHERE learning_path_id = ?", pathId);
        LocalDate withCatchUp = lastPlannedDate(viewPath(courseId));

        mockMvc.perform(post("/learning-path/catch-up").with(csrf()).param("courseId", String.valueOf(courseId))
                        .param("enable", "false"))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId))
                .andExpect(flash().attribute("successMessage", "Đã tắt Chế độ Bù bài. Lộ trình trở về thời lượng học chuẩn."));

        assertThat(pathRow(pathId).get("catch_up_mode")).isEqualTo(false);
        LearningPathResponse path = viewPath(courseId);
        assertThat(lastPlannedDate(path)).isAfter(withCatchUp);
        assertThat(plannedDates(path)).noneMatch(d -> d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    @Test
    @DisplayName("ITC_LP_18: chỉ chọn học Thứ 2, Thứ 4, Thứ 6 → mọi bài (10 bài) đều rơi vào 3 ngày này")
    void customStudyDays() throws Exception {
        addLessons(courseId, 5, "VOCABULARY");
        createPath(courseId, "1.0", ALL_DAYS);

        mockMvc.perform(recalibrate("/learning-path/recalibrate/apply", "3", today, "MONDAY", "WEDNESDAY", "FRIDAY"))
                .andExpect(redirectedUrl("/learning-path?courseId=" + courseId));

        LearningPathResponse path = viewPath(courseId);
        assertThat(path.getItems()).hasSize(10);
        assertThat(plannedDates(path)).allMatch(d -> Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                .contains(d.getDayOfWeek()));
    }
}
