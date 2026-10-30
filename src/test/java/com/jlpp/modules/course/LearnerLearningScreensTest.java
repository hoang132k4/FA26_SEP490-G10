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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerLearningScreensTest {

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

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User learner;
    private Course course;
    private Long lesson1Id = 99001L;
    private Long lesson2Id = 99002L;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("test_learner@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("test_learner@jeps.edu.vn")
                        .username("test_learner")
                        .fullName("Nguyễn Học Viên Test")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        course = courseRepository.saveAndFlush(Course.builder()
                .id(8888L)
                .title("Khóa học Tiếng Nhật JLPT N4 Test")
                .level("N4")
                .status("PUBLISHED")
                .price(499000.0)
                .shortDescription("Khóa học thực chiến N4")
                .lessonCount(2)
                .build());

        jdbcTemplate.update("""
            INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, lesson1Id, course.getId(), "Bài 1: Khởi động N4", 1, "Chương 1: Mở đầu", 25, "VIDEO", "### Nội dung bài 1");

        jdbcTemplate.update("""
            INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, lesson2Id, course.getId(), "Bài 2: Ngữ pháp N4", 2, "Chương 1: Mở đầu", 30, "READING", "### Nội dung bài 2");

        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(course)
                .status("ACTIVE")
                .progressPercent(50)
                .build());

        studyMaterialRepository.save(StudyMaterial.builder()
                .lessonId(lesson1Id)
                .materialTitle("Tài liệu bài 1 PDF")
                .materialType("PDF")
                .jlptLevel("N4")
                .skillType("GRAMMAR")
                .status("ACTIVE")
                .fileUrl("https://example.com/mat1.pdf")
                .build());
    }

    @Test
    @DisplayName("UC: View Learner Dashboard - thành công với dữ liệu thực tế")
    @WithMockUser(username = "test_learner@jeps.edu.vn", roles = {"LEARNER"})
    void testViewLearnerDashboard() throws Exception {
        mockMvc.perform(get("/learning/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/dashboard"))
                .andExpect(model().attributeExists("learner"))
                .andExpect(model().attributeExists("registrations"))
                .andExpect(model().attributeExists("activeCourse"))
                .andExpect(model().attribute("totalRegisteredCourses", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("UC: View Lesson List - hiển thị danh sách bài học và nhóm theo chương")
    @WithMockUser(username = "test_learner@jeps.edu.vn", roles = {"LEARNER"})
    void testViewLessonList() throws Exception {
        mockMvc.perform(get("/learning/courses/{courseId}/lessons", course.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-list"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("lessons"))
                .andExpect(model().attributeExists("groupedLessons"))
                .andExpect(model().attribute("totalCount", 2));
    }

    @Test
    @DisplayName("UC: Study Materials - tìm kiếm và hiển thị tài liệu học tập")
    @WithMockUser(username = "test_learner@jeps.edu.vn", roles = {"LEARNER"})
    void testViewStudyMaterials() throws Exception {
        mockMvc.perform(get("/learning/materials")
                        .param("level", "N4")
                        .param("type", "PDF"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials"))
                .andExpect(model().attribute("totalCount", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("UC: Study Lesson - tải chi tiết bài học và tài liệu liên quan")
    @WithMockUser(username = "test_learner@jeps.edu.vn", roles = {"LEARNER"})
    void testStudyLesson() throws Exception {
        mockMvc.perform(get("/learning/lessons/{lessonId}", lesson1Id))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attributeExists("lesson"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("courseLessons"))
                .andExpect(model().attributeExists("attachedMaterials"));
    }

    @Test
    @DisplayName("UC: Complete Lesson - đánh dấu hoàn thành và cập nhật tiến độ")
    @WithMockUser(username = "test_learner@jeps.edu.vn", roles = {"LEARNER"})
    void testCompleteLesson() throws Exception {
        mockMvc.perform(post("/learning/lessons/{lessonId}/complete", lesson1Id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/lessons/" + lesson2Id))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
