package com.jeps.modules.learningpath;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.course.service.CourseRegistrationService;
import com.jeps.modules.exam.service.ExamService;
import com.jeps.modules.learningpath.dto.LearningPathDtos.LearningPathResponse;
import com.jeps.modules.learningpath.dto.LearningPathDtos.LearningPathTimelineItemDto;
import com.jeps.modules.learningpath.dto.LearningPathDtos.LeaveRecordDto;
import com.jeps.modules.learningpath.dto.LearningPathDtos.RecalibratePreviewResponse;
import com.jeps.modules.learningpath.dto.LearningPathDtos.RecalibrateRequest;
import com.jeps.modules.learningpath.dto.LearningPathDtos.SubmitLeaveRequest;
import com.jeps.modules.learningpath.dto.LearningPathDtos.ToggleCatchUpRequest;
import com.jeps.modules.learningpath.entity.LearningPath;
import com.jeps.modules.learningpath.repository.LearningPathRepository;
import com.jeps.modules.learningpath.service.LessonProgressService;
import com.jeps.modules.learningpath.service.SkillAnalysisService;
import com.jeps.modules.learningpath.service.SkillFocusRecommendationService;
import com.jeps.modules.learningpath.service.impl.LearningPathServiceImpl;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Report 5.1 – sheet LearningPathService (UTC_LPS_01 → UTC_LPS_36).
 * Service layer: repository / collaborator là mock; ngày hiện tại được cố định bằng
 * Mockito.mockStatic(LocalDate.class, CALLS_REAL_METHODS) như mô tả trong môi trường test của report.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LearningPathServiceTest {

    private static final LocalDate MONDAY_23 = LocalDate.of(2026, 11, 23);
    private static final long USER_ID = 7L;
    private static final long COURSE_ID = 1L;

    @Mock private LearningPathRepository learningPathRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private CourseRegistrationService courseRegistrationService;
    @Mock private SkillAnalysisService skillAnalysisService;
    @Mock private SkillFocusRecommendationService skillFocusRecommendationService;
    @Mock private LessonProgressService lessonProgressService;
    @Mock private ExamService examService;

    /** Cấu hình giống ObjectMapper mặc định của Spring Boot (JavaTimeModule, bỏ qua thuộc tính lạ). */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private LearningPathServiceImpl service;
    private MockedStatic<LocalDate> clock;
    private Course course;
    private LearningPath lp;
    private List<Lesson> lessons;

    @BeforeEach
    void setUp() {
        service = new LearningPathServiceImpl(learningPathRepository, userRepository, courseRepository, lessonRepository,
                courseRegistrationRepository, courseRegistrationService, skillAnalysisService,
                skillFocusRecommendationService, lessonProgressService, examService, objectMapper);
        today(MONDAY_23);

        User user = User.builder().username("learner01").email("learner01@jeps.edu.vn").build();
        user.setId(USER_ID);
        when(userRepository.findByEmailIgnoreCase("learner01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("learner01")).thenReturn(Optional.of(user));

        course = Course.builder().id(COURSE_ID).title("N5 Course").level("N5").status("PUBLISHED").build();
        lp = LearningPath.builder().id(5L).userId(USER_ID).courseId(COURSE_ID)
                .dailyHoursSetup(BigDecimal.ONE).scheduledDate(MONDAY_23).pathStatus("ACTIVE").build();
        lessons = lessons(3);
    }

    @AfterEach
    void tearDown() {
        clock.close();
    }

    // ------------------------------------------------------------------ fixtures

    /** Cố định LocalDate.now(); các phương thức khác của LocalDate vẫn chạy thật. */
    private void today(LocalDate date) {
        if (clock != null) clock.close();
        clock = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
        clock.when(() -> LocalDate.now()).thenReturn(date);
    }

    private List<Lesson> lessons(int count) {
        List<Lesson> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Lesson lesson = Lesson.builder().course(course).title("Lesson " + i).sectionName("Chapter 1")
                    .durationMinutes(60).lessonType("VIDEO").skillType("VOCABULARY").status("ACTIVE").orderIndex(i).build();
            lesson.setId(100L + i);
            result.add(lesson);
        }
        return result;
    }

    /** Học viên đã đăng ký khóa học id=1 (PUBLISHED) và có lộ trình lp. */
    private void hasPublishedPath() {
        CourseRegistration registration = CourseRegistration.builder().course(course).progressPercent(0).build();
        when(courseRegistrationService.getUserRegistrations(USER_ID)).thenReturn(List.of(registration));
        when(courseRegistrationRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(registration));
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(USER_ID, COURSE_ID))
                .thenReturn(Optional.of(lp));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(COURSE_ID)).thenAnswer(inv -> lessons);
    }

    private void hasNoPublishedPath() {
        when(courseRegistrationService.getUserRegistrations(USER_ID)).thenReturn(List.of());
    }

    private void withLeaves(LeaveRecordDto... records) throws Exception {
        lp.setLeaveRecords(objectMapper.writeValueAsString(List.of(records)));
    }

    private static LeaveRecordDto leave(String id, LocalDate start, LocalDate end) {
        return LeaveRecordDto.builder().id(id).startDate(start).endDate(end).build();
    }

    private List<LeaveRecordDto> savedLeaves() throws Exception {
        return objectMapper.readValue(lp.getLeaveRecords(), new TypeReference<List<LeaveRecordDto>>() { });
    }

    private static SubmitLeaveRequest leaveRequest(LocalDate start, LocalDate end) {
        SubmitLeaveRequest request = new SubmitLeaveRequest();
        request.setCourseId(COURSE_ID);
        request.setStartDate(start);
        request.setEndDate(end);
        return request;
    }

    private static ToggleCatchUpRequest catchUp(Boolean enable, Integer extraMinutes, Boolean weekends) {
        ToggleCatchUpRequest request = new ToggleCatchUpRequest();
        request.setCourseId(COURSE_ID);
        request.setEnable(enable);
        request.setExtraMinutes(extraMinutes);
        request.setIncludeWeekends(weekends);
        return request;
    }

    private static LocalDate nov(int day) {
        return LocalDate.of(2026, 11, day);
    }

    private static List<LocalDate> plannedDates(LearningPathResponse response) {
        return response.getItems().stream().map(LearningPathTimelineItemDto::getPlannedDate).toList();
    }

    // ------------------------------------------------------------------ submitLeaveOfAbsence

    @Nested
    @DisplayName("submitLeaveOfAbsence")
    class SubmitLeave {

        @Test
        @DisplayName("UTC_LPS_01: báo nghỉ 25 → 27/11/2026 tạo bản ghi mới (3 ngày, sắp tới)")
        void registersLeave() throws Exception {
            hasPublishedPath();

            service.submitLeaveOfAbsence("learner01", leaveRequest(nov(25), nov(27)));

            verify(learningPathRepository, times(1)).save(lp);
            List<LeaveRecordDto> records = savedLeaves();
            assertThat(records).hasSize(1);
            assertThat(UUID.fromString(records.get(0).getId())).isNotNull();
            assertThat(records.get(0).getTotalDays()).isEqualTo(3);
            assertThat(records.get(0).isUpcoming()).isTrue();
        }

        @Test
        @DisplayName("UTC_LPS_02: báo nghỉ 1 ngày ngay hôm nay (activeNow = true)")
        void singleDayToday() throws Exception {
            hasPublishedPath();

            service.submitLeaveOfAbsence("learner01", leaveRequest(MONDAY_23, MONDAY_23));

            verify(learningPathRepository, times(1)).save(lp);
            LeaveRecordDto record = savedLeaves().get(0);
            assertThat(record.getTotalDays()).isEqualTo(1);
            assertThat(record.isActiveNow()).isTrue();
        }

        @Test
        @DisplayName("UTC_LPS_03: ngày kết thúc là hôm qua bị từ chối")
        void endDateInPast() {
            hasPublishedPath();

            assertThatThrownBy(() -> service.submitLeaveOfAbsence("learner01", leaveRequest(nov(20), nov(22))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ngày kết thúc nghỉ không được ở trong quá khứ.");
            verify(learningPathRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTC_LPS_04: ngày bắt đầu hôm qua nhưng kết thúc hôm nay được chấp nhận")
        void pastStartCurrentEnd() throws Exception {
            hasPublishedPath();

            service.submitLeaveOfAbsence("learner01", leaveRequest(nov(22), MONDAY_23));

            verify(learningPathRepository, times(1)).save(lp);
            assertThat(savedLeaves()).hasSize(1);
        }

        @Test
        @DisplayName("UTC_LPS_05: ngày bắt đầu sau ngày kết thúc bị từ chối")
        void startAfterEnd() {
            assertThatThrownBy(() -> service.submitLeaveOfAbsence("learner01", leaveRequest(nov(28), nov(25))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ngày bắt đầu nghỉ không được sau ngày kết thúc nghỉ.");
        }

        @Test
        @DisplayName("UTC_LPS_06: thiếu ngày bắt đầu bị từ chối")
        void missingStartDate() {
            assertThatThrownBy(() -> service.submitLeaveOfAbsence("learner01", leaveRequest(null, nov(27))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ngày bắt đầu và ngày kết thúc nghỉ không được để trống.");
        }

        @Test
        @DisplayName("UTC_LPS_07: chưa có lộ trình học tập thì không thể báo nghỉ")
        void noLearningPath() {
            hasNoPublishedPath();

            assertThatThrownBy(() -> service.submitLeaveOfAbsence("learner01", leaveRequest(nov(25), nov(27))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Không tìm thấy lộ trình học tập để ghi nhận báo nghỉ.");
        }
    }

    // ------------------------------------------------------------------ cancelLeaveOfAbsence

    @Nested
    @DisplayName("cancelLeaveOfAbsence")
    class CancelLeave {

        @Test
        @DisplayName("UTC_LPS_08: hủy đợt nghỉ leave-uuid-001")
        void cancelsLeave() throws Exception {
            hasPublishedPath();
            withLeaves(leave("leave-uuid-001", nov(24), nov(25)), leave("leave-uuid-002", nov(28), nov(28)));

            service.cancelLeaveOfAbsence("learner01", COURSE_ID, "leave-uuid-001");

            verify(learningPathRepository, times(1)).save(lp);
            assertThat(savedLeaves()).extracting(LeaveRecordDto::getId).containsExactly("leave-uuid-002");
        }

        @Test
        @DisplayName("UTC_LPS_09: mã đợt nghỉ rỗng bị từ chối")
        void blankLeaveId() {
            assertThatThrownBy(() -> service.cancelLeaveOfAbsence("learner01", COURSE_ID, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Mã đợt nghỉ không hợp lệ.");
        }

        @Test
        @DisplayName("UTC_LPS_10: mã đợt nghỉ không tồn tại bị từ chối, không lưu gì "
                + "(report ghi \"bỏ qua và vẫn lưu\" – code hiện tại đã đổi, cần cập nhật report)")
        void unknownLeaveId() throws Exception {
            hasPublishedPath();
            withLeaves(leave("leave-uuid-001", nov(24), nov(25)));

            assertThatThrownBy(() -> service.cancelLeaveOfAbsence("learner01", COURSE_ID, "leave-uuid-999"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy đợt báo nghỉ cần hủy.");
            verify(learningPathRepository, never()).save(any());
            assertThat(savedLeaves()).extracting(LeaveRecordDto::getId).containsExactly("leave-uuid-001");
        }

        @Test
        @DisplayName("UTC_LPS_11: chưa có lộ trình học tập thì không thể hủy đợt nghỉ")
        void noLearningPath() {
            hasNoPublishedPath();

            assertThatThrownBy(() -> service.cancelLeaveOfAbsence("learner01", COURSE_ID, "leave-uuid-001"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Không tìm thấy lộ trình học tập.");
        }
    }

    // ------------------------------------------------------------------ toggleCatchUpMode

    @Nested
    @DisplayName("toggleCatchUpMode")
    class CatchUp {

        @Test
        @DisplayName("UTC_LPS_12: bật bù bài +30 phút/ngày, không học cuối tuần")
        void enables() {
            hasPublishedPath();

            service.toggleCatchUpMode("learner01", catchUp(true, 30, false));

            verify(learningPathRepository, times(1)).save(lp);
            assertThat(lp.getCatchUpMode()).isTrue();
            assertThat(lp.getCatchUpExtraMinutes()).isEqualTo(30);
            assertThat(lp.getCatchUpIncludeWeekends()).isFalse();
        }

        @Test
        @DisplayName("UTC_LPS_13: bật bù bài +60 phút/ngày kèm cuối tuần")
        void enablesWithWeekends() {
            hasPublishedPath();

            service.toggleCatchUpMode("learner01", catchUp(true, 60, true));

            assertThat(lp.getCatchUpExtraMinutes()).isEqualTo(60);
            assertThat(lp.getCatchUpIncludeWeekends()).isTrue();
        }

        @Test
        @DisplayName("UTC_LPS_14: extraMinutes = null dùng mặc định 30")
        void nullExtraMinutes() {
            hasPublishedPath();

            service.toggleCatchUpMode("learner01", catchUp(true, null, null));

            assertThat(lp.getCatchUpExtraMinutes()).isEqualTo(30);
        }

        @Test
        @DisplayName("UTC_LPS_15: extraMinutes = 0 được thay bằng mặc định 30")
        void zeroExtraMinutes() {
            hasPublishedPath();

            service.toggleCatchUpMode("learner01", catchUp(true, 0, null));

            assertThat(lp.getCatchUpExtraMinutes()).isEqualTo(30);
        }

        @Test
        @DisplayName("UTC_LPS_16: extraMinutes = 1 được chấp nhận")
        void oneExtraMinute() {
            hasPublishedPath();

            service.toggleCatchUpMode("learner01", catchUp(true, 1, null));

            assertThat(lp.getCatchUpExtraMinutes()).isEqualTo(1);
        }

        @Test
        @DisplayName("UTC_LPS_17: tắt bù bài giữ nguyên extraMinutes = 45")
        void disables() {
            hasPublishedPath();
            lp.setCatchUpMode(true);
            lp.setCatchUpExtraMinutes(45);

            service.toggleCatchUpMode("learner01", catchUp(false, null, null));

            assertThat(lp.getCatchUpMode()).isFalse();
            assertThat(lp.getCatchUpExtraMinutes()).isEqualTo(45);
        }

        @Test
        @DisplayName("UTC_LPS_18: request = null bị từ chối")
        void nullRequest() {
            assertThatThrownBy(() -> service.toggleCatchUpMode("learner01", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Yêu cầu điều chỉnh chế độ bù bài không hợp lệ.");
        }

        @Test
        @DisplayName("UTC_LPS_19: chưa có lộ trình học tập thì không thể bật bù bài")
        void noLearningPath() {
            hasNoPublishedPath();

            assertThatThrownBy(() -> service.toggleCatchUpMode("learner01", catchUp(true, 30, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Không tìm thấy lộ trình học tập.");
        }
    }

    // ------------------------------------------------------------------ applyRecalibratedSchedule

    @Nested
    @DisplayName("applyRecalibratedSchedule")
    class Apply {

        @Test
        @DisplayName("UTC_LPS_20: áp dụng 2 giờ/ngày, học Thứ 2-4-6, bắt đầu 23/11/2026")
        void appliesSchedule() {
            hasPublishedPath();
            RecalibrateRequest request = RecalibrateRequest.builder().courseId(COURSE_ID).dailyHours(new BigDecimal("2.0"))
                    .studyDays(List.of("MON", "WED", "FRI")).startDate(MONDAY_23).build();

            service.applyRecalibratedSchedule("learner01", request);

            verify(learningPathRepository, times(1)).save(lp);
            assertThat(lp.getDailyHoursSetup()).isEqualByComparingTo("2.0");
            assertThat(lp.getStudyDays()).isEqualTo("MON,WED,FRI");
            assertThat(lp.getScheduledDate()).isEqualTo(MONDAY_23);
        }

        @Test
        @DisplayName("UTC_LPS_21: dailyHours suy ra từ weeklyHours = 14 / 7 ngày = 2.00")
        void derivesDailyHours() {
            hasPublishedPath();
            RecalibrateRequest request = RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(14)
                    .studyDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")).build();

            service.applyRecalibratedSchedule("learner01", request);

            assertThat(lp.getDailyHoursSetup()).isEqualByComparingTo("2.00");
        }

        @Test
        @DisplayName("UTC_LPS_22: ngày bắt đầu là hôm nay được chấp nhận")
        void startToday() {
            hasPublishedPath();
            lp.setScheduledDate(nov(30));

            service.applyRecalibratedSchedule("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(7).startDate(MONDAY_23).build());

            verify(learningPathRepository, times(1)).save(lp);
            assertThat(lp.getScheduledDate()).isEqualTo(MONDAY_23);
        }

        @Test
        @DisplayName("UTC_LPS_23: ngày bắt đầu là hôm qua bị từ chối")
        void startYesterday() {
            hasPublishedPath();

            assertThatThrownBy(() -> service.applyRecalibratedSchedule("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(7).startDate(nov(22)).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ngày bắt đầu học dự kiến không được trước ngày hôm nay (23/11/2026).");
            verify(learningPathRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTC_LPS_24: đã hoàn thành toàn bộ bài học thì không cho thiết lập lại lịch")
        void courseCompleted() {
            hasPublishedPath();
            Set<Long> allIds = new HashSet<>();
            lessons.forEach(l -> allIds.add(l.getId()));
            when(lessonProgressService.getCompletedLessonIds(eq(USER_ID), eq(COURSE_ID), any(), anyInt())).thenReturn(allIds);

            assertThatThrownBy(() -> service.applyRecalibratedSchedule("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(7).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bạn đã hoàn thành tất cả các bài học trong khóa học này. Không thể thiết lập lại lịch học cho lộ trình đã hoàn tất.");
            verify(learningPathRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTC_LPS_25: chưa có lộ trình học tập thì không thể cập nhật")
        void noLearningPath() {
            hasNoPublishedPath();

            assertThatThrownBy(() -> service.applyRecalibratedSchedule("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(7).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Không tìm thấy lộ trình học tập với khóa học đã đăng ký và xuất bản để cập nhật.");
        }
    }

    // ------------------------------------------------------------------ getRecalibratePreview

    @Nested
    @DisplayName("getRecalibratePreview")
    class Preview {

        @Test
        @DisplayName("UTC_LPS_26: tính bản xem trước 14 giờ/tuần, không ghi database")
        void calculatesPreview() {
            hasPublishedPath();

            RecalibratePreviewResponse preview = service.getRecalibratePreview("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(14).startDate(MONDAY_23).build());

            assertThat(preview.isHasPath()).isTrue();
            assertThat(preview.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(preview.getNewWeeklyHours()).isEqualTo(14);
            verify(learningPathRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTC_LPS_27: ngày bắt đầu là hôm qua bị từ chối")
        void startYesterday() {
            hasPublishedPath();

            assertThatThrownBy(() -> service.getRecalibratePreview("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(14).startDate(nov(22)).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ngày bắt đầu học dự kiến không được trước ngày hôm nay (23/11/2026).");
        }

        @Test
        @DisplayName("UTC_LPS_28: chưa có lộ trình thì trả về trạng thái rỗng")
        void noLearningPath() {
            hasNoPublishedPath();

            RecalibratePreviewResponse preview = service.getRecalibratePreview("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(14).build());

            assertThat(preview.isHasPath()).isFalse();
            assertThat(preview.getMessage()).isEqualTo("Chưa đủ dữ liệu để điều chỉnh lịch học hoặc bạn chưa đăng ký khóa học này.");
        }

        @Test
        @DisplayName("UTC_LPS_29: khóa học đang DRAFT thì trả về \"Khóa học chưa được công bố.\"")
        void courseNotPublished() {
            hasPublishedPath();
            Course draft = Course.builder().id(COURSE_ID).title("N5 Course").level("N5").status("DRAFT").build();
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(draft));

            RecalibratePreviewResponse preview = service.getRecalibratePreview("learner01",
                    RecalibrateRequest.builder().courseId(COURSE_ID).weeklyHours(14).build());

            assertThat(preview.isHasPath()).isFalse();
            assertThat(preview.getMessage()).isEqualTo("Khóa học chưa được công bố.");
        }
    }

    // ------------------------------------------------------------------ getLearningPath (timeline)

    @Nested
    @DisplayName("getLearningPath – tính timeline và trạng thái rỗng")
    class Timeline {

        @Test
        @DisplayName("UTC_LPS_30: khóa học 999999 chưa đăng ký thì trả về trạng thái rỗng")
        void notEnrolledCourse() {
            hasPublishedPath();

            LearningPathResponse response = service.getLearningPath("learner01", 999999L);

            assertThat(response.isHasPath()).isFalse();
            assertThat(response.getItems()).isEmpty();
        }

        @Test
        @DisplayName("UTC_LPS_31: ngày nghỉ 24/11 bị bỏ qua → [23, 25, 26]")
        void skipsLeaveDate() throws Exception {
            hasPublishedPath();
            withLeaves(leave("leave-1", nov(24), nov(24)));

            LearningPathResponse response = service.getLearningPath("learner01", COURSE_ID);

            assertThat(response.isHasPath()).isTrue();
            assertThat(plannedDates(response)).containsExactly(nov(23), nov(25), nov(26));
        }

        @Test
        @DisplayName("UTC_LPS_32: hai đợt nghỉ chồng nhau 24→25 và 25→26 → [23, 27, 28]")
        void overlappingLeaves() throws Exception {
            hasPublishedPath();
            withLeaves(leave("leave-1", nov(24), nov(25)), leave("leave-2", nov(25), nov(26)));

            LearningPathResponse response = service.getLearningPath("learner01", COURSE_ID);

            assertThat(response.isHasPath()).isTrue();
            assertThat(plannedDates(response)).containsExactly(nov(23), nov(27), nov(28));
        }

        @Test
        @DisplayName("UTC_LPS_33: không còn đợt nghỉ thì trở về lịch gốc → [23, 24, 25]")
        void noLeaves() throws Exception {
            hasPublishedPath();
            withLeaves();

            LearningPathResponse response = service.getLearningPath("learner01", COURSE_ID);

            assertThat(response.isHasPath()).isTrue();
            assertThat(plannedDates(response)).containsExactly(nov(23), nov(24), nov(25));
        }

        @Test
        @DisplayName("UTC_LPS_34: bù bài +30 phút (giới hạn 120 phút/ngày) xếp 2 bài mỗi ngày")
        void catchUpPacksTwoLessonsPerDay() {
            hasPublishedPath();
            lessons = lessons(4);
            lp.setDailyHoursSetup(new BigDecimal("1.5"));
            lp.setCatchUpMode(true);
            lp.setCatchUpExtraMinutes(30);

            LearningPathResponse response = service.getLearningPath("learner01", COURSE_ID);

            assertThat(response.isHasPath()).isTrue();
            assertThat(plannedDates(response)).containsExactly(nov(23), nov(23), nov(24), nov(24));
        }

        @Test
        @DisplayName("UTC_LPS_35: bù bài kèm cuối tuần, hôm nay Thứ 6 27/11 → [27, 27, 28, 28]")
        void catchUpIncludesSaturday() {
            today(nov(27));
            hasPublishedPath();
            lessons = lessons(4);
            lp.setScheduledDate(nov(27));
            lp.setStudyDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
            lp.setDailyHoursSetup(new BigDecimal("1.5"));
            lp.setCatchUpMode(true);
            lp.setCatchUpExtraMinutes(30);
            lp.setCatchUpIncludeWeekends(true);

            LearningPathResponse response = service.getLearningPath("learner01", COURSE_ID);

            assertThat(response.isHasPath()).isTrue();
            assertThat(plannedDates(response)).containsExactly(nov(27), nov(27), nov(28), nov(28));
        }

        @Test
        @DisplayName("UTC_LPS_36: lộ trình kết thúc 25/11 nhưng ngày thi 24/11 → cảnh báo trễ 1 ngày")
        void warnsWhenBehindExamDate() {
            hasPublishedPath();
            lp.setTargetExamDate(nov(24));

            LearningPathResponse response = service.getLearningPath("learner01", COURSE_ID);

            assertThat(response.isHasPath()).isTrue();
            assertThat(plannedDates(response)).last().isEqualTo(nov(25));
            assertThat(response.getExamBufferDaysText()).isEqualTo("Cảnh báo: Lộ trình trễ 1 ngày so với kỳ thi!");
        }
    }
}
