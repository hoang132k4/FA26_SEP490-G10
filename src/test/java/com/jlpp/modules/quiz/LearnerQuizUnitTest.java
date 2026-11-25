package com.jlpp.modules.quiz;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.dto.QuizResultDto;
import com.jlpp.modules.quiz.entity.Exercise;
import com.jlpp.modules.quiz.service.QuizService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit & Integration Test Suite for LearnerQuiz Module
 * Strictly traced to Report-5.1_Unit Test_All10Modules_backup.xls: Sheet 'LearnerQuiz' (UTC0898 - UTC0949)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerQuizUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private QuizService quizService;

    private User learnerA;
    private User learnerB;
    private Course testCourse;
    private Lesson testLesson;
    private Exercise testExercise;

    @BeforeEach
    void setUp() {
        learnerA = userRepository.findByEmailIgnoreCase("learner_quiz_a@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_quiz_a@jeps.edu.vn")
                        .username("learner_quiz_a")
                        .password("$2a$10$encryptedPasswordPlaceholder")
                        .fullName("Learner Quiz A")
                        .phone("0911223344")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .build()));

        learnerB = userRepository.findByEmailIgnoreCase("learner_quiz_b@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_quiz_b@jeps.edu.vn")
                        .username("learner_quiz_b")
                        .password("$2a$10$encryptedPasswordPlaceholder")
                        .fullName("Learner Quiz B")
                        .phone("0922334455")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .build()));

        testCourse = courseRepository.save(Course.builder()
                .title("Khóa học Luyện Quiz N5")
                .level("N5")
                .price(0.0)
                .status("PUBLISHED")
                .lessonCount(5)
                .build());

        testLesson = lessonRepository.save(Lesson.builder()
                .course(testCourse)
                .title("Bài 1: Trắc nghiệm ngữ pháp căn bản")
                .orderIndex(1)
                .sectionName("Chương 1")
                .lessonType("QUIZ")
                .durationMinutes(20)
                .status("ACTIVE")
                .build());

        testExercise = quizService.getOrCreateExerciseForLesson(testLesson.getId());
    }

    // =========================================================================
    // MÀN HÌNH SCR-18: Làm Bài tập Trắc nghiệm Quiz (/quiz/do) [UTC0898 - UTC0926]
    // =========================================================================

    @Test
    @DisplayName("UTC0898 [SCR-18]: Kiểm tra nạp giao diện Làm Bài tập Quiz (HTTP 200 OK)")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0898_viewDoQuiz_200OK() throws Exception {
        mockMvc.perform(get("/quiz/do").param("lessonId", String.valueOf(testLesson.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"))
                .andExpect(model().attributeExists("exercise"))
                .andExpect(model().attributeExists("questions"))
                .andExpect(model().attributeExists("lesson"))
                .andExpect(model().attributeExists("course"));
    }

    @Test
    @DisplayName("UTC0906 [SCR-18]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập làm quiz")
    @WithAnonymousUser
    void test_UTC0906_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/quiz/do").param("lessonId", String.valueOf(testLesson.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0914 [SCR-18]: Kiểm tra nạp danh sách câu hỏi trắc nghiệm theo bài học")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0914_loadQuestionsForLesson() throws Exception {
        mockMvc.perform(get("/quiz/do").param("lessonId", String.valueOf(testLesson.getId())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questions", not(empty())))
                .andExpect(model().attribute("totalQuestions", greaterThan(0)));
    }

    @Test
    @DisplayName("UTC0915 [SCR-18]: Kiểm tra mở bài tập trắc nghiệm trực tiếp theo exerciseId")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0915_loadQuestionsByExerciseId() throws Exception {
        mockMvc.perform(get("/quiz/do").param("exerciseId", String.valueOf(testExercise.getExerciseId())))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"))
                .andExpect(model().attribute("exercise", hasProperty("exerciseId", is(testExercise.getExerciseId()))));
    }

    @Test
    @DisplayName("UTC0917 [SCR-18]: Kiểm tra truy cập /quiz/do không truyền tham số an toàn chuyển hướng về dashboard")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0917_missingParameters_redirectToDashboard() throws Exception {
        mockMvc.perform(get("/quiz/do"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));
    }

    @Test
    @DisplayName("UTC0920 [SCR-18]: Kiểm tra nộp bài khi chưa đăng nhập chuyển hướng đăng nhập")
    @WithAnonymousUser
    void test_UTC0920_submitUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/quiz/submit")
                        .with(csrf())
                        .param("exerciseId", String.valueOf(testExercise.getExerciseId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    private String resolveCorrectOption(QuizQuestionDto question) {
        if (question.getOptions() == null || question.getOptions().isEmpty()) {
            throw new IllegalStateException("Question " + question.getQuestionId() + " has no options");
        }
        return question.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .map(com.jlpp.modules.quiz.dto.QuestionOptionDto::getLabel)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question " + question.getQuestionId() + " has no correct option"));
    }

    @Test
    @DisplayName("UTC0921 [SCR-18]: Kiểm tra nộp bài tập trắc nghiệm thành công và chuyển hướng đến màn hình kết quả")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0921_submitQuiz_success() throws Exception {
        List<QuizQuestionDto> questions = quizService.getQuestionsForExercise(testExercise.getExerciseId());

        var req = post("/quiz/submit")
                .with(csrf())
                .param("exerciseId", String.valueOf(testExercise.getExerciseId()));

        for (QuizQuestionDto q : questions) {
            req.param("answer_" + q.getQuestionId(), resolveCorrectOption(q));
        }

        mockMvc.perform(req)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/quiz/result/*"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-19: Kết quả Quiz & Sensei AI (/quiz/result/{id}) [UTC0927 - UTC0949]
    // =========================================================================

    @Test
    @DisplayName("UTC0927 [SCR-19]: Kiểm tra nạp giao diện Kết quả Quiz hợp lệ (HTTP 200 OK)")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0927_viewQuizResult_200OK() throws Exception {
        List<QuizQuestionDto> questions = quizService.getQuestionsForExercise(testExercise.getExerciseId());
        Map<Long, String> answers = new HashMap<>();
        for (QuizQuestionDto q : questions) {
            answers.put(q.getQuestionId(), resolveCorrectOption(q));
        }
        QuizResultDto result = quizService.submitQuiz(testExercise.getExerciseId(), learnerA.getId(), answers);

        mockMvc.perform(get("/quiz/result/{resultId}", result.getResultId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attributeExists("lesson"))
                .andExpect(model().attributeExists("course"));
    }

    @Test
    @DisplayName("UTC0935 [SCR-19]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập xem kết quả quiz")
    @WithAnonymousUser
    void test_UTC0935_unauthenticatedResultAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/quiz/result/{resultId}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0943 [SCR-19]: Kiểm tra tính chính xác của điểm số và trạng thái hoàn thành bài nộp")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0943_scoreCalculation_passed() throws Exception {
        List<QuizQuestionDto> questions = quizService.getQuestionsForExercise(testExercise.getExerciseId());
        Map<Long, String> answers = new HashMap<>();
        for (QuizQuestionDto q : questions) {
            answers.put(q.getQuestionId(), resolveCorrectOption(q));
        }
        QuizResultDto result = quizService.submitQuiz(testExercise.getExerciseId(), learnerA.getId(), answers);

        mockMvc.perform(get("/quiz/result/{resultId}", result.getResultId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("result", hasProperty("correctCount", is(5))))
                .andExpect(model().attribute("result", hasProperty("totalQuestions", is(5))))
                .andExpect(model().attribute("result", hasProperty("passed", is(true))));
    }

    @Test
    @DisplayName("UTC0948 [SCR-19]: Kiểm tra bảo mật IDOR: Học viên B xem bài nộp của Học viên A bị chặn với HTTP 403 Forbidden")
    @WithMockUser(username = "learner_quiz_b@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0948_idorProtection_forbiddenForOtherLearner() throws Exception {
        List<QuizQuestionDto> questions = quizService.getQuestionsForExercise(testExercise.getExerciseId());
        Map<Long, String> answers = new HashMap<>();
        for (QuizQuestionDto q : questions) {
            answers.put(q.getQuestionId(), resolveCorrectOption(q));
        }
        QuizResultDto resultA = quizService.submitQuiz(testExercise.getExerciseId(), learnerA.getId(), answers);

        mockMvc.perform(get("/quiz/result/{resultId}", resultA.getResultId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("UTC0949 [SCR-19]: Kiểm tra xem kết quả bài nộp không tồn tại trả về HTTP 404 Not Found")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0949_nonExistentResult_notFound() throws Exception {
        mockMvc.perform(get("/quiz/result/{resultId}", 9999999L))
                .andExpect(status().isNotFound());
    }
}
