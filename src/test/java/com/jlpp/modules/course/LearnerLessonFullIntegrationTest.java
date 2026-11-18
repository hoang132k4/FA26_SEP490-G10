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
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerLessonFullIntegrationTest {

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
    private FlashcardRepository flashcardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User learner;
    private Course testCourse;
    private Long lesson1Id = 96001L;
    private Long lesson2Id = 96002L;
    private Long lesson3Id = 96003L;
    private Long milestoneLessonId = 96004L;
    private Long lessonAfterMilestoneId = 96005L;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("learner_lesson_test@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_lesson_test@jeps.edu.vn")
                        .username("learner_lesson_test")
                        .fullName("Nguyễn Học Viên Lesson Test")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        testCourse = courseRepository.saveAndFlush(Course.builder()
                .id(8201L)
                .title("Khóa học Đầy đủ N4 Chuẩn Quốc Tế")
                .level("N4")
                .status("PUBLISHED")
                .price(450000.0)
                .shortDescription("Toàn bộ giáo trình N4")
                .lessonCount(5)
                .build());

        // 1. Lesson 1: Theory
        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, lesson1Id, testCourse.getId(), "Bài 1: Bắt đầu N4", 1, "Chương 1: Khởi động", 20, "THEORY", "Nội dung bài 1");

        // 2. Lesson 2: Grammar
        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, lesson2Id, testCourse.getId(), "Bài 2: Ngữ pháp ~te kudasai", 2, "Chương 1: Khởi động", 30, "GRAMMAR", "Nội dung bài 2");

        // 3. Lesson 3: Reading
        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, lesson3Id, testCourse.getId(), "Bài 3: Đọc hiểu ngắn", 3, "Chương 2: Luyện tập", 35, "READING", "Nội dung bài 3");

        // 4. Lesson 4: Milestone Exam
        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, milestoneLessonId, testCourse.getId(), "Bài 4: Đánh giá cột mốc Milestone giữa khóa", 4, "Chương 2: Luyện tập", 45, "EXAM", "Nội dung bài thi cột mốc");

        // 5. Lesson 5: Final Lesson
        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, lessonAfterMilestoneId, testCourse.getId(), "Bài 5: Tổng kết khóa học N4", 5, "Chương 3: Kết thúc", 40, "THEORY", "Nội dung tổng kết");

        // Enroll learner in testCourse
        registrationRepository.save(CourseRegistration.builder()
                .user(learner)
                .course(testCourse)
                .status("ACTIVE")
                .progressPercent(20)
                .build());

        // Seed attached study materials (Audio & PDF)
        studyMaterialRepository.save(StudyMaterial.builder()
                .lessonId(lesson1Id)
                .materialTitle("File nghe hội thoại bài 1")
                .materialType("AUDIO")
                .fileUrl("/audio/n4_listening_01.wav")
                .jlptLevel("N4")
                .skillType("LISTENING")
                .status("ACTIVE")
                .build());

        studyMaterialRepository.save(StudyMaterial.builder()
                .lessonId(lesson1Id)
                .materialTitle("Tài liệu bài 1 PDF")
                .materialType("PDF")
                .fileUrl("https://example.com/doc1.pdf")
                .jlptLevel("N4")
                .skillType("GRAMMAR")
                .status("ACTIVE")
                .build());

        // Seed attached flashcards
        flashcardRepository.save(Flashcard.builder()
                .lessonId(lesson1Id)
                .vocabulary("約束")
                .reading("やくそく")
                .meaning("Lời hứa")
                .jlptLevel("N4")
                .reviewStatus("NEW")
                .build());
    }

    // ==========================================
    // 1. LESSON LIST (/learning/lessons & /learning/courses/{id}/lessons)
    // ==========================================

    @Test
    @DisplayName("Lesson List: Hiển thị giáo trình nhóm theo chương kèm tiến độ học viên")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewLessonList_WithCourseId() throws Exception {
        mockMvc.perform(get("/learning/courses/{courseId}/lessons", testCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-list"))
                .andExpect(model().attributeExists("course", "lessons", "groupedLessons", "registration"))
                .andExpect(model().attribute("totalCount", is(5)))
                .andExpect(model().attribute("progressPercent", is(20)))
                .andExpect(model().attribute("completedCount", is(1)))
                .andExpect(model().attribute("activeMenu", is("my-courses")));
    }

    @Test
    @DisplayName("Lesson List: Gọi /learning/lessons không truyền courseId tự động lấy khóa học đã đăng ký")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewLessonList_WithoutCourseId_PicksRegisteredCourse() throws Exception {
        mockMvc.perform(get("/learning/lessons"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-list"))
                .andExpect(model().attribute("course", hasProperty("id", is(testCourse.getId()))));
    }

    // ==========================================
    // 2. STUDY LESSON (/learning/lessons/{lessonId})
    // ==========================================

    @Test
    @DisplayName("Study Lesson: Tải đầy đủ chi tiết bài học, tài liệu nghe và flashcard liên kết")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testStudyLesson_LoadsDetailsMaterialsAndFlashcards() throws Exception {
        mockMvc.perform(get("/learning/lessons/{lessonId}", lesson1Id))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attributeExists("lesson", "course", "courseLessons", "groupedLessons"))
                .andExpect(model().attribute("currentLessonIndex", is(1)))
                .andExpect(model().attribute("totalLessonsCount", is(5)))
                .andExpect(model().attribute("prevLesson", nullValue())) // bài đầu tiên không có bài trước
                .andExpect(model().attribute("nextLesson", notNullValue())) // bài kế tiếp là bài 2
                .andExpect(model().attribute("attachedMaterials", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(model().attribute("lessonFlashcards", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Study Lesson: Bài học cuối cùng nextLesson sẽ là null")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testStudyLesson_LastLesson_NextLessonIsNull() throws Exception {
        // Hoàn thành milestone để có thể mở bài 5
        // Giả lập tiến độ 80% (4/5 bài) để milestone hoàn tất
        CourseRegistration reg = registrationRepository.findByUserIdAndCourseId(learner.getId(), testCourse.getId()).orElseThrow();
        reg.setProgressPercent(80);
        registrationRepository.saveAndFlush(reg);

        mockMvc.perform(get("/learning/lessons/{lessonId}", lessonAfterMilestoneId))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attribute("currentLessonIndex", is(5)))
                .andExpect(model().attribute("prevLesson", notNullValue()))
                .andExpect(model().attribute("nextLesson", nullValue()));
    }

    @Test
    @DisplayName("Study Lesson: Gặp bài thi cột mốc chưa hoàn thành sẽ bị chặn và điều hướng tới bài thi")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testStudyLesson_MilestoneGate_BlocksWhenMilestoneNotDone() throws Exception {
        // Learner mới chỉ hoàn thành 20% (bài 1), muốn học bài 5 (sau bài milestone 4)
        mockMvc.perform(get("/learning/lessons/{lessonId}", lessonAfterMilestoneId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/exams/milestone*"));
    }

    @Test
    @DisplayName("Study Lesson: Khi yêu cầu chính bài học Milestone sẽ tự chuyển hướng tới trang thi")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testStudyLesson_MilestoneLessonItself_RedirectsToExam() throws Exception {
        mockMvc.perform(get("/learning/lessons/{lessonId}", milestoneLessonId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/exams/milestone*"));
    }

    @Test
    @DisplayName("Study Lesson: ID bài học không tồn tại sẽ trả về lỗi")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testStudyLesson_NotFound_ThrowsException() {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/learning/lessons/{lessonId}", 99999999L));
        });
    }

    // ==========================================
    // 3. COMPLETE LESSON (/learning/lessons/{lessonId}/complete)
    // ==========================================

    @Test
    @DisplayName("Complete Lesson: Đánh dấu hoàn thành bài 1 và tự chuyển tiếp sang bài 2")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCompleteLesson_HasNextLesson_RedirectsToNext() throws Exception {
        mockMvc.perform(post("/learning/lessons/{lessonId}/complete", lesson1Id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/lessons/" + lesson2Id))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Complete Lesson: Bấm sang bài tiếp theo sẽ đánh dấu hoàn thành bài cũ và tăng thanh tiến độ")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCompleteLesson_IncrementsProgressAndMarksCompletedInSidebar() throws Exception {
        // Complete lesson 1
        mockMvc.perform(post("/learning/lessons/{lessonId}/complete", lesson1Id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/lessons/" + lesson2Id));

        // Follow to lesson 2
        mockMvc.perform(get("/learning/lessons/{lessonId}", lesson2Id))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/lesson-study"))
                .andExpect(model().attributeExists("completedLessonIds"))
                .andExpect(model().attribute("completedLessonsCount", greaterThanOrEqualTo(1)))
                .andExpect(model().attribute("registration", hasProperty("progressPercent", greaterThan(0))));
    }

    @Test
    @DisplayName("Complete Lesson: Hoàn thành bài học cuối cùng sẽ chuyển về danh sách bài học khóa học")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCompleteLesson_LastLesson_RedirectsToLessonList() throws Exception {
        mockMvc.perform(post("/learning/lessons/{lessonId}/complete", lessonAfterMilestoneId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/courses/" + testCourse.getId() + "/lessons"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // ==========================================
    // 4. STUDY MATERIALS HUB (/learning/materials)
    // ==========================================

    @Test
    @DisplayName("Study Materials: Lọc tài liệu theo khóa học và đếm số lượng tài liệu âm thanh/PDF")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewStudyMaterials_FilterByCourseId() throws Exception {
        mockMvc.perform(get("/learning/courses/{courseId}/materials", testCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials", "filterCourse"))
                .andExpect(model().attribute("totalCount", greaterThanOrEqualTo(2)))
                .andExpect(model().attribute("audioCount", greaterThanOrEqualTo(1L)))
                .andExpect(model().attribute("pdfCount", greaterThanOrEqualTo(1L)))
                .andExpect(model().attribute("activeMenu", is("listening")));
    }

    @Test
    @DisplayName("Study Materials: Tìm kiếm tài liệu theo từ khóa và kỹ năng")
    @WithMockUser(username = "learner_lesson_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewStudyMaterials_SearchByKeyword() throws Exception {
        mockMvc.perform(get("/learning/materials")
                        .param("keyword", "hội thoại")
                        .param("type", "AUDIO"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/learning/materials"))
                .andExpect(model().attributeExists("materials"))
                .andExpect(model().attribute("audioCount", greaterThanOrEqualTo(1L)));
    }
}
