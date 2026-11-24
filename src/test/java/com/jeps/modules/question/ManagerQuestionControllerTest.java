package com.jeps.modules.question;

import com.jeps.modules.question.controller.ManagerQuestionController;
import com.jeps.modules.question.dto.ManagerQuestionForm;
import com.jeps.modules.question.dto.QuestionImportException;
import com.jeps.modules.question.dto.QuestionUsageStats;
import com.jeps.modules.question.entity.Question;
import com.jeps.modules.question.service.ManagerQuestionService;
import com.jeps.modules.question.service.ManagerQuestionService.ManagerQuestionOptions;
import com.jeps.modules.question.service.QuestionAudioStorageService;
import com.jeps.modules.question.service.QuestionImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerQuestionController.class)
@ActiveProfiles("test")
@WithMockUser(username = "manager@test.com", roles = "MANAGER")
class ManagerQuestionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManagerQuestionService managerQuestionService;
    @MockBean private QuestionImportService questionImportService;
    @MockBean private QuestionAudioStorageService audioStorageService;
    @MockBean private com.jeps.modules.user.repository.UserRepository userRepository;
    @MockBean private com.jeps.modules.notification.service.NotificationService notificationService;

    private static final ManagerQuestionOptions OPTIONS = new ManagerQuestionOptions(
            List.of("MULTI_CHOICE", "PASSAGE"),
            List.of("N1", "N2", "N3", "N4", "N5"),
            List.of("VOCABULARY", "GRAMMAR", "READING", "LISTENING"),
            List.of("DRAFT", "ACTIVE", "ARCHIVED"));

    @BeforeEach
    void setUp() {
        when(managerQuestionService.options()).thenReturn(OPTIONS);
    }

    @Test
    void createPage_unknownParent_redirectsWithError() throws Exception {
        when(managerQuestionService.findById(999L)).thenThrow(new IllegalArgumentException("Không tìm thấy câu hỏi."));

        mockMvc.perform(get("/manager/questions/new").param("parentId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attribute("questionError", "Không tìm thấy câu hỏi."));
    }

    // ── LIST ──

    @Test
    void list_noParams_returnsListView() throws Exception {
        when(managerQuestionService.search(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Page.empty());
        when(managerQuestionService.count()).thenReturn(0L);

        mockMvc.perform(get("/manager/questions"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/list"))
                .andExpect(model().attributeExists("questions", "options", "questionCount"));
    }

    @Test
    void list_withSearchParam_passedToService() throws Exception {
        when(managerQuestionService.search(eq("kanji"), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Page.empty());
        when(managerQuestionService.count()).thenReturn(0L);

        mockMvc.perform(get("/manager/questions").param("search", "kanji"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "kanji"));
    }

    @Test
    void list_modelContainsQuestionsAndOptions() throws Exception {
        when(managerQuestionService.search(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Page.empty());
        when(managerQuestionService.count()).thenReturn(5L);

        mockMvc.perform(get("/manager/questions"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("questions", "options"))
                .andExpect(model().attribute("questionCount", 5L));
    }

    // ── CREATE PAGE ──

    @Test
    void createPage_noParentId_emptyForm() throws Exception {
        mockMvc.perform(get("/manager/questions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attributeExists("questionForm"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    void createPage_withParentId_prefillsFromParent() throws Exception {
        Question parent = Question.builder()
                .questionType("PASSAGE")
                .content("Đoạn văn")
                .jlptLevel("N4")
                .skillType("READING")
                .status("ACTIVE")
                .build();
        parent.setId(10L);
        when(managerQuestionService.findById(10L)).thenReturn(parent);

        mockMvc.perform(get("/manager/questions/new").param("parentId", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"));
    }

    // ── EDIT PAGE ──

    @Test
    void editPage_found_returnsFormViewWithUsageStats() throws Exception {
        Question q = Question.builder()
                .questionType("MULTI_CHOICE")
                .content("Câu hỏi?")
                .jlptLevel("N4")
                .status("ACTIVE")
                .build();
        q.setId(1L);

        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setContent("Câu hỏi?");

        when(managerQuestionService.findById(1L)).thenReturn(q);
        when(managerQuestionService.toForm(q)).thenReturn(form);
        when(managerQuestionService.getUsageStats(1L)).thenReturn(new QuestionUsageStats(2, 3, 1, 1));

        mockMvc.perform(get("/manager/questions/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("usageStats"));
    }

    @Test
    void editPage_passageType_includesChildren() throws Exception {
        Question q = Question.builder()
                .questionType("PASSAGE")
                .content("Đoạn văn")
                .status("ACTIVE")
                .build();
        q.setId(1L);

        when(managerQuestionService.findById(1L)).thenReturn(q);
        when(managerQuestionService.toForm(q)).thenReturn(new ManagerQuestionForm());
        when(managerQuestionService.findChildren(1L)).thenReturn(Collections.emptyList());
        when(managerQuestionService.getUsageStats(1L)).thenReturn(new QuestionUsageStats(0, 0, 0, 0));

        mockMvc.perform(get("/manager/questions/1/edit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("children"));
    }

    @Test
    void editPage_notFound_redirectsWithError() throws Exception {
        when(managerQuestionService.findById(999L)).thenThrow(new IllegalArgumentException("Không tìm thấy câu hỏi."));

        mockMvc.perform(get("/manager/questions/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionError"));
    }

    // ── CREATE POST ──

    @Test
    void create_valid_redirectsWithSuccess() throws Exception {
        Question created = Question.builder().content("Q").questionType("MULTI_CHOICE").status("DRAFT").build();
        created.setId(1L);
        when(managerQuestionService.create(any())).thenReturn(created);

        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Câu hỏi mới")
                        .param("optionA", "A")
                        .param("optionB", "B")
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionSuccess"));
    }

    @Test
    void create_validationErrors_redisplaysForm() throws Exception {
        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    void create_illegalArgument_redisplaysFormWithError() throws Exception {
        when(managerQuestionService.create(any())).thenThrow(new IllegalArgumentException("Đáp án không hợp lệ."));

        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Câu hỏi")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attributeExists("questionError"));
    }

    @Test
    void create_databaseError_showsGenericMessageInsteadOfSql() throws Exception {
        when(managerQuestionService.create(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                "ERROR: new row for relation \"questions\" violates check constraint \"chk_questions_skill_type\""));

        mockMvc.perform(post("/manager/questions").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Câu hỏi")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questionError", com.jeps.common.util.UserMessages.DATABASE_ERROR));
    }

    @Test
    void import_databaseError_showsGenericMessageInsteadOfSql() throws Exception {
        when(questionImportService.importFile(any(), any())).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint"));

        mockMvc.perform(multipart("/manager/questions/import")
                        .file(new MockMultipartFile("file", "q.xlsx", "application/octet-stream", new byte[]{1}))
                        .with(csrf()))
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attribute("questionError", com.jeps.common.util.UserMessages.DATABASE_ERROR));
    }

    @Test
    void create_withAudioFile_uploadsAndSetsUrl() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("audioFile", "q.mp3", "audio/mpeg", new byte[]{1, 2, 3});
        when(audioStorageService.store(any())).thenReturn("/audio/uuid.mp3");
        Question created = Question.builder().content("Q").questionType("MULTI_CHOICE").status("DRAFT").build();
        created.setId(1L);
        when(managerQuestionService.create(any())).thenReturn(created);

        mockMvc.perform(multipart("/manager/questions").file(audio).with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Câu hỏi audio")
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"));
    }

    @Test
    void create_audioUploadFails_redisplaysFormWithUploadError() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("audioFile", "q.mp3", "audio/mpeg", new byte[]{1});
        when(audioStorageService.store(any())).thenThrow(new java.io.IOException("Ổ đĩa đầy"));

        mockMvc.perform(multipart("/manager/questions").file(audio).with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Câu hỏi audio")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attribute("questionError", "Lỗi upload file audio: Ổ đĩa đầy"));
        org.mockito.Mockito.verify(managerQuestionService, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void create_emptyAudioFile_skipsUpload() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("audioFile", "", "audio/mpeg", new byte[0]);
        Question created = Question.builder().content("Q").questionType("MULTI_CHOICE").status("DRAFT").build();
        created.setId(1L);
        when(managerQuestionService.create(any())).thenReturn(created);

        mockMvc.perform(multipart("/manager/questions").file(empty).with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Câu hỏi")
                        .param("status", "DRAFT"))
                .andExpect(redirectedUrl("/manager/questions"));
        org.mockito.Mockito.verify(audioStorageService, org.mockito.Mockito.never()).store(any());
    }

    @Test
    void update_audioUploadFails_redisplaysEditFormWithUploadError() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("audioFile", "q.mp3", "audio/mpeg", new byte[]{1});
        when(audioStorageService.store(any())).thenThrow(new java.io.IOException("Ổ đĩa đầy"));

        mockMvc.perform(multipart("/manager/questions/1").file(audio).with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Cập nhật")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute("questionError", "Lỗi upload file audio: Ổ đĩa đầy"));
    }

    // ── UPDATE POST ──

    @Test
    void update_valid_redirectsWithSuccess() throws Exception {
        mockMvc.perform(post("/manager/questions/1").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Cập nhật")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionSuccess"));
    }

    @Test
    void update_validationErrors_redisplaysForm() throws Exception {
        mockMvc.perform(post("/manager/questions/1").with(csrf())
                        .param("questionType", "")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attribute("isEdit", true));
    }

    @Test
    void update_illegalArgument_redisplaysFormWithError() throws Exception {
        doThrow(new IllegalArgumentException("Câu hỏi đang được sử dụng."))
                .when(managerQuestionService).update(eq(1L), any());

        mockMvc.perform(post("/manager/questions/1").with(csrf())
                        .param("questionType", "MULTI_CHOICE")
                        .param("content", "Cập nhật")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/question/form"))
                .andExpect(model().attributeExists("questionError"));
    }

    // ── DELETE ──

    @Test
    void delete_success_redirectsWithSuccess() throws Exception {
        mockMvc.perform(post("/manager/questions/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionSuccess"));
    }

    @Test
    void delete_blocked_redirectsWithError() throws Exception {
        doThrow(new IllegalArgumentException("Câu hỏi đang được sử dụng."))
                .when(managerQuestionService).delete(1L);

        mockMvc.perform(post("/manager/questions/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionError"));
    }

    // ── IMPORT TEMPLATE ──

    @Test
    void importTemplate_returnsExcelResponse() throws Exception {
        when(questionImportService.buildTemplate(false)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/manager/questions/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mau-import-cau-hoi.xlsx\""));
    }

    // ── IMPORT FILE ──

    @Test
    void import_success_redirectsWithCount() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        when(questionImportService.importFile(any(), isNull())).thenReturn(List.of(
                new QuestionImportService.ImportedRow(2,
                        Question.builder().content("Q").questionType("MULTI_CHOICE").status("DRAFT").build(),
                        Collections.emptyMap())));

        mockMvc.perform(multipart("/manager/questions/import").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionSuccess"));
    }

    @Test
    void import_importException_redirectsWithErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        when(questionImportService.importFile(any(), isNull()))
                .thenThrow(new QuestionImportException(List.of("Dòng 2: thiếu content")));

        mockMvc.perform(multipart("/manager/questions/import").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/questions"))
                .andExpect(flash().attributeExists("questionError", "importErrors"));
    }
}
