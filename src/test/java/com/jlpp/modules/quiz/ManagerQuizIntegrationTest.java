package com.jlpp.modules.quiz;

import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.jlpp.support.QuestionExcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
// The production schema keys lessons by lesson_id; the H2 schema generated from JPA entities lacks that column.
@Sql(statements = {
        "ALTER TABLE lessons ADD COLUMN IF NOT EXISTS lesson_id BIGINT",
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_id BIGINT",
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_title VARCHAR(255)",
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS jlpt_level VARCHAR(10)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ManagerQuizIntegrationTest {

    private static final String MARK = "IT-QUIZ ";
    private static final long LESSON_ID = 900_001L;

    @Autowired private MockMvc mockMvc;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void seedLesson() {
        jdbc.update("INSERT INTO lessons (id, lesson_id, title, lesson_title, section_name, order_index) VALUES (?, ?, ?, ?, ?, 1)",
                LESSON_ID, LESSON_ID, MARK + "Bài 1", MARK + "Bài 1", MARK + "Chương 1");
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = "LEARNER")
    void learnerIsForbiddenFromQuizManagement() throws Exception {
        mockMvc.perform(get("/manager/quizzes")).andExpect(status().isForbidden());
    }

    @Test
    void listFiltersQuizzesBySectionAndLevel() throws Exception {
        saveQuiz("Quiz lọc", "DRAFT", 5);

        mockMvc.perform(get("/manager/quizzes").param("section", MARK + "Chương 1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/list"))
                .andExpect(model().attribute("quizzes", hasSize(1)));
        mockMvc.perform(get("/manager/quizzes").param("level", "N1").param("section", MARK + "Chương 1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quizzes", hasSize(0)));
    }

    @Test
    void createDraftQuizIsStored() throws Exception {
        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", String.valueOf(LESSON_ID))
                        .param("title", "  " + MARK + "Quiz mới  ")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attribute("quizSuccess", "Đã tạo quiz."));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT lesson_id, total_questions, passing_score, status FROM exercises WHERE exercise_title = ?",
                MARK + "Quiz mới");
        assertEquals(LESSON_ID, ((Number) row.get("LESSON_ID")).longValue());
        assertEquals(0, row.get("TOTAL_QUESTIONS"));
        assertEquals(60, row.get("PASSING_SCORE"));
        assertEquals("DRAFT", row.get("STATUS"));
    }

    @Test
    void createWithPassingScoreAbove100RedisplaysFormWithValidationError() throws Exception {
        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", String.valueOf(LESSON_ID))
                        .param("title", MARK + "Quiz điểm sai")
                        .param("passingScore", "150")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attributeHasFieldErrors("quizForm", "passingScore"));

        assertEquals(0L, countQuizzes("Quiz điểm sai"));
    }

    @Test
    void createPublishedQuizIsRejected() throws Exception {
        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", String.valueOf(LESSON_ID))
                        .param("title", MARK + "Quiz publish ngay")
                        .param("passingScore", "60")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quizError", containsString("Không thể tạo quiz với trạng thái PUBLISHED")));

        assertEquals(0L, countQuizzes("Quiz publish ngay"));
    }

    @Test
    void createForUnknownLessonIsRejected() throws Exception {
        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", "123456789")
                        .param("title", MARK + "Quiz sai bài")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quizError", "Bài học được chọn không tồn tại."));
    }

    @Test
    void editPageOfMissingQuizRedirectsWithError() throws Exception {
        mockMvc.perform(get("/manager/quizzes/{id}/edit", 999_999L))
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attribute("quizError", "Không tìm thấy quiz."));
    }

    @Test
    void publishingQuizWithoutQuestionsKeepsItDraft() throws Exception {
        long quizId = saveQuiz("Quiz thiếu câu", "DRAFT", 0);

        mockMvc.perform(updateQuiz(quizId, "Quiz thiếu câu", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quizError", containsString("quiz chưa có câu hỏi nào")));

        assertEquals("DRAFT", statusOf(quizId));
    }

    @Test
    void linkingQuestionsUpdatesTotalAndAllowsPublishing() throws Exception {
        long quizId = saveQuiz("Quiz đủ câu", "DRAFT", 0);
        Question q1 = saveQuestion("ACTIVE");
        Question q2 = saveQuestion("ACTIVE");

        mockMvc.perform(addQuestions(quizId, q1, q2))
                .andExpect(redirectedUrl("/manager/quizzes/" + quizId + "/questions"))
                .andExpect(flash().attribute("quizSuccess", "Đã thêm câu hỏi vào quiz."));
        assertEquals(2, totalQuestionsOf(quizId));

        mockMvc.perform(updateQuiz(quizId, "Quiz đủ câu", "PUBLISHED"))
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attribute("quizSuccess", "Đã cập nhật quiz."));

        assertEquals("PUBLISHED", statusOf(quizId));
    }

    @Test
    void questionsPageListsLinkedQuestions() throws Exception {
        long quizId = saveQuiz("Quiz xem câu", "DRAFT", 5);
        Question q = saveQuestion("ACTIVE");
        mockMvc.perform(addQuestions(quizId, q));

        mockMvc.perform(get("/manager/quizzes/{id}/questions", quizId))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/questions"))
                .andExpect(model().attribute("quizQuestions", hasSize(1)))
                .andExpect(content().string(not(containsString("Độ khó"))));
    }

    @Test
    void addingSameQuestionTwiceLinksItOnce() throws Exception {
        long quizId = saveQuiz("Quiz trùng câu", "DRAFT", 5);
        Question q = saveQuestion("ACTIVE");

        mockMvc.perform(addQuestions(quizId, q));
        mockMvc.perform(addQuestions(quizId, q))
                .andExpect(flash().attribute("quizSuccess", "Đã thêm câu hỏi vào quiz."));

        assertEquals(1L, linkCount(quizId));
    }

    @Test
    void addingWithoutSelectingQuestionsShowsError() throws Exception {
        long quizId = saveQuiz("Quiz chưa chọn", "DRAFT", 5);

        mockMvc.perform(post("/manager/quizzes/{id}/questions/add", quizId).with(csrf()))
                .andExpect(flash().attribute("quizError", "Vui lòng chọn ít nhất một câu hỏi."));
    }

    @Test
    void addingArchivedQuestionIsRejected() throws Exception {
        long quizId = saveQuiz("Quiz câu lưu trữ", "DRAFT", 5);
        Question archived = saveQuestion("ARCHIVED");

        mockMvc.perform(addQuestions(quizId, archived))
                .andExpect(flash().attribute("quizError", "Câu hỏi #" + archived.getId() + " chưa được kích hoạt (ACTIVE)."));

        assertEquals(0L, linkCount(quizId));
    }

    @Test
    void removeUnlinksQuestion() throws Exception {
        long quizId = saveQuiz("Quiz gỡ câu", "DRAFT", 5);
        Question q = saveQuestion("ACTIVE");
        mockMvc.perform(addQuestions(quizId, q));

        mockMvc.perform(post("/manager/quizzes/{id}/questions/{q}/remove", quizId, q.getId()).with(csrf()))
                .andExpect(flash().attribute("quizSuccess", "Đã gỡ câu hỏi khỏi quiz."));

        assertEquals(0L, linkCount(quizId));
        assertEquals(0, totalQuestionsOf(quizId));
    }

    @Test
    void importTemplateIsDownloadedAsExcelFile() throws Exception {
        mockMvc.perform(get("/manager/quizzes/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }

    @Test
    void importCreatesActiveQuestionsAndLinksOnlyChoiceQuestions() throws Exception {
        long quizId = saveQuiz("Quiz import", "DRAFT", 5);
        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer", "status"),
                List.of(
                        List.of("", "MULTI_CHOICE", MARK + "Câu import", "A", "B", "A", "DRAFT"),
                        List.of("1", "PASSAGE", MARK + "Đoạn import", "", "", "", ""),
                        List.of("1", "MULTI_CHOICE", MARK + "Câu con import", "A", "B", "B", "")));

        mockMvc.perform(multipart("/manager/quizzes/{id}/import", quizId).file(file).with(csrf()))
                .andExpect(flash().attribute("quizSuccess", "Đã import và gắn 1 câu hỏi vào quiz."));

        assertEquals(1L, linkCount(quizId));
        assertEquals(3L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE content LIKE ? AND status = 'ACTIVE'", Long.class, MARK + "%"));
    }

    @Test
    void importEmptyFileShowsError() throws Exception {
        long quizId = saveQuiz("Quiz file rỗng", "DRAFT", 5);
        var empty = new org.springframework.mock.web.MockMultipartFile("file", "questions.xlsx", null, new byte[0]);

        mockMvc.perform(multipart("/manager/quizzes/{id}/import", quizId).file(empty).with(csrf()))
                .andExpect(flash().attribute("importErrors", List.of("Vui lòng chọn file Excel (.xlsx).")));
    }

    // ── helpers ──

    private long saveQuiz(String title, String status, int totalQuestions) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO exercises (lesson_id, exercise_title, total_questions, passing_score, status) VALUES (?, ?, ?, 60, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, LESSON_ID);
            ps.setString(2, MARK + title);
            ps.setInt(3, totalQuestions);
            ps.setString(4, status);
            return ps;
        }, keys);
        return ((Number) keys.getKeys().get("EXERCISE_ID")).longValue();
    }

    private Question saveQuestion(String status) {
        return questionRepository.saveAndFlush(Question.builder()
                .content(MARK + "Câu hỏi " + System.nanoTime())
                .questionType("MULTI_CHOICE")
                .options("[{\"key\":\"A\",\"text\":\"A\"},{\"key\":\"B\",\"text\":\"B\"}]")
                .answerKey("[\"A\"]")
                .status(status)
                .build());
    }

    private org.springframework.test.web.servlet.RequestBuilder addQuestions(long quizId, Question... questions) {
        var request = post("/manager/quizzes/{id}/questions/add", quizId).with(csrf());
        for (Question q : questions) request.param("questionIds", String.valueOf(q.getId()));
        return request;
    }

    private org.springframework.test.web.servlet.RequestBuilder updateQuiz(long quizId, String title, String status) {
        return post("/manager/quizzes/{id}", quizId).with(csrf())
                .param("lessonId", String.valueOf(LESSON_ID))
                .param("title", MARK + title)
                .param("passingScore", "60")
                .param("status", status);
    }

    private long countQuizzes(String title) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exercises WHERE exercise_title = ?", Long.class, MARK + title);
    }

    private long linkCount(long quizId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exercise_questions WHERE exercise_id = ?", Long.class, quizId);
    }

    private int totalQuestionsOf(long quizId) {
        return jdbc.queryForObject("SELECT total_questions FROM exercises WHERE exercise_id = ?", Integer.class, quizId);
    }

    private String statusOf(long quizId) {
        return jdbc.queryForObject("SELECT status FROM exercises WHERE exercise_id = ?", String.class, quizId);
    }
}
