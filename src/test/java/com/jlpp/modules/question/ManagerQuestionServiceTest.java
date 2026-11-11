package com.jlpp.modules.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.dto.QuestionListRow;
import com.jlpp.modules.question.dto.QuestionUsageStats;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.jlpp.modules.question.service.ManagerQuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerQuestionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QuestionRepository questionRepository;

    private ManagerQuestionService service;

    @BeforeEach
    void setUp() {
        service = new ManagerQuestionService(questionRepository, objectMapper);
    }

    // ── search ──

    @Test
    void searchAttachesBatchedExamAndQuizUsageCounts() {
        Question q1 = question(1L, "MULTI_CHOICE", "ACTIVE");
        Question q2 = question(2L, "MULTI_CHOICE", "ACTIVE");
        when(questionRepository.searchForManager("kanji", "", "N5", "", ""))
                .thenReturn(List.of(q1, q2));
        when(questionRepository.batchCountExamUsages(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(questionRepository.batchCountQuizUsages(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2, 5}));
        when(questionRepository.batchCountChildren(List.of(1L, 2L)))
                .thenReturn(List.of());

        List<QuestionListRow> rows = service.search(" kanji ", null, "N5", null, null);

        assertEquals(2, rows.size());
        assertEquals(3L, rows.get(0).examCount());
        assertEquals(0L, rows.get(0).quizCount());
        assertEquals(0L, rows.get(1).examCount());
        assertEquals(5L, rows.get(1).quizCount());
        assertEquals(5L, rows.get(1).totalUsages());
    }

    @Test
    void searchWithNoResultsSkipsUsageQueries() {
        when(questionRepository.searchForManager("", "", "", "", "")).thenReturn(List.of());

        List<QuestionListRow> rows = service.search(null, null, null, null, null);

        assertTrue(rows.isEmpty());
        verify(questionRepository, never()).batchCountExamUsages(anyList());
        verify(questionRepository, never()).batchCountQuizUsages(anyList());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.findById(999L));
        assertEquals("Không tìm thấy câu hỏi.", ex.getMessage());
    }

    // ── create ──

    @Test
    void createSingleChoiceStoresTrimmedOptionsAndAnswerKeyAsJson() throws Exception {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("B"));
        form.setContent("  今日は何曜日ですか。 ");
        form.setOptionA(" 月曜日 ");
        form.setOptionB("火曜日");
        form.setOptionC("   ");
        form.setJlptLevel("N5");

        Question saved = service.create(form);

        assertEquals("今日は何曜日ですか。", saved.getContent());
        assertEquals("N5", saved.getJlptLevel());
        assertEquals(List.of(
                Map.of("key", "A", "text", "月曜日"),
                Map.of("key", "B", "text", "火曜日")), readOptions(saved.getOptions()));
        assertEquals(List.of("B"), objectMapper.readValue(saved.getAnswerKey(), new TypeReference<List<String>>() {}));
    }

    @Test
    void createMultiChoiceWithFourOptionsAndOneCorrectAnswer() throws Exception {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("D"));
        form.setOptionC("C");
        form.setOptionD("D");

        Question saved = service.create(form);

        assertEquals(4, readOptions(saved.getOptions()).size());
        assertEquals(List.of("D"), objectMapper.readValue(saved.getAnswerKey(), new TypeReference<List<String>>() {}));
    }

    @Test
    void createPassageDropsOptionsAndAnswerKey() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("PASSAGE", List.of("A"));

        Question saved = service.create(form);

        assertEquals("PASSAGE", saved.getQuestionType());
        assertNull(saved.getOptions());
        assertNull(saved.getAnswerKey());
    }

    @Test
    void createPassageDoesNotRequireOptions() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType("PASSAGE");
        form.setContent("Đoạn văn");

        service.create(form);

        verify(questionRepository).save(any(Question.class));
    }

    @ParameterizedTest
    @CsvSource({
            "questionType, INVALID, Loại câu hỏi không hợp lệ.",
            "status,       DELETED, Trạng thái không hợp lệ.",
            "jlptLevel,    N9,      Cấp độ JLPT không hợp lệ.",
            "skillType,    WRITING, Loại kỹ năng không hợp lệ."
    })
    void createRejectsValuesOutsideEnums(String field, String value, String message) {
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        switch (field) {
            case "questionType" -> form.setQuestionType(value);
            case "status" -> form.setStatus(value);
            case "jlptLevel" -> form.setJlptLevel(value);
            case "skillType" -> form.setSkillType(value);
            default -> throw new IllegalStateException(field);
        }

        assertRejected(form, message);
    }

    @Test
    void createRejectsMissingQuestionType() {
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setQuestionType(null);

        assertRejected(form, "Loại câu hỏi không hợp lệ.");
    }

    @Test
    void createRejectsWhenOptionAOrBMissing() {
        assertRejected(choice(f -> f.setOptionA(null)), "Cần ít nhất đáp án A và B.");
        assertRejected(choice(f -> f.setOptionB("  ")), "Cần ít nhất đáp án A và B.");
    }

    @Test
    void createRejectsWhenNoCorrectAnswerChosen() {
        assertRejected(choice(f -> f.setAnswerKeys(null)), "Vui lòng chọn đáp án đúng.");
        assertRejected(choice(f -> f.setAnswerKeys(List.of())), "Vui lòng chọn đáp án đúng.");
    }

    @Test
    void createRejectsCorrectAnswerPointingToEmptyOption() {
        assertRejected(choice(f -> f.setAnswerKeys(List.of("C"))),
                "Đáp án đúng 'C' không có trong danh sách đáp án.");
    }

    @Test
    void createRejectsMultipleCorrectAnswers() {
        assertRejected(choice(f -> f.setAnswerKeys(List.of("A", "B"))),
                "Câu hỏi trắc nghiệm chỉ được chọn 1 đáp án đúng.");
    }

    @Test
    void createAllowsBlankOptionalEnumsAndStoresThemAsNull() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setJlptLevel("");
        form.setSkillType(" ");

        Question saved = service.create(form);

        assertNull(saved.getJlptLevel());
        assertNull(saved.getSkillType());
    }

    // ── parent / child ──

    @Test
    void createChildQuestionUnderPassage() {
        when(questionRepository.findById(50L)).thenReturn(Optional.of(question(50L, "PASSAGE", "ACTIVE")));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question saved = service.create(choice(f -> f.setParentId(50L)));

        assertEquals(50L, saved.getParentId());
    }

    @Test
    void createRejectsPassageAsChild() {
        ManagerQuestionForm form = choiceForm("PASSAGE", null);
        form.setParentId(50L);

        assertRejected(form, "Câu hỏi con không được là loại PASSAGE.");
        verify(questionRepository, never()).findById(anyLong());
    }

    @Test
    void createRejectsParentThatIsNotPassage() {
        when(questionRepository.findById(50L)).thenReturn(Optional.of(question(50L, "MULTI_CHOICE", "ACTIVE")));

        assertRejected(choice(f -> f.setParentId(50L)), "Câu hỏi cha phải là loại PASSAGE.");
    }

    @Test
    void createRejectsMissingParent() {
        when(questionRepository.findById(50L)).thenReturn(Optional.empty());

        assertRejected(choice(f -> f.setParentId(50L)), "Câu hỏi cha phải là loại PASSAGE.");
    }

    // ── update / status transition ──

    @Test
    void updateAppliesFormToExistingQuestion() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setStatus("ACTIVE");
        form.setContent("Nội dung mới");

        Question saved = service.update(1L, form);

        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("Nội dung mới", saved.getContent());
    }

    @Test
    void updateThrowsWhenQuestionMissing() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.update(999L, choiceForm("MULTI_CHOICE", List.of("A"))));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void archivingUnusedQuestionIsAllowed() {
        Question existing = question(1L, "MULTI_CHOICE", "ACTIVE");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.countActiveExamUsages(1L)).thenReturn(0L);
        when(questionRepository.countActiveQuizUsages(1L)).thenReturn(0L);
        when(questionRepository.save(existing)).thenReturn(existing);

        Question saved = service.update(1L, choice(f -> f.setStatus("ARCHIVED")));

        assertEquals("ARCHIVED", saved.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "2, 0, Không thể chuyển sang ARCHIVED: câu hỏi đang được sử dụng trong 2 đề thi đang hoạt động. Hãy gỡ câu hỏi hoặc chuyển đề thi/quiz sang trạng thái khác trước.",
            "0, 3, Không thể chuyển sang ARCHIVED: câu hỏi đang được sử dụng trong 3 quiz đang hoạt động. Hãy gỡ câu hỏi hoặc chuyển đề thi/quiz sang trạng thái khác trước.",
            "1, 2, Không thể chuyển sang ARCHIVED: câu hỏi đang được sử dụng trong 1 đề thi đang hoạt động và 2 quiz đang hoạt động. Hãy gỡ câu hỏi hoặc chuyển đề thi/quiz sang trạng thái khác trước."
    })
    void archivingQuestionUsedByActiveExamOrQuizIsBlocked(long activeExams, long activeQuizzes, String message) {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, "MULTI_CHOICE", "ACTIVE")));
        when(questionRepository.countActiveExamUsages(1L)).thenReturn(activeExams);
        when(questionRepository.countActiveQuizUsages(1L)).thenReturn(activeQuizzes);

        assertRejected(choice(f -> f.setStatus("ARCHIVED")), message, true);
    }

    @Test
    void updatingAlreadyArchivedQuestionSkipsUsageCheck() {
        Question existing = question(1L, "MULTI_CHOICE", "ARCHIVED");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);

        service.update(1L, choice(f -> f.setStatus("ARCHIVED")));

        verify(questionRepository, never()).countActiveExamUsages(anyLong());
        verify(questionRepository, never()).countActiveQuizUsages(anyLong());
    }

    // ── delete ──

    @Test
    void deleteUnusedQuestionWithoutChildren() {
        Question q = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L)).thenReturn(List.of());

        service.delete(1L);

        verify(questionRepository).delete(q);
        verify(questionRepository, never()).deleteAll(anyList());
    }

    @Test
    void deleteBlockedWhenQuestionIsUsed() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, "MULTI_CHOICE", "ACTIVE")));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));

        assertTrue(ex.getMessage().startsWith("Không thể xóa câu hỏi đang được sử dụng"));
        verify(questionRepository, never()).delete(any(Question.class));
    }

    @Test
    void deletePassageRemovesChildrenBeforeParent() {
        Question passage = question(1L, "PASSAGE", "DRAFT");
        List<Question> children = List.of(question(2L, "MULTI_CHOICE", "DRAFT"), question(3L, "MULTI_CHOICE", "DRAFT"));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(passage));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L)).thenReturn(children);
        when(questionRepository.isUsedAnywhere(2L)).thenReturn(false);
        when(questionRepository.isUsedAnywhere(3L)).thenReturn(false);

        service.delete(1L);

        InOrder order = inOrder(questionRepository);
        order.verify(questionRepository).deleteAll(children);
        order.verify(questionRepository).delete(passage);
    }

    @Test
    void deletePassageBlockedWhenAnyChildIsUsed() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, "PASSAGE", "ACTIVE")));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L))
                .thenReturn(List.of(question(2L, "MULTI_CHOICE", "ACTIVE")));
        when(questionRepository.isUsedAnywhere(2L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));

        assertTrue(ex.getMessage().contains("câu hỏi con #2 đang được sử dụng"));
        verify(questionRepository, never()).deleteAll(anyList());
        verify(questionRepository, never()).delete(any(Question.class));
    }

    @Test
    void deleteThrowsWhenQuestionMissing() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(999L));
    }

    // ── toForm / stats / options ──

    @Test
    void toFormRestoresOptionsAndAnswerKeysFromJson() {
        Question q = question(1L, "MULTI_CHOICE", "ACTIVE");
        q.setOptions("[{\"key\":\"A\",\"text\":\"一\"},{\"key\":\"B\",\"text\":\"二\"},{\"key\":\"D\",\"text\":\"四\"}]");
        q.setAnswerKey("[\"A\",\"D\"]");
        q.setParentId(9L);

        ManagerQuestionForm form = service.toForm(q);

        assertEquals(1L, form.getId());
        assertEquals("一", form.getOptionA());
        assertEquals("二", form.getOptionB());
        assertNull(form.getOptionC());
        assertEquals("四", form.getOptionD());
        assertEquals(List.of("A", "D"), form.getAnswerKeys());
        assertEquals(9L, form.getParentId());
    }

    @Test
    void toFormToleratesMalformedJson() {
        Question q = question(1L, "MULTI_CHOICE", "ACTIVE");
        q.setOptions("not json");
        q.setAnswerKey("{broken");

        ManagerQuestionForm form = service.toForm(q);

        assertNull(form.getOptionA());
        assertEquals(List.of(), form.getAnswerKeys());
    }

    @Test
    void toFormOfPassageLeavesOptionsEmpty() {
        ManagerQuestionForm form = service.toForm(question(1L, "PASSAGE", "ACTIVE"));

        assertNull(form.getOptionA());
        assertNull(form.getAnswerKeys());
    }

    @Test
    void savedQuestionRoundTripsThroughToForm() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm original = choiceForm("MULTI_CHOICE", List.of("B"));
        original.setOptionC("C");

        ManagerQuestionForm restored = service.toForm(service.create(original));

        assertEquals("A", restored.getOptionA());
        assertEquals("B", restored.getOptionB());
        assertEquals("C", restored.getOptionC());
        assertNull(restored.getOptionD());
        assertEquals(List.of("B"), restored.getAnswerKeys());
    }

    @Test
    void usageStatsCombinesFourCounts() {
        when(questionRepository.countExamUsages(1L)).thenReturn(4L);
        when(questionRepository.countQuizUsages(1L)).thenReturn(2L);
        when(questionRepository.countActiveExamUsages(1L)).thenReturn(1L);
        when(questionRepository.countActiveQuizUsages(1L)).thenReturn(0L);

        QuestionUsageStats stats = service.getUsageStats(1L);

        assertEquals(new QuestionUsageStats(4, 2, 1, 0), stats);
        assertEquals(6, stats.totalUsages());
    }

    @Test
    void optionsExposeEnumValues() {
        ManagerQuestionService.ManagerQuestionOptions options = service.options();

        assertEquals(List.of("MULTI_CHOICE", "PASSAGE"), options.questionTypes());
        assertEquals(List.of("DRAFT", "ACTIVE", "ARCHIVED"), options.statuses());
        assertEquals(List.of("N5", "N4", "N3", "N2", "N1"), options.jlptLevels());
    }

    // ── helpers ──

    private void assertRejected(ManagerQuestionForm form, String message) {
        assertRejected(form, message, false);
    }

    private void assertRejected(ManagerQuestionForm form, String message, boolean update) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (update) service.update(1L, form);
                    else service.create(form);
                });
        assertEquals(message, ex.getMessage());
        verify(questionRepository, never()).save(any());
    }

    private List<Map<String, String>> readOptions(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private static ManagerQuestionForm choice(Consumer<ManagerQuestionForm> customizer) {
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        customizer.accept(form);
        return form;
    }

    private static ManagerQuestionForm choiceForm(String type, List<String> answers) {
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType(type);
        form.setContent("Câu hỏi");
        form.setStatus("DRAFT");
        form.setOptionA("A");
        form.setOptionB("B");
        form.setAnswerKeys(answers);
        return form;
    }

    private static Question question(long id, String type, String status) {
        Question q = Question.builder().content("Câu " + id).questionType(type).status(status).build();
        q.setId(id);
        return q;
    }
}
