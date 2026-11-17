package com.jlpp.modules.quiz;

import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.service.QuestionImportService;
import com.jlpp.modules.quiz.dto.ExerciseQuestionForm;
import com.jlpp.modules.quiz.dto.ManagerQuizForm;
import com.jlpp.modules.quiz.dto.QuizGenerateForm;
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
import static org.mockito.Mockito.lenient;
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
        assertEquals(List.of("DRAFT", "PUBLISHED"), service.statuses());
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
    @ValueSource(strings = {"ACTIVE", "draft", "ARCHIVED"})
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
        when(repository.update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "DRAFT")).thenReturn(1);

        service.update(QUIZ_ID, quizForm("DRAFT"));

        verify(repository).update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "DRAFT");
        verify(repository).lessonHasOtherQuiz(LESSON_ID, QUIZ_ID);
    }

    @Test
    void createRejectsLessonThatAlreadyHasQuiz() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.lessonHasOtherQuiz(LESSON_ID, null)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(quizForm("DRAFT")));

        assertEquals("Bài học này đã có quiz. Mỗi bài học chỉ được gắn một quiz.", ex.getMessage());
        verifyNoInsert();
    }

    @Test
    void updateRejectsMovingToLessonThatAlreadyHasQuiz() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.lessonHasOtherQuiz(LESSON_ID, QUIZ_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.update(QUIZ_ID, quizForm("DRAFT")));
        verify(repository, never()).update(anyLong(), anyLong(), any(), anyInt(), any());
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

    // ── generate ──

    @Test
    void generateRejectsInvalidLevel() {
        stubQuizExists();
        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("X9");
        form.setCounts(Map.of("VOCABULARY", 5));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generateQuestions(QUIZ_ID, form));
        assertEquals("Vui lòng chọn cấp độ JLPT.", ex.getMessage());
    }

    @Test
    void generateRejectsNullLevel() {
        stubQuizExists();
        QuizGenerateForm form = new QuizGenerateForm();
        form.setCounts(Map.of("VOCABULARY", 5));

        assertThrows(IllegalArgumentException.class, () -> service.generateQuestions(QUIZ_ID, form));
    }

    @Test
    void generateRejectsNegativeCount() {
        stubQuizExists();
        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", -3));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generateQuestions(QUIZ_ID, form));
        assertTrue(ex.getMessage().contains("không được âm"));
    }

    @Test
    void generateRejectsAllCountsZero() {
        stubQuizExists();
        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", 0, "GRAMMAR", 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generateQuestions(QUIZ_ID, form));
        assertEquals("Hãy nhập số câu cho ít nhất một kỹ năng.", ex.getMessage());
    }

    @Test
    void generateWithInsufficientPoolThrowsWithSkillLabel() {
        stubQuizExists();
        when(repository.findGeneratableQuestionIds(QUIZ_ID, "N5", "VOCABULARY"))
                .thenReturn(List.of(1L, 2L));

        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", 5));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generateQuestions(QUIZ_ID, form));
        assertTrue(ex.getMessage().contains("Từ vựng"));
        assertTrue(ex.getMessage().contains("chỉ còn 2 câu"));
    }

    @Test
    void generatePicksCorrectCountAndLinksToQuiz() {
        stubQuizExists();
        when(repository.findGeneratableQuestionIds(QUIZ_ID, "N5", "VOCABULARY"))
                .thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        lenient().when(repository.questionExists(anyLong())).thenReturn(true);
        lenient().when(repository.questionIsActive(anyLong())).thenReturn(true);

        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", 3));

        int count = service.generateQuestions(QUIZ_ID, form);

        assertEquals(3, count);
    }

    // ── remove edge case ──

    @Test
    void removingOnlyQuestionFromDraftQuizIsAllowed() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("DRAFT")));
        when(repository.deleteExerciseQuestion(QUIZ_ID, 1L)).thenReturn(1);

        service.removeQuestionFromQuiz(QUIZ_ID, 1L);

        verify(repository).deleteExerciseQuestion(QUIZ_ID, 1L);
        verify(repository).syncTotalQuestions(QUIZ_ID);
    }

    // ── questionFilterOptions ──

    @Test
    void countQuizQuestionsDelegatesToRepository() {
        when(repository.countExerciseQuestions(QUIZ_ID)).thenReturn(7L);

        assertEquals(7L, service.countQuizQuestions(QUIZ_ID));
    }

    @Test
    void questionFilterOptionsContainsAllLevelsAndSkills() {
        ManagerQuizService.QuizQuestionOptions opts = service.questionFilterOptions();

        assertEquals(List.of("VOCABULARY", "GRAMMAR", "LISTENING", "READING"), opts.skillTypes());
        assertEquals(List.of("N5", "N4", "N3", "N2", "N1"), opts.jlptLevels());
    }

    // ── additional search tests ──

    @Test
    void searchPassesAllNonNullValuesThrough() {
        service.search("keyword", "N3", "READING", "DRAFT");

        verify(repository).search("keyword", "N3", "READING", "DRAFT");
    }

    @Test
    void searchAllNulls_sendsEmptyStrings() {
        service.search(null, null, null, null);

        verify(repository).search("", "", "", "");
    }

    // ── lessonGroups ──

    @Test
    void lessonGroupsGroupsByGroupField() {
        when(repository.findLessons()).thenReturn(List.of(
                new com.jlpp.modules.quiz.dto.LessonOption(1L, "Bài 1", "N5"),
                new com.jlpp.modules.quiz.dto.LessonOption(2L, "Bài 2", "N5"),
                new com.jlpp.modules.quiz.dto.LessonOption(3L, "Bài 3", "N4")));

        var groups = service.lessonGroups();

        assertEquals(2, groups.size());
        assertEquals(2, groups.get("N5").size());
        assertEquals(1, groups.get("N4").size());
    }

    @Test
    void lessonGroupsEmptyList_returnsEmptyMap() {
        when(repository.findLessons()).thenReturn(List.of());

        var groups = service.lessonGroups();

        assertTrue(groups.isEmpty());
    }

    // ── findById additional ──

    @Test
    void findByIdReturnsQuizWhenPresent() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("DRAFT")));

        QuizOption result = service.findById(QUIZ_ID);

        assertEquals(QUIZ_ID, result.id());
        assertEquals("Quiz bài 1", result.title());
    }

    // ── create additional ──

    @Test
    void createWithNullTitle_insertsEmptyString() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        ManagerQuizForm form = quizForm("DRAFT");
        form.setTitle(null);

        service.create(form);

        verify(repository).insert(LESSON_ID, "", 70, "DRAFT");
    }

    // ── update additional ──

    @Test
    void updateRejectsMissingLessonId() {
        ManagerQuizForm form = quizForm("DRAFT");
        form.setLessonId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(QUIZ_ID, form));
        assertEquals("Bài học được chọn không tồn tại.", ex.getMessage());
    }

    @Test
    void updateRejectsInvalidStatus() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(QUIZ_ID, quizForm("ACTIVE")));
        assertEquals("Trạng thái quiz không hợp lệ.", ex.getMessage());
    }

    @Test
    void updateToDraftFromPublishedSucceeds() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "DRAFT")).thenReturn(1);

        service.update(QUIZ_ID, quizForm("DRAFT"));

        verify(repository).update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "DRAFT");
    }

    // ── quiz questions additional ──

    @Test
    void getQuizQuestionsReturnsListFromRepository() {
        stubQuizExists();
        var expected = List.of(new com.jlpp.modules.quiz.dto.ExerciseQuestionRow(10L, "Q1", "MULTI_CHOICE", "N5", "VOCABULARY"));
        when(repository.findExerciseQuestions(QUIZ_ID)).thenReturn(expected);

        var result = service.getQuizQuestions(QUIZ_ID);

        assertEquals(expected, result);
    }

    @Test
    void findAvailableQuestionsPassesBothFiltersNonNull() {
        service.findAvailableQuestions(QUIZ_ID, "N4", "GRAMMAR");

        verify(repository).findAvailableQuestions(QUIZ_ID, "N4", "GRAMMAR");
    }

    // ── addQuestions additional ──

    @Test
    void addQuestionsSyncsTotalQuestionsAfterInsert() {
        stubQuizExists();
        stubActiveQuestion(1L);

        service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of(1L)));

        verify(repository).syncTotalQuestions(QUIZ_ID);
    }

    @Test
    void addQuestionsPartiallyExisting_onlyInsertsNew() {
        stubQuizExists();
        stubActiveQuestion(1L);
        stubActiveQuestion(2L);
        when(repository.exerciseQuestionExists(QUIZ_ID, 1L)).thenReturn(true);

        service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of(1L, 2L)));

        verify(repository, never()).insertExerciseQuestion(QUIZ_ID, 1L);
        verify(repository).insertExerciseQuestion(QUIZ_ID, 2L);
    }

    // ── generate additional ──

    @Test
    void generateWithNullCounts_throwsNoSkillSelected() {
        stubQuizExists();
        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generateQuestions(QUIZ_ID, form));
        assertEquals("Hãy nhập số câu cho ít nhất một kỹ năng.", ex.getMessage());
    }

    @Test
    void generateMultipleSkills_picksCorrectCountPerSkill() {
        stubQuizExists();
        when(repository.findGeneratableQuestionIds(QUIZ_ID, "N5", "VOCABULARY"))
                .thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(repository.findGeneratableQuestionIds(QUIZ_ID, "N5", "GRAMMAR"))
                .thenReturn(List.of(10L, 11L, 12L, 13L, 14L));
        lenient().when(repository.questionExists(anyLong())).thenReturn(true);
        lenient().when(repository.questionIsActive(anyLong())).thenReturn(true);

        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", 2, "GRAMMAR", 3));

        int count = service.generateQuestions(QUIZ_ID, form);

        assertEquals(5, count);
    }

    @Test
    void generateWithZeroForOneSkill_skipsZeroSkill() {
        stubQuizExists();
        when(repository.findGeneratableQuestionIds(QUIZ_ID, "N5", "VOCABULARY"))
                .thenReturn(List.of(1L, 2L, 3L));
        lenient().when(repository.questionExists(anyLong())).thenReturn(true);
        lenient().when(repository.questionIsActive(anyLong())).thenReturn(true);

        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", 2, "GRAMMAR", 0));

        int count = service.generateQuestions(QUIZ_ID, form);

        assertEquals(2, count);
        verify(repository, never()).findGeneratableQuestionIds(QUIZ_ID, "N5", "GRAMMAR");
    }

    @Test
    void generateMissingQuiz_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        QuizGenerateForm form = new QuizGenerateForm();
        form.setJlptLevel("N5");
        form.setCounts(Map.of("VOCABULARY", 1));

        assertThrows(IllegalArgumentException.class, () -> service.generateQuestions(999L, form));
    }

    // ── remove additional ──

    @Test
    void removeQuestionFromArchivedQuiz_succeeds() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("ARCHIVED")));
        when(repository.deleteExerciseQuestion(QUIZ_ID, 1L)).thenReturn(1);

        service.removeQuestionFromQuiz(QUIZ_ID, 1L);

        verify(repository).deleteExerciseQuestion(QUIZ_ID, 1L);
        verify(repository).syncTotalQuestions(QUIZ_ID);
    }

    @Test
    void removePublished_withMultipleQuestions_succeeds() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("PUBLISHED")));
        when(repository.countExerciseQuestions(QUIZ_ID)).thenReturn(3L);
        when(repository.deleteExerciseQuestion(QUIZ_ID, 1L)).thenReturn(1);

        service.removeQuestionFromQuiz(QUIZ_ID, 1L);

        verify(repository).deleteExerciseQuestion(QUIZ_ID, 1L);
        verify(repository).syncTotalQuestions(QUIZ_ID);
    }

    // ── NEW: additional edge cases ──

    @Test
    void search_emptyStrings_sendsEmpty() {
        service.search("", "", "", "");
        verify(repository).search("", "", "", "");
    }

    @Test
    void findById_found_returnsQuiz() {
        when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz("PUBLISHED")));

        QuizOption result = service.findById(QUIZ_ID);

        assertEquals(QUIZ_ID, result.id());
        assertEquals("PUBLISHED", result.status());
    }

    @Test
    void create_nullPassingScore_throwsNpe() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        ManagerQuizForm form = quizForm("DRAFT");
        form.setPassingScore(null);

        assertThrows(NullPointerException.class, () -> service.create(form));
    }

    @Test
    void update_lessonIdFromForm() {
        when(repository.lessonExists(LESSON_ID)).thenReturn(true);
        when(repository.update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "DRAFT")).thenReturn(1);

        service.update(QUIZ_ID, quizForm("DRAFT"));

        verify(repository).update(QUIZ_ID, LESSON_ID, "Quiz bài 1", 70, "DRAFT");
    }

    @Test
    void addQuestions_emptyIds_throwsIllegalArgument() {
        stubQuizExists();
        assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToQuiz(QUIZ_ID, questionForm(List.of())));
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
