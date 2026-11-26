package com.jeps.modules.course;

import com.jeps.common.constants.RoleEnum;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit & Integration Test Suite for LearnerCourse Module
 * Strictly traced to Report-5.1_Unit Test_All10Modules_backup.xls: Sheet 'LearnerCourse' (UTC0588 - UTC0702)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerCourseUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseRegistrationRepository registrationRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private com.jeps.modules.course.service.PublicCatalogService publicCatalogService;

    private User learner;
    private Course n5Course;
    private Course n4Course;
    private Course freeCourse;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("learner_utc_course@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_utc_course@jeps.edu.vn")
                        .username("learner_utc_course")
                        .fullName("Học Viên Kiểm Thử Khóa Học")
                        .phone("0901234567")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        n5Course = courseRepository.save(Course.builder()
                .title("Khóa học Tiếng Nhật N5 Căn Bản")
                .level("N5")
                .price(299000.0)
                .status("PUBLISHED")
                .shortDescription("Nhập môn bảng chữ cái và ngữ pháp N5")
                .lessonCount(10)
                .build());

        n4Course = courseRepository.save(Course.builder()
                .title("Khóa học Tiếng Nhật N4 Thực Chiến")
                .level("N4")
                .price(499000.0)
                .status("PUBLISHED")
                .shortDescription("Luyện thi N4 toàn diện")
                .lessonCount(15)
                .build());

        freeCourse = courseRepository.save(Course.builder()
                .title("Khóa học Trải Nghiệm Miễn Phí 0Đ")
                .level("N5")
                .price(0.0)
                .status("PUBLISHED")
                .shortDescription("Trải nghiệm học tập miễn phí")
                .lessonCount(3)
                .build());
    }

    // =========================================================================
    // MÀN HÌNH SCR-03: Danh mục Khóa học Công khai (/courses) [UTC0588 - UTC0614]
    // =========================================================================

    @Test
    @DisplayName("UTC0588 [SCR-03]: Kiểm tra nạp giao diện Danh mục Khóa học công khai (HTTP 200 OK)")
    void test_UTC0588_viewCourseCatalog_200OK() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attributeExists("courseCount"));
    }

    @Test
    @DisplayName("UTC0596 [SCR-03]: Kiểm tra bảo mật: Khách vãng lai chưa đăng nhập truy cập bình thường (HTTP 200 OK)")
    @WithAnonymousUser
    void test_UTC0596_unauthenticatedAccess_allowed() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"));
    }

    @Test
    @DisplayName("UTC0597 [SCR-03]: Kiểm tra bảo mật: Người dùng đã đăng nhập (Learner) truy cập bình thường")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0597_authenticatedLearner_allowed() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"));
    }

    @Test
    @DisplayName("UTC0599 [SCR-03]: Kiểm tra chống tấn công XSS trên tham số tìm kiếm (HTML Escape)")
    void test_UTC0599_xssProtection_escapesScript() throws Exception {
        mockMvc.perform(get("/courses").param("keyword", "<script>alert('XSS')</script>"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(content().string(not(containsString("<script>alert('XSS')</script>"))));
    }

    @Test
    @DisplayName("UTC0600 [SCR-03]: Kiểm tra chống tấn công SQL Injection trên tham số truy vấn")
    void test_UTC0600_sqlInjectionProtection_safe() throws Exception {
        mockMvc.perform(get("/courses").param("keyword", "1' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"));
    }

    @Test
    @DisplayName("UTC0601 [SCR-03]: Kiểm tra tìm kiếm khóa học theo từ khóa hợp lệ")
    void test_UTC0601_searchCourses_byKeyword() throws Exception {
        mockMvc.perform(get("/courses").param("keyword", "N5 Căn Bản"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(model().attributeExists("courses"));
    }

    @Test
    @DisplayName("UTC0602 [SCR-03]: Kiểm tra lọc khóa học theo cấp độ JLPT (Level N4)")
    void test_UTC0602_filterCourses_byLevel_N4() throws Exception {
        mockMvc.perform(get("/courses").param("level", "N4"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(model().attributeExists("courses"));
    }

    @Test
    @DisplayName("UTC0603 [SCR-03]: Kiểm tra kết hợp đồng thời bộ lọc cấp độ và từ khóa tìm kiếm")
    void test_UTC0603_searchAndFilter_combined() throws Exception {
        mockMvc.perform(get("/courses")
                        .param("keyword", "Thực Chiến")
                        .param("level", "N4"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(model().attributeExists("courses"));
    }

    @Test
    @DisplayName("UTC0604 [SCR-03]: Kiểm tra tìm kiếm từ khóa không khớp trả về danh sách rỗng an toàn")
    void test_UTC0604_searchNoMatch_emptyResult() throws Exception {
        mockMvc.perform(get("/courses").param("q", "TuKhoaKhongTonTai_999999"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(model().attribute("courses", hasSize(0)));
    }

    // =========================================================================
    // MÀN HÌNH SCR-04: Chi tiết Khóa học (/courses/{id}) [UTC0615 - UTC0641]
    // =========================================================================

    @Test
    @DisplayName("UTC0615 [SCR-04]: Kiểm tra nạp giao diện Chi tiết Khóa học hợp lệ (HTTP 200 OK)")
    void test_UTC0615_viewCourseDetail_success() throws Exception {
        mockMvc.perform(get("/courses/{id}", n4Course.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/course-detail"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("lessons"));
    }

    @Test
    @DisplayName("UTC0628 [SCR-04]: Kiểm tra nạp đầy đủ giáo trình bài học và thông tin khóa học")
    void test_UTC0628_courseDetail_loadsSyllabus() throws Exception {
        mockMvc.perform(get("/courses/{id}", n4Course.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("course", hasProperty("title", equalTo("Khóa học Tiếng Nhật N4 Thực Chiến"))));
    }

    @Test
    @DisplayName("UTC0629 [SCR-04]: Kiểm tra truy cập ID khóa học không tồn tại trả về mã lỗi 404 Not Found")
    void test_UTC0629_courseDetail_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/courses/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("UTC0631 [SCR-04]: Kiểm tra học viên đã đăng ký khóa học sẽ hiển thị trạng thái đã sở hữu (isEnrolled=true)")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0631_courseDetail_enrolledLearner_showsContinue() throws Exception {
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(n4Course)
                .status("ACTIVE")
                .progressPercent(30)
                .build());

        mockMvc.perform(get("/courses/{id}", n4Course.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isEnrolled", true));
    }

    // =========================================================================
    // MÀN HÌNH SCR-12: Khóa học của tôi (/learning/my-courses) [UTC0642 - UTC0668]
    // =========================================================================

    @Test
    @DisplayName("UTC0642 [SCR-12]: Kiểm tra bảo mật: Chưa đăng nhập truy cập /learning/my-courses bị chuyển hướng sang /auth/login")
    @WithAnonymousUser
    void test_UTC0642_myCourses_unauthenticated_redirectsLogin() throws Exception {
        mockMvc.perform(get("/learning/my-courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("UTC0655 [SCR-12]: Kiểm tra hiển thị danh sách khóa học đã đăng ký kèm thanh tiến độ %")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0655_myCourses_displaysEnrolledCoursesAndProgress() throws Exception {
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(n5Course)
                .status("ACTIVE")
                .progressPercent(75)
                .build());

        mockMvc.perform(get("/learning/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/my-courses"))
                .andExpect(model().attributeExists("registrations"))
                .andExpect(model().attribute("totalCount", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("UTC0657 [SCR-12]: Kiểm tra trạng thái rỗng khi học viên chưa đăng ký khóa học nào")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0657_myCourses_emptyEnrollment_state() throws Exception {
        registrationRepository.deleteAll();

        mockMvc.perform(get("/learning/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/my-courses"))
                .andExpect(model().attribute("totalCount", 0));
    }

    // =========================================================================
    // MÀN HÌNH SCR-13: Khóa học Gợi ý Cá nhân (/learning/courses/recommended) [UTC0669 - UTC0687]
    // =========================================================================

    @Test
    @DisplayName("UTC0669 [SCR-13]: Kiểm tra nạp danh sách khóa học gợi ý thích ứng theo cấp độ học viên")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0669_recommendedCourses_loadsAdaptiveItems() throws Exception {
        mockMvc.perform(get("/learning/courses/recommended"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/recommended-courses"))
                .andExpect(model().attributeExists("recommendedItems"))
                .andExpect(model().attributeExists("learner"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-14: Đăng ký Ghi danh Khóa học (/learning/courses/{id}/register) [UTC0688 - UTC0702]
    // =========================================================================

    @Test
    @DisplayName("UTC0688 [SCR-14]: Kiểm tra hiển thị biểu mẫu đăng ký khóa học với thông tin điền sẵn")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0688_registerCourseForm_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/register", n4Course.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/register-course"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("registrationRequest"));
    }

    @Test
    @DisplayName("UTC0693 [SCR-14]: Kiểm tra để trống trường bắt buộc (Họ và tên) kích hoạt thông báo lỗi validation")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0693_registerCourse_emptyPhone_validationError() throws Exception {
        mockMvc.perform(post("/learning/courses/{id}/register", n4Course.getId())
                        .with(csrf())
                        .param("courseId", String.valueOf(n4Course.getId()))
                        .param("fullName", "")
                        .param("email", "test@jeps.edu.vn")
                        .param("phone", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/register-course"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("UTC0700 [SCR-14]: Kiểm tra đăng ký khóa học miễn phí (0Đ) được kích hoạt ngay lập tức")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0700_registerFreeCourse_instantActivation() throws Exception {
        mockMvc.perform(post("/learning/courses/{id}/register", freeCourse.getId())
                        .with(csrf())
                        .param("courseId", String.valueOf(freeCourse.getId()))
                        .param("fullName", "Nguyễn Văn A")
                        .param("email", "learner_utc_course@jeps.edu.vn")
                        .param("phone", "0912345678")
                        .param("paymentMethod", "FREE")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + freeCourse.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("UTC0702 [SCR-14]: Kiểm tra đăng ký lại khóa học đã sở hữu bị chặn với thông báo cảnh báo")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0702_registerAlreadyOwnedCourse_rejected() throws Exception {
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(n5Course)
                .status("ACTIVE")
                .progressPercent(10)
                .build());

        mockMvc.perform(post("/learning/courses/{id}/register", n5Course.getId())
                        .with(csrf())
                        .param("courseId", String.valueOf(n5Course.getId()))
                        .param("fullName", "Nguyễn Văn A")
                        .param("email", "learner_utc_course@jeps.edu.vn")
                        .param("phone", "0912345678")
                        .param("paymentMethod", "FREE")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + n5Course.getId()))
                .andExpect(flash().attributeExists("warningMessage"));
    }

    // =========================================================================
    // CÁC NHÁNH ĐIỀU HƯỚNG & TRANG CHỦ MỞ RỘNG (BOOST COVERAGE >= 80%)
    // =========================================================================

    @Test
    @DisplayName("Kiểm tra trang chủ công khai (Home /) nạp danh sách khóa học nổi bật")
    void test_publicHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-home"))
                .andExpect(model().attributeExists("featuredCourses"))
                .andExpect(model().attributeExists("activeMenu"));
    }

    @Test
    @DisplayName("Kiểm tra trang chủ công khai (Home /) khi người dùng đã đăng nhập")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_publicHomePage_authenticated() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-home"))
                .andExpect(model().attributeExists("featuredCourses"))
                .andExpect(model().attributeExists("learner"))
                .andExpect(model().attributeExists("registrations"));
    }

    @Test
    @DisplayName("Kiểm tra trang giới thiệu thi thử JLPT (/mock-exam)")
    void test_publicMockExamIntro() throws Exception {
        mockMvc.perform(get("/mock-exam"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-JLPT-mock-exam"))
                .andExpect(model().attributeExists("mockExams"))
                .andExpect(model().attributeExists("activeMenu"));
    }

    @Test
    @DisplayName("Kiểm tra chi tiết đề thi thử JLPT không tồn tại (/mock-exam/999999) trả về 404")
    void test_publicMockExamDetails_notFound() throws Exception {
        mockMvc.perform(get("/mock-exam/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Kiểm tra trang giới thiệu (/about)")
    void test_publicAboutPage() throws Exception {
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-about"));
    }

    @Test
    @DisplayName("Kiểm tra điều hướng Dashboard học viên (/learning/dashboard -> /learning/my-courses)")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_learnerDashboard() throws Exception {
        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/my-courses"));
    }

    @Test
    @DisplayName("Kiểm tra điều hướng /learning/courses với các bộ lọc level và keyword")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_learnerCourseListRedirects() throws Exception {
        mockMvc.perform(get("/learning/courses").param("level", "N5").param("keyword", "Kanji"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?level=N5&q=Kanji"));

        mockMvc.perform(get("/learning/courses").param("level", "N4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?level=N4"));

        mockMvc.perform(get("/learning/courses").param("keyword", "Nghe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?q=Nghe"));

        mockMvc.perform(get("/learning/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));
    }

    @Test
    @DisplayName("Kiểm tra điều hướng chi tiết /learning/courses/{id} sang /courses/{id}")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_learnerCourseDetailRedirect() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}", n5Course.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + n5Course.getId()));
    }

    @Test
    @DisplayName("Kiểm tra nạp form đăng ký khi đã đăng ký trước đó chuyển hướng về trang chi tiết kèm cảnh báo")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_registerPage_alreadyEnrolled() throws Exception {
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(freeCourse)
                .status("ACTIVE")
                .progressPercent(50)
                .build());

        mockMvc.perform(get("/learning/courses/{id}/register", freeCourse.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + freeCourse.getId()))
                .andExpect(flash().attributeExists("warningMessage"));
    }

    @Test
    @DisplayName("Kiểm tra xem khóa học của tôi khi có khóa đã hoàn thành 100%")
    @WithMockUser(username = "learner_utc_course@jeps.edu.vn", roles = {"LEARNER"})
    void test_myCourses_completedProgress() throws Exception {
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(n5Course)
                .status("COMPLETED")
                .progressPercent(100)
                .build());

        mockMvc.perform(get("/learning/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/my-courses"))
                .andExpect(model().attributeExists("registrations"))
                .andExpect(model().attribute("completedCount", greaterThanOrEqualTo(1L)));
    }

    @Test
    @DisplayName("Kiểm tra PublicCatalogService kiểm tra trạng thái public và danh sách đề thi JLPT")
    void test_publicCatalogService_methods() {
        boolean isPub = publicCatalogService.isPublic(n5Course);
        assertThat(isPub, is(true));

        var mockExams = publicCatalogService.getJlptMockExams();
        assertThat(mockExams, notNullValue());
    }
}
