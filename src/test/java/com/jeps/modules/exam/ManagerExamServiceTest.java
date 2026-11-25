package com.jeps.modules.exam;

import com.jeps.modules.exam.dto.ExamQuestionForm;
import com.jeps.modules.exam.dto.GeneratePreview;
import com.jeps.modules.exam.dto.ManagerExamForm;
import com.jeps.modules.exam.dto.SectionProgress;
import com.jeps.modules.exam.entity.Exam;
import com.jeps.modules.exam.entity.ExamQuestion;
import com.jeps.modules.exam.entity.ExamQuestionId;
import com.jeps.modules.exam.repository.ExamQuestionRepository;
import com.jeps.modules.exam.repository.ExamRepository;
import com.jeps.modules.exam.service.JlptExamStructure;
import com.jeps.modules.exam.service.ManagerExamService;
import com.jeps.modules.question.dto.QuestionImportException;
import com.jeps.modules.question.entity.Question;
import com.jeps.modules.question.repository.QuestionRepository;
import com.jeps.modules.question.service.QuestionImportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerExamServiceTest {

    private static final long EXAM_ID = 7L;

    @Mock private ExamRepository examRepository;
    @Mock private ExamQuestionRepository examQuestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private EntityManager entityManager;
    @Mock private QuestionImportService questionImportService;

    @InjectMocks
    private ManagerExamService service;

    // ── JLPT structure ──

    @ParameterizedTest
    @CsvSource({"N5,67,90,80,true", "N4,85,115,90,true", "N3,102,140,95,false", "N2,107,155,90,false", "N1,107,165,100,false"})
    void jlptStructureFollowsOfficialFormat(String level, int total, int duration, int passing, boolean combined) {
        JlptExamStructure s = JlptExamStructure.of(level);

        assertEquals(total, s.totalQuestions());
        assertEquals(duration, s.durationMinutes());
        assertEquals(passing, s.passingScore());
        assertEquals(combined, s.languageReadingCombined());
    }

    @Test
    void unknownLevelFallsBackToN5Structure() {
        assertEquals("N5", JlptExamStructure.of("N9").level());
    }

    // ── search / findById ──

    @Test
    void searchTrimsFiltersAndReplacesNullWithEmpty() {
        service.search("  đề N5 ", null, " N5 ", null);

        verify(examRepository).searchForManager("đề N5", "", "N5", "");
    }

    @Test
    void findByIdThrowsWhenExamMissing() {
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.findById(999L));
        assertEquals("Không tìm thấy đề thi.", ex.getMessage());
    }

    // ── create ──

    @Test
    void newFormIsDraftWithN5DefaultSectionCounts() {
        ManagerExamForm form = service.newForm();

        assertEquals("N5", form.getJlptLevel());
        assertEquals("DRAFT", form.getStatus());
        assertEquals(38, form.getLanguageQuestions());
        assertEquals(5, form.getReadingQuestions());
        assertEquals(24, form.getListeningQuestions());
    }

    @ParameterizedTest
    @CsvSource({"N5,90,80", "N4,115,90", "N3,140,95", "N2,155,90", "N1,165,100"})
    void createAppliesFixedJlptDurationAndPassingScore(String level, int duration, int passingScore) {
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        Exam saved = service.create(examForm("JLPT_MOCK", level, "DRAFT"));

        assertEquals(duration, saved.getDurationMinutes());
        assertEquals(passingScore, saved.getPassingScore());
    }

    @Test
    void createStoresSectionCountsAndTheirSumAsTotal() {
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerExamForm form = examForm("PLACEMENT_TEST", "N4", "DRAFT", 10, 4, 6);
        form.setExamTitle("  Đề đầu vào  ");
        form.setDescription("   ");

        Exam saved = service.create(form);

        assertEquals("Đề đầu vào", saved.getExamTitle());
        assertNull(saved.getDescription());
        assertEquals("PLACEMENT_TEST", saved.getExamType());
        assertEquals(10, saved.getLanguageQuestions());
        assertEquals(4, saved.getReadingQuestions());
        assertEquals(6, saved.getListeningQuestions());
        assertEquals(20, saved.getTotalQuestions());
        assertEquals(180, saved.getTotalPoints());
        assertEquals("DRAFT", saved.getStatus());
    }

    @Test
    void createRejectsExamWithoutAnyQuestion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(examForm("JLPT_MOCK", "N5", "DRAFT", 0, 0, 0)));

        assertEquals("Đề thi phải có ít nhất 1 câu hỏi.", ex.getMessage());
        verify(examRepository, never()).save(any());
    }

    @Test
    void createRejectsNegativeSectionCount() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(examForm("JLPT_MOCK", "N5", "DRAFT", 5, -1, 5)));

        assertTrue(ex.getMessage().contains("Đọc hiểu"));
        verify(examRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "ACTIVE"})
    void createRejectsPublishableStatusBecauseNewExamHasNoQuestions(String status) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(examForm("JLPT_MOCK", "N5", status)));

        assertTrue(ex.getMessage().contains("Không thể tạo đề thi với trạng thái " + status));
        verify(examRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "INVALID,  N5,  DRAFT,   Loại đề thi không hợp lệ.",
            "NULL,     N5,  DRAFT,   Loại đề thi không hợp lệ.",
            "JLPT_MOCK,N9,  DRAFT,   Cấp độ JLPT không hợp lệ.",
            "JLPT_MOCK,NULL,DRAFT,   Cấp độ JLPT không hợp lệ.",
            "JLPT_MOCK,N5,  INVALID, Trạng thái đề thi không hợp lệ.",
            "JLPT_MOCK,N5,  NULL,    Trạng thái đề thi không hợp lệ."
    })
    void createRejectsInvalidDomainValues(String type, String level, String status, String message) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(examForm(type, level, status)));

        assertEquals(message, ex.getMessage());
        verify(examRepository, never()).save(any());
    }

    // ── update ──

    @Test
    void updateDraftOverwritesFieldsAndRecomputesJlptSettings() {
        Exam existing = exam(EXAM_ID, "DRAFT");
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(existing));
        when(examRepository.save(existing)).thenReturn(existing);
        ManagerExamForm form = examForm("MILESTONE", "N1", "DRAFT", 20, 5, 5);
        form.setDescription(" Mô tả ");

        Exam saved = service.update(EXAM_ID, form);

        assertEquals("MILESTONE", saved.getExamType());
        assertEquals("N1", saved.getJlptLevel());
        assertEquals(165, saved.getDurationMinutes());
        assertEquals(100, saved.getPassingScore());
        assertEquals(30, saved.getTotalQuestions());
        assertEquals(180, saved.getTotalPoints());
        assertEquals(20, saved.getLanguageQuestions());
        assertEquals("Mô tả", saved.getDescription());
    }

    @Test
    void updateToPublishedSucceedsWhenEverySectionMatches() {
        Exam existing = exam(EXAM_ID, "DRAFT");
        stubLinked("LANGUAGE", 3);
        stubLinked("READING", 2);
        stubLinked("LISTENING", 1);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(existing));
        when(examRepository.save(existing)).thenReturn(existing);

        Exam saved = service.update(EXAM_ID, examForm("JLPT_MOCK", "N5", "PUBLISHED", 3, 2, 1));

        assertEquals("PUBLISHED", saved.getStatus());
    }

    @Test
    void updateToPublishedRejectsExamWithQuestionsNoLongerActive() {
        stubLinked("LANGUAGE", 3);
        stubLinked("READING", 2);
        stubLinked("LISTENING", 1);
        when(examQuestionRepository.findInactiveQuestionIds(EXAM_ID)).thenReturn(List.of(12L, 15L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(EXAM_ID, examForm("JLPT_MOCK", "N5", "PUBLISHED", 3, 2, 1)));

        assertEquals("Không thể chuyển sang PUBLISHED: đề còn câu hỏi không ở trạng thái ACTIVE (#12, #15). "
                + "Hãy gỡ các câu này khỏi đề hoặc kích hoạt lại trước.", ex.getMessage());
        verify(examRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "ACTIVE"})
    void updateToPublishableStatusFailsWhenASectionIsIncomplete(String status) {
        stubLinked("LANGUAGE", 3);
        stubLinked("READING", 0);
        stubLinked("LISTENING", 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(EXAM_ID, examForm("JLPT_MOCK", "N5", status, 3, 2, 1)));

        assertTrue(ex.getMessage().contains("Đọc hiểu 0/2"));
        assertFalse(ex.getMessage().contains("Nghe hiểu"));
        verify(examRepository, never()).save(any());
    }

    @Test
    void updateToPublishedFailsWhenASectionHasMoreQuestionsThanConfigured() {
        stubLinked("LANGUAGE", 5);
        stubLinked("READING", 2);
        stubLinked("LISTENING", 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(EXAM_ID, examForm("JLPT_MOCK", "N5", "PUBLISHED", 3, 2, 1)));

        assertTrue(ex.getMessage().contains("Kiến thức ngôn ngữ (Từ vựng · Ngữ pháp) 5/3"));
    }

    @Test
    void updateThrowsWhenExamMissing() {
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.update(999L, examForm("JLPT_MOCK", "N5", "DRAFT")));
        verify(examRepository, never()).save(any());
    }

    @Test
    void toFormCopiesEditableFields() {
        Exam exam = new Exam(EXAM_ID, "Đề", "JLPT_MOCK", "N3", 140, 40, 95, "DRAFT", "desc");
        exam.setLanguageQuestions(20);
        exam.setReadingQuestions(10);
        exam.setListeningQuestions(10);

        ManagerExamForm form = service.toForm(exam);

        assertEquals(EXAM_ID, form.getExamId());
        assertEquals("Đề", form.getExamTitle());
        assertEquals("JLPT_MOCK", form.getExamType());
        assertEquals("N3", form.getJlptLevel());
        assertEquals(20, form.getLanguageQuestions());
        assertEquals(10, form.getReadingQuestions());
        assertEquals(10, form.getListeningQuestions());
        assertEquals("DRAFT", form.getStatus());
        assertEquals("desc", form.getDescription());
    }

    @Test
    void toFormFallsBackToLevelDefaultsForLegacyExam() {
        Exam legacy = new Exam(EXAM_ID, "Đề cũ", "JLPT_MOCK", "N3", 140, 40, 95, "DRAFT", null);

        ManagerExamForm form = service.toForm(legacy);

        assertEquals(58, form.getLanguageQuestions());
        assertEquals(16, form.getReadingQuestions());
        assertEquals(28, form.getListeningQuestions());
    }

    @Test
    void sectionProgressReportsLinkedAgainstRequiredInJlptOrder() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT", 3, 2, 1)));
        stubLinked("LANGUAGE", 3);
        stubLinked("READING", 1);

        List<SectionProgress> progress = service.sectionProgress(EXAM_ID);

        assertEquals(List.of("LANGUAGE", "READING", "LISTENING"), progress.stream().map(SectionProgress::section).toList());
        assertTrue(progress.get(0).complete());
        assertEquals(1, progress.get(1).linked());
        assertEquals(2, progress.get(1).required());
        assertFalse(progress.get(2).complete());
    }

    // ── addQuestionsToExam ──

    @Test
    void addQuestionsAppendsAfterCurrentMaxOrderInSection() {
        stubExamExists();
        when(examQuestionRepository.maxOrderIndex(EXAM_ID, "READING")).thenReturn(3);
        stubQuestion(10L, "ACTIVE");
        stubQuestion(11L, "ACTIVE");

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L, 11L), "READING"));

        List<ExamQuestion> saved = captureSavedExamQuestions(2);
        assertEquals(10L, saved.get(0).getId().getQuestionId());
        assertEquals(4, saved.get(0).getOrderIndex());
        assertEquals(11L, saved.get(1).getId().getQuestionId());
        assertEquals(5, saved.get(1).getOrderIndex());
        for (ExamQuestion eq : saved) {
            assertEquals(EXAM_ID, eq.getId().getExamId());
            assertEquals("READING", eq.getSection());
            assertEquals(1, eq.getPoints());
        }
    }

    @Test
    void addQuestionsWithoutSectionPlacesEachQuestionBySkillType() {
        stubExamExists();
        stubQuestion(10L, "ACTIVE", "READING");
        stubQuestion(11L, "ACTIVE", "VOCABULARY");
        stubQuestion(12L, "ACTIVE", "LISTENING");

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L, 11L, 12L), null));

        List<ExamQuestion> saved = captureSavedExamQuestions(3);
        assertEquals("READING", saved.get(0).getSection());
        assertEquals("LANGUAGE", saved.get(1).getSection());
        assertEquals("LISTENING", saved.get(2).getSection());
    }

    @Test
    void addQuestionsRejectsSelectionThatExceedsSectionQuota() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT", 10, 2, 5)));
        stubQuestion(10L, "ACTIVE");
        stubQuestion(11L, "ACTIVE");
        stubLinked("READING", 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L, 11L), "READING")));

        assertTrue(ex.getMessage().contains("Đọc hiểu chỉ cần 2 câu, đã gán 1 nên chỉ thêm được 1 câu nữa"));
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void addQuestionsIgnoresDuplicateIdsInSelection() {
        stubExamExists();
        stubQuestion(10L, "ACTIVE");

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L, 10L), "LANGUAGE"));

        captureSavedExamQuestions(1);
    }

    @Test
    void addQuestionsSkipsAlreadyLinkedQuestionWithoutConsumingAnOrderSlot() {
        stubExamExists();
        when(examQuestionRepository.maxOrderIndex(EXAM_ID, "LANGUAGE")).thenReturn(0);
        stubQuestion(10L, "ACTIVE");
        stubQuestion(11L, "ACTIVE");
        when(examQuestionRepository.findLinkedQuestionIds(eq(EXAM_ID), any())).thenReturn(List.of(10L));

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L, 11L), "LANGUAGE"));

        List<ExamQuestion> saved = captureSavedExamQuestions(1);
        assertEquals(11L, saved.get(0).getId().getQuestionId());
        assertEquals(1, saved.get(0).getOrderIndex());
    }

    @Test
    void addQuestionsRejectsNullSelection() {
        stubExamExists();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(null, "LANGUAGE")));

        assertEquals("Vui lòng chọn ít nhất một câu hỏi.", ex.getMessage());
    }

    @Test
    void addQuestionsRejectsEmptySelection() {
        stubExamExists();

        assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(), "LANGUAGE")));
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void addQuestionsRejectsUnknownSection() {
        stubExamExists();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L), "WRITING")));

        assertEquals("Section không hợp lệ.", ex.getMessage());
    }

    @Test
    void addQuestionsLoadsSelectionWithBatchQueries() {
        stubExamExists();
        stubQuestion(10L, "ACTIVE");
        stubQuestion(11L, "ACTIVE");
        stubQuestion(12L, "ACTIVE");

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L, 11L, 12L), "LANGUAGE"));

        verify(questionRepository).findAllById(List.of(10L, 11L, 12L));
        verify(questionRepository, never()).findById(anyLong());
        verify(examQuestionRepository).findLinkedQuestionIds(EXAM_ID, List.of(10L, 11L, 12L));
        verify(examQuestionRepository, never()).existsById(any());
        verify(examQuestionRepository).saveAll(any());
        verify(examQuestionRepository, never()).save(any());
    }

    @Test
    void addQuestionsRejectsMissingQuestion() {
        stubExamExists();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(999L), "LANGUAGE")));

        assertEquals("Câu hỏi #999 không tồn tại.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "ARCHIVED"})
    void addQuestionsRejectsQuestionThatIsNotActive(String status) {
        stubExamExists();
        stubQuestion(10L, status);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L), "LANGUAGE")));

        assertTrue(ex.getMessage().contains("chưa được kích hoạt"));
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void addQuestionsThrowsWhenExamMissing() {
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(999L, questionForm(List.of(10L), "LANGUAGE")));
    }

    // ── removeQuestionFromExam ──

    @Test
    void removeQuestionDeletesLink() {
        ExamQuestionId id = new ExamQuestionId(EXAM_ID, 10L);
        when(examQuestionRepository.existsById(id)).thenReturn(true);
        stubExamExists();

        service.removeQuestionFromExam(EXAM_ID, 10L);

        verify(examQuestionRepository).deleteById(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "ACTIVE"})
    void removeQuestionBlockedWhileExamIsOpen(String status) {
        when(examQuestionRepository.existsById(new ExamQuestionId(EXAM_ID, 10L))).thenReturn(true);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, status)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removeQuestionFromExam(EXAM_ID, 10L));

        assertEquals("Không thể gỡ câu hỏi khỏi đề thi đang " + status + ". Hãy chuyển đề về DRAFT trước.", ex.getMessage());
        verify(examQuestionRepository, never()).deleteById(any());
    }

    @Test
    void addQuestionsRejectsQuestionOfAnotherLevel() {
        stubExamExists();
        Question n1 = question(10L, "ACTIVE");
        n1.setJlptLevel("N1");
        stubQuestion(n1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L), "LANGUAGE")));

        assertEquals("Câu hỏi #10 thuộc cấp N1, không khớp cấp độ đề thi (N5).", ex.getMessage());
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void updateRejectsLevelChangeWhenExamHasQuestions() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT")));
        when(examQuestionRepository.countByExamId(EXAM_ID)).thenReturn(3L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(EXAM_ID, examForm("JLPT_MOCK", "N4", "DRAFT")));

        assertEquals("Không thể đổi cấp độ khi đề thi đã có câu hỏi. Hãy gỡ hết câu hỏi trước.", ex.getMessage());
        verify(examRepository, never()).save(any());
    }

    @Test
    void updateKeepsCourseExamTimingAndScaleWhenLevelIsUnchanged() {
        Exam midterm = new Exam(EXAM_ID, "Giữa kỳ", "MIDTERM", "N5", 35, 5, 50, "DRAFT", null);
        midterm.setTotalPoints(100);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(midterm));
        when(examRepository.save(midterm)).thenReturn(midterm);

        Exam saved = service.update(EXAM_ID, examForm("MIDTERM", "N5", "DRAFT", 5, 0, 0));

        assertEquals(35, saved.getDurationMinutes());
        assertEquals(50, saved.getPassingScore());
        assertEquals(100, saved.getTotalPoints());
        assertEquals(5, saved.getTotalQuestions());
    }

    @Test
    void toFormUsesLinkedCountsForExamWithoutSectionCounts() {
        when(examQuestionRepository.countByExamId(EXAM_ID)).thenReturn(5L);
        stubLinked("LANGUAGE", 4);
        stubLinked("READING", 1);

        ManagerExamForm form = service.toForm(exam(EXAM_ID, "PUBLISHED"));

        assertEquals(4, form.getLanguageQuestions());
        assertEquals(1, form.getReadingQuestions());
        assertEquals(0, form.getListeningQuestions());
    }

    @Test
    void toFormUsesJlptDefaultsForEmptyExamWithoutSectionCounts() {
        ManagerExamForm form = service.toForm(exam(EXAM_ID, "DRAFT"));

        assertEquals(38, form.getLanguageQuestions());
        assertEquals(5, form.getReadingQuestions());
        assertEquals(24, form.getListeningQuestions());
    }

    @Test
    void examTypesIncludeCourseMidtermAndFinal() {
        assertTrue(service.options().examTypes().containsAll(List.of("MIDTERM", "FINAL")));
    }

    @Test
    void removeQuestionThrowsWhenNotLinked() {
        when(examQuestionRepository.existsById(new ExamQuestionId(EXAM_ID, 10L))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.removeQuestionFromExam(EXAM_ID, 10L));
        verify(examQuestionRepository, never()).deleteById(any());
    }

    // ── reorderSection ──

    @Test
    void reorderParksRowsAboveMaxThenWritesFinalPositivePositions() {
        when(examQuestionRepository.findByExamIdAndSectionOrdered(EXAM_ID, "LANGUAGE"))
                .thenReturn(List.of(link(10L, 1), link(20L, 2), link(30L, 5)));
        Query query = mock(Query.class, Answers.RETURNS_SELF);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        service.reorderSection(EXAM_ID, "LANGUAGE", List.of(30L, 10L, 20L));

        ArgumentCaptor<Object> values = ArgumentCaptor.forClass(Object.class);
        verify(query, times(18)).setParameter(anyInt(), values.capture());
        assertEquals(List.of(
                6, EXAM_ID, 30L, 7, EXAM_ID, 10L, 8, EXAM_ID, 20L,
                1, EXAM_ID, 30L, 2, EXAM_ID, 10L, 3, EXAM_ID, 20L), values.getAllValues());
        verify(query, times(6)).executeUpdate();
    }

    @ParameterizedTest
    @MethodSource("staleReorderLists")
    void reorderRejectsListThatDoesNotMatchSection(List<Long> ids) {
        when(examQuestionRepository.findByExamIdAndSectionOrdered(EXAM_ID, "LANGUAGE"))
                .thenReturn(List.of(link(10L, 1), link(20L, 2)));

        assertThrows(IllegalArgumentException.class, () -> service.reorderSection(EXAM_ID, "LANGUAGE", ids));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    static java.util.stream.Stream<List<Long>> staleReorderLists() {
        return java.util.stream.Stream.of(
                List.of(10L),
                List.of(10L, 20L, 30L),
                List.of(10L, 30L),
                List.of(10L, 10L));
    }

    @Test
    void reorderRejectsUnknownSection() {
        assertThrows(IllegalArgumentException.class, () -> service.reorderSection(EXAM_ID, "WRITING", List.of(10L)));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    // ── importQuestions ──

    @Test
    void importLinksChoiceQuestionsAndSkipsPassagesAndChildren() {
        stubExamExists();
        Question passage = question(1L, "ACTIVE");
        passage.setQuestionType("PASSAGE");
        Question child = question(2L, "ACTIVE");
        child.setParentId(1L);
        Question reading = question(3L, "ACTIVE");
        reading.setSkillType("READING");
        Question explicit = question(4L, "ACTIVE");
        stubQuestion(reading);
        stubQuestion(explicit);
        when(questionImportService.importFile(any(), eq("ACTIVE"))).thenReturn(List.of(
                row(2, passage, Map.of()),
                row(3, child, Map.of()),
                row(4, reading, Map.of()),
                row(5, explicit, Map.of("section", " listening "))));

        int linked = service.importQuestions(EXAM_ID, excelFile());

        assertEquals(2, linked);
        List<ExamQuestion> saved = captureSavedExamQuestions(2);
        assertEquals(3L, saved.get(0).getId().getQuestionId());
        assertEquals("READING", saved.get(0).getSection());
        assertEquals(4L, saved.get(1).getId().getQuestionId());
        assertEquals("LISTENING", saved.get(1).getSection());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {"VOCABULARY,LANGUAGE", "GRAMMAR,LANGUAGE", "NULL,LANGUAGE", "LISTENING,LISTENING"})
    void importDerivesSectionFromSkillTypeWhenColumnBlank(String skillType, String expectedSection) {
        stubExamExists();
        Question q = question(3L, "ACTIVE");
        q.setSkillType(skillType);
        stubQuestion(q);
        when(questionImportService.importFile(any(), eq("ACTIVE"))).thenReturn(List.of(row(2, q, Map.of("section", "  "))));

        service.importQuestions(EXAM_ID, excelFile());

        assertEquals(expectedSection, captureSavedExamQuestions(1).get(0).getSection());
    }

    @Test
    void importRejectsWholeFileWhenASectionWouldExceedItsQuota() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT", 10, 1, 5)));
        Question r1 = question(3L, "ACTIVE");
        r1.setSkillType("READING");
        Question r2 = question(4L, "ACTIVE");
        r2.setSkillType("READING");
        when(questionImportService.importFile(any(), eq("ACTIVE"))).thenReturn(List.of(row(2, r1, Map.of()), row(3, r2, Map.of())));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> service.importQuestions(EXAM_ID, excelFile()));

        assertTrue(ex.getErrors().get(0).contains("Đọc hiểu chỉ cần 1 câu"));
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void importReportsInvalidSectionWithRowNumber() {
        stubExamExists();
        when(questionImportService.importFile(any(), eq("ACTIVE")))
                .thenReturn(List.of(row(9, question(3L, "ACTIVE"), Map.of("section", "writing"))));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> service.importQuestions(EXAM_ID, excelFile()));

        assertTrue(ex.getErrors().get(0).startsWith("Dòng 9: section 'WRITING' không hợp lệ"));
    }

    @Test
    void importFailsWhenFileHasNoChoiceQuestions() {
        stubExamExists();
        Question passage = question(1L, "ACTIVE");
        passage.setQuestionType("PASSAGE");
        when(questionImportService.importFile(any(), eq("ACTIVE"))).thenReturn(List.of(row(2, passage, Map.of())));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> service.importQuestions(EXAM_ID, excelFile()));

        assertTrue(ex.getErrors().get(0).contains("không có câu hỏi trắc nghiệm"));
    }

    @Test
    void importThrowsBeforeParsingWhenExamMissing() {
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.importQuestions(999L, excelFile()));
        verify(questionImportService, never()).importFile(any(), any());
    }

    @Test
    void importTemplateIncludesExamColumns() {
        byte[] template = {1, 2, 3};
        when(questionImportService.buildTemplate(true)).thenReturn(template);

        assertEquals(template, service.importTemplate());
    }

    // ── NEW: getExamQuestions edge cases ──

    @Test
    void getExamQuestions_truncatesLongContent() {
        Question q = question(100L, "ACTIVE");
        q.setContent("A".repeat(120));
        q.setQuestionType("MULTIPLE_CHOICE");
        q.setJlptLevel("N5");
        q.setSkillType("VOCABULARY");
        ExamQuestion eq = ExamQuestion.builder()
                .id(new ExamQuestionId(EXAM_ID, 100L))
                .question(q).section("LANGUAGE").orderIndex(1).build();
        when(examQuestionRepository.findByExamIdWithQuestion(EXAM_ID)).thenReturn(List.of(eq));

        var rows = service.getExamQuestions(EXAM_ID);

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).content().length() <= 83);
    }

    @Test
    void getExamQuestions_empty_returnsEmptyList() {
        when(examQuestionRepository.findByExamIdWithQuestion(EXAM_ID)).thenReturn(List.of());
        assertTrue(service.getExamQuestions(EXAM_ID).isEmpty());
    }

    // ── NEW: addQuestionsToExam edge cases ──

    @Test
    void addQuestions_forcedSectionOverridesDefault() {
        stubExamExists();
        stubQuestion(200L, "ACTIVE", "READING");
        when(examQuestionRepository.countByExamIdAndSection(EXAM_ID, "LISTENING")).thenReturn(0L);
        when(examQuestionRepository.maxOrderIndex(EXAM_ID, "LISTENING")).thenReturn(0);

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(200L), "LISTENING"));

        var saved = captureSavedExamQuestions(1);
        assertEquals("LISTENING", saved.get(0).getSection());
    }

    // ── NEW: reorderSection edge cases ──

    @Test
    void reorderSection_nullIds_throwsIllegalArgument() {
        when(examQuestionRepository.findByExamIdAndSectionOrdered(EXAM_ID, "LANGUAGE"))
                .thenReturn(List.of(link(10L, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> service.reorderSection(EXAM_ID, "LANGUAGE", null));
    }

    // ── NEW: generateQuestions edge cases ──

    @Test
    void previewQuestions_emptyBank_throwsWithShortageMessage() {
        Exam exam = exam(EXAM_ID, "DRAFT", 1, 0, 0);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(questionRepository.findAvailableForExam(eq(EXAM_ID), anyString(), anyString())).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.previewQuestions(EXAM_ID));
        assertTrue(ex.getMessage().contains("ngân hàng"));
    }

    @Test
    void previewQuestions_balancesLanguageSkills() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT")));
        List<Question> bank = new ArrayList<>();
        for (long i = 1; i <= 30; i++) bank.add(question(i, "ACTIVE", "VOCABULARY"));
        for (long i = 101; i <= 130; i++) bank.add(question(i, "ACTIVE", "GRAMMAR"));
        for (long i = 201; i <= 205; i++) bank.add(question(i, "ACTIVE", "READING"));
        for (long i = 301; i <= 324; i++) bank.add(question(i, "ACTIVE", "LISTENING"));
        when(questionRepository.findAvailableForExam(eq(EXAM_ID), eq("N5"), anyString())).thenReturn(bank);

        var preview = service.previewQuestions(EXAM_ID);

        assertEquals(67, preview.totalAdded());
        var language = preview.sections().get("LANGUAGE");
        assertEquals(38, language.size());
        assertEquals(19, language.stream().filter(q -> "VOCABULARY".equals(q.skillType())).count());
        assertEquals("VOCABULARY", language.get(0).skillType());
    }

    @Test
    void previewQuestions_rejectsExamThatIsNotDraft() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "PUBLISHED")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.previewQuestions(EXAM_ID));

        assertEquals("Chỉ tự động chọn câu cho đề thi ở trạng thái DRAFT.", ex.getMessage());
        verify(questionRepository, never()).findAvailableForExam(anyLong(), anyString(), anyString());
    }

    @Test
    void previewQuestions_onlyPicksQuestionsStillMissingPerSection() {
        stubExamExists();
        stubBank(fullN5Bank());
        stubLinked("LANGUAGE", 30);
        stubLinked("READING", 5);

        var preview = service.previewQuestions(EXAM_ID);

        assertEquals(8, preview.sections().get("LANGUAGE").size());
        assertTrue(preview.sections().get("READING").isEmpty());
        assertEquals(24, preview.sections().get("LISTENING").size());
        assertEquals(32, preview.totalAdded());
    }

    @Test
    void previewQuestions_examAlreadyComplete_throws() {
        stubExamExists();
        stubBank(fullN5Bank());
        stubLinked("LANGUAGE", 38);
        stubLinked("READING", 5);
        stubLinked("LISTENING", 24);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.previewQuestions(EXAM_ID));

        assertEquals("Đề thi đã đủ số câu từng phần theo cấu hình.", ex.getMessage());
    }

    @Test
    void previewQuestions_usesExamSectionCounts() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT", 2, 1, 1)));
        stubBank(fullN5Bank());

        var preview = service.previewQuestions(EXAM_ID);

        assertEquals(2, preview.sections().get("LANGUAGE").size());
        assertEquals(1, preview.sections().get("READING").size());
        assertEquals(1, preview.sections().get("LISTENING").size());
    }

    @Test
    void previewQuestions_sectionAlreadyOverStructure_throws() {
        stubExamExists();
        stubBank(fullN5Bank());
        stubLinked("LANGUAGE", 40);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.previewQuestions(EXAM_ID));

        assertTrue(ex.getMessage().contains("đã gán 40 câu, vượt số câu đã cấu hình (38 câu)"));
    }

    @Test
    void previewQuestions_keepsPassageChildrenTogether() {
        stubExamExists();
        List<Question> bank = new ArrayList<>(fullN5Bank());
        bank.removeIf(q -> "READING".equals(q.getSkillType()));
        for (long id = 501; id <= 503; id++) bank.add(child(id, 500L, "READING"));
        bank.add(question(510L, "ACTIVE", "READING"));
        bank.add(question(511L, "ACTIVE", "READING"));
        stubBank(bank);

        var reading = service.previewQuestions(EXAM_ID).sections().get("READING").stream()
                .map(GeneratePreview.PreviewQuestion::questionId).toList();

        assertEquals(5, reading.size());
        assertTrue(reading.containsAll(List.of(501L, 502L, 503L, 510L, 511L)));
    }

    @Test
    void previewQuestions_passagesThatCannotMakeExactCount_throws() {
        stubExamExists();
        List<Question> bank = new ArrayList<>(fullN5Bank());
        bank.removeIf(q -> "READING".equals(q.getSkillType()));
        for (long id = 501; id <= 503; id++) bank.add(child(id, 500L, "READING"));
        for (long id = 601; id <= 603; id++) bank.add(child(id, 600L, "READING"));
        stubBank(bank);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.previewQuestions(EXAM_ID));

        assertTrue(ex.getMessage().contains("không ghép được đúng 5 câu"));
    }

    @Test
    void regenerateSection_unknownSection_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.regenerateSection(EXAM_ID, "WRITING", preview(Map.of())));

        assertEquals("Section không hợp lệ.", ex.getMessage());
    }

    @Test
    void regenerateSection_replacesOnlyChosenSection() {
        stubExamExists();
        stubBank(fullN5Bank());
        GeneratePreview current = preview(Map.of(
                "LANGUAGE", ids(901, 938, "VOCABULARY"),
                "READING", ids(951, 955, "READING"),
                "LISTENING", ids(961, 984, "LISTENING")));

        GeneratePreview updated = service.regenerateSection(EXAM_ID, "LISTENING", current);

        assertEquals(current.sections().get("LANGUAGE"), updated.sections().get("LANGUAGE"));
        assertEquals(current.sections().get("READING"), updated.sections().get("READING"));
        var listening = updated.sections().get("LISTENING");
        assertEquals(24, listening.size());
        assertTrue(listening.stream().allMatch(q -> q.questionId() >= 301 && q.questionId() <= 324));
    }

    @Test
    void regenerateSection_rejectsExamThatIsNotDraft() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "PUBLISHED")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.regenerateSection(EXAM_ID, "LISTENING", preview(Map.of())));

        assertEquals("Chỉ tự động chọn câu cho đề thi ở trạng thái DRAFT.", ex.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmGenerate_savesInBulk() {
        stubExamExists();
        stubBank(fullN5Bank());

        int added = service.confirmGenerate(EXAM_ID, fullN5Preview());

        assertEquals(67, added);
        ArgumentCaptor<List<ExamQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(examQuestionRepository, times(3)).saveAll(captor.capture());
        assertEquals(38, captor.getAllValues().get(0).size());
        assertEquals(1, captor.getAllValues().get(0).get(0).getOrderIndex());
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmGenerate_appendsAfterExistingOrderInSection() {
        stubExamExists();
        stubBank(fullN5Bank());
        stubLinked("LANGUAGE", 2);
        when(examQuestionRepository.maxOrderIndex(EXAM_ID, "LANGUAGE")).thenReturn(2);

        List<GeneratePreview.PreviewQuestion> language = new ArrayList<>(ids(1, 30, "VOCABULARY"));
        language.addAll(ids(101, 106, "GRAMMAR"));
        service.confirmGenerate(EXAM_ID, preview(Map.of("LANGUAGE", language)));

        ArgumentCaptor<List<ExamQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(examQuestionRepository).saveAll(captor.capture());
        assertEquals(List.of(3, 4), captor.getValue().subList(0, 2).stream().map(ExamQuestion::getOrderIndex).toList());
    }

    @Test
    void confirmGenerate_keepsExamSectionCounts() {
        Exam custom = exam(EXAM_ID, "DRAFT", 10, 5, 5);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(custom));
        stubBank(fullN5Bank());

        int added = service.confirmGenerate(EXAM_ID, preview(Map.of("LANGUAGE", ids(1, 10, "VOCABULARY"),
                "READING", ids(201, 205, "READING"), "LISTENING", ids(301, 305, "LISTENING"))));

        assertEquals(20, added);
        assertEquals(10, custom.getLanguageQuestions());
        assertEquals(5, custom.getReadingQuestions());
        assertEquals(5, custom.getListeningQuestions());
        verify(examRepository, never()).save(any());
    }

    @Test
    void confirmGenerate_rejectsExamThatIsNotDraft() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "PUBLISHED")));

        assertThrows(IllegalArgumentException.class, () -> service.confirmGenerate(EXAM_ID, fullN5Preview()));
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void confirmGenerate_questionNoLongerAvailable_rejectsStalePreview() {
        stubExamExists();
        List<Question> bank = new ArrayList<>(fullN5Bank());
        bank.removeIf(q -> q.getId() == 7L); // archived or linked by hand after the preview was made
        stubBank(bank);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.confirmGenerate(EXAM_ID, fullN5Preview()));

        assertEquals(STALE_PREVIEW, ex.getMessage());
        verify(examQuestionRepository, never()).saveAll(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void confirmGenerate_sectionFilledSincePreview_rejectsStalePreview() {
        stubExamExists();
        stubBank(fullN5Bank());
        stubLinked("LANGUAGE", 5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.confirmGenerate(EXAM_ID, fullN5Preview()));

        assertEquals(STALE_PREVIEW, ex.getMessage());
        verify(examQuestionRepository, never()).saveAll(any());
    }

    @Test
    void addQuestions_blankSectionFallsBackToSkill() {
        stubExamExists();
        stubQuestion(200L, "ACTIVE", "READING");
        when(examQuestionRepository.maxOrderIndex(EXAM_ID, "READING")).thenReturn(0);

        service.addQuestionsToExam(EXAM_ID, questionForm(List.of(200L), "   "));

        assertEquals("READING", captureSavedExamQuestions(1).get(0).getSection());
    }

    @Test
    void createRejectsMissingSectionCount() {
        ManagerExamForm form = examForm("JLPT_MOCK", "N5", "DRAFT");
        form.setLanguageQuestions(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(form));

        assertEquals("Số câu phần Kiến thức ngôn ngữ (Từ vựng · Ngữ pháp) không hợp lệ.", ex.getMessage());
        verify(examRepository, never()).save(any());
    }

    @Test
    void createWithNullTitle_storesEmptyTitle() {
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerExamForm form = examForm("JLPT_MOCK", "N5", "DRAFT");
        form.setExamTitle(null);

        assertEquals("", service.create(form).getExamTitle());
    }

    // ── simple queries ──

    @Test
    void findAvailableQuestions_normalizesFilters() {
        service.findAvailableQuestions(EXAM_ID, " N5 ", null, -1);

        verify(questionRepository).findAvailableForExam(EXAM_ID, "N5", "",
                org.springframework.data.domain.PageRequest.of(0, ManagerExamService.PAGE_SIZE));
    }

    @Test
    void countByType_delegatesToRepository() {
        when(examRepository.countByExamTypeIgnoreCase("JLPT_MOCK")).thenReturn(4L);

        assertEquals(4L, service.countByType("JLPT_MOCK"));
    }

    @Test
    void countExamQuestions_delegatesToRepository() {
        when(examQuestionRepository.countByExamId(EXAM_ID)).thenReturn(12L);

        assertEquals(12L, service.countExamQuestions(EXAM_ID));
    }

    private static final String STALE_PREVIEW =
            "Kết quả xem trước đã cũ vì câu hỏi trong đề hoặc ngân hàng vừa thay đổi. Hãy bấm Tự động chọn câu để chọn lại.";

    private void stubBank(List<Question> bank) {
        when(questionRepository.findAvailableForExam(eq(EXAM_ID), eq("N5"), anyString())).thenReturn(bank);
    }

    /** 30 VOCABULARY (1-30), 30 GRAMMAR (101-130), 5 READING (201-205), 24 LISTENING (301-324). */
    private static List<Question> fullN5Bank() {
        List<Question> bank = new ArrayList<>();
        for (long i = 1; i <= 30; i++) bank.add(question(i, "ACTIVE", "VOCABULARY"));
        for (long i = 101; i <= 130; i++) bank.add(question(i, "ACTIVE", "GRAMMAR"));
        for (long i = 201; i <= 205; i++) bank.add(question(i, "ACTIVE", "READING"));
        for (long i = 301; i <= 324; i++) bank.add(question(i, "ACTIVE", "LISTENING"));
        return bank;
    }

    private static GeneratePreview fullN5Preview() {
        List<GeneratePreview.PreviewQuestion> language = new ArrayList<>(ids(1, 30, "VOCABULARY"));
        language.addAll(ids(101, 108, "GRAMMAR"));
        return preview(Map.of("LANGUAGE", language, "READING", ids(201, 205, "READING"), "LISTENING", ids(301, 324, "LISTENING")));
    }

    private static List<GeneratePreview.PreviewQuestion> ids(long from, long to, String skill) {
        return java.util.stream.LongStream.rangeClosed(from, to)
                .mapToObj(i -> new GeneratePreview.PreviewQuestion(i, "Câu " + i, "MULTI_CHOICE", "N5", skill)).toList();
    }

    private static GeneratePreview preview(Map<String, List<GeneratePreview.PreviewQuestion>> bySection) {
        Map<String, List<GeneratePreview.PreviewQuestion>> ordered = new java.util.LinkedHashMap<>();
        for (String section : List.of("LANGUAGE", "READING", "LISTENING")) {
            ordered.put(section, bySection.getOrDefault(section, List.of()));
        }
        return new GeneratePreview(EXAM_ID, "N5", ordered);
    }

    private static Question child(long id, long parentId, String skill) {
        Question q = question(id, "ACTIVE", skill);
        q.setParentId(parentId);
        return q;
    }

    // ── helpers ──

    private void stubExamExists() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT")));
    }

    private void stubLinked(String section, long count) {
        when(examQuestionRepository.countByExamIdAndSection(EXAM_ID, section)).thenReturn(count);
    }

    private final Map<Long, Question> stubbedQuestions = new java.util.HashMap<>();

    private void stubQuestion(long id, String status) {
        stubQuestion(question(id, status));
    }

    private void stubQuestion(long id, String status, String skillType) {
        Question q = question(id, status);
        q.setSkillType(skillType);
        stubQuestion(q);
    }

    /** Serves findAllById from the questions stubbed so far, like the database would. */
    private void stubQuestion(Question q) {
        stubbedQuestions.put(q.getId(), q);
        lenient().doAnswer(inv -> {
            List<Question> found = new ArrayList<>();
            for (Object id : (Iterable<?>) inv.getArgument(0)) {
                if (stubbedQuestions.containsKey(id)) found.add(stubbedQuestions.get(id));
            }
            return found;
        }).when(questionRepository).findAllById(any());
    }

    @SuppressWarnings("unchecked")
    private List<ExamQuestion> captureSavedExamQuestions(int expectedCount) {
        ArgumentCaptor<List<ExamQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(examQuestionRepository, org.mockito.Mockito.atLeastOnce()).saveAll(captor.capture());
        List<ExamQuestion> saved = captor.getAllValues().stream().flatMap(List::stream).toList();
        assertEquals(expectedCount, saved.size());
        return new ArrayList<>(saved);
    }

    private static ManagerExamForm examForm(String type, String level, String status) {
        return examForm(type, level, status, 3, 2, 1);
    }

    private static ManagerExamForm examForm(String type, String level, String status, int language, int reading, int listening) {
        ManagerExamForm form = new ManagerExamForm();
        form.setExamTitle("Đề thi thử");
        form.setExamType(type);
        form.setJlptLevel(level);
        form.setStatus(status);
        form.setLanguageQuestions(language);
        form.setReadingQuestions(reading);
        form.setListeningQuestions(listening);
        return form;
    }

    private static ExamQuestionForm questionForm(List<Long> ids, String section) {
        ExamQuestionForm form = new ExamQuestionForm();
        form.setQuestionIds(ids);
        form.setSection(section);
        return form;
    }

    /** Đề N5 chưa lưu số câu theo phần → dùng mặc định 38/5/24. */
    private static Exam exam(long id, String status) {
        return new Exam(id, "Đề thi thử", "JLPT_MOCK", "N5", 90, 67, 80, status, null);
    }

    private static Exam exam(long id, String status, int language, int reading, int listening) {
        Exam exam = exam(id, status);
        exam.setLanguageQuestions(language);
        exam.setReadingQuestions(reading);
        exam.setListeningQuestions(listening);
        return exam;
    }

    private static Question question(long id, String status) {
        Question q = Question.builder().content("Câu " + id).status(status).jlptLevel("N5").build();
        q.setId(id);
        return q;
    }

    private static Question question(long id, String status, String skillType) {
        Question q = question(id, status);
        q.setSkillType(skillType);
        q.setJlptLevel("N5");
        return q;
    }

    private static ExamQuestion link(long questionId, int order) {
        return ExamQuestion.builder()
                .id(new ExamQuestionId(EXAM_ID, questionId))
                .section("LANGUAGE")
                .orderIndex(order)
                .build();
    }

    private static QuestionImportService.ImportedRow row(int rowNumber, Question q, Map<String, String> values) {
        return new QuestionImportService.ImportedRow(rowNumber, q, values);
    }

    private static MockMultipartFile excelFile() {
        return new MockMultipartFile("file", "questions.xlsx", null, new byte[]{1});
    }
}
