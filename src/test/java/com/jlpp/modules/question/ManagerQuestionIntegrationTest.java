package com.jlpp.modules.question;

import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.entity.ExamQuestion;
import com.jlpp.modules.exam.entity.ExamQuestionId;
import com.jlpp.modules.exam.repository.ExamQuestionRepository;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.dto.QuestionListRow;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ManagerQuestionIntegrationTest {

    private static final String MARK = "IT-QUESTION ";

    @Autowired private MockMvc mockMvc;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamQuestionRepository examQuestionRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = "LEARNER")
    void learnerIsForbiddenFromQuestionBank() throws Exception {
        mockMvc.perform(get("/manager/questions")).andExpect(status().isForbidden());
    }

    @Test
    void listShowsQuestionWithExamUsageCount() throws Exception {
        Question q = saveChoice("Câu dùng trong đề", "ACTIVE");
        linkToExam(q, "DRAFT");

        MvcResult result = mockMvc.perform(get("/manager/questions").param("search", MARK + "Câu dùng trong đề"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/list"))
                .andExpect(model().attribute("questions", hasSize(1)))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<QuestionListRow> rows = (List<QuestionListRow>) result.getModelAndView().getModel().get("questions");
        assertEquals(q.getId(), rows.get(0).id());
        assertEquals(1L, rows.get(0).examCount());
        assertEquals(0L, rows.get(0).quizCount());
    }

    @Test
    void createSingleChoiceQuestionAndReopenItForEditing() throws Exception {
        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", MARK + "今日は何曜日ですか。")
                        .param("jlptLevel", "N5")
                        .param("skillType", "VOCABULARY")
                        .param("status", "ACTIVE")
                        .param("optionA", "月曜日")
                        .param("optionB", "火曜日")
                        .param("optionC", "水曜日")
                        .param("answerKeys", "B"))
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attribute("questionSuccess", "Đã thêm câu hỏi."));

        Question saved = findByContent(MARK + "今日は何曜日ですか。");
        MvcResult edit = mockMvc.perform(get("/manager/questions/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andReturn();
        ManagerQuestionForm form = (ManagerQuestionForm) edit.getModelAndView().getModel().get("questionForm");
        assertEquals("月曜日", form.getOptionA());
        assertEquals("火曜日", form.getOptionB());
        assertEquals("水曜日", form.getOptionC());
        assertNull(form.getOptionD());
        assertEquals(List.of("B"), form.getAnswerKeys());
    }

    @Test
    void createWithoutContentRedisplaysFormWithValidationError() throws Exception {
        long before = questionRepository.count();

        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "")
                        .param("status", "DRAFT")
                        .param("optionA", "A").param("optionB", "B").param("answerKeys", "A"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("questionForm", "content"));

        assertEquals(before, questionRepository.count());
    }

    @Test
    void createSingleChoiceWithTwoCorrectAnswersIsRejected() throws Exception {
        long before = questionRepository.count();

        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", MARK + "Câu hai đáp án")
                        .param("status", "DRAFT")
                        .param("optionA", "A").param("optionB", "B")
                        .param("answerKeys", "A", "B"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questionError", "Câu hỏi trắc nghiệm chỉ được chọn 1 đáp án đúng."));

        assertEquals(before, questionRepository.count());
    }

    @Test
    void createChildQuestionUnderPassage() throws Exception {
        Question passage = savePassage("Đoạn văn");

        mockMvc.perform(get("/manager/questions/new").param("parentId", String.valueOf(passage.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", MARK + "Câu con")
                        .param("status", "DRAFT")
                        .param("parentId", String.valueOf(passage.getId()))
                        .param("optionA", "A").param("optionB", "B").param("answerKeys", "A"))
                .andExpect(flash().attribute("questionSuccess", "Đã thêm câu hỏi."));

        assertEquals(passage.getId(), findByContent(MARK + "Câu con").getParentId());
    }

    @Test
    void editPageOfMissingQuestionRedirectsWithError() throws Exception {
        mockMvc.perform(get("/manager/questions/{id}/edit", 999_999L))
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attribute("questionError", "Không tìm thấy câu hỏi."));
    }

    @Test
    void archivingQuestionUsedInPublishedExamIsBlocked() throws Exception {
        Question q = saveChoice("Câu trong đề đang mở", "ACTIVE");
        linkToExam(q, "PUBLISHED");

        mockMvc.perform(update(q, "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questionError", containsString("1 đề thi đang hoạt động")));

        assertEquals("ACTIVE", statusOf(q));
    }

    @Test
    void archivingQuestionUsedOnlyInDraftExamIsAllowed() throws Exception {
        Question q = saveChoice("Câu trong đề nháp", "ACTIVE");
        linkToExam(q, "DRAFT");

        mockMvc.perform(update(q, "ARCHIVED"))
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attribute("questionSuccess", "Đã cập nhật câu hỏi."));

        assertEquals("ARCHIVED", statusOf(q));
    }

    @Test
    void deleteUnusedQuestion() throws Exception {
        Question q = saveChoice("Câu xóa được", "DRAFT");

        mockMvc.perform(post("/manager/questions/{id}/delete", q.getId()).with(csrf()))
                .andExpect(flash().attribute("questionSuccess", "Đã xóa câu hỏi."));

        assertFalse(questionRepository.existsById(q.getId()));
    }

    @Test
    void deleteQuestionUsedInExamIsBlocked() throws Exception {
        Question q = saveChoice("Câu đang dùng", "ACTIVE");
        linkToExam(q, "DRAFT");

        mockMvc.perform(post("/manager/questions/{id}/delete", q.getId()).with(csrf()))
                .andExpect(flash().attribute("questionError", containsString("Không thể xóa câu hỏi đang được sử dụng")));

        assertEquals(true, questionRepository.existsById(q.getId()));
    }

    @Test
    void deletePassageAlsoDeletesItsChildren() throws Exception {
        Question passage = savePassage("Đoạn văn có câu con");
        Question child = saveChoice("Câu con của đoạn", "DRAFT");
        child.setParentId(passage.getId());
        questionRepository.save(child);

        mockMvc.perform(post("/manager/questions/{id}/delete", passage.getId()).with(csrf()))
                .andExpect(flash().attribute("questionSuccess", "Đã xóa câu hỏi."));

        assertFalse(questionRepository.existsById(passage.getId()));
        assertFalse(questionRepository.existsById(child.getId()));
    }

    @Test
    void importTemplateIsDownloadedAsExcelFile() throws Exception {
        mockMvc.perform(get("/manager/questions/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }

    @Test
    void importCreatesPassageWithChildrenAndStandaloneQuestion() throws Exception {
        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer", "status"),
                List.of(
                        List.of("", "MULTI_CHOICE", MARK + "Câu đơn import", "A", "B", "A", "ACTIVE"),
                        List.of("1", "PASSAGE", MARK + "Đoạn văn import", "", "", "", ""),
                        List.of("1", "MULTI_CHOICE", MARK + "Câu con import", "A", "B", "B", "")));

        mockMvc.perform(multipart("/manager/questions/import").file(file).with(csrf()))
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attribute("questionSuccess", "Đã import 3 câu hỏi vào ngân hàng."));

        Question multi = findByContent(MARK + "Câu đơn import");
        Question passage = findByContent(MARK + "Đoạn văn import");
        Question child = findByContent(MARK + "Câu con import");
        assertEquals("ACTIVE", multi.getStatus());
        assertEquals("DRAFT", child.getStatus());
        assertEquals(passage.getId(), child.getParentId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void importWithInvalidRowSavesNothing() throws Exception {
        try {
            var file = QuestionExcel.xlsx(
                    List.of("question_type", "content", "option_a", "option_b", "answer"),
                    List.of(
                            List.of("MULTI_CHOICE", MARK + "Câu đúng", "A", "B", "A"),
                            List.of("MULTI_CHOICE", MARK + "Câu thiếu đáp án", "A", "B", "")));

            mockMvc.perform(multipart("/manager/questions/import").file(file).with(csrf()))
                    .andExpect(flash().attribute("questionError", containsString("Import thất bại")))
                    .andExpect(flash().attribute("importErrors", hasSize(1)));

            assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM questions WHERE content LIKE ?", Long.class, MARK + "%"));
        } finally {
            jdbc.update("DELETE FROM questions WHERE content LIKE ?", MARK + "%");
        }
    }

    // ── helpers ──

    private Question saveChoice(String content, String status) {
        return questionRepository.save(Question.builder()
                .content(MARK + content)
                .questionType("MULTI_CHOICE")
                .options("[{\"key\":\"A\",\"text\":\"A\"},{\"key\":\"B\",\"text\":\"B\"}]")
                .answerKey("[\"A\"]")
                .status(status)
                .build());
    }

    private Question savePassage(String content) {
        return questionRepository.save(Question.builder()
                .content(MARK + content).questionType("PASSAGE").status("ACTIVE").build());
    }

    private void linkToExam(Question q, String examStatus) {
        Exam exam = examRepository.save(new Exam(null, MARK + "Đề", "JLPT_MOCK", "N5", 105, 10, 80, examStatus, null));
        examQuestionRepository.save(ExamQuestion.builder()
                .id(new ExamQuestionId(exam.getExamId(), q.getId()))
                .exam(exam).question(q).section("LANGUAGE").orderIndex(1).points(1)
                .build());
        entityManager.flush();
    }

    private org.springframework.test.web.servlet.RequestBuilder update(Question q, String status) {
        return post("/manager/questions/{id}", q.getId()).with(csrf())
                .param("questionType", q.getQuestionType())
                .param("content", q.getContent())
                .param("status", status)
                .param("optionA", "A").param("optionB", "B").param("answerKeys", "A");
    }

    private Question findByContent(String content) {
        entityManager.flush();
        Long id = jdbc.queryForObject("SELECT id FROM questions WHERE content = ?", Long.class, content);
        entityManager.clear();
        return questionRepository.findById(id).orElseThrow();
    }

    private String statusOf(Question q) {
        entityManager.flush();
        return jdbc.queryForObject("SELECT status FROM questions WHERE id = ?", String.class, q.getId());
    }
}
