package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.ExamQuestionForm;
import com.jlpp.modules.exam.dto.ManagerExamForm;
import com.jlpp.modules.exam.dto.SectionProgress;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.entity.ExamQuestion;
import com.jlpp.modules.exam.entity.ExamQuestionId;
import com.jlpp.modules.exam.repository.ExamQuestionRepository;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.exam.service.JlptExamStructure;
import com.jlpp.modules.exam.service.ManagerExamService;
import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.jlpp.modules.question.service.QuestionImportService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(examQuestionRepository, never()).save(any());
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
        when(examQuestionRepository.existsById(new ExamQuestionId(EXAM_ID, 10L))).thenReturn(true);

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
        verify(examQuestionRepository, never()).save(any());
    }

    @Test
    void addQuestionsRejectsUnknownSection() {
        stubExamExists();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionsToExam(EXAM_ID, questionForm(List.of(10L), "WRITING")));

        assertEquals("Section không hợp lệ.", ex.getMessage());
    }

    @Test
    void addQuestionsRejectsMissingQuestion() {
        stubExamExists();
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

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
        verify(examQuestionRepository, never()).save(any());
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

        service.removeQuestionFromExam(EXAM_ID, 10L);

        verify(examQuestionRepository).deleteById(id);
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
        when(questionRepository.findById(3L)).thenReturn(Optional.of(reading));
        when(questionRepository.findById(4L)).thenReturn(Optional.of(explicit));
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
        when(questionRepository.findById(3L)).thenReturn(Optional.of(q));
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
        verify(examQuestionRepository, never()).save(any());
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

    // ── helpers ──

    private void stubExamExists() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(EXAM_ID, "DRAFT")));
    }

    private void stubLinked(String section, long count) {
        when(examQuestionRepository.countByExamIdAndSection(EXAM_ID, section)).thenReturn(count);
    }

    private void stubQuestion(long id, String status) {
        when(questionRepository.findById(id)).thenReturn(Optional.of(question(id, status)));
    }

    private void stubQuestion(long id, String status, String skillType) {
        Question q = question(id, status);
        q.setSkillType(skillType);
        when(questionRepository.findById(id)).thenReturn(Optional.of(q));
    }

    private List<ExamQuestion> captureSavedExamQuestions(int expectedCount) {
        ArgumentCaptor<ExamQuestion> captor = ArgumentCaptor.forClass(ExamQuestion.class);
        verify(examQuestionRepository, times(expectedCount)).save(captor.capture());
        return new ArrayList<>(captor.getAllValues());
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
        Question q = Question.builder().content("Câu " + id).status(status).build();
        q.setId(id);
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
