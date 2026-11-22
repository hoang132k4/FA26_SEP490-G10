package com.jeps.modules.course;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.dto.CourseRegistrationRequest;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerCourseFullIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRegistrationRepository registrationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User learner;
    private Course activeCourse;
    private Course completedCourse;
    private Course freeCourse;
    private Course paidCourse;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("learner_full_test@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_full_test@jeps.edu.vn")
                        .username("learner_full_test")
                        .fullName("Nguyễn Học Viên Toàn Diện")
                        .phone("0987654321")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        activeCourse = courseRepository.saveAndFlush(Course.builder()
                .id(8101L)
                .title("Khóa học Tiếng Nhật N4 Cấp tốc")
                .level("N4")
                .status("PUBLISHED")
                .price(399000.0)
                .shortDescription("Ôn luyện ngữ pháp và từ vựng N4")
                .lessonCount(3)
                .build());

        completedCourse = courseRepository.saveAndFlush(Course.builder()
                .id(8102L)
                .title("Khóa học Tiếng Nhật N5 Nhập môn")
                .level("N5")
                .status("PUBLISHED")
                .price(0.0)
                .shortDescription("Bảng chữ cái và chào hỏi N5")
                .lessonCount(2)
                .build());

        freeCourse = courseRepository.saveAndFlush(Course.builder()
                .id(8103L)
                .title("Khóa học Miễn Phí Kanji N5")
                .level("N5")
                .status("PUBLISHED")
                .price(0.0)
                .shortDescription("50 chữ Hán căn bản")
                .lessonCount(1)
                .build());

        paidCourse = courseRepository.saveAndFlush(Course.builder()
                .id(8104L)
                .title("Khóa học Luyện thi Đọc hiểu N3")
                .level("N3")
                .status("PUBLISHED")
                .price(599000.0)
                .shortDescription("Chiến thuật đọc hiểu N3 chuyên sâu")
                .lessonCount(4)
                .build());

        // Insert lessons for activeCourse
        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, 95001L, activeCourse.getId(), "Bài 1: Khởi động N4", 1, "Chương 1", 20, "THEORY", "Nội dung bài 1");

        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, 95002L, activeCourse.getId(), "Bài 2: Ngữ pháp N4", 2, "Chương 1", 25, "GRAMMAR", "Nội dung bài 2");

        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, 95003L, activeCourse.getId(), "Bài 3: Luyện tập N4", 3, "Chương 2", 30, "READING", "Nội dung bài 3");

        // Seed Active registration (progress 33% = 1 of 3 completed)
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(activeCourse)
                .status("ACTIVE")
                .progressPercent(33)
                .build());

        // Seed Completed registration (progress 100%)
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(completedCourse)
                .status("COMPLETED")
                .progressPercent(100)
                .build());
    }

    // ==========================================
    // 1. DASHBOARD (/learning/dashboard)
    // ==========================================

    @Test
    @DisplayName("Dashboard: Thành công hiển thị khóa học đang học, bài học kế tiếp và tiến độ")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testDashboard_SuccessWithActiveAndCompletedCourses() throws Exception {
        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/dashboard"))
                .andExpect(model().attributeExists("learner"))
                .andExpect(model().attribute("totalRegisteredCourses", is(2)))
                .andExpect(model().attribute("completedCourses", is(1L)))
                .andExpect(model().attribute("activeCourse", notNullValue()))
                .andExpect(model().attribute("activeCourseLessonCount", is(3)))
                .andExpect(model().attribute("activeCourseCompletedLessons", is(1)))
                .andExpect(model().attribute("nextLesson", notNullValue()))
                .andExpect(model().attribute("activeMenu", is("learner-dashboard")));
    }

    @Test
    @DisplayName("Dashboard: Học viên chưa đăng ký khóa học nào hiển thị trạng thái rỗng")
    @WithMockUser(username = "new_empty_learner@jeps.edu.vn", roles = {"LEARNER"})
    void testDashboard_EmptyRegistrations() throws Exception {
        userRepository.save(User.builder()
                .email("new_empty_learner@jeps.edu.vn")
                .username("new_empty_learner")
                .fullName("Học viên mới tinh")
                .password("Password123@")
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/dashboard"))
                .andExpect(model().attribute("totalRegisteredCourses", is(0)))
                .andExpect(model().attribute("completedCourses", is(0L)))
                .andExpect(model().attribute("activeCourse", nullValue()))
                .andExpect(model().attribute("nextLesson", nullValue()));
    }

    @Test
    @DisplayName("Dashboard: Người dùng chưa đăng nhập bị chặn (Unauthorized / Redirect login)")
    @WithAnonymousUser
    void testDashboard_Unauthenticated_Redirects() throws Exception {
        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    // ==========================================
    // 2. MY COURSES (/learning/my-courses)
    // ==========================================

    @Test
    @DisplayName("My Courses: Hiển thị phân loại khóa học đang học và đã hoàn thành")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testMyCourses_DisplaysAllRegistrationsWithStats() throws Exception {
        mockMvc.perform(get("/learning/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/my-courses"))
                .andExpect(model().attribute("totalCount", is(2)))
                .andExpect(model().attribute("inProgressCount", is(1L)))
                .andExpect(model().attribute("completedCount", is(1L)))
                .andExpect(model().attribute("activeMenu", is("my-courses")));
    }

    // ==========================================
    // 3. RECOMMENDED COURSES (/learning/courses/recommended)
    // ==========================================

    @Test
    @DisplayName("Recommended Courses: Gợi ý khóa học theo trình độ học viên")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testRecommendedCourses_Success() throws Exception {
        mockMvc.perform(get("/learning/courses/recommended"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/recommended-courses"))
                .andExpect(model().attributeExists("learner"))
                .andExpect(model().attributeExists("recommendedItems"))
                .andExpect(model().attributeExists("otherCourses"))
                .andExpect(model().attribute("levels", hasItems("N5", "N4", "N3", "N2", "N1")))
                .andExpect(model().attribute("activeMenu", is("recommended")));
    }

    // ==========================================
    // 4. COURSE CATALOG FILTERS & REDIRECTS (/learning/courses)
    // ==========================================

    @Test
    @DisplayName("Course Catalog: Điều hướng sang /courses có bộ lọc level")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCourseCatalog_FilterLevelOnly() throws Exception {
        mockMvc.perform(get("/learning/courses").param("level", "N4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?level=N4"));
    }

    @Test
    @DisplayName("Course Catalog: Điều hướng sang /courses có bộ lọc keyword")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCourseCatalog_FilterKeywordOnly() throws Exception {
        mockMvc.perform(get("/learning/courses").param("keyword", "Kanji"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?q=Kanji"));
    }

    @Test
    @DisplayName("Course Catalog: Điều hướng sang /courses có cả level và keyword")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCourseCatalog_FilterBothLevelAndKeyword() throws Exception {
        mockMvc.perform(get("/learning/courses")
                        .param("level", "N5")
                        .param("keyword", "Căn bản"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?level=N5&q=Căn bản"));
    }

    @Test
    @DisplayName("Course Catalog: Không có bộ lọc điều hướng sang /courses thuần")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCourseCatalog_NoFilter() throws Exception {
        mockMvc.perform(get("/learning/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));
    }

    @Test
    @DisplayName("Course Detail: /learning/courses/{id} điều hướng sang /courses/{id}")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCourseDetail_Redirect() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}", activeCourse.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + activeCourse.getId()));
    }

    // ==========================================
    // 5. REGISTER COURSE (/learning/courses/{id}/register)
    // ==========================================

    @Test
    @DisplayName("Register Course GET: Hiển thị form đăng ký với thông tin học viên được điền sẵn")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testRegisterCoursePage_NotEnrolled_DisplaysForm() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/register", freeCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/register-course"))
                .andExpect(model().attributeExists("learner", "course", "registrationRequest"))
                .andExpect(model().attribute("course", hasProperty("id", is(freeCourse.getId()))));
    }

    @Test
    @DisplayName("Register Course GET: Đã đăng ký trước đó sẽ chuyển hướng và cảnh báo")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testRegisterCoursePage_AlreadyEnrolled_RedirectsWithWarning() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/register", activeCourse.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + activeCourse.getId()))
                .andExpect(flash().attributeExists("warningMessage"));
    }

    @Test
    @DisplayName("Register Course POST: Đăng ký thành công khóa học mới")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testHandleRegisterCourse_Success() throws Exception {
        mockMvc.perform(post("/learning/courses/{id}/register", paidCourse.getId())
                        .with(csrf())
                        .param("courseId", paidCourse.getId().toString())
                        .param("fullName", "Nguyễn Học Viên Toàn Diện")
                        .param("email", "learner_full_test@jeps.edu.vn")
                        .param("phone", "0987654321")
                        .param("paymentMethod", "VNPAY")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + paidCourse.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify registration saved in repository
        assertTrue(registrationRepository.existsByUserIdAndCourseId(learner.getId(), paidCourse.getId()));
    }

    @Test
    @DisplayName("Register Course POST: Lỗi validation khi để trống thông tin bắt buộc")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testHandleRegisterCourse_ValidationError_ReRendersForm() throws Exception {
        mockMvc.perform(post("/learning/courses/{id}/register", freeCourse.getId())
                        .with(csrf())
                        .param("courseId", freeCourse.getId().toString())
                        .param("fullName", "") // trống họ tên
                        .param("email", "not-an-email") // sai định dạng email
                        .param("termsAccepted", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/register-course"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Register Course POST: Đã đăng ký rồi báo warning khi đăng ký lại")
    @WithMockUser(username = "learner_full_test@jeps.edu.vn", roles = {"LEARNER"})
    void testHandleRegisterCourse_AlreadyRegistered_RedirectsWithWarning() throws Exception {
        mockMvc.perform(post("/learning/courses/{id}/register", activeCourse.getId())
                        .with(csrf())
                        .param("courseId", activeCourse.getId().toString())
                        .param("fullName", "Nguyễn Học Viên Toàn Diện")
                        .param("email", "learner_full_test@jeps.edu.vn")
                        .param("phone", "0987654321")
                        .param("paymentMethod", "FREE")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + activeCourse.getId()))
                .andExpect(flash().attributeExists("warningMessage"));
    }
}
