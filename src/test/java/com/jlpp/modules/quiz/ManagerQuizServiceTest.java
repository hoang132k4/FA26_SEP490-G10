package com.jlpp.modules.quiz;

import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.service.QuestionImportService;
import com.jlpp.modules.quiz.dto.ExerciseQuestionForm;
import com.jlpp.modules.quiz.dto.ManagerQuizForm;
import com.jlpp.modules.quiz.dto.QuizOption;
import com.jlpp.modules.quiz.repository.ManagerQuizRepository;
import com.jlpp.modules.quiz.service.ManagerQuizService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerQuizServiceTest {

    private static final long QUIZ_ID = 5L;
    private static final long LESSON_ID = 12L;

    @Mock private ManagerQuizRepository repository;
    @Mock private QuestionImportService questionImportService;

    @InjectMocks
    private ManagerQuizService service;

    // ── search / lookup ──

    @Test
    void searchTrimsTextAndReplacesNullWithEmpty() {
        service.search("  Quiz 1 ", " N5 ", null, null);

        verify(repository).search("Quiz 1", "N5", "", "");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.findById(999L));
        assertEquals("Không tìm thấy quiz.", ex.getMessage());
    }

    @Test
    void statusesListAllExerciseStatuses() {
        assertEquals(List.of("DRAFT", "PUBLISHED", "ARCHIVED"), service.statuses());
    }

    @Test
    void toFormCopiesQuizFields() {
        ManagerQuizForm form = service.toForm(quiz("DRAFT"));

        assertEquals(QUIZ_ID, form.getId());
        assertEquals(LESSON_ID, form.getLessonId());
        assertEquals("Quiz bài 1", form.getTitle());
        assertEquals(70, form.getPassingScore());
        assertEquals("DRAFT", form.getStatus());
    }

    // ── create ──

    @Test
    void createDraftInsertsTrimmedTitle() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        ManagerQuizForm form = quizForm("DRAFT");
        form.setTitle("  Quiz bài 1  ");

        service.create(form);

        verify(repository).insert(LESSON_ID, "Quiz bài 1", 70, "DRAFT");
    }

    @Test
    void createPublishedIsRejectedBecauseNewQuizHasNoQuestions() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(quizForm("PUBLISHED")));

        assertTrue(ex.getMessage().startsWith("Không thể tạo quiz với trạng thái PUBLISHED"));
        verifyNoInsert();
    }

    @Test
    void createRejectsMissingLessonId() {
        ManagerQuizForm form = quizForm("DRAFT");
        form.setLessonId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(form));

        assertEquals("Bài học được chọn không tồn tại.", ex.getMessage());
        verifyNoInsert();
    }

    @Test
    void createRejectsUnknownLesson() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.create(quizForm("DRAFT")));
        verifyNoInsert();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ACTIVE", "draft"})
    void createRejectsInvalidStatus(String status) {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(quizForm(status)));

        assertEquals("Trạng thái quiz không hợp lệ.", ex.getMessage());
        verifyNoInsert();
    }

    // ── update ──

    @Test
    void updateDraftWritesChanges() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "ARCHIVED")).thenReturn(1);

        service.update(QUIZ_ID, quizForm("ARCHIVED"));

        verify(repository).update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "ARCHIVED");
    }

    @Test
    void updateToPublishedSucceedsWhenQuizHasQuestions() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.countExerciseQuestions(QUIZ_ID)).thenReturn(1L);
        when(repository.update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "PUBLISHED")).thenReturn(1);

        service.update(QUIZ_ID, quizForm("PUBLISHED"));

        verify(repository).update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "PUBLISHED");
    }

    @Test
    void updateToPublishedFailsWhenQuizHasNoQuestions() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.countExerciseQuestions(QUIZ_ID)).thenReturn(0L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(QUIZ_ID, quizForm("PUBLISHED")));

        assertTrue(ex.getMessage().contains("quiz chưa có câu hỏi nào"));
        verify(repository, never()).update(anyLong(), anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void updateThrowsWhenNoRowWasChanged() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.update(999L, LESSON_ID, "Quiz bài 1", 70, "DRAFT")).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(999L, quizForm("DRAFT")));

        assertEquals("Không tìm thấy quiz cần sửa.", ex.getMessage());
    }

    // ── quiz questions ──

    @Test
    void getQuizQuestionsThrowsWhenQuizMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getQuizQuestions(999L));
        verify(repository, never()).findExerciseQuestions(anyLong());
    }

    @Test
    void findAvailableQuestionsNormalizesFilters() {
        service.findAvailableQuestions(QUIZ_ID, " N5 ", null);

        verify(repository).findAvailableQuestions(QUIZ_ID, "N5", "");
    }

    @Test
    void addQuestionsInsertsEachNewActiveQuestion() {
        stubQuizExists();
        stubActiveQuestion(1L);
        stubActiveQuestion(2L);

        service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of(1L, 2L)));

        verify(repository).insertExerciseQuestion(QUIZ_ID, 1L);
        verify(repository).insertExerciseQuestion(QUIZ_ID, 2L);
    }

    @Test
    void addQuestionsSkipsQuestionsAlreadyInQuiz() {
        stubQuizExists();
        stubActiveQuestion(1L);
        when(repository.exerciseQuestionExists(QUIZ_ID, 1L)).thenReturn(true);

        service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of(1L)));

        verify(repository, never()).insertExerciseQuestion(anyLong(), anyLong());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void addQuestionsRejectsEmptySelection(List<Long> ids) {
        stubQuizExists();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToQuiz(QUIZ_ID, questionForm(ids)));

        assertEquals("Vui lòng chọn ít nhất một câu hỏi.", ex.getMessage());
    }

    @Test
    void addQuestionsRejectsMissingQuestion() {
        stubQuizExists();
        when(repository.questionExists(99L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of(99L))));

        assertEquals("Câu hỏi #99 không tồn tại.", ex.getMessage());
        verify(repository, never()).insertExerciseQuestion(anyLong(), anyLong());
    }

    @Test
    void addQuestionsRejectsInactiveQuestion() {
        stubQuizExists();
        when(repository.questionExists(1L)).thenReturn(true);
        when(repository.questionIsActive(1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of(1L))));

        assertEquals("Câu hỏi #1 chưa được kích hoạt (ACTIVE).", ex.getMessage());
        verify(repository, never()).insertExerciseQuestion(anyLong(), anyLong());
    }

    @Test
    void addQuestionsThrowsWhenQuizMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToQuiz(999L, questionForm(List.of(1L))));
    }

    @Test
    void removeQuestionDeletesLinkAndSyncsCount() {
        stubQuizExists();
        when(repository.deleteExerciseQuestion(QUIZ_ID, 1L)).thenReturn(1);

        service.removeQuestionFromQuiz(QUIZ_ID, 1L);

        verify(repository).deleteExerciseQuestion(QUIZ_ID, 1L);
        verify(repository).syncTotalQuestions(QUIZ_ID);
    }

    @Test
    void removeLastQuestionOfPublishedQuizIsRejected() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("PUBLISHED")));
        when(repository.countExerciseQuestions(QUIZ_ID)).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removeQuestionFromQuiz(QUIZ_ID, 1L));

        assertTrue(ex.getMessage().contains("câu hỏi cuối cùng"));
        verify(repository, never()).deleteExerciseQuestion(anyLong(), anyLong());
    }

    @Test
    void removeQuestionThrowsWhenNotInQuiz() {
        stubQuizExists();
        when(repository.deleteExerciseQuestion(QUIZ_ID, 1L)).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removeQuestionFromQuiz(QUIZ_ID, 1L));

        assertEquals("Câu hỏi này không nằm trong quiz.", ex.getMessage());
    }

    // ── import ──

    @Test
    void importLinksOnlyTopLevelChoiceQuestions() {
        stubQuizExists();
        Question passage = question(1L, "PASSAGE", null);
        Question child = question(2L, "MULTI_CHOICE", 1L);
        Question choice = question(3L, "MULTI_CHOICE", null);
        Question multi = question(4L, "MULTI_CHOICE", null);
        when(questionImportService.importFile(any(), eq("ACTIVE"))).thenReturn(List.of(
                row(2, passage), row(3, child), row(4, choice), row(5, multi)));
        stubActiveQuestion(3L);
        stubActiveQuestion(4L);

        int linked = service.importQuestions(QUIZ_ID, excelFile());

        assertEquals(2, linked);
        verify(repository).insertExerciseQuestion(QUIZ_ID, 3L);
        verify(repository).insertExerciseQuestion(QUIZ_ID, 4L);
        verify(repository).syncTotalQuestions(QUIZ_ID);
        verify(repository, never()).insertExerciseQuestion(QUIZ_ID, 1L);
        verify(repository, never()).insertExerciseQuestion(QUIZ_ID, 2L);
    }

    @Test
    void importFailsWhenFileHasNoChoiceQuestions() {
        stubQuizExists();
        when(questionImportService.importFile(any(), eq("ACTIVE")))
                .thenReturn(List.of(row(2, question(1L, "PASSAGE", null))));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> service.importQuestions(QUIZ_ID, excelFile()));

        assertEquals(List.of("File không có câu hỏi trắc nghiệm nào để gắn vào quiz."), ex.getErrors());
    }

    @Test
    void importThrowsBeforeParsingWhenQuizMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.importQuestions(999L, excelFile()));
        verify(questionImportService, never()).importFile(any(), any());
    }

    @Test
    void importTemplateExcludesExamColumns() {
        byte[] template = {9};
        when(questionImportService.buildTemplate(false)).thenReturn(template);

        assertEquals(template, service.importTemplate());
    }

    // ── helpers ──

    private void stubQuizExists() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("DRAFT")));
    }

    private void stubActiveQuestion(long id) {
        when(repository.questionExists(id)).thenReturn(true);
        when(repository.questionIsActive(id)).thenReturn(true);
    }

    private void verifyNoInsert() {
        verify(repository, never()).insert(anyLong(), anyString(), anyInt(), anyString());
    }

    private static ManagerQuizForm quizForm(String status) {
        ManagerQuizForm form = new ManagerQuizForm();
        form.setLessonId(LESSON_ID);
        form.setTitle("Quiz bài 1");
        form.setPassingScore(70);
        form.setStatus(status);
        return form;
    }

    private static ExerciseQuestionForm questionForm(List<Long> ids) {
        ExerciseQuestionForm form = new ExerciseQuestionForm();
        form.setQuestionIds(ids);
        return form;
    }

    private static QuizOption quiz(String status) {
        return new QuizOption(QUIZ_ID, LESSON_ID, "Bài 1", "Quiz bài 1", 10, 70, status, 0);
    }

    private static Question question(long id, String type, Long parentId) {
        Question q = Question.builder().content("Câu " + id).questionType(type).parentId(parentId).status("ACTIVE").build();
        q.setId(id);
        return q;
    }

    private static QuestionImportService.ImportedRow row(int rowNumber, Question q) {
        return new QuestionImportService.ImportedRow(rowNumber, q, Map.of());
    }

    private static MockMultipartFile excelFile() {
        return new MockMultipartFile("file", "questions.xlsx", null, new byte[]{1});
    }
}
