package com.jeps.modules.exam;

import com.jeps.common.util.UserMessages;
import com.jeps.modules.exam.controller.ManagerExamController;
import com.jeps.modules.exam.dto.ExamQuestionForm;
import com.jeps.modules.exam.dto.ExamQuestionRow;
import com.jeps.modules.exam.dto.GeneratePreview;
import com.jeps.modules.exam.dto.GeneratePreview.PreviewQuestion;
import com.jeps.modules.exam.dto.ManagerExamForm;
import com.jeps.modules.exam.dto.SectionProgress;
import com.jeps.modules.exam.entity.Exam;
import com.jeps.modules.exam.service.ManagerExamService;
import com.jeps.modules.exam.service.ManagerExamService.ManagerExamOptions;
import com.jeps.modules.question.dto.QuestionImportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

@WebMvcTest(ManagerExamController.class)
@ActiveProfiles("test")
@WithMockUser(username = "manager@test.com", roles = "MANAGER")
class ManagerExamControllerTest {

    private static final long EXAM_ID = 1L;
    private static final String QUESTIONS_URL = "/manager/exams/1/questions";
    private static final String PREVIEW_KEY = "generatePreview_1";

    @Autowired private MockMvc mockMvc;
    @MockBean private ManagerExamService managerExamService;
    @MockBean private com.jeps.modules.user.repository.UserRepository userRepository;
    @MockBean private com.jeps.modules.notification.service.NotificationService notificationService;

    private Exam exam;

    @BeforeEach
    void setUp() {
        exam = new Exam(EXAM_ID, "Đề thi thử N5", "JLPT_MOCK", "N5", 90, 67, 80, "DRAFT", null);
        exam.setLanguageQuestions(2);
        exam.setReadingQuestions(1);
        exam.setListeningQuestions(1);
        when(managerExamService.options()).thenReturn(new ManagerExamOptions(
                List.of("JLPT_MOCK", "PLACEMENT_TEST", "MILESTONE"),
                List.of("N5", "N4", "N3", "N2", "N1"),
                List.of("DRAFT", "PUBLISHED", "ARCHIVED")));
        List<SectionProgress> progress = List.of(
                new SectionProgress("LANGUAGE", "Kiến thức ngôn ngữ", 1, 2),
                new SectionProgress("READING", "Đọc hiểu", 0, 1),
                new SectionProgress("LISTENING", "Nghe hiểu", 0, 1));
        when(managerExamService.sectionProgress(anyLong())).thenReturn(progress);
        when(managerExamService.sectionProgress(any(Exam.class))).thenReturn(progress);
    }

    // ── LIST ──

    @Test
    void list_showsExamsAndCountsPerType() throws Exception {
        when(managerExamService.search("", "", "", "", 0)).thenReturn(new PageImpl<>(List.of(exam)));
        when(managerExamService.countsByType()).thenReturn(Map.of("JLPT_MOCK", 3L, "PLACEMENT_TEST", 1L));

        mockMvc.perform(get("/manager/exams"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/list"))
                .andExpect(model().attributeExists("exams", "options", "pageUrl"))
                .andExpect(model().attribute("mockCount", 3L))
                .andExpect(model().attribute("placementCount", 1L))
                .andExpect(model().attribute("milestoneCount", 0L));
    }

    @Test
    void list_passesFiltersAndPageToService() throws Exception {
        when(managerExamService.search("thi thử", "JLPT_MOCK", "N4", "PUBLISHED", 2)).thenReturn(Page.empty());
        when(managerExamService.countsByType()).thenReturn(Map.of());

        mockMvc.perform(get("/manager/exams")
                        .param("search", "thi thử").param("examType", "JLPT_MOCK")
                        .param("jlptLevel", "N4").param("status", "PUBLISHED").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "thi thử"))
                .andExpect(model().attribute("examType", "JLPT_MOCK"))
                .andExpect(model().attribute("jlptLevel", "N4"))
                .andExpect(model().attribute("status", "PUBLISHED"));
        verify(managerExamService).search("thi thử", "JLPT_MOCK", "N4", "PUBLISHED", 2);
    }

    // ── CREATE / EDIT PAGES ──

    @Test
    void createPage_showsDefaultFormWithoutSectionProgress() throws Exception {
        ManagerExamForm form = validForm();
        when(managerExamService.newForm()).thenReturn(form);

        mockMvc.perform(get("/manager/exams/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attribute("examForm", form))
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attributeExists("jlptStructures"))
                .andExpect(model().attributeDoesNotExist("sectionProgress"));
    }

    @Test
    void editPage_found_showsFormWithSectionProgress() throws Exception {
        ManagerExamForm form = validForm();
        form.setExamId(EXAM_ID);
        when(managerExamService.findById(EXAM_ID)).thenReturn(exam);
        when(managerExamService.toForm(exam)).thenReturn(form);

        mockMvc.perform(get("/manager/exams/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attribute("examForm", form))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("sectionProgress"));
    }

    @Test
    void editPage_notFound_redirectsWithError() throws Exception {
        when(managerExamService.findById(999L)).thenThrow(new IllegalArgumentException("Không tìm thấy đề thi."));

        mockMvc.perform(get("/manager/exams/999/edit"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examError", "Không tìm thấy đề thi."));
    }

    // ── CREATE ──

    @Test
    void create_valid_redirectsWithSuccess() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams")))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examSuccess", "Đã tạo đề thi."));

        ArgumentCaptor<ManagerExamForm> captor = ArgumentCaptor.forClass(ManagerExamForm.class);
        verify(managerExamService).create(captor.capture());
        assertEquals("Đề thi thử N5", captor.getValue().getExamTitle());
        assertEquals(2, captor.getValue().getLanguageQuestions());
    }

    @Test
    void create_blankTitle_redisplaysFormWithoutSaving() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams"), "examTitle", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attributeHasFieldErrors("examForm", "examTitle"))
                .andExpect(model().attribute("isEdit", false));
        verify(managerExamService, never()).create(any());
    }

    @Test
    void create_negativeSectionCount_redisplaysFormWithoutSaving() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams"), "readingQuestions", "-1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("examForm", "readingQuestions"));
        verify(managerExamService, never()).create(any());
    }

    @Test
    void create_sectionCountAbove200_redisplaysFormWithoutSaving() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams"), "listeningQuestions", "201"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("examForm", "listeningQuestions"));
        verify(managerExamService, never()).create(any());
    }

    @Test
    void create_sectionCountExactly200_isAccepted() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams"), "listeningQuestions", "200"))
                .andExpect(redirectedUrl("/manager/exams"));
        verify(managerExamService).create(any());
    }

    @Test
    void create_rejectedByService_redisplaysFormWithMessage() throws Exception {
        when(managerExamService.create(any())).thenThrow(new IllegalArgumentException("Đề thi mới phải ở trạng thái DRAFT."));

        mockMvc.perform(withForm(post("/manager/exams")))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attribute("examError", "Đề thi mới phải ở trạng thái DRAFT."));
    }

    @Test
    void create_databaseError_showsGenericMessageInsteadOfSql() throws Exception {
        when(managerExamService.create(any())).thenThrow(new DataIntegrityViolationException("ERROR: duplicate key exams_pkey"));

        mockMvc.perform(withForm(post("/manager/exams")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("examError", UserMessages.DATABASE_ERROR));
    }

    // ── UPDATE ──

    @Test
    void update_valid_usesPathIdAndRedirectsWithSuccess() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams/1"), "examId", "999"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examSuccess", "Đã cập nhật đề thi."));

        ArgumentCaptor<ManagerExamForm> captor = ArgumentCaptor.forClass(ManagerExamForm.class);
        verify(managerExamService).update(eq(EXAM_ID), captor.capture());
        assertEquals(EXAM_ID, captor.getValue().getExamId());
    }

    @Test
    void update_validationError_redisplaysEditForm() throws Exception {
        mockMvc.perform(withForm(post("/manager/exams/1"), "status", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/form"))
                .andExpect(model().attributeHasFieldErrors("examForm", "status"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("sectionProgress"));
        verify(managerExamService, never()).update(anyLong(), any());
    }

    @Test
    void update_rejectedByService_redisplaysEditFormWithMessage() throws Exception {
        when(managerExamService.update(eq(EXAM_ID), any()))
                .thenThrow(new IllegalArgumentException("Không thể đổi cấp độ của đề thi đã có câu hỏi."));

        mockMvc.perform(withForm(post("/manager/exams/1"), "jlptLevel", "N4"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute("examError", "Không thể đổi cấp độ của đề thi đã có câu hỏi."));
    }

    @Test
    void update_databaseError_showsGenericMessageInsteadOfSql() throws Exception {
        when(managerExamService.update(eq(EXAM_ID), any())).thenThrow(new DataIntegrityViolationException("SQL detail"));

        mockMvc.perform(withForm(post("/manager/exams/1")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("examError", UserMessages.DATABASE_ERROR));
    }

    // ── BULK STATUS ──

    @Test
    void bulkStatus_success_redirectsWithCount() throws Exception {
        when(managerExamService.bulkUpdateStatus(List.of(1L, 2L), "ARCHIVED")).thenReturn(2);

        mockMvc.perform(post("/manager/exams/bulk-status").with(csrf()).param("ids", "1", "2").param("status", "ARCHIVED"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examSuccess", "Đã cập nhật trạng thái 2 đề thi sang ARCHIVED."));
    }

    @Test
    void bulkStatus_rejected_redirectsWithError() throws Exception {
        when(managerExamService.bulkUpdateStatus(List.of(1L), "PUBLISHED"))
                .thenThrow(new IllegalArgumentException("Đề thi #1: số câu chưa khớp cấu trúc. Không thể chuyển sang PUBLISHED."));

        mockMvc.perform(post("/manager/exams/bulk-status").with(csrf()).param("ids", "1").param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examError", "Đề thi #1: số câu chưa khớp cấu trúc. Không thể chuyển sang PUBLISHED."))
                .andExpect(flash().attributeCount(1));
    }

    // ── QUESTIONS PAGE ──

    @Test
    void questionsPage_found_filtersAvailableQuestionsByExamLevel() throws Exception {
        when(managerExamService.findById(EXAM_ID)).thenReturn(exam);
        when(managerExamService.getExamQuestions(EXAM_ID)).thenReturn(List.of(
                new ExamQuestionRow(10L, "Câu 10", "SINGLE_CHOICE", "N5", "VOCABULARY", "LANGUAGE", 1)));
        when(managerExamService.findAvailableQuestions(EXAM_ID, "N5", "GRAMMAR", 1)).thenReturn(Page.empty());

        mockMvc.perform(get(QUESTIONS_URL).param("jlptLevel", "N3").param("skillType", "GRAMMAR").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/questions"))
                .andExpect(model().attribute("exam", exam))
                .andExpect(model().attributeExists("examQuestions", "availableQuestions", "addForm",
                        "sectionProgress", "jlptLevels", "skillTypes"))
                .andExpect(model().attribute("filterSkillType", "GRAMMAR"));
        verify(managerExamService).findAvailableQuestions(EXAM_ID, "N5", "GRAMMAR", 1);
    }

    @Test
    void questionsPage_examNotFound_redirectsToList() throws Exception {
        when(managerExamService.findById(999L)).thenThrow(new IllegalArgumentException("Không tìm thấy đề thi."));

        mockMvc.perform(get("/manager/exams/999/questions"))
                .andExpect(redirectedUrl("/manager/exams"))
                .andExpect(flash().attribute("examError", "Không tìm thấy đề thi."));
    }

    // ── ADD QUESTIONS ──

    @Test
    void addQuestions_success_passesSelectionAndSection() throws Exception {
        mockMvc.perform(post(QUESTIONS_URL + "/add").with(csrf())
                        .param("questionIds", "10", "11").param("section", "READING"))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examSuccess", "Đã thêm câu hỏi vào đề thi."));

        ArgumentCaptor<ExamQuestionForm> captor = ArgumentCaptor.forClass(ExamQuestionForm.class);
        verify(managerExamService).addQuestionsToExam(eq(EXAM_ID), captor.capture());
        assertEquals(List.of(10L, 11L), captor.getValue().getQuestionIds());
        assertEquals("READING", captor.getValue().getSection());
    }

    @Test
    void addQuestions_rejected_redirectsWithError() throws Exception {
        doThrow(new IllegalArgumentException("Phần Đọc hiểu chỉ còn 1 chỗ trống."))
                .when(managerExamService).addQuestionsToExam(eq(EXAM_ID), any());

        mockMvc.perform(post(QUESTIONS_URL + "/add").with(csrf()).param("questionIds", "10", "11"))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Phần Đọc hiểu chỉ còn 1 chỗ trống."));
    }

    @Test
    void addQuestions_databaseError_showsGenericMessage() throws Exception {
        doThrow(new DataIntegrityViolationException("SQL detail"))
                .when(managerExamService).addQuestionsToExam(eq(EXAM_ID), any());

        mockMvc.perform(post(QUESTIONS_URL + "/add").with(csrf()).param("questionIds", "10"))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", UserMessages.DATABASE_ERROR));
    }

    // ── AUTO GENERATE ──

    @Test
    void generate_showsPreviewAndKeepsItInSession() throws Exception {
        GeneratePreview preview = preview(10L);
        when(managerExamService.previewQuestions(EXAM_ID)).thenReturn(preview);
        when(managerExamService.findById(EXAM_ID)).thenReturn(exam);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post(QUESTIONS_URL + "/generate").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/generate-preview"))
                .andExpect(model().attribute("preview", preview))
                .andExpect(model().attributeExists("exam", "sectionLabels", "sectionProgress"));
        assertSame(preview, session.getAttribute(PREVIEW_KEY));
    }

    @Test
    void generate_notEnoughQuestions_redirectsWithErrorAndStoresNothing() throws Exception {
        when(managerExamService.previewQuestions(EXAM_ID))
                .thenThrow(new IllegalArgumentException("Ngân hàng không đủ câu cho phần Nghe hiểu."));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post(QUESTIONS_URL + "/generate").with(csrf()).session(session))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Ngân hàng không đủ câu cho phần Nghe hiểu."));
        assertNull(session.getAttribute(PREVIEW_KEY));
    }

    @Test
    void regenerate_withoutPreviewInSession_asksToStartAgain() throws Exception {
        mockMvc.perform(post(QUESTIONS_URL + "/generate/regenerate").with(csrf()).param("section", "READING"))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Phiên preview đã hết hạn. Hãy bấm lại nút Tự động chọn câu."));
        verify(managerExamService, never()).regenerateSection(anyLong(), any(), any());
    }

    @Test
    void regenerate_replacesPreviewInSessionAndNamesTheSection() throws Exception {
        GeneratePreview current = preview(10L);
        GeneratePreview updated = preview(20L);
        when(managerExamService.regenerateSection(EXAM_ID, "READING", current)).thenReturn(updated);
        when(managerExamService.findById(EXAM_ID)).thenReturn(exam);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(PREVIEW_KEY, current);

        mockMvc.perform(post(QUESTIONS_URL + "/generate/regenerate").with(csrf()).session(session).param("section", "READING"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/exam/generate-preview"))
                .andExpect(model().attribute("preview", updated))
                .andExpect(model().attribute("examSuccess", "Đã chọn lại phần Đọc hiểu."));
        assertSame(updated, session.getAttribute(PREVIEW_KEY));
    }

    @Test
    void regenerate_unknownSection_keepsPreviewAndShowsError() throws Exception {
        GeneratePreview current = preview(10L);
        when(managerExamService.regenerateSection(EXAM_ID, "WRITING", current))
                .thenThrow(new IllegalArgumentException("Phần đề không hợp lệ: WRITING"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(PREVIEW_KEY, current);

        mockMvc.perform(post(QUESTIONS_URL + "/generate/regenerate").with(csrf()).session(session).param("section", "WRITING"))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Phần đề không hợp lệ: WRITING"));
        assertSame(current, session.getAttribute(PREVIEW_KEY));
    }

    @Test
    void confirm_withoutPreviewInSession_asksToStartAgain() throws Exception {
        mockMvc.perform(post(QUESTIONS_URL + "/generate/confirm").with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Phiên preview đã hết hạn. Hãy bấm lại nút Tự động chọn câu."));
        verify(managerExamService, never()).confirmGenerate(anyLong(), any());
    }

    @Test
    void confirm_linksPreviewAndClearsSession() throws Exception {
        GeneratePreview preview = preview(10L);
        when(managerExamService.confirmGenerate(EXAM_ID, preview)).thenReturn(4);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(PREVIEW_KEY, preview);

        mockMvc.perform(post(QUESTIONS_URL + "/generate/confirm").with(csrf()).session(session))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examSuccess",
                        "Đã xác nhận và gắn 4 câu hỏi vào đề thi. Hãy xem lại trước khi đăng đề."));
        assertNull(session.getAttribute(PREVIEW_KEY));
    }

    @Test
    void confirm_stalePreview_showsErrorAndKeepsSession() throws Exception {
        GeneratePreview preview = preview(10L);
        when(managerExamService.confirmGenerate(EXAM_ID, preview))
                .thenThrow(new IllegalArgumentException("Câu hỏi #10 không còn dùng được. Hãy tạo lại preview."));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(PREVIEW_KEY, preview);

        mockMvc.perform(post(QUESTIONS_URL + "/generate/confirm").with(csrf()).session(session))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Câu hỏi #10 không còn dùng được. Hãy tạo lại preview."));
        assertSame(preview, session.getAttribute(PREVIEW_KEY));
    }

    // ── REORDER ──

    @Test
    void reorder_success_returnsNoContent() throws Exception {
        mockMvc.perform(post(QUESTIONS_URL + "/reorder").with(csrf())
                        .param("section", "LANGUAGE").param("questionIds", "12", "10", "11"))
                .andExpect(status().isNoContent());
        verify(managerExamService).reorderSection(EXAM_ID, "LANGUAGE", List.of(12L, 10L, 11L));
    }

    @Test
    void reorder_withoutQuestionIds_passesEmptyList() throws Exception {
        mockMvc.perform(post(QUESTIONS_URL + "/reorder").with(csrf()).param("section", "LANGUAGE"))
                .andExpect(status().isNoContent());
        verify(managerExamService).reorderSection(EXAM_ID, "LANGUAGE", List.of());
    }

    @Test
    void reorder_rejected_returnsBadRequestWithMessage() throws Exception {
        doThrow(new IllegalArgumentException("Danh sách câu hỏi đã thay đổi. Hãy tải lại trang."))
                .when(managerExamService).reorderSection(EXAM_ID, "LANGUAGE", List.of(10L));

        mockMvc.perform(post(QUESTIONS_URL + "/reorder").with(csrf()).param("section", "LANGUAGE").param("questionIds", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Danh sách câu hỏi đã thay đổi. Hãy tải lại trang."));
    }

    @Test
    void reorder_databaseError_returnsBadRequestWithGenericMessage() throws Exception {
        doThrow(new DataIntegrityViolationException("SQL detail"))
                .when(managerExamService).reorderSection(EXAM_ID, "LANGUAGE", List.of(10L));

        mockMvc.perform(post(QUESTIONS_URL + "/reorder").with(csrf()).param("section", "LANGUAGE").param("questionIds", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(UserMessages.DATABASE_ERROR));
    }

    // ── REMOVE ──

    @Test
    void removeQuestion_success_redirectsWithSuccess() throws Exception {
        mockMvc.perform(post(QUESTIONS_URL + "/10/remove").with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examSuccess", "Đã gỡ câu hỏi khỏi đề thi."));
        verify(managerExamService).removeQuestionFromExam(EXAM_ID, 10L);
    }

    @Test
    void removeQuestion_blocked_redirectsWithError() throws Exception {
        doThrow(new IllegalArgumentException("Không thể gỡ câu hỏi khỏi đề thi đã đăng."))
                .when(managerExamService).removeQuestionFromExam(EXAM_ID, 10L);

        mockMvc.perform(post(QUESTIONS_URL + "/10/remove").with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Không thể gỡ câu hỏi khỏi đề thi đã đăng."));
    }

    // ── IMPORT ──

    @Test
    void importTemplate_returnsExcelFile() throws Exception {
        when(managerExamService.importTemplate()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/manager/exams/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mau-import-de-thi.xlsx\""))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void import_success_redirectsWithCount() throws Exception {
        MockMultipartFile file = excelFile();
        when(managerExamService.importQuestions(eq(EXAM_ID), any())).thenReturn(5);

        mockMvc.perform(multipart("/manager/exams/1/import").file(file).with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examSuccess", "Đã import và gắn 5 câu hỏi vào đề thi."));
    }

    @Test
    void import_invalidRows_redirectsWithRowErrors() throws Exception {
        List<String> errors = List.of("Dòng 3: thiếu nội dung câu hỏi.", "Dòng 5: phần đề không hợp lệ.");
        when(managerExamService.importQuestions(eq(EXAM_ID), any())).thenThrow(new QuestionImportException(errors));

        mockMvc.perform(multipart("/manager/exams/1/import").file(excelFile()).with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError",
                        "Import thất bại, chưa có dữ liệu nào được lưu. Sửa các lỗi sau rồi thử lại:"))
                .andExpect(flash().attribute("importErrors", errors));
    }

    @Test
    void import_rejectedByService_redirectsWithError() throws Exception {
        when(managerExamService.importQuestions(eq(EXAM_ID), any()))
                .thenThrow(new IllegalArgumentException("Phần Nghe hiểu vượt quá số câu cấu hình."));

        mockMvc.perform(multipart("/manager/exams/1/import").file(excelFile()).with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", "Phần Nghe hiểu vượt quá số câu cấu hình."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void import_databaseError_showsGenericMessage() throws Exception {
        when(managerExamService.importQuestions(eq(EXAM_ID), any())).thenThrow(new DataIntegrityViolationException("SQL detail"));

        mockMvc.perform(multipart("/manager/exams/1/import").file(excelFile()).with(csrf()))
                .andExpect(redirectedUrl(QUESTIONS_URL))
                .andExpect(flash().attribute("examError", UserMessages.DATABASE_ERROR));
    }

    // ── Helpers ──

    private static ManagerExamForm validForm() {
        ManagerExamForm form = new ManagerExamForm();
        form.setExamTitle("Đề thi thử N5");
        form.setExamType("JLPT_MOCK");
        form.setJlptLevel("N5");
        form.setLanguageQuestions(2);
        form.setReadingQuestions(1);
        form.setListeningQuestions(1);
        form.setStatus("DRAFT");
        return form;
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withForm(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String... overrides) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("examTitle", "Đề thi thử N5");
        fields.put("examType", "JLPT_MOCK");
        fields.put("jlptLevel", "N5");
        fields.put("languageQuestions", "2");
        fields.put("readingQuestions", "1");
        fields.put("listeningQuestions", "1");
        fields.put("status", "DRAFT");
        for (int i = 0; i < overrides.length; i += 2) fields.put(overrides[i], overrides[i + 1]);
        request.with(csrf());
        fields.forEach(request::param);
        return request;
    }

    private static GeneratePreview preview(long firstQuestionId) {
        Map<String, List<PreviewQuestion>> sections = new LinkedHashMap<>();
        sections.put("LANGUAGE", List.of(new PreviewQuestion(firstQuestionId, "Câu " + firstQuestionId, "SINGLE_CHOICE", "N5", "VOCABULARY")));
        sections.put("READING", List.of(new PreviewQuestion(firstQuestionId + 1, "Câu đọc", "SINGLE_CHOICE", "N5", "READING")));
        return new GeneratePreview(EXAM_ID, "N5", sections);
    }

    private static MockMultipartFile excelFile() {
        return new MockMultipartFile("file", "de-thi.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
    }
}
