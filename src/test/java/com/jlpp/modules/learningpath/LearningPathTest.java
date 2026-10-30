package com.jlpp.modules.learningpath;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.learningpath.dto.LearningPathResponse;
import com.jlpp.modules.learningpath.dto.RecalibratePreviewResponse;
import com.jlpp.modules.learningpath.dto.RecalibrateRequest;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LearningPathService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LearningPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearningPathRepository learningPathRepository;

    @Autowired
    private LearningPathService learningPathService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String LEARNER_WITH_PATH = "learner_with_path@jeps.edu.vn";
    private static final String LEARNER_NO_PATH = "learner_no_path@jeps.edu.vn";

    private User userWithPath;
    private User userNoPath;
    private Course courseN4;
    private LearningPath learningPath;

    @BeforeEach
    void setUp() {
        learningPathRepository.deleteAll();
        courseRegistrationRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM lessons");
        courseRepository.deleteAll();

        userRepository.findByEmailIgnoreCase(LEARNER_WITH_PATH).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(LEARNER_NO_PATH).ifPresent(userRepository::delete);

        userWithPath = userRepository.save(User.builder()
                .email(LEARNER_WITH_PATH)
                .username("learner_with_path")
                .fullName("Nguyễn Học Viên Có Lộ Trình")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .targetJlptLevel("N4")
                .build());

        userNoPath = userRepository.save(User.builder()
                .email(LEARNER_NO_PATH)
                .username("learner_no_path")
                .fullName("Trần Học Viên Chưa Có Lộ Trình")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .targetJlptLevel("N3")
                .build());

        courseN4 = courseRepository.save(Course.builder()
                .id(101L)
                .title("Khóa học JLPT N4 Toàn diện")
                .code("JLPT-N4-ALL")
                .level("N4")
                .price(1500000.0)
                .durationHours(30)
                .lessonCount(4)
                .status("PUBLISHED")
                .build());

        // Insert 4 lessons for courseN4 directly via JdbcTemplate to bypass H2 identity mismatch
        for (long i = 1; i <= 4; i++) {
            jdbcTemplate.update(
                    "INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    200L + i,
                    courseN4.getId(),
                    "Bài " + i + ": Ngữ pháp và từ vựng cơ bản phần " + i,
                    (int) i,
                    "Chương " + ((i <= 2) ? "1" : "2"),
                    45,
                    "VIDEO"
            );
        }

        // Register userWithPath into courseN4 with 50% progress
        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(userWithPath)
                .course(courseN4)
                .registeredAt(LocalDateTime.now().minusWeeks(2))
                .status("ACTIVE")
                .progressPercent(50)
                .build());

        // Create real learning path in DB for userWithPath
        learningPath = learningPathRepository.save(LearningPath.builder()
                .userId(userWithPath.getId())
                .courseId(courseN4.getId())
                .dailyHoursSetup(new BigDecimal("1.5"))
                .targetLevel("N4")
                .currentLevel("N5")
                .pathStatus("ACTIVE")
                .scheduledDate(LocalDate.now().minusWeeks(2))
                .build());
    }

    // ==========================================
    // VIEW LEARNING PATH (UC-27) TESTS
    // ==========================================

    @Test
    @DisplayName("LP-01: Anonymous user viewing /learning-path redirects to /auth/login")
    void viewLearningPath_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/learning-path"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("LP-02: Learner without learning path views /learning-path and sees official empty state")
    @WithMockUser(username = LEARNER_NO_PATH, roles = {"LEARNER"})
    void viewLearningPath_authenticated_noPath_displaysOfficialEmptyState() throws Exception {
        mockMvc.perform(get("/learning-path"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/view"))
                .andExpect(model().attribute("hasPath", false))
                .andExpect(content().string(containsString("Bạn chưa có lộ trình học tập")))
                .andExpect(content().string(containsString("Khám phá khóa học")))
                .andExpect(content().string(not(containsString("Bài 1: Ngữ pháp"))));
    }

    @Test
    @DisplayName("LP-03: Learner with valid learning path views own path, timeline, and accurate metrics")
    @WithMockUser(username = LEARNER_WITH_PATH, roles = {"LEARNER"})
    void viewLearningPath_authenticated_withValidPath_displaysDetailsAndTimeline() throws Exception {
        mockMvc.perform(get("/learning-path"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/view"))
                .andExpect(model().attribute("hasPath", true))
                .andExpect(content().string(containsString("Khóa học JLPT N4 Toàn diện")))
                .andExpect(content().string(containsString("50%")))
                .andExpect(content().string(containsString("Bài 1: Ngữ pháp và từ vựng cơ bản phần 1")))
                .andExpect(content().string(containsString("Đã hoàn thành")))
                .andExpect(content().string(containsString("Đang học")));
    }

    @Test
    @DisplayName("LP-04: Learner isolation - Learner B cannot view Learner A's learning path")
    @WithMockUser(username = LEARNER_NO_PATH, roles = {"LEARNER"})
    void viewLearningPath_crossUserAccess_isolationVerified() throws Exception {
        mockMvc.perform(get("/learning-path"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasPath", false))
                .andExpect(content().string(not(containsString("Khóa học JLPT N4 Toàn diện"))));
    }

    // ==========================================
    // RECALIBRATE SCHEDULE (UC-28) TESTS
    // ==========================================

    @Test
    @DisplayName("REC-01: Anonymous user viewing /learning-path/recalibrate redirects to /auth/login")
    void recalibrateSchedule_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/learning-path/recalibrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("REC-02: Learner without learning path viewing /learning-path/recalibrate sees empty state")
    @WithMockUser(username = LEARNER_NO_PATH, roles = {"LEARNER"})
    void recalibrateSchedule_noPath_displaysEmptyState() throws Exception {
        mockMvc.perform(get("/learning-path/recalibrate"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/recalibrate"))
                .andExpect(model().attribute("hasPath", false))
                .andExpect(content().string(containsString("Chưa đủ dữ liệu để điều chỉnh lịch học")));
    }

    @Test
    @DisplayName("REC-03: Learner with learning path views recalibration in Simulation Preview mode")
    @WithMockUser(username = LEARNER_WITH_PATH, roles = {"LEARNER"})
    void recalibrateSchedule_withValidPath_displaysSimulationBannerAndCurrentSchedule() throws Exception {
        mockMvc.perform(get("/learning-path/recalibrate"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/recalibrate"))
                .andExpect(model().attribute("hasPath", true))
                .andExpect(model().attributeExists("preview"))
                .andExpect(content().string(containsString("Chế độ xem trước & mô phỏng (Simulation Preview Mode)")))
                .andExpect(content().string(containsString("Thiết lập tham số điều chỉnh")));
    }

    @Test
    @DisplayName("REC-04: POST /learning-path/recalibrate calculates simulation and NEVER writes to Database")
    @WithMockUser(username = LEARNER_WITH_PATH, roles = {"LEARNER"})
    void recalibrateSchedule_postSimulation_calculatesCorrectPaceAndZeroDbWrites() throws Exception {
        long initialPathCount = learningPathRepository.count();

        mockMvc.perform(post("/learning-path/recalibrate")
                        .with(csrf())
                        .param("weeklyHours", "20"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/recalibrate"))
                .andExpect(model().attribute("hasPath", true))
                .andExpect(model().attribute("simulationSuccess", true))
                .andExpect(content().string(containsString("20 giờ/tuần")))
                .andExpect(content().string(containsString("Cường độ Tăng tốc")));

        // Strict validation: Database count MUST be unchanged
        long finalPathCount = learningPathRepository.count();
        assertThat(finalPathCount).isEqualTo(initialPathCount);

        // Verify the original LearningPath record in DB is unmodified
        LearningPath unmodified = learningPathRepository.findById(learningPath.getId()).orElseThrow();
        assertThat(unmodified.getDailyHoursSetup()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("REC-05: POST /learning-path/recalibrate with invalid weeklyHours falls back to valid preview")
    @WithMockUser(username = LEARNER_WITH_PATH, roles = {"LEARNER"})
    void recalibrateSchedule_invalidWeeklyHours_handledGracefully() throws Exception {
        mockMvc.perform(post("/learning-path/recalibrate")
                        .with(csrf())
                        .param("weeklyHours", "100")) // Exceeds max 40
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/recalibrate"))
                .andExpect(model().hasErrors());
    }

    // ==========================================
    // SERVICE LAYER DIRECT UNIT TESTS
    // ==========================================

    @Test
    @DisplayName("SRV-01: LearningPathService.getLearningPath returns empty response when no path in DB")
    void service_getLearningPath_noPath_returnsEmptyResponse() {
        LearningPathResponse response = learningPathService.getLearningPath(LEARNER_NO_PATH);

        assertThat(response).isNotNull();
        assertThat(response.isHasPath()).isFalse();
        assertThat(response.getEmptyStateMessage()).isEqualTo("Bạn chưa có lộ trình học tập.");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("SRV-02: LearningPathService.getLearningPath calculates accurate completion percentage and item states")
    void service_getLearningPath_withPath_calculatesAccurateProgress() {
        LearningPathResponse response = learningPathService.getLearningPath(LEARNER_WITH_PATH);

        assertThat(response).isNotNull();
        assertThat(response.isHasPath()).isTrue();
        assertThat(response.getCourseTitle()).isEqualTo("Khóa học JLPT N4 Toàn diện");
        assertThat(response.getProgressPercent()).isEqualTo(50);
        assertThat(response.getTotalLessons()).isEqualTo(4);
        assertThat(response.getCompletedLessons()).isEqualTo(2);
        assertThat(response.getRemainingLessons()).isEqualTo(2);

        List<?> items = response.getItems();
        assertThat(items).hasSize(4);
    }

    @Test
    @DisplayName("SRV-03: LearningPathService.getRecalibratePreview generates intensive pace analysis when hours >= 15")
    void service_getRecalibratePreview_intensivePace_analysisCorrect() {
        RecalibrateRequest request = RecalibrateRequest.builder()
                .weeklyHours(20)
                .startDate(LocalDate.of(2026, 11, 1))
                .build();

        RecalibratePreviewResponse preview = learningPathService.getRecalibratePreview(LEARNER_WITH_PATH, request);

        assertThat(preview).isNotNull();
        assertThat(preview.isHasPath()).isTrue();
        assertThat(preview.getNewWeeklyHours()).isEqualTo(20);
        assertThat(preview.getPaceAnalysisText()).contains("Cường độ Tăng tốc (Intensive)");
    }
}
