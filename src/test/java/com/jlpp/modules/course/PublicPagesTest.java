package com.jlpp.modules.course;

import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.lesson.entity.Lesson;
import com.jlpp.modules.lesson.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PublicPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    void guestCanViewPublicPagesAndDatabaseBackedCatalogs() throws Exception {
        courseRepository.save(course(91001L, "Course from database", "N3", true));
        examRepository.save(exam(92001L, "Mock exam from database", "JLPT_MOCK", "N3"));
        examRepository.save(exam(92002L, "Placement test", "PLACEMENT_TEST", "N4"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-home"))
                .andExpect(content().string(containsString("Course from database")));

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-about"));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-courses"))
                .andExpect(content().string(containsString("Course from database")))
                .andExpect(content().string(containsString("Course preview")));

        mockMvc.perform(get("/mock-exam"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-JLPT-mock-exam"))
                .andExpect(content().string(containsString("Mock exam from database")))
                .andExpect(content().string(not(containsString("Placement test"))));
    }

    @Test
    void guestCanPreviewCourseAndLessonMetadataButNotLessonDetails() throws Exception {
        courseRepository.save(course(91002L, "Public course preview", "N5", false));
        lessonRepository.save(new Lesson(93001L, 91002L, "Hiragana basics", null, 1, null,
                "Reading", "20 minutes", 20, "VIDEO", "Alphabet", "PUBLISHED",
                "This detailed lesson description must stay private."));

        mockMvc.perform(get("/courses/91002"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/public/public-course-detail"))
                .andExpect(content().string(containsString("Public course preview")))
                .andExpect(content().string(containsString("1 bài học")))
                .andExpect(content().string(containsString("Hiragana basics")))
                .andExpect(content().string(containsString("Reading")))
                .andExpect(content().string(containsString("20 minutes")))
                .andExpect(content().string(containsString("Nội dung bài học chỉ dành cho")))
                .andExpect(content().string(not(containsString("This detailed lesson description must stay private."))));
    }

    @Test
    void guestCanSeeMockExamDetailsButMustRegisterBeforeStarting() throws Exception {
        examRepository.save(exam(92003L, "JLPT N5 Mock", "JLPT_MOCK", "N5"));

        mockMvc.perform(get("/mock-exam/92003"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JLPT N5 Mock")))
                .andExpect(content().string(containsString("Register to take the test")));

        mockMvc.perform(get("/exams/92003/start"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register?continue=/exams/92003/start"));

        mockMvc.perform(get("/exams"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mock-exam"));

        mockMvc.perform(get("/exams/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register?continue=/exams/history"));

        mockMvc.perform(get("/exams/result/92003"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register?continue=/exams/result/92003"));

        mockMvc.perform(get("/exams/92003/start").sessionAttr("role", "LEARNER"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/exam/take-exam"))
                .andExpect(content().string(containsString("Chưa có bảng câu hỏi")))
                .andExpect(content().string(not(containsString("日本へ行ったら"))));
    }

    private Course course(Long id, String title, String level, boolean featured) {
        return new Course(id, title, level, 250000.0, "Description from test fixture",
                "PUBLISHED", "30 giờ", null, 10, featured);
    }

    private Exam exam(Long id, String title, String type, String level) {
        return new Exam(id, title, type, level, 60, 30, 20,
                "PUBLISHED", "Description from test fixture");
    }
}
