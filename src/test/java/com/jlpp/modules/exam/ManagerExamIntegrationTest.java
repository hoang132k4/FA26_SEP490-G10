package com.jlpp.modules.exam;

import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.jlpp.support.QuestionExcel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
class ManagerExamIntegrationTest {

    private static final String MARK = "IT-EXAM ";

    @Autowired private MockMvc mockMvc;
    @Autowired private ExamRepository examRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    // ── Access control ──

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = "LEARNER")
    void learnerIsForbiddenFromExamManagement() throws Exception {
        mockMvc.perform(get("/manager/exams")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/manager/exams").with(anonymous()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login*"));
    }

    // ── List ──

    @Test
    void listShowsExamsAndCountsPerType() throws Exception {
        Exam mock = saveExam("Đề thi thử N5", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        saveExam("Đề đầu vào", "PLACEMENT_TEST", "N4", "DRAFT", 10, 5, 5);

        mockMvc.perform(get("/manager/exams").param("search", MARK + "Đề thi thử").param("jlptLevel", "N5"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/list"))
                .andExpect(model().attribute("exams", hasSize(1)))
                .andExpect(model().attribute("exams", org.hamcrest.Matchers.contains(
                        org.hamcrest.Matchers.hasProperty("examId", org.hamcrest.Matchers.is(mock.getExamId())))));
    }

    // ── Create / update ──

    @Test
    void createPageIsPrefilledWithN5Structure() throws Exception {
        mockMvc.perform(get("/manager/exams/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attribute("examForm", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.hasProperty("jlptLevel", org.hamcrest.Matchers.is("N5")),
                        org.hamcrest.Matchers.hasProperty("languageQuestions", org.hamcrest.Matchers.is(38)),
                        org.hamcrest.Matchers.hasProperty("readingQuestions", org.hamcrest.Matchers.is(5)),
                        org.hamcrest.Matchers.hasProperty("listeningQuestions", org.hamcrest.Matchers.is(24)))))
                .andExpect(content().string(containsString("\"durationMinutes\":90")));
    }

    @Test
    void createDraftExamStoresFixedJlptSettingsAndSectionCounts() throws Exception {
        mockMvc.perform(withSections(post("/manager/exams").with(csrf()), 30, 10, 20)
                        .param("examTitle", MARK + "Đề N3 mới")
                        .param("examType", "JLPT_MOCK")
                        .param("jlptLevel", "N3")
                        .param("status", "DRAFT"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examSuccess", "Đã tạo đề thi."));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT duration_minutes, passing_score, total_questions, language_questions,
                       reading_questions, listening_questions, status
                FROM exams WHERE exam_title = ?""", MARK + "Đề N3 mới");
        assertEquals(140, row.get("DURATION_MINUTES"));
        assertEquals(95, row.get("PASSING_SCORE"));
        assertEquals(60, row.get("TOTAL_QUESTIONS"));
        assertEquals(30, row.get("LANGUAGE_QUESTIONS"));
        assertEquals(10, row.get("READING_QUESTIONS"));
        assertEquals(20, row.get("LISTENING_QUESTIONS"));
        assertEquals("DRAFT", row.get("STATUS"));
    }

    @Test
    void createWithBlankTitleRedisplaysFormWithValidationError() throws Exception {
        long before = examRepository.count();

        mockMvc.perform(withSections(post("/manager/exams").with(csrf()), 10, 5, 5)
                        .param("examTitle", "  ")
                        .param("examType", "JLPT_MOCK")
                        .param("jlptLevel", "N5")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attributeHasFieldErrors("examForm", "examTitle"));

        assertEquals(before, examRepository.count());
    }

    @Test
    void createWithMissingSectionCountShowsFieldError() throws Exception {
        mockMvc.perform(post("/manager/exams").with(csrf())
                        .param("examTitle", MARK + "Đề thiếu số câu")
                        .param("examType", "JLPT_MOCK")
                        .param("jlptLevel", "N5")
                        .param("languageQuestions", "10")
                        .param("readingQuestions", "5")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("examForm", "listeningQuestions"));
    }

    @Test
    void createPublishedExamIsRejected() throws Exception {
        long before = examRepository.count();

        mockMvc.perform(withSections(post("/manager/exams").with(csrf()), 10, 5, 5)
                        .param("examTitle", MARK + "Đề publish ngay")
                        .param("examType", "JLPT_MOCK")
                        .param("jlptLevel", "N5")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("examError", containsString("Không thể tạo đề thi với trạng thái PUBLISHED")));

        assertEquals(before, examRepository.count());
    }

    @Test
    void editPageOfMissingExamRedirectsWithError() throws Exception {
        mockMvc.perform(get("/manager/exams/{id}/edit", 999_999L))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examError", "Không tìm thấy đề thi."));
    }

    @Test
    void publishingExamWithIncompleteSectionsKeepsItDraft() throws Exception {
        Exam exam = saveExam("Đề thiếu câu", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);

        mockMvc.perform(updateExam(exam, "PUBLISHED", 10, 5, 5))
                .andExpect(status().isOk())
                .andExpect(model().attribute("examError", containsString("số câu đã gán chưa khớp cấu trúc đề")))
                .andExpect(model().attribute("examError", containsString("Nghe hiểu 0/5")));

        assertEquals("DRAFT", statusOf(exam));
    }

    @Test
    void examCanBePublishedOnceEverySectionIsFilled() throws Exception {
        Exam exam = saveExam("Đề đủ câu", "JLPT_MOCK", "N5", "DRAFT", 2, 1, 0);
        Question q1 = saveQuestion("ACTIVE", "VOCABULARY");
        Question q2 = saveQuestion("ACTIVE", "GRAMMAR");
        Question q3 = saveQuestion("ACTIVE", "READING");

        mockMvc.perform(addQuestions(exam, q1, q2, q3))
                .andExpect(flash().attribute("examSuccess", "Đã thêm câu hỏi vào đề thi."));
        mockMvc.perform(updateExam(exam, "PUBLISHED", 2, 1, 0))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examSuccess", "Đã cập nhật đề thi."));

        assertEquals("PUBLISHED", statusOf(exam));
    }

    @Test
    void editPageShowsProgressOfEachSection() throws Exception {
        Exam exam = saveExam("Đề xem tiến độ", "JLPT_MOCK", "N5", "DRAFT", 2, 1, 1);
        mockMvc.perform(addQuestions(exam, saveQuestion("ACTIVE", "READING")));

        mockMvc.perform(get("/manager/exams/{id}/edit", exam.getExamId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sectionProgress", hasSize(3)))
                .andExpect(content().string(containsString("Đọc hiểu: 1/1")));
    }

    // ── Exam questions ──

    @Test
    void addedQuestionsAppearOnQuestionsPageInOrder() throws Exception {
        Exam exam = saveExam("Đề gắn câu", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        Question q1 = saveQuestion("ACTIVE", "READING");
        Question q2 = saveQuestion("ACTIVE", "READING");

        mockMvc.perform(addQuestions(exam, q1, q2))
                .andExpect(redirectedUrl("/manager/exams/" + exam.getExamId() + "/questions"));

        List<Map<String, Object>> links = links(exam);
        assertEquals(2, links.size());
        assertEquals(q1.getId(), ((Number) links.get(0).get("QUESTION_ID")).longValue());
        assertEquals(1, links.get(0).get("ORDER_INDEX"));
        assertEquals(2, links.get(1).get("ORDER_INDEX"));
        assertEquals("READING", links.get(0).get("SECTION"));

        mockMvc.perform(get("/manager/exams/{id}/questions", exam.getExamId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/questions"))
                .andExpect(model().attribute("examQuestions", hasSize(2)))
                .andExpect(model().attribute("sectionProgress", hasSize(3)))
                .andExpect(content().string(containsString("data-qid=\"" + q1.getId() + "\"")))
                .andExpect(content().string(not(containsString("Độ khó"))));
    }

    @Test
    void addingWithoutSelectingQuestionsShowsErrorInsteadOfCrashing() throws Exception {
        Exam exam = saveExam("Đề chưa chọn câu", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);

        mockMvc.perform(post("/manager/exams/{id}/questions/add", exam.getExamId()).with(csrf()))
                .andExpect(redirectedUrl("/manager/exams/" + exam.getExamId() + "/questions"))
                .andExpect(flash().attribute("examError", "Vui lòng chọn ít nhất một câu hỏi."));
    }

    @Test
    void addingMoreQuestionsThanSectionNeedsIsRejected() throws Exception {
        Exam exam = saveExam("Đề vượt phần", "JLPT_MOCK", "N5", "DRAFT", 10, 1, 5);

        mockMvc.perform(addQuestions(exam, saveQuestion("ACTIVE", "READING"), saveQuestion("ACTIVE", "READING")))
                .andExpect(flash().attribute("examError", containsString("Đọc hiểu chỉ cần 1 câu")));

        assertEquals(0, links(exam).size());
    }

    @Test
    void addingDraftQuestionIsRejected() throws Exception {
        Exam exam = saveExam("Đề câu nháp", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        Question draft = saveQuestion("DRAFT");

        mockMvc.perform(addQuestions(exam, draft))
                .andExpect(flash().attribute("examError", "Câu hỏi #" + draft.getId() + " chưa được kích hoạt (ACTIVE)."));

        assertEquals(0, links(exam).size());
    }

    @Test
    void reorderSavesDraggedOrderOfSection() throws Exception {
        Exam exam = saveExam("Đề đổi thứ tự", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        Question q1 = saveQuestion("ACTIVE");
        Question q2 = saveQuestion("ACTIVE");
        Question q3 = saveQuestion("ACTIVE");
        mockMvc.perform(addQuestions(exam, q1, q2, q3));

        mockMvc.perform(post("/manager/exams/{e}/questions/reorder", exam.getExamId()).with(csrf())
                        .param("section", "LANGUAGE")
                        .param("questionIds", q3.getId().toString(), q1.getId().toString(), q2.getId().toString()))
                .andExpect(status().isNoContent());

        List<Map<String, Object>> links = links(exam);
        assertEquals(List.of(q3.getId(), q1.getId(), q2.getId()),
                links.stream().map(row -> ((Number) row.get("QUESTION_ID")).longValue()).toList());
        assertEquals(List.of(1, 2, 3),
                links.stream().map(row -> ((Number) row.get("ORDER_INDEX")).intValue()).toList());
    }

    @Test
    void reorderWithStaleListReturnsBadRequestAndKeepsOrder() throws Exception {
        Exam exam = saveExam("Đề đổi thứ tự cũ", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        Question q1 = saveQuestion("ACTIVE");
        Question q2 = saveQuestion("ACTIVE");
        mockMvc.perform(addQuestions(exam, q1, q2));

        mockMvc.perform(post("/manager/exams/{e}/questions/reorder", exam.getExamId()).with(csrf())
                        .param("section", "LANGUAGE")
                        .param("questionIds", q2.getId().toString()))
                .andExpect(status().isBadRequest());

        assertEquals(q1.getId(), ((Number) links(exam).get(0).get("QUESTION_ID")).longValue());
    }

    @Test
    void removeUnlinksQuestionButKeepsItInBank() throws Exception {
        Exam exam = saveExam("Đề gỡ câu", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        Question q = saveQuestion("ACTIVE");
        mockMvc.perform(addQuestions(exam, q));

        mockMvc.perform(post("/manager/exams/{e}/questions/{q}/remove", exam.getExamId(), q.getId()).with(csrf()))
                .andExpect(flash().attribute("examSuccess", "Đã gỡ câu hỏi khỏi đề thi."));

        assertEquals(0, links(exam).size());
        assertEquals(true, questionRepository.existsById(q.getId()));
    }

    @Test
    void removingQuestionNotInExamShowsError() throws Exception {
        Exam exam = saveExam("Đề gỡ sai", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);

        mockMvc.perform(post("/manager/exams/{e}/questions/{q}/remove", exam.getExamId(), 999_999L).with(csrf()))
                .andExpect(flash().attribute("examError", "Câu hỏi này không nằm trong đề thi."));
    }

    // ── Import ──

    @Test
    void importTemplateIsDownloadedAsExcelFile() throws Exception {
        mockMvc.perform(get("/manager/exams/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("mau-import-de-thi.xlsx")));
    }

    @Test
    void importCreatesActiveQuestionsAndLinksThemToExam() throws Exception {
        Exam exam = saveExam("Đề import", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        var file = QuestionExcel.xlsx(
                List.of("question_type", "content", "skill_type", "option_a", "option_b", "answer", "status", "section"),
                List.of(
                        List.of("MULTI_CHOICE", MARK + "Câu import 1", "VOCABULARY", "あ", "い", "A", "DRAFT", ""),
                        List.of("MULTI_CHOICE", MARK + "Câu import 2", "READING", "う", "え", "B", "", "")));

        mockMvc.perform(multipart("/manager/exams/{id}/import", exam.getExamId()).file(file).with(csrf()))
                .andExpect(flash().attribute("examSuccess", "Đã import và gắn 2 câu hỏi vào đề thi."));

        entityManager.flush();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT q.status, eq.section FROM exam_questions eq
                JOIN questions q ON q.id = eq.question_id
                WHERE eq.exam_id = ? ORDER BY q.content""", exam.getExamId());
        assertEquals(2, rows.size());
        assertEquals("ACTIVE", rows.get(0).get("STATUS"));
        assertEquals("LANGUAGE", rows.get(0).get("SECTION"));
        assertEquals("READING", rows.get(1).get("SECTION"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void importWithInvalidRowSavesNothing() throws Exception {
        Exam exam = examRepository.save(new Exam(null, MARK + "Đề import lỗi", "JLPT_MOCK", "N5", 90, 67, 80, "DRAFT", null));
        try {
            var file = QuestionExcel.xlsx(
                    List.of("content", "option_a", "option_b", "answer", "section"),
                    List.of(
                            List.of(MARK + "Câu hợp lệ", "あ", "い", "A", ""),
                            List.of(MARK + "Câu sai phần", "う", "え", "A", "WRITING")));

            mockMvc.perform(multipart("/manager/exams/{id}/import", exam.getExamId()).file(file).with(csrf()))
                    .andExpect(flash().attribute("examError", containsString("Import thất bại")))
                    .andExpect(flash().attribute("importErrors",
                            List.of("Dòng 3: section 'WRITING' không hợp lệ (LANGUAGE, READING, LISTENING).")));

            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM questions WHERE content LIKE ?", Long.class, MARK + "Câu%"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM exam_questions WHERE exam_id = ?", Long.class, exam.getExamId()));
        } finally {
            jdbc.update("DELETE FROM exam_questions WHERE exam_id = ?", exam.getExamId());
            jdbc.update("DELETE FROM questions WHERE content LIKE ?", MARK + "%");
            jdbc.update("DELETE FROM exams WHERE exam_id = ?", exam.getExamId());
        }
    }

    @Test
    void importWithWrongFileTypeShowsError() throws Exception {
        Exam exam = saveExam("Đề import csv", "JLPT_MOCK", "N5", "DRAFT", 10, 5, 5);
        var csv = new org.springframework.mock.web.MockMultipartFile("file", "questions.csv", "text/csv", "a,b".getBytes());

        mockMvc.perform(multipart("/manager/exams/{id}/import", exam.getExamId()).file(csv).with(csrf()))
                .andExpect(flash().attribute("importErrors", List.of("Chỉ hỗ trợ file .xlsx. Hãy tải file mẫu và điền dữ liệu.")));
    }

    // ── helpers ──

    private Exam saveExam(String title, String type, String level, String status, int language, int reading, int listening) {
        Exam exam = new Exam(null, MARK + title, type, level, 90, language + reading + listening, 80, status, null);
        exam.setLanguageQuestions(language);
        exam.setReadingQuestions(reading);
        exam.setListeningQuestions(listening);
        return examRepository.save(exam);
    }

    private Question saveQuestion(String status) {
        return saveQuestion(status, "VOCABULARY");
    }

    private Question saveQuestion(String status, String skillType) {
        Question q = Question.builder()
                .content(MARK + "Câu hỏi " + System.nanoTime())
                .questionType("MULTI_CHOICE")
                .jlptLevel("N5")
                .skillType(skillType)
                .options("[{\"key\":\"A\",\"text\":\"あ\"},{\"key\":\"B\",\"text\":\"い\"}]")
                .answerKey("[\"A\"]")
                .status(status)
                .build();
        return questionRepository.save(q);
    }

    private org.springframework.test.web.servlet.RequestBuilder addQuestions(Exam exam, Question... questions) {
        var request = post("/manager/exams/{id}/questions/add", exam.getExamId()).with(csrf());
        for (Question q : questions) request.param("questionIds", String.valueOf(q.getId()));
        return request;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withSections(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, int language, int reading, int listening) {
        return request.param("languageQuestions", String.valueOf(language))
                .param("readingQuestions", String.valueOf(reading))
                .param("listeningQuestions", String.valueOf(listening));
    }

    private org.springframework.test.web.servlet.RequestBuilder updateExam(Exam exam, String status, int language, int reading, int listening) {
        return withSections(post("/manager/exams/{id}", exam.getExamId()).with(csrf()), language, reading, listening)
                .param("examTitle", exam.getExamTitle())
                .param("examType", exam.getExamType())
                .param("jlptLevel", exam.getJlptLevel())
                .param("status", status);
    }

    private List<Map<String, Object>> links(Exam exam) {
        entityManager.flush();
        return jdbc.queryForList(
                "SELECT question_id, section, order_index, points FROM exam_questions WHERE exam_id = ? ORDER BY order_index",
                exam.getExamId());
    }

    private String statusOf(Exam exam) {
        entityManager.flush();
        return jdbc.queryForObject("SELECT status FROM exams WHERE exam_id = ?", String.class, exam.getExamId());
    }
}
