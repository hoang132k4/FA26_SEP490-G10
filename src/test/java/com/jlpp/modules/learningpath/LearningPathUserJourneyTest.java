package com.jlpp.modules.learningpath;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.learningpath.dto.LearningPathProgressDto;
import com.jlpp.modules.learningpath.dto.LearningPathResponse;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LearningPathService;
import com.jlpp.modules.learningpath.service.LessonProgressService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LearningPathUserJourneyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearningPathRepository learningPathRepository;

    @Autowired
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LearningPathService learningPathService;

    @Autowired
    private LessonProgressService lessonProgressService;

    private static final String JOURNEY_USER_EMAIL = "journey_learner@jeps.edu.vn";
    private User testUser;
    private Course testCourse;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;
    private LearningPath testLp;

    @BeforeEach
    void setUp() {
        learningPathRepository.deleteAll();
        courseRegistrationRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM lessons");
        courseRepository.deleteAll();

        userRepository.findByEmailIgnoreCase(JOURNEY_USER_EMAIL).ifPresent(userRepository::delete);

        testUser = userRepository.save(User.builder()
                .email(JOURNEY_USER_EMAIL)
                .username("journey_learner")
                .fullName("Journey Test Learner")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .targetJlptLevel("N3")
                .build());

        testCourse = courseRepository.save(Course.builder()
                .id(901L)
                .title("Khóa học N3 Journey Test")
                .code("JLPT-N3-JOURNEY")
                .level("N3")
                .price(0.0)
                .status("PUBLISHED")
                .durationHours(10)
                .lessonCount(3)
                .build());

        for (long i = 1; i <= 3; i++) {
            jdbcTemplate.update(
                    "INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, skill_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    800L + i,
                    testCourse.getId(),
                    "Bài " + i + ": Bài học kiểm thử số " + i,
                    (int) i,
                    "Chương 1",
                    30 + (int)(i * 10),
                    "VIDEO",
                    (i == 1 ? "VOCABULARY" : (i == 2 ? "GRAMMAR" : "READING"))
            );
        }

        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(testUser)
                .course(testCourse)
                .registeredAt(LocalDateTime.now().minusDays(2))
                .status("ACTIVE")
                .progressPercent(0)
                .build());

        testLp = learningPathRepository.save(LearningPath.builder()
                .userId(testUser.getId())
                .courseId(testCourse.getId())
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .currentLevel("N4")
                .pathStatus("ACTIVE")
                .scheduledDate(LocalDate.now().minusDays(2))
                .build());
    }

    @Test
    @DisplayName("Verify Full 13-Step Learner Journey: View -> Complete -> Recalibrate")
    @WithMockUser(username = JOURNEY_USER_EMAIL, roles = {"LEARNER"})
    void testCompleteLearnerJourney() throws Exception {
        // Step 4 & 5: Open Learning Path and view skill strengths & weaknesses
        mockMvc.perform(get("/learning-path"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning-path/view"))
                .andExpect(model().attributeExists("path"))
                .andExpect(model().attribute("hasPath", true));

        LearningPathResponse initialPath = learningPathService.getLearningPath(JOURNEY_USER_EMAIL);
        assertThat(initialPath.getTotalLessons()).isEqualTo(3);
        assertThat(initialPath.getCompletedLessons()).isEqualTo(0);
        assertThat(initialPath.getItems().get(0).getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(initialPath.getItems().get(0).getActionText()).isEqualTo("Học tiếp");

        // Step 7 & 8: Complete Lesson 1 (id: 801L) via LearnerCourseController
        mockMvc.perform(post("/learning/lessons/{lessonId}/complete", 801L)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/lessons/802"));

        // Step 9 & 10: Return to Learning Path and verify progress synchronization
        LearningPathResponse pathAfterStep1 = learningPathService.getLearningPath(JOURNEY_USER_EMAIL);
        assertThat(pathAfterStep1.getCompletedLessons()).isEqualTo(1);
        assertThat(pathAfterStep1.getRemainingLessons()).isEqualTo(2);
        assertThat(pathAfterStep1.getItems().get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(pathAfterStep1.getItems().get(0).getActionText()).isEqualTo("Ôn lại");
        assertThat(pathAfterStep1.getItems().get(1).getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(pathAfterStep1.getItems().get(1).getActionText()).isEqualTo("Học tiếp");

        // Verify JSONB column state in DB
        LearningPathProgressDto progressJson = lessonProgressService.getLessonProgress(testUser.getId(), testCourse.getId());
        assertThat(progressJson.isLessonCompleted(801L)).isTrue();
        assertThat(progressJson.isLessonCompleted(802L)).isFalse();
        String completedAtLesson1 = progressJson.getLessonProgress(801L).getCompletedAt();
        assertThat(completedAtLesson1).isNotNull();

        // Step 11 & 12: Recalibrate schedule (Increase weekly hours to 21 hours/week)
        mockMvc.perform(post("/learning-path/recalibrate/apply")
                .with(csrf())
                .param("weeklyHours", "21"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning-path"));

        // Step 13: Verify completed lesson history and timestamp are preserved
        LearningPathResponse pathAfterRecalibrate = learningPathService.getLearningPath(JOURNEY_USER_EMAIL);
        assertThat(pathAfterRecalibrate.getWeeklyHours()).isEqualTo(21);
        assertThat(pathAfterRecalibrate.getCompletedLessons()).isEqualTo(1);
        assertThat(pathAfterRecalibrate.getItems().get(0).getStatus()).isEqualTo("COMPLETED");

        LearningPathProgressDto progressAfterRecalibrate = lessonProgressService.getLessonProgress(testUser.getId(), testCourse.getId());
        assertThat(progressAfterRecalibrate.getLessonProgress(801L).getCompletedAt()).isEqualTo(completedAtLesson1);
    }
}
