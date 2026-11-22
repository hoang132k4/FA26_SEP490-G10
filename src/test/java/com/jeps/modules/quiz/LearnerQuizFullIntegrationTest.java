package com.jeps.modules.quiz;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.quiz.entity.*;
import com.jeps.modules.quiz.repository.*;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
@Sql(statements = {
        "ALTER TABLE lessons ADD COLUMN IF NOT EXISTS lesson_id BIGINT",
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_id BIGINT",
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_title VARCHAR(255)",
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS jlpt_level VARCHAR(10)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class LearnerQuizFullIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private ExerciseQuestionRepository exerciseQuestionRepository;

    @Autowired
    private ExerciseResultRepository exerciseResultRepository;

    @Autowired
    private ExerciseResultDetailRepository exerciseResultDetailRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User learnerA;
    private User learnerB;
    private Long testLessonId = 98001L;
    private Exercise testExercise;
    private Question q1;
    private Question q2;

    @BeforeEach
    void setUp() {
        learnerA = userRepository.findByEmailIgnoreCase("learner_quiz_a@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_quiz_a@jeps.edu.vn")
                        .username("learner_quiz_a")
                        .fullName("Học viên Quiz A")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        learnerB = userRepository.findByEmailIgnoreCase("learner_quiz_b@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_quiz_b@jeps.edu.vn")
                        .username("learner_quiz_b")
                        .fullName("Học viên Quiz B")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        Course course = courseRepository.saveAndFlush(Course.builder()
                .id(8401L)
                .title("Khóa học Ngữ pháp N4")
                .level("N4")
                .status("PUBLISHED")
                .price(199000.0)
                .build());

        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, testLessonId, course.getId(), "Bài 1: Trắc nghiệm ngữ pháp", 1, "Chương 1", 20, "QUIZ", "Nội dung bài học");

        // Create Exercise
        testExercise = exerciseRepository.saveAndFlush(Exercise.builder()
                .lessonId(testLessonId)
                .exerciseTitle("Bài tập trắc nghiệm bài #1")
                .totalQuestions(2)
                .passingScore(60)
                .status("PUBLISHED")
                .build());

        // Question 1: Đâu là nghĩa của từ '食べる'?
        q1 = questionRepository.saveAndFlush(Question.builder()
                .content("Đâu là nghĩa của từ '食べる'?")
                .explanation("'食べる' nghĩa là ăn cơm/ăn uống.")
                .category("VOCABULARY")
                .jlptLevel("N5")
                .difficulty("EASY")
                .build());

        questionOptionRepository.saveAndFlush(QuestionOption.builder()
                .questionId(q1.getId()).label("A").optionText("Ăn").isCorrect(true).build());
        questionOptionRepository.saveAndFlush(QuestionOption.builder()
                .questionId(q1.getId()).label("B").optionText("Uống").isCorrect(false).build());

        // Question 2: Chọn trợ từ thích hợp: 本____読みます。
        q2 = questionRepository.saveAndFlush(Question.builder()
                .content("Chọn trợ từ thích hợp: 本____読みます。")
                .explanation("Trợ từ 'を' chỉ đối tượng chịu tác động của tha động từ.")
                .category("GRAMMAR")
                .jlptLevel("N5")
                .difficulty("EASY")
                .build());

        questionOptionRepository.saveAndFlush(QuestionOption.builder()
                .questionId(q2.getId()).label("A").optionText("に").isCorrect(false).build());
        questionOptionRepository.saveAndFlush(QuestionOption.builder()
                .questionId(q2.getId()).label("B").optionText("を").isCorrect(true).build());

        // Link questions to Exercise
        exerciseQuestionRepository.saveAndFlush(ExerciseQuestion.builder()
                .exerciseId(testExercise.getExerciseId())
                .questionId(q1.getId())
                .questionOrder(1)
                .build());

        exerciseQuestionRepository.saveAndFlush(ExerciseQuestion.builder()
                .exerciseId(testExercise.getExerciseId())
                .questionId(q2.getId())
                .questionOrder(2)
                .build());
    }

    // ==========================================
    // 1. START QUIZ (/quiz/do)
    // ==========================================

    @Test
    @DisplayName("Start Quiz: Mở bài tập theo lessonId tải đầy đủ câu hỏi và các lựa chọn")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testDoQuiz_WithLessonId() throws Exception {
        mockMvc.perform(get("/quiz/do").param("lessonId", testLessonId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"))
                .andExpect(model().attributeExists("exercise", "questions", "lesson", "course"))
                .andExpect(model().attribute("totalQuestions", is(2)))
                .andExpect(model().attribute("activeMenu", is("my-courses")));
    }

    @Test
    @DisplayName("Start Quiz: Mở bài tập trực tiếp theo exerciseId")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testDoQuiz_WithExerciseId() throws Exception {
        mockMvc.perform(get("/quiz/do").param("exerciseId", testExercise.getExerciseId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"))
                .andExpect(model().attributeExists("exercise", "questions"))
                .andExpect(model().attribute("totalQuestions", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Start Quiz: Thiếu cả lessonId và exerciseId sẽ chuyển hướng về Dashboard")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testDoQuiz_MissingParameters_RedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/quiz/do"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));
    }

    // ==========================================
    // 2. SUBMIT QUIZ (/quiz/submit)
    // ==========================================

    @Test
    @DisplayName("Submit Quiz: Chưa đăng nhập sẽ chuyển hướng sang trang đăng nhập")
    @WithAnonymousUser
    void testSubmitQuiz_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/quiz/submit")
                        .with(csrf())
                        .param("exerciseId", testExercise.getExerciseId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http*://*/auth/login*"));
    }

    @Test
    @DisplayName("Submit Quiz: Trả lời đúng toàn bộ (100%) -> Đạt PASSED và lưu chi tiết từng câu")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testSubmitQuiz_AllCorrect_Passed100Percent() throws Exception {
        mockMvc.perform(post("/quiz/submit")
                        .with(csrf())
                        .param("exerciseId", testExercise.getExerciseId().toString())
                        .param("answer_" + q1.getId(), "A") // đúng
                        .param("answer_" + q2.getId(), "B")) // đúng
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/quiz/result/*"));

        List<ExerciseResult> results = exerciseResultRepository.findByUserIdAndExerciseIdOrderBySubmittedDateDesc(learnerA.getId(), testExercise.getExerciseId());
        assertFalse(results.isEmpty());
        ExerciseResult res = results.get(0);

        assertEquals("PASSED", res.getResultStatus());
        assertEquals(20, res.getScore()); // 2 câu x 10 điểm = 20
        assertEquals(100.0, res.getPercentage().doubleValue(), 0.01);
        assertEquals(1, res.getAttemptNumber());

        List<ExerciseResultDetail> details = exerciseResultDetailRepository.findByExerciseResultId(res.getExerciseResultId());
        assertEquals(2, details.size());
        assertTrue(details.stream().allMatch(d -> Boolean.TRUE.equals(d.getIsCorrect())));
    }

    @Test
    @DisplayName("Submit Quiz: Trả lời sai toàn bộ (0%) -> FAILED và ghi nhận phản hồi cần cải thiện")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testSubmitQuiz_AllWrong_Failed0Percent() throws Exception {
        mockMvc.perform(post("/quiz/submit")
                        .with(csrf())
                        .param("exerciseId", testExercise.getExerciseId().toString())
                        .param("answer_" + q1.getId(), "B") // sai
                        .param("answer_" + q2.getId(), "A")) // sai
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/quiz/result/*"));

        List<ExerciseResult> results = exerciseResultRepository.findByUserIdAndExerciseIdOrderBySubmittedDateDesc(learnerA.getId(), testExercise.getExerciseId());
        assertFalse(results.isEmpty());
        ExerciseResult res = results.get(0);

        assertEquals("FAILED", res.getResultStatus());
        assertEquals(0, res.getScore());
        assertEquals(0.0, res.getPercentage().doubleValue(), 0.01);
        assertTrue(res.getFeedback().contains("Bạn chưa đạt điểm chuẩn tối thiểu"));
    }

    @Test
    @DisplayName("Submit Quiz: Làm lại lần 2 tự động tăng số lần thử (Attempt Number = 2)")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testSubmitQuiz_MultipleAttempts_IncrementsAttemptNumber() throws Exception {
        // Lần 1
        mockMvc.perform(post("/quiz/submit")
                        .with(csrf())
                        .param("exerciseId", testExercise.getExerciseId().toString())
                        .param("answer_" + q1.getId(), "B"))
                .andExpect(status().is3xxRedirection());

        // Lần 2
        mockMvc.perform(post("/quiz/submit")
                        .with(csrf())
                        .param("exerciseId", testExercise.getExerciseId().toString())
                        .param("answer_" + q1.getId(), "A")
                        .param("answer_" + q2.getId(), "B"))
                .andExpect(status().is3xxRedirection());

        List<ExerciseResult> results = exerciseResultRepository.findByUserIdAndExerciseIdOrderBySubmittedDateDesc(learnerA.getId(), testExercise.getExerciseId());
        assertEquals(2, results.size());
        // Bài làm mới nhất có attemptNumber = 2
        assertEquals(2, results.get(0).getAttemptNumber());
    }

    // ==========================================
    // 3. VIEW RESULT (/quiz/result/{resultId})
    // ==========================================

    @Test
    @DisplayName("View Result: Người làm bài xem được kết quả chi tiết kèm giải thích từng câu")
    @WithMockUser(username = "learner_quiz_a@jeps.edu.vn", roles = {"LEARNER"})
    void testViewResult_Owner_Success() throws Exception {
        ExerciseResult saved = exerciseResultRepository.saveAndFlush(ExerciseResult.builder()
                .exerciseId(testExercise.getExerciseId())
                .userId(learnerA.getId())
                .attemptNumber(1)
                .score(20)
                .percentage(BigDecimal.valueOf(100.0))
                .resultStatus("PASSED")
                .feedback("Xuất sắc!")
                .build());

        exerciseResultDetailRepository.saveAndFlush(ExerciseResultDetail.builder()
                .exerciseResultId(saved.getExerciseResultId())
                .questionId(q1.getId())
                .selectedOption("A")
                .isCorrect(true)
                .build());

        mockMvc.perform(get("/quiz/result/{resultId}", saved.getExerciseResultId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/result"))
                .andExpect(model().attributeExists("result", "lesson", "course"))
                .andExpect(model().attribute("result", hasProperty("passed", is(true))))
                .andExpect(model().attribute("result", hasProperty("score", is(20))));
    }

    @Test
    @DisplayName("View Result Bảo mật (IDOR): Học viên khác không được xem kết quả của người khác (403 Forbidden)")
    @WithMockUser(username = "learner_quiz_b@jeps.edu.vn", roles = {"LEARNER"})
    void testViewResult_OtherLearner_Forbidden() throws Exception {
        // Kết quả này thuộc về learnerA
        ExerciseResult saved = exerciseResultRepository.saveAndFlush(ExerciseResult.builder()
                .exerciseId(testExercise.getExerciseId())
                .userId(learnerA.getId())
                .attemptNumber(1)
                .score(20)
                .percentage(BigDecimal.valueOf(100.0))
                .resultStatus("PASSED")
                .build());

        // learnerB cố tình truy cập vào ID của learnerA
        mockMvc.perform(get("/quiz/result/{resultId}", saved.getExerciseResultId()))
                .andExpect(status().isForbidden());
    }
}
