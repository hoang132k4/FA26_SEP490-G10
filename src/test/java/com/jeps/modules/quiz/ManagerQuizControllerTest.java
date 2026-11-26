package com.jeps.modules.quiz;

import com.jeps.modules.question.dto.QuestionImportException;
import com.jeps.modules.quiz.dto.ExerciseQuestionRow;
import com.jeps.modules.quiz.dto.LessonOption;
import com.jeps.modules.quiz.dto.ManagerQuizForm;
import com.jeps.modules.quiz.dto.QuizOption;
import com.jeps.modules.quiz.controller.ManagerQuizController;
import com.jeps.modules.quiz.service.ManagerQuizService;
import com.jeps.modules.quiz.service.ManagerQuizService.QuizQuestionOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.jeps.common.util.UserMessages;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerQuizController.class)
@ActiveProfiles("test")
@WithMockUser(username = "manager@test.com", roles = "MANAGER")
class ManagerQuizControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ManagerQuizService managerQuizService;
    @MockBean private com.jeps.modules.user.repository.UserRepository userRepository;
    @MockBean private com.jeps.modules.notification.service.NotificationService notificationService;

    private static final QuizOption QUIZ = new QuizOption(1L, 100L, "Bài 1", "Quiz Test", 5, 60, "DRAFT", 3);

    // ── LIST ──

    @Test
    void list_returnsViewWithModelAttributes() throws Exception {
        when(managerQuizService.search(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(QUIZ)));
        when(managerQuizService.levelSections()).thenReturn(Map.of("N4", List.of("Chương 1")));
        when(managerQuizService.lessonGroups()).thenReturn(Map.of("Nhóm A", List.of(new LessonOption(1L, "Bài 1", "Nhóm A"))));
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT", "PUBLISHED", "ARCHIVED"));
        when(managerQuizService.count()).thenReturn(1L);

        mockMvc.perform(get("/manager/quizzes"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/list"))
                .andExpect(model().attributeExists("quizzes", "levelSections", "statuses", "quizCount"))
                .andExpect(model().attributeDoesNotExist("lessonGroups"));
        verify(managerQuizService, never()).lessonGroups();
    }

    @Test
    void list_passesFilterParamsToService() throws Exception {
        when(managerQuizService.search(eq("abc"), eq("N3"), eq("Chương 1"), eq("DRAFT"), anyInt())).thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));
        when(managerQuizService.levelSections()).thenReturn(Collections.emptyMap());
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(Collections.emptyList());
        when(managerQuizService.count()).thenReturn(0L);

        mockMvc.perform(get("/manager/quizzes")
                        .param("search", "abc")
                        .param("level", "N3")
                        .param("section", "Chương 1")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "abc"))
                .andExpect(model().attribute("level", "N3"))
                .andExpect(model().attribute("section", "Chương 1"))
                .andExpect(model().attribute("status", "DRAFT"));
    }

    // ── CREATE PAGE ──

    @Test
    void createPage_returnsFormView() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));

        mockMvc.perform(get("/manager/quizzes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attributeExists("quizForm"))
                .andExpect(model().attribute("isEdit", false));
    }

    // ── EDIT PAGE ──

    @Test
    void editPage_quizFound_returnsFormWithData() throws Exception {
        ManagerQuizForm form = new ManagerQuizForm();
        form.setTitle("Quiz Test");
        when(managerQuizService.findById(1L)).thenReturn(QUIZ);
        when(managerQuizService.toForm(QUIZ)).thenReturn(form);
        when(managerQuizService.countQuizQuestions(1L)).thenReturn(3L);
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));

        mockMvc.perform(get("/manager/quizzes/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute("linkedCount", 3L));
    }

    @Test
    void editPage_quizNotFound_redirectsWithError() throws Exception {
        when(managerQuizService.findById(999L)).thenThrow(new IllegalArgumentException("Không tìm thấy quiz."));

        mockMvc.perform(get("/manager/quizzes/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attributeExists("quizError"));
    }

    // ── CREATE POST ──

    @Test
    void create_validDraft_redirects() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));

        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", "100")
                        .param("title", "New Quiz")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attributeExists("quizSuccess"));
    }

    @Test
    void create_validationError_returnsFormPage() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));

        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("title", "")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    void create_serviceThrowsIllegalArg_showsError() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));
        doThrow(new IllegalArgumentException("Trạng thái không hợp lệ.")).when(managerQuizService).create(any());

        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", "100")
                        .param("title", "Quiz")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attributeExists("quizError"));
    }

    @Test
    void create_serviceThrowsDataAccess_showsError() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));
        doThrow(new DataIntegrityViolationException("ERROR: null value in column \"x\" of relation \"exercises\""))
                .when(managerQuizService).create(any());

        mockMvc.perform(post("/manager/quizzes").with(csrf())
                        .param("lessonId", "100")
                        .param("title", "Quiz")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attribute("quizError", UserMessages.DATABASE_ERROR));
    }

    // ── UPDATE POST ──

    @Test
    void update_valid_redirectsWithSuccess() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));

        mockMvc.perform(post("/manager/quizzes/1").with(csrf())
                        .param("lessonId", "100")
                        .param("title", "Updated")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attributeExists("quizSuccess"));
    }

    @Test
    void update_validationError_returnsFormPage() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));

        mockMvc.perform(post("/manager/quizzes/1").with(csrf())
                        .param("title", "")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attribute("isEdit", true));
    }

    @Test
    void update_serviceThrows_showsError() throws Exception {
        when(managerQuizService.lessonGroups()).thenReturn(Collections.emptyMap());
        when(managerQuizService.statuses()).thenReturn(List.of("DRAFT"));
        doThrow(new IllegalArgumentException("Lỗi")).when(managerQuizService).update(eq(1L), any());

        mockMvc.perform(post("/manager/quizzes/1").with(csrf())
                        .param("lessonId", "100")
                        .param("title", "Quiz")
                        .param("passingScore", "60")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/form"))
                .andExpect(model().attributeExists("quizError"));
    }

    // ── QUESTIONS PAGE ──

    @Test
    void questionsPage_quizFound_returnsView() throws Exception {
        when(managerQuizService.findById(1L)).thenReturn(QUIZ);
        when(managerQuizService.getQuizQuestions(1L)).thenReturn(List.of(
                new ExerciseQuestionRow(10L, "Câu hỏi?", "MULTI_CHOICE", "N4", "VOCABULARY")));
        when(managerQuizService.findAvailableQuestions(eq(1L), anyString(), anyString(), anyInt()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(managerQuizService.questionFilterOptions()).thenReturn(new QuizQuestionOptions(List.of("N4"), List.of("VOCABULARY")));

        mockMvc.perform(get("/manager/quizzes/1/questions"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/quiz/questions"))
                .andExpect(model().attributeExists("quiz", "quizQuestions", "availableQuestions", "addForm"));
    }

    @Test
    void questionsPage_quizNotFound_redirects() throws Exception {
        when(managerQuizService.findById(999L)).thenThrow(new IllegalArgumentException("Không tìm thấy quiz."));

        mockMvc.perform(get("/manager/quizzes/999/questions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes"))
                .andExpect(flash().attributeExists("quizError"));
    }

    // ── ADD QUESTIONS ──

    @Test
    void addQuestions_success_redirects() throws Exception {
        mockMvc.perform(post("/manager/quizzes/1/questions/add").with(csrf())
                        .param("questionIds", "10", "11"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizSuccess"));
    }

    @Test
    void addQuestions_error_redirectsWithError() throws Exception {
        doThrow(new IllegalArgumentException("Câu hỏi không tồn tại."))
                .when(managerQuizService).addQuestionsToQuiz(eq(1L), any());

        mockMvc.perform(post("/manager/quizzes/1/questions/add").with(csrf())
                        .param("questionIds", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizError"));
    }

    // ── GENERATE QUESTIONS ──

    @Test
    void generateQuestions_success_redirects() throws Exception {
        when(managerQuizService.generateQuestions(eq(1L), any())).thenReturn(5);

        mockMvc.perform(post("/manager/quizzes/1/questions/generate").with(csrf())
                        .param("jlptLevel", "N4")
                        .param("counts[VOCABULARY]", "3")
                        .param("counts[GRAMMAR]", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizSuccess"));
    }

    @Test
    void generateQuestions_error_redirectsWithError() throws Exception {
        when(managerQuizService.generateQuestions(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Không đủ câu hỏi."));

        mockMvc.perform(post("/manager/quizzes/1/questions/generate").with(csrf())
                        .param("jlptLevel", "N4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizError"));
    }

    // ── REMOVE QUESTION ──

    @Test
    void removeQuestion_success_redirects() throws Exception {
        mockMvc.perform(post("/manager/quizzes/1/questions/10/remove").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizSuccess"));
    }

    @Test
    void removeQuestion_error_redirectsWithError() throws Exception {
        doThrow(new IllegalArgumentException("Không thể gỡ."))
                .when(managerQuizService).removeQuestionFromQuiz(1L, 10L);

        mockMvc.perform(post("/manager/quizzes/1/questions/10/remove").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizError"));
    }

    // ── IMPORT TEMPLATE ──

    @Test
    void importTemplate_returnsExcelFile() throws Exception {
        when(managerQuizService.importTemplate()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/manager/quizzes/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mau-import-quiz.xlsx\""));
    }

    // ── IMPORT QUESTIONS ──

    @Test
    void importQuestions_success_redirects() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        when(managerQuizService.importQuestions(eq(1L), any())).thenReturn(5);

        mockMvc.perform(multipart("/manager/quizzes/1/import").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizSuccess"));
    }

    @Test
    void importQuestions_importException_showsErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        when(managerQuizService.importQuestions(eq(1L), any()))
                .thenThrow(new QuestionImportException(List.of("Dòng 2: thiếu content")));

        mockMvc.perform(multipart("/manager/quizzes/1/import").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizError", "importErrors"));
    }

    @Test
    void importQuestions_illegalArg_showsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        when(managerQuizService.importQuestions(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Quiz không tồn tại."));

        mockMvc.perform(multipart("/manager/quizzes/1/import").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/quizzes/1/questions"))
                .andExpect(flash().attributeExists("quizError"));
    }
}
