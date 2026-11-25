package com.jlpp.modules.course;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.entity.StudyMaterial;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.course.repository.StudyMaterialRepository;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit & Integration Test Suite for LearnerLesson Module
 * Strictly traced to Report-5.1_Unit Test_All10Modules_backup.xls: Sheet 'LearnerLesson' (UTC0703 - UTC0769)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerLessonUnitTest {

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
    private StudyMaterialRepository studyMaterialRepository;

    private User learner;
    private Course testCourse;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("learner_lesson_utc@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_lesson_utc@jeps.edu.vn")
                        .username("learner_lesson_utc")
                        .password("$2a$10$encryptedPasswordPlaceholder")
                        .fullName("Learner Lesson Tester")
                        .phone("0988776655")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .build()));

        testCourse = courseRepository.save(Course.builder()
                .title("Khóa học Kiểm thử Bài học N5")
                .level("N5")
                .price(0.0)
                .status("PUBLISHED")
                .lessonCount(10)
                .build());

        lesson1 = lessonRepository.save(Lesson.builder()
                .course(testCourse)
                .title("Bài 1: Giới thiệu căn bản")
                .orderIndex(1)
                .sectionName("Chương 1: Kiến thức cơ sở")
                .lessonType("THEORY")
                .durationMinutes(25)
                .status("ACTIVE")
                .build());

        lesson2 = lessonRepository.save(Lesson.builder()
                .course(testCourse)
                .title("Bài 2: Ngữ pháp nhập môn")
                .orderIndex(2)
                .sectionName("Chương 1: Kiến thức cơ sở")
                .lessonType("THEORY")
                .durationMinutes(30)
                .status("ACTIVE")
                .build());

        lesson3 = lessonRepository.save(Lesson.builder()
                .course(testCourse)
                .title("Bài 3: Luyện nghe và đọc")
                .orderIndex(3)
                .sectionName("Chương 2: Luyện tập tổng hợp")
                .lessonType("PRACTICE")
                .durationMinutes(40)
                .status("ACTIVE")
                .build());

        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(testCourse)
                .status("ACTIVE")
                .progressPercent(0)
                .build());

        studyMaterialRepository.save(StudyMaterial.builder()
                .lessonId(lesson1.getId())
                .materialTitle("Tài liệu tổng hợp ngữ pháp N5")
                .materialType("PDF")
                .jlptLevel("N5")
                .skillType("GRAMMAR")
                .fileUrl("/uploads/grammar_n5.pdf")
                .status("ACTIVE")
                .build());

        studyMaterialRepository.save(StudyMaterial.builder()
                .lessonId(lesson1.getId())
                .materialTitle("File âm thanh hội thoại mẫu")
                .materialType("AUDIO")
                .jlptLevel("N5")
                .skillType("LISTENING")
                .audioUrl("/uploads/listening_n5.mp3")
                .status("ACTIVE")
                .build());
    }

    // =========================================================================
    // MÀN HÌNH SCR-15: Giáo trình Danh sách Bài học (/learning/courses/{id}/lessons) [UTC0703 - UTC0723]
    // =========================================================================

    @Test
    @DisplayName("UTC0703 [SCR-15]: Kiểm tra nạp giao diện Danh sách Bài học (HTTP 200 OK)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0703_viewLessonList_200OK() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/lessons", testCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-list"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("lessons"))
                .andExpect(model().attributeExists("groupedLessons"));
    }

    @Test
    @DisplayName("UTC0711 [SCR-15]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập danh sách bài học")
    @WithAnonymousUser
    void test_UTC0711_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/lessons", testCourse.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0719 [SCR-15]: Kiểm tra cấu trúc nhóm bài học theo chương (Grouped Lessons)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0719_groupedLessonsStructure() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/lessons", testCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("groupedLessons", hasKey("Chương 1: Kiến thức cơ sở")))
                .andExpect(model().attribute("groupedLessons", hasKey("Chương 2: Luyện tập tổng hợp")));
    }

    @Test
    @DisplayName("UTC0721 [SCR-15]: Kiểm tra hiển thị trạng thái và tiến độ học tập")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0721_lessonProgressDisplay() throws Exception {
        mockMvc.perform(get("/learning/courses/{id}/lessons", testCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("progressPercent"))
                .andExpect(model().attributeExists("completedCount"))
                .andExpect(model().attribute("totalCount", 3));
    }

    @Test
    @DisplayName("UTC0723 [SCR-15]: Kiểm tra truy cập danh sách bài học không truyền courseId")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0723_viewLessonsWithoutCourseId() throws Exception {
        mockMvc.perform(get("/learning/lessons"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-list"))
                .andExpect(model().attributeExists("course"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-16: Phòng Học Bài học Lý thuyết (/learning/lessons/{id}) [UTC0724 - UTC0749]
    // =========================================================================

    @Test
    @DisplayName("UTC0724 [SCR-16]: Kiểm tra nạp giao diện Phòng Học Bài học hợp lệ (HTTP 200 OK)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0724_studyLesson_200OK() throws Exception {
        mockMvc.perform(get("/learning/lessons/{id}", lesson1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attributeExists("lesson"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("groupedLessons"));
    }

    @Test
    @DisplayName("UTC0732 [SCR-16]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập phòng học")
    @WithAnonymousUser
    void test_UTC0732_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/learning/lessons/{id}", lesson1.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0741 [SCR-16]: Kiểm tra điều hướng: Nút 'Bài trước' và 'Bài tiếp theo'")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0741_navigationPrevAndNextLesson() throws Exception {
        mockMvc.perform(get("/learning/lessons/{id}", lesson2.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevLesson", notNullValue()))
                .andExpect(model().attribute("nextLesson", notNullValue()));
    }

    @Test
    @DisplayName("UTC0742 [SCR-16]: Kiểm tra điều hướng biên: Bài học đầu tiên không có bài trước")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0742_firstLesson_prevLessonNull() throws Exception {
        mockMvc.perform(get("/learning/lessons/{id}", lesson1.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevLesson", nullValue()))
                .andExpect(model().attribute("nextLesson", notNullValue()));
    }

    @Test
    @DisplayName("UTC0744 [SCR-16]: Kiểm tra điều hướng biên: Bài học cuối cùng không có bài tiếp theo")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0744_lastLesson_nextLessonNull() throws Exception {
        mockMvc.perform(get("/learning/lessons/{id}", lesson3.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevLesson", notNullValue()))
                .andExpect(model().attribute("nextLesson", nullValue()));
    }

    @Test
    @DisplayName("UTC0748 [SCR-16]: Kiểm tra đánh dấu hoàn thành bài học và chuyển tiếp sang bài kế tiếp")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0748_completeLesson_advanceToNext() throws Exception {
        mockMvc.perform(post("/learning/lessons/{id}/complete", lesson1.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/lessons/" + lesson2.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("UTC0748 [SCR-16]: Kiểm tra hoàn thành bài học cuối cùng quay lại danh sách bài học khóa học")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0748_completeLastLesson_redirectToCourseLessons() throws Exception {
        mockMvc.perform(post("/learning/lessons/{id}/complete", lesson3.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/courses/" + testCourse.getId() + "/lessons"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-17: Kho Tài liệu & File Âm thanh (/learning/materials) [UTC0750 - UTC0769]
    // =========================================================================

    @Test
    @DisplayName("UTC0750 [SCR-17]: Kiểm tra nạp giao diện Kho Tài liệu (HTTP 200 OK)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0750_viewMaterials_200OK() throws Exception {
        mockMvc.perform(get("/learning/materials"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials"))
                .andExpect(model().attributeExists("totalCount"))
                .andExpect(model().attributeExists("audioCount"))
                .andExpect(model().attributeExists("pdfCount"));
    }

    @Test
    @DisplayName("UTC0758 [SCR-17]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập kho tài liệu")
    @WithAnonymousUser
    void test_UTC0758_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/learning/materials"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0766 [SCR-17]: Kiểm tra lọc tài liệu theo cấp độ khóa học")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0766_filterMaterialsByLevel() throws Exception {
        mockMvc.perform(get("/learning/materials").param("level", "N5"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials"));
    }

    @Test
    @DisplayName("UTC0767 [SCR-17]: Kiểm tra tìm kiếm tài liệu theo từ khóa")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0767_searchMaterialsByKeyword() throws Exception {
        mockMvc.perform(get("/learning/materials").param("keyword", "ngữ pháp"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials"));
    }

    @Test
    @DisplayName("Kiểm tra xem kho tài liệu theo khóa học kết hợp lọc kỹ năng và loại tài liệu")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_materialsByCourseIdAndFilters() throws Exception {
        mockMvc.perform(get("/learning/courses/{courseId}/materials", testCourse.getId())
                        .param("level", "N5")
                        .param("skill", "GRAMMAR")
                        .param("type", "PDF"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials"))
                .andExpect(model().attributeExists("filterCourse"));
    }

    @Test
    @DisplayName("Kiểm tra phòng học với route đầy đủ cả courseId và lessonId")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_studyLessonWithCourseIdRoute() throws Exception {
        mockMvc.perform(get("/learning/courses/{courseId}/lessons/{lessonId}", testCourse.getId(), lesson1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attributeExists("lesson"))
                .andExpect(model().attributeExists("course"));
    }

    @Test
    @DisplayName("Chặn quyền: Học viên chưa đăng ký khóa học không được truy cập danh sách bài học (/courses/{id}/lessons)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_unenrolledLearner_cannotAccessLessonList_redirectsToCourseDetail() throws Exception {
        Course unenrolledCourse = courseRepository.save(Course.builder()
                .title("Khóa học Chưa Cấp Phép N3")
                .level("N3")
                .price(499000.0)
                .status("PUBLISHED")
                .build());

        mockMvc.perform(get("/learning/courses/{courseId}/lessons", unenrolledCourse.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + unenrolledCourse.getId()))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Chặn quyền: Học viên chưa đăng ký khóa học không được học bài học chính thức (không phải học thử)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_unenrolledLearner_cannotAccessNonFreeLesson_redirectsToCourseDetail() throws Exception {
        Course unenrolledCourse = courseRepository.save(Course.builder()
                .title("Khóa học Ngữ Pháp N2")
                .level("N2")
                .price(599000.0)
                .status("PUBLISHED")
                .build());

        Lesson lockedLesson = lessonRepository.save(Lesson.builder()
                .course(unenrolledCourse)
                .title("Bài 1: Cấu trúc N2 Nâng cao")
                .orderIndex(1)
                .isFreePreview(false)
                .status("ACTIVE")
                .build());

        mockMvc.perform(get("/learning/lessons/{id}", lockedLesson.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + unenrolledCourse.getId()))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Cho phép: Học viên chưa đăng ký vẫn được học bài cho phép học thử (isFreePreview = true)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_unenrolledLearner_canAccessFreePreviewLesson_success() throws Exception {
        Course unenrolledCourse = courseRepository.save(Course.builder()
                .title("Khóa học Hán tự N1")
                .level("N1")
                .price(799000.0)
                .status("PUBLISHED")
                .build());

        Lesson freeLesson = lessonRepository.save(Lesson.builder()
                .course(unenrolledCourse)
                .title("Bài 1: Giới thiệu Hán tự N1 (Học thử)")
                .orderIndex(1)
                .isFreePreview(true)
                .status("ACTIVE")
                .build());

        mockMvc.perform(get("/learning/lessons/{id}", freeLesson.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attributeExists("lesson"));
    }

    @Test
    @DisplayName("Khách vãng lai (Guest chưa đăng nhập) có thể xem chi tiết khóa học và danh sách bài học (200 OK)")
    void test_guest_canViewCourseDetailAndSyllabus_200OK() throws Exception {
        mockMvc.perform(get("/courses/{id}", testCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/course-detail"))
                .andExpect(model().attribute("isEnrolled", false))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("lessons"))
                .andExpect(model().attributeExists("groupedLessons"));
    }

    @Test
    @DisplayName("Học viên đăng nhập nhưng chưa đăng ký khóa học xem chi tiết khóa học (isEnrolled = false)")
    @WithMockUser(username = "learner_lesson_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_unenrolledLearner_canViewCourseDetail_isEnrolledFalse() throws Exception {
        Course anotherCourse = courseRepository.save(Course.builder()
                .title("Khóa học Ngữ Pháp N3")
                .level("N3")
                .price(450000.0)
                .status("PUBLISHED")
                .build());

        mockMvc.perform(get("/courses/{id}", anotherCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/course-detail"))
                .andExpect(model().attribute("isEnrolled", false))
                .andExpect(model().attributeExists("course"));
    }
}
