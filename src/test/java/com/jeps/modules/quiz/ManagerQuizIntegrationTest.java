package com.jeps.modules.quiz;

import com.jeps.modules.question.entity.Question;
import com.jeps.modules.question.repository.QuestionRepository;
import com.jeps.modules.quiz.dto.ExerciseQuestionRow;
import com.jeps.modules.quiz.dto.LessonOption;
import com.jeps.modules.quiz.service.ManagerQuizService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
        "ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_id BIGINT"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ManagerQuizIntegrationTest {

    private static final String MARK = "IT-QUIZ ";
    private static final long LESSON_ID = 900_001L;
    private static final long SEED_COURSE_ID = 900_100L;

    @Autowired private MockMvc mockMvc;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void seedLesson() {
        // Every lesson belongs to a course (lessons.course_id is NOT NULL).
        saveCourse(SEED_COURSE_ID, "Khoá mẫu", "N5");
        jdbc.update("INSERT INTO lessons (lesson_id, course_id, title, section_name, order_index) VALUES (?, ?, ?, ?, 1)",
                LESSON_ID, SEED_COURSE_ID, MARK + "Bài 1", MARK + "Chương 1");
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
                .andExpect(model().attribute("quizzes", org.hamcrest.Matchers.hasProperty("content", hasSize(1))));
        mockMvc.perform(get("/manager/quizzes").param("level", "N1").param("section", MARK + "Chương 1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quizzes", org.hamcrest.Matchers.hasProperty("content", hasSize(0))));
    }

    @Test
    void listIsPaginatedAndKeepsFiltersInPageLinks() throws Exception {
        // exercises.lesson_id is unique, so each quiz gets its own lesson.
        for (int n = 1; n <= ManagerQuizService.PAGE_SIZE + 1; n++) {
            long lessonId = 910_000L + n;
            saveLesson(lessonId, SEED_COURSE_ID, MARK + "Chương trang");
            jdbc.update("INSERT INTO exercises (lesson_id, exercise_title, total_questions, passing_score, status) VALUES (?, ?, 0, 60, 'DRAFT')",
                    lessonId, MARK + "Quiz trang " + n);
        }

        mockMvc.perform(get("/manager/quizzes").param("search", MARK + "Quiz trang"))
                .andExpect(model().attribute("quizzes", org.hamcrest.Matchers.hasProperty("content",
                        hasSize(ManagerQuizService.PAGE_SIZE))))
                .andExpect(model().attribute("pageUrl", containsString("search=")))
                .andExpect(content().string(containsString("page=1")));
        mockMvc.perform(get("/manager/quizzes").param("search", MARK + "Quiz trang").param("page", "1"))
                .andExpect(model().attribute("quizzes", org.hamcrest.Matchers.hasProperty("content", hasSize(1))));
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
    @SuppressWarnings("unchecked")
    void lessonPickerAndSectionFilterHandleMissingSectionAndLevel() throws Exception {
        saveCourse(900_101L, "Khoá N5", "N5");
        saveCourse(900_102L, "Khoá chưa gán cấp", "");
        saveLesson(900_011L, 900_101L, "Chương A");
        saveLesson(900_012L, 900_101L, null);
        saveLesson(900_013L, 900_101L, "   ");
        saveLesson(900_014L, 900_102L, null);

        var levelSections = (Map<String, List<String>>) mockMvc.perform(get("/manager/quizzes"))
                .andReturn().getModelAndView().getModel().get("levelSections");
        assertTrue(levelSections.get("N5").contains("Chương A"));
        assertTrue(levelSections.get("N5").stream().noneMatch(s -> s == null || s.isBlank()));

        var lessonGroups = (Map<String, List<LessonOption>>) mockMvc.perform(get("/manager/quizzes/new"))
                .andReturn().getModelAndView().getModel().get("lessonGroups");
        assertEquals(List.of(900_011L), lessonIds(lessonGroups, "N5 · Chương A"));
        assertEquals(List.of(900_012L, 900_013L), lessonIds(lessonGroups, "N5 · " + MARK + "Khoá N5"));
        assertEquals(List.of(900_014L), lessonIds(lessonGroups, MARK + "Khoá chưa gán cấp"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void availableQuestionsArePaginated() throws Exception {
        long quizId = saveQuiz("Quiz phân trang", "DRAFT", 0);
        for (int i = 0; i < 21; i++) saveQuestion("ACTIVE", "N1", "LISTENING");

        var result = mockMvc.perform(get("/manager/quizzes/{id}/questions", quizId)
                        .param("jlptLevel", "N1").param("skillType", "LISTENING"))
                .andExpect(content().string(containsString("skillType=LISTENING")))
                .andExpect(content().string(containsString("Đến trang")))
                .andReturn();
        var first = (org.springframework.data.domain.Page<ExerciseQuestionRow>) result.getModelAndView().getModel().get("availableQuestions");
        assertEquals(20, first.getContent().size());
        assertTrue(first.getTotalElements() >= 21);

        var second = (org.springframework.data.domain.Page<ExerciseQuestionRow>) mockMvc
                .perform(get("/manager/quizzes/{id}/questions", quizId)
                        .param("jlptLevel", "N1").param("skillType", "LISTENING").param("page", "1"))
                .andReturn().getModelAndView().getModel().get("availableQuestions");
        assertEquals(1, second.getNumber());
        assertTrue(!second.getContent().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void questionsPageListsActiveQuestionsNotYetInQuiz() throws Exception {
        long quizId = saveQuiz("Quiz câu khả dụng", "DRAFT", 0);
        Question linked = saveQuestion("ACTIVE", "N5", "VOCABULARY");
        Question available = saveQuestion("ACTIVE", "N5", "VOCABULARY");
        mockMvc.perform(addQuestions(quizId, linked));

        var rows = ((org.springframework.data.domain.Page<ExerciseQuestionRow>) mockMvc.perform(get("/manager/quizzes/{id}/questions", quizId)
                        .param("jlptLevel", "N5").param("skillType", "VOCABULARY"))
                .andReturn().getModelAndView().getModel().get("availableQuestions")).getContent();

        List<Long> ids = rows.stream().map(ExerciseQuestionRow::questionId).toList();
        assertTrue(ids.contains(available.getId()));
        assertTrue(!ids.contains(linked.getId()));
        ExerciseQuestionRow row = rows.stream().filter(r -> r.questionId() == available.getId()).findFirst().orElseThrow();
        assertEquals("N5", row.jlptLevel());
        assertEquals("VOCABULARY", row.skillType());
    }

    @Test
    void publishingQuizWithArchivedQuestionIsRejected() throws Exception {
        long quizId = saveQuiz("Quiz có câu đã lưu trữ", "DRAFT", 0);
        Question question = saveQuestion("ACTIVE");
        mockMvc.perform(addQuestions(quizId, question));
        jdbc.update("UPDATE questions SET status = 'ARCHIVED' WHERE id = ?", question.getId());

        mockMvc.perform(updateQuiz(quizId, "Quiz có câu đã lưu trữ", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quizError", containsString("quiz còn câu hỏi không ở trạng thái ACTIVE (#" + question.getId() + ")")));

        assertEquals("DRAFT", statusOf(quizId));
    }

    @Test
    void secondQuizForSameLessonIsRejected() throws Exception {
        saveQuiz("Quiz có sẵn", "DRAFT", 0);

        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", String.valueOf(LESSON_ID))
                        .param("title", MARK + "Quiz thứ hai")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attribute("quizError", "Bài học này đã có quiz. Mỗi bài học chỉ được gắn một quiz."));

        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM exercises WHERE lesson_id = ?", Long.class, LESSON_ID));
    }

    @Test
    void editingQuizWithoutChangingLessonIsAllowed() throws Exception {
        long quizId = saveQuiz("Quiz sửa tên", "DRAFT", 0);

        mockMvc.perform(updateQuiz(quizId, "Quiz đã đổi tên", "DRAFT"))
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attribute("quizSuccess", "Đã cập nhật quiz."));
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
    void linkedQuestionsGetIncreasingOrderAcrossBatches() throws Exception {
        long quizId = saveQuiz("Quiz thứ tự", "DRAFT", 0);
        Question q1 = saveQuestion("ACTIVE");
        Question q2 = saveQuestion("ACTIVE");
        Question q3 = saveQuestion("ACTIVE");

        mockMvc.perform(addQuestions(quizId, q3, q1));
        mockMvc.perform(addQuestions(quizId, q2));

        assertEquals(List.of(q3.getId(), q1.getId(), q2.getId()), jdbc.queryForList(
                "SELECT question_id FROM exercise_questions WHERE exercise_id = ? ORDER BY question_order",
                Long.class, quizId));
        assertEquals(List.of(1, 2, 3), jdbc.queryForList(
                "SELECT question_order FROM exercise_questions WHERE exercise_id = ? ORDER BY question_order",
                Integer.class, quizId));
        @SuppressWarnings("unchecked")
        List<ExerciseQuestionRow> rows = (List<ExerciseQuestionRow>) mockMvc
                .perform(get("/manager/quizzes/{id}/questions", quizId))
                .andReturn().getModelAndView().getModel().get("quizQuestions");
        assertEquals(List.of(q3.getId(), q1.getId(), q2.getId()), rows.stream().map(ExerciseQuestionRow::questionId).toList());
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
    void addingPassageIsRejected() throws Exception {
        long quizId = saveQuiz("Quiz bài đọc", "DRAFT", 5);
        Question passage = questionRepository.saveAndFlush(Question.builder()
                .content(MARK + "Đoạn văn").questionType("PASSAGE").jlptLevel("N5").skillType("READING").status("ACTIVE").build());

        mockMvc.perform(get("/manager/quizzes/{id}/questions", quizId).param("skillType", "READING"))
                .andExpect(content().string(not(containsString(MARK + "Đoạn văn"))));
        mockMvc.perform(addQuestions(quizId, passage))
                .andExpect(flash().attribute("quizError", "Câu hỏi #" + passage.getId()
                        + " là bài đọc/nghe (PASSAGE), quiz chỉ gắn được câu trắc nghiệm."));

        assertEquals(0L, linkCount(quizId));
    }

    @Test
    void editPageOfQuizWithoutLessonLeavesLessonUnselected() throws Exception {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO exercises (lesson_id, exercise_title, total_questions, passing_score, status) VALUES (NULL, ?, 0, 60, 'DRAFT')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, MARK + "Quiz không bài học");
            return ps;
        }, keys);
        long quizId = ((Number) keys.getKeys().get("EXERCISE_ID")).longValue();

        var result = mockMvc.perform(get("/manager/quizzes/{id}/edit", quizId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("-- Chọn bài học --")))
                .andReturn();

        var form = (com.jeps.modules.quiz.dto.ManagerQuizForm) result.getModelAndView().getModel().get("quizForm");
        assertEquals(null, form.getLessonId());
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

    // ── Auto generate ──

    @Test
    void generatePicksRequestedCountPerSkillAtChosenLevel() throws Exception {
        long quizId = saveQuiz("Quiz tự động", "DRAFT", 0);
        for (int i = 0; i < 3; i++) saveQuestion("ACTIVE", "N5", "VOCABULARY");
        for (int i = 0; i < 2; i++) saveQuestion("ACTIVE", "N5", "GRAMMAR");
        Question otherLevel = saveQuestion("ACTIVE", "N4", "VOCABULARY");
        Question draft = saveQuestion("DRAFT", "N5", "VOCABULARY");

        mockMvc.perform(generate(quizId, "N5").param("counts[VOCABULARY]", "2").param("counts[GRAMMAR]", "2"))
                .andExpect(redirectedUrl("/manager/quizzes/" + quizId + "/questions"))
                .andExpect(flash().attribute("quizSuccess", "Đã tự động chọn 4 câu hỏi từ ngân hàng câu hỏi."));

        assertEquals(4, linkCount(quizId));
        assertEquals(4, totalQuestionsOf(quizId));
        assertEquals(2L, jdbc.queryForObject("""
                SELECT COUNT(*) FROM exercise_questions eq JOIN questions q ON q.id = eq.question_id
                WHERE eq.exercise_id = ? AND q.skill_type = 'VOCABULARY' AND q.jlpt_level = 'N5' AND q.status = 'ACTIVE'
                """, Long.class, quizId));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM exercise_questions WHERE question_id IN (?, ?)",
                Long.class, otherLevel.getId(), draft.getId()));
    }

    @Test
    void generateWithNotEnoughQuestionsLinksNothing() throws Exception {
        long quizId = saveQuiz("Quiz thiếu câu", "DRAFT", 0);
        saveQuestion("ACTIVE", "N5", "VOCABULARY");
        saveQuestion("ACTIVE", "N5", "GRAMMAR");

        mockMvc.perform(generate(quizId, "N5").param("counts[VOCABULARY]", "1").param("counts[GRAMMAR]", "5"))
                .andExpect(flash().attribute("quizError", containsString("Ngữ pháp cần 5 câu")))
                .andExpect(flash().attribute("quizError", containsString("chỉ còn 1 câu")));

        assertEquals(0, linkCount(quizId));
    }

    @Test
    void questionsPageShowsGenerateFormWithEverySkill() throws Exception {
        long quizId = saveQuiz("Quiz form tự động", "DRAFT", 0);

        mockMvc.perform(get("/manager/quizzes/{id}/questions", quizId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tự động chọn câu từ ngân hàng")))
                .andExpect(content().string(containsString("name=\"counts[VOCABULARY]\"")))
                .andExpect(content().string(containsString("name=\"counts[LISTENING]\"")));
    }

    @Test
    void generateWithoutAnyCountShowsError() throws Exception {
        long quizId = saveQuiz("Quiz rỗng", "DRAFT", 0);

        mockMvc.perform(generate(quizId, "N5").param("counts[VOCABULARY]", "0"))
                .andExpect(flash().attribute("quizError", "Hãy nhập số câu cho ít nhất một kỹ năng."));
    }

    @Test
    void bulkStatusPublishesQuizzesThatAreReady() throws Exception {
        long quizId = saveQuiz("Quiz publish hàng loạt", "DRAFT", 0);
        mockMvc.perform(addQuestions(quizId, saveQuestion("ACTIVE")));

        mockMvc.perform(post("/manager/quizzes/bulk-status").with(csrf())
                        .param("ids", String.valueOf(quizId)).param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attribute("quizSuccess", "Đã cập nhật trạng thái 1 quiz sang PUBLISHED."));

        assertEquals("PUBLISHED", statusOf(quizId));
    }

    @Test
    void bulkStatusRejectsPublishingQuizWithoutQuestions() throws Exception {
        long empty = saveQuiz("Quiz rỗng hàng loạt", "DRAFT", 0);

        mockMvc.perform(post("/manager/quizzes/bulk-status").with(csrf())
                        .param("ids", String.valueOf(empty)).param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attribute("quizError", "Quiz #" + empty + " chưa có câu hỏi. Không thể chuyển sang PUBLISHED."));

        assertEquals("DRAFT", statusOf(empty));
    }

    // ── helpers ──

    private void saveCourse(long id, String title, String jlptLevel) {
        jdbc.update("INSERT INTO courses (course_id, title, level, price) VALUES (?, ?, ?, 0)",
                id, MARK + title, jlptLevel);
    }

    private void saveLesson(long id, Long courseId, String section) {
        jdbc.update("INSERT INTO lessons (lesson_id, course_id, title, section_name, order_index) VALUES (?, ?, ?, ?, 1)",
                id, courseId, MARK + "Bài " + id, section);
    }

    private static List<Long> lessonIds(Map<String, List<LessonOption>> groups, String group) {
        return groups.getOrDefault(group, List.of()).stream().map(LessonOption::id).sorted().toList();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder generate(long quizId, String level) {
        return post("/manager/quizzes/{id}/questions/generate", quizId).with(csrf()).param("jlptLevel", level);
    }

    private Question saveQuestion(String status, String level, String skillType) {
        return questionRepository.saveAndFlush(Question.builder()
                .content(MARK + "Câu hỏi " + System.nanoTime())
                .questionType("MULTI_CHOICE")
                .jlptLevel(level)
                .skillType(skillType)
                .options("[{\"key\":\"A\",\"text\":\"A\"},{\"key\":\"B\",\"text\":\"B\"}]")
                .answerKey("[\"A\"]")
                .status(status)
                .build());
    }

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
