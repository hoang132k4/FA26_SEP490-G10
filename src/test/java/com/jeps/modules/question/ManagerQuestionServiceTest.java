package com.jeps.modules.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeps.modules.question.dto.ManagerQuestionForm;
import com.jeps.modules.question.dto.QuestionListRow;
import com.jeps.modules.question.dto.QuestionUsageStats;
import com.jeps.modules.question.entity.Question;
import com.jeps.modules.question.repository.QuestionRepository;
import com.jeps.modules.question.service.ManagerQuestionService;
import com.jeps.modules.quiz.entity.QuestionOption;
import com.jeps.modules.quiz.repository.QuestionOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerQuestionServiceTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, ManagerQuestionService.PAGE_SIZE);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    private ManagerQuestionService service;

    @BeforeEach
    void setUp() {
        service = new ManagerQuestionService(questionRepository, questionOptionRepository, objectMapper);
    }

    // ── search ──

    @Test
    void searchAttachesBatchedExamAndQuizUsageCounts() {
        Question q1 = question(1L, "MULTI_CHOICE", "ACTIVE");
        Question q2 = question(2L, "MULTI_CHOICE", "ACTIVE");
        when(questionRepository.searchForManager("kanji", "", "N5", "", "", FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(q1, q2)));
        when(questionRepository.batchCountExamUsages(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(questionRepository.batchCountQuizUsages(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2, 5}));
        when(questionRepository.batchCountChildren(List.of(1L, 2L)))
                .thenReturn(List.of());

        List<QuestionListRow> rows = service.search(" kanji ", null, "N5", null, null, 0).getContent();

        assertEquals(2, rows.size());
        assertEquals(3L, rows.get(0).examCount());
        assertEquals(0L, rows.get(0).quizCount());
        assertEquals(0L, rows.get(1).examCount());
        assertEquals(5L, rows.get(1).quizCount());
        assertEquals(5L, rows.get(1).totalUsages());
    }

    @Test
    void searchReturnsRequestedPageWithTotalCount() {
        Pageable second = PageRequest.of(1, ManagerQuestionService.PAGE_SIZE);
        when(questionRepository.searchForManager("", "", "", "", "", second))
                .thenReturn(new PageImpl<>(List.of(question(21L, "MULTI_CHOICE", "ACTIVE")), second, 21));

        Page<QuestionListRow> page = service.search(null, null, null, null, null, 1);

        assertEquals(1, page.getNumber());
        assertEquals(21, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(21L, page.getContent().get(0).id());
    }

    @Test
    void searchClampsNegativePageToFirstPage() {
        when(questionRepository.searchForManager("", "", "", "", "", FIRST_PAGE)).thenReturn(Page.empty(FIRST_PAGE));

        assertEquals(0, service.search(null, null, null, null, null, -3).getNumber());
    }

    @Test
    void searchWithNoResultsSkipsUsageQueries() {
        when(questionRepository.searchForManager("", "", "", "", "", FIRST_PAGE)).thenReturn(new PageImpl<>(List.of()));

        List<QuestionListRow> rows = service.search(null, null, null, null, null, 0).getContent();

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
            "skillType,    WRITING, Loại kỹ năng không hợp lệ.",
            "skillType,    KANJI,   Loại kỹ năng không hợp lệ."
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
        form.setJlptLevel("N5");
        form.setSkillType("VOCABULARY");
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
    void findChildrenReturnsChildrenOrderedById() {
        List<Question> children = List.of(question(11L, "MULTI_CHOICE", "ACTIVE"), question(12L, "MULTI_CHOICE", "ACTIVE"));
        when(questionRepository.findByParentIdOrderById(10L)).thenReturn(children);

        assertEquals(children, service.findChildren(10L));
    }

    @Test
    void toFormIgnoresOptionWithUnknownKey() {
        Question q = question(1L, "MULTI_CHOICE", "ACTIVE");
        q.setOptions("[{\"key\":\"A\",\"text\":\"あ\"},{\"key\":\"E\",\"text\":\"lạ\"},{\"text\":\"không có key\"}]");

        ManagerQuestionForm form = service.toForm(q);

        assertEquals("あ", form.getOptionA());
        assertNull(form.getOptionB());
        assertNull(form.getOptionC());
        assertNull(form.getOptionD());
    }

    @Test
    void createWithNullContent_throwsForNonListeningPassage() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(choice(f -> f.setContent(null))));
    }

    @Test
    void optionsExposeEnumValues() {
        ManagerQuestionService.ManagerQuestionOptions options = service.options();

        assertEquals(List.of("MULTI_CHOICE", "PASSAGE"), options.questionTypes());
        assertEquals(List.of("DRAFT", "ACTIVE", "ARCHIVED"), options.statuses());
        assertEquals(List.of("N5", "N4", "N3", "N2", "N1"), options.jlptLevels());
        assertEquals(List.of("VOCABULARY", "GRAMMAR", "LISTENING", "READING"), options.skillTypes());
    }

    // ── search: additional ──

    @Test
    void search_nullSearchParam_normalizesToEmpty() {
        when(questionRepository.searchForManager("", "", "", "", "", FIRST_PAGE)).thenReturn(new PageImpl<>(List.of()));

        List<QuestionListRow> rows = service.search(null, null, null, null, null, 0).getContent();

        assertTrue(rows.isEmpty());
        verify(questionRepository).searchForManager("", "", "", "", "", FIRST_PAGE);
    }

    @Test
    void search_singleResult_childCountIncluded() {
        Question q = question(1L, "PASSAGE", "ACTIVE");
        when(questionRepository.searchForManager("", "", "", "", "", FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(q)));
        when(questionRepository.batchCountExamUsages(List.of(1L))).thenReturn(List.of());
        when(questionRepository.batchCountQuizUsages(List.of(1L))).thenReturn(List.of());
        when(questionRepository.batchCountChildren(List.of(1L))).thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));

        List<QuestionListRow> rows = service.search(null, null, null, null, null, 0).getContent();

        assertEquals(1, rows.size());
        assertEquals(3L, rows.get(0).childCount());
    }

    @Test
    void search_questionNotInBatchMaps_returnsZeroCounts() {
        Question q = question(5L, "MULTI_CHOICE", "ACTIVE");
        when(questionRepository.searchForManager("", "", "", "", "", FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(q)));
        when(questionRepository.batchCountExamUsages(List.of(5L))).thenReturn(List.of());
        when(questionRepository.batchCountQuizUsages(List.of(5L))).thenReturn(List.of());
        when(questionRepository.batchCountChildren(List.of(5L))).thenReturn(List.of());

        List<QuestionListRow> rows = service.search(null, null, null, null, null, 0).getContent();

        assertEquals(0L, rows.get(0).examCount());
        assertEquals(0L, rows.get(0).quizCount());
        assertEquals(0L, rows.get(0).childCount());
    }

    // ── create: additional ──

    @Test
    void create_multiChoice_optionCDPresent_threeOptionsStored() throws Exception {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setOptionC("C");

        Question saved = service.create(form);

        assertEquals(3, readOptions(saved.getOptions()).size());
    }

    @Test
    void create_contentWithLeadingTrailingWhitespace_trimmed() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setContent("  Nội dung có khoảng trắng  ");

        Question saved = service.create(form);

        assertEquals("Nội dung có khoảng trắng", saved.getContent());
    }

    @Test
    void create_audioUrlBlank_storedAsNull() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setAudioUrl("   ");

        Question saved = service.create(form);

        assertNull(saved.getAudioUrl());
    }

    @Test
    void create_audioUrlPresent_storedTrimmed() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setAudioUrl("  /uploads/audio.mp3  ");

        Question saved = service.create(form);

        assertEquals("/uploads/audio.mp3", saved.getAudioUrl());
    }

    @Test
    void create_explanationPresent_storedTrimmed() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setExplanation("  Giải thích  ");

        Question saved = service.create(form);

        assertEquals("Giải thích", saved.getExplanation());
    }

    @Test
    void create_explanationBlank_storedAsNull() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setExplanation("  ");

        Question saved = service.create(form);

        assertNull(saved.getExplanation());
    }

    @Test
    void create_nullStatus_throwsInvalidStatus() {
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setStatus(null);

        assertRejected(form, "Trạng thái không hợp lệ.");
    }

    @Test
    void create_answerKeyD_withOptionDFilled_accepted() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("D"));
        form.setOptionC("C");
        form.setOptionD("D");

        Question saved = service.create(form);

        assertNotNull(saved.getAnswerKey());
    }

    @Test
    void create_answerKeyC_withOptionCBlank_rejected() {
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("C"));

        assertRejected(form, "Đáp án đúng 'C' không có trong danh sách đáp án.");
    }

    // ── update: additional ──

    @Test
    void update_draftToActive_allowed() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setStatus("ACTIVE");
        form.setJlptLevel("N5");
        form.setSkillType("GRAMMAR");

        Question saved = service.update(1L, form);

        assertEquals("ACTIVE", saved.getStatus());
    }

    @Test
    void update_changeContentAndOptions_savedCorrectly() throws Exception {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);
        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("B"));
        form.setContent("Nội dung mới");
        form.setOptionA("Đáp án mới A");
        form.setOptionB("Đáp án mới B");

        Question saved = service.update(1L, form);

        assertEquals("Nội dung mới", saved.getContent());
        List<Map<String, String>> opts = readOptions(saved.getOptions());
        assertEquals("Đáp án mới A", opts.get(0).get("text"));
        assertEquals("Đáp án mới B", opts.get(1).get("text"));
    }

    // ── delete: additional ──

    @Test
    void delete_passageWithMultipleChildren_allUnused_deletesAll() {
        Question passage = question(1L, "PASSAGE", "DRAFT");
        List<Question> children = List.of(
                question(2L, "MULTI_CHOICE", "DRAFT"),
                question(3L, "MULTI_CHOICE", "DRAFT"),
                question(4L, "MULTI_CHOICE", "DRAFT"));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(passage));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L)).thenReturn(children);
        when(questionRepository.isUsedAnywhere(2L)).thenReturn(false);
        when(questionRepository.isUsedAnywhere(3L)).thenReturn(false);
        when(questionRepository.isUsedAnywhere(4L)).thenReturn(false);

        service.delete(1L);

        verify(questionRepository).deleteAll(children);
        verify(questionRepository).delete(passage);
    }

    @Test
    void delete_passageWithSecondChildUsed_blocked() {
        Question passage = question(1L, "PASSAGE", "ACTIVE");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(passage));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L))
                .thenReturn(List.of(question(2L, "MULTI_CHOICE", "ACTIVE"), question(3L, "MULTI_CHOICE", "ACTIVE")));
        when(questionRepository.isUsedAnywhere(2L)).thenReturn(false);
        when(questionRepository.isUsedAnywhere(3L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("câu hỏi con #3 đang được sử dụng"));
    }

    // ── toForm: additional ──

    @Test
    void toForm_preservesAudioUrlAndExplanation() {
        Question q = question(1L, "MULTI_CHOICE", "ACTIVE");
        q.setAudioUrl("/uploads/audio.mp3");
        q.setExplanation("Giải thích đáp án.");

        ManagerQuestionForm form = service.toForm(q);

        assertEquals("/uploads/audio.mp3", form.getAudioUrl());
        assertEquals("Giải thích đáp án.", form.getExplanation());
    }

    // ── Fix 1: inherit audio from parent ──

    @Test
    void create_childInheritsAudioFromParent() {
        Question parent = question(10L, "PASSAGE", "ACTIVE");
        parent.setAudioUrl("https://example.com/audio.mp3");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(20L);
            return q;
        });
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(20L)).thenReturn(List.of());

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setParentId(10L);

        Question saved = service.create(form);

        assertEquals("https://example.com/audio.mp3", saved.getAudioUrl());
    }

    @Test
    void create_childKeepsOwnAudioIfAlreadySet() {
        Question parent = question(10L, "PASSAGE", "ACTIVE");
        parent.setAudioUrl("https://example.com/parent.mp3");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(20L);
            return q;
        });
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(20L)).thenReturn(List.of());

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setParentId(10L);
        form.setAudioUrl("https://example.com/child.mp3");

        Question saved = service.create(form);

        assertEquals("https://example.com/child.mp3", saved.getAudioUrl());
    }

    @Test
    void create_childDoesNotInheritWhenParentHasNoAudio() {
        Question parent = question(10L, "PASSAGE", "ACTIVE");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(20L);
            return q;
        });
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(20L)).thenReturn(List.of());

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setParentId(10L);

        Question saved = service.create(form);

        assertNull(saved.getAudioUrl());
    }

    @Test
    void create_childDoesNotInheritWhenParentAudioIsBlank() {
        Question parent = question(10L, "PASSAGE", "ACTIVE");
        parent.setAudioUrl("   ");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(20L);
            return q;
        });
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(20L)).thenReturn(List.of());

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setParentId(10L);

        Question saved = service.create(form);

        assertNull(saved.getAudioUrl());
    }

    // ── Fix 2: sync question_options ──

    @Test
    void create_syncsToQuestionOptionsTable() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(1L)).thenReturn(List.of());

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));
        form.setOptionC("C");
        service.create(form);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuestionOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(questionOptionRepository).saveAll(captor.capture());
        List<QuestionOption> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertEquals("A", saved.get(0).getLabel());
        assertTrue(saved.get(0).getIsCorrect());
        assertEquals("B", saved.get(1).getLabel());
        assertEquals(false, saved.get(1).getIsCorrect());
        assertEquals("C", saved.get(2).getLabel());
    }

    @Test
    void create_passage_doesNotSyncQuestionOptions() {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });

        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType("PASSAGE");
        form.setContent("Đoạn văn");
        form.setStatus("DRAFT");
        service.create(form);

        verify(questionOptionRepository, never()).saveAll(anyList());
    }

    // ── sync on update ──

    @Test
    void update_syncsQuestionOptionsTable() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);

        QuestionOption oldA = new QuestionOption();
        oldA.setQuestionId(1L);
        oldA.setLabel("A");
        oldA.setOptionText("Old A");
        oldA.setIsCorrect(true);
        QuestionOption oldB = new QuestionOption();
        oldB.setQuestionId(1L);
        oldB.setLabel("B");
        oldB.setOptionText("Old B");
        oldB.setIsCorrect(false);
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(1L)).thenReturn(List.of(oldA, oldB));

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("B"));
        form.setOptionA("New A");
        form.setOptionB("New B");

        service.update(1L, form);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuestionOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(questionOptionRepository).saveAll(captor.capture());
        List<QuestionOption> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("New A", saved.get(0).getOptionText());
        assertEquals(false, saved.get(0).getIsCorrect());
        assertEquals("New B", saved.get(1).getOptionText());
        assertTrue(saved.get(1).getIsCorrect());
    }

    @Test
    void update_removesStaleOptionsFromQuestionOptionsTable() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);

        QuestionOption oldA = new QuestionOption();
        oldA.setLabel("A");
        QuestionOption oldB = new QuestionOption();
        oldB.setLabel("B");
        QuestionOption oldC = new QuestionOption();
        oldC.setLabel("C");
        when(questionOptionRepository.findByQuestionIdOrderByLabelAsc(1L)).thenReturn(List.of(oldA, oldB, oldC));

        ManagerQuestionForm form = choiceForm("MULTI_CHOICE", List.of("A"));

        service.update(1L, form);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuestionOption>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(questionOptionRepository).deleteAll(deleteCaptor.capture());
        assertEquals(1, deleteCaptor.getValue().size());
        assertEquals("C", deleteCaptor.getValue().get(0).getLabel());
    }

    // ── delete cleans up question_options ──

    @Test
    void delete_cleansUpQuestionOptionsForSingleQuestion() {
        Question q = question(1L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L)).thenReturn(List.of());

        service.delete(1L);

        verify(questionOptionRepository).deleteByQuestionId(1L);
        verify(questionRepository).delete(q);
    }

    @Test
    void delete_passageCleansUpQuestionOptionsForChildrenAndParent() {
        Question passage = question(1L, "PASSAGE", "DRAFT");
        Question child1 = question(2L, "MULTI_CHOICE", "DRAFT");
        Question child2 = question(3L, "MULTI_CHOICE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(passage));
        when(questionRepository.isUsedAnywhere(1L)).thenReturn(false);
        when(questionRepository.findByParentIdOrderById(1L)).thenReturn(List.of(child1, child2));
        when(questionRepository.isUsedAnywhere(2L)).thenReturn(false);
        when(questionRepository.isUsedAnywhere(3L)).thenReturn(false);

        service.delete(1L);

        verify(questionOptionRepository).deleteByQuestionIdIn(List.of(2L, 3L));
        verify(questionOptionRepository).deleteByQuestionId(1L);
    }

    // ── status rules ──

    @Test
    void draftingQuestionUsedByActiveExamIsBlocked() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, "MULTI_CHOICE", "ACTIVE")));
        when(questionRepository.countActiveExamUsages(1L)).thenReturn(1L);
        when(questionRepository.countActiveQuizUsages(1L)).thenReturn(0L);

        assertRejected(choice(f -> f.setStatus("DRAFT")),
                "Không thể chuyển sang DRAFT: câu hỏi đang được sử dụng trong 1 đề thi đang hoạt động. Hãy gỡ câu hỏi hoặc chuyển đề thi/quiz sang trạng thái khác trước.",
                true);
    }

    @Test
    void bulkDraft_blockedWhenQuestionUsedInActiveQuiz() {
        when(questionRepository.findChildIdsByParentIds(List.of(1L, 2L))).thenReturn(List.of());
        when(questionRepository.findIdsUsedInActiveExams(List.of(1L, 2L))).thenReturn(List.of());
        when(questionRepository.findIdsUsedInActiveQuizzes(List.of(1L, 2L))).thenReturn(List.of(2L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.bulkUpdateStatus(List.of(1L, 2L), "DRAFT"));

        assertEquals("Câu hỏi #2 đang được sử dụng trong đề thi/quiz đang hoạt động. Không thể chuyển sang DRAFT.", ex.getMessage());
        verify(questionRepository, never()).bulkSetStatus(anyList(), anyString());
    }

    @Test
    void activeListeningPassageWithoutAudio_rejected() {
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType("PASSAGE");
        form.setJlptLevel("N5");
        form.setSkillType("LISTENING");
        form.setStatus("ACTIVE");

        assertRejected(form, "Bài nghe ACTIVE phải có file audio.");
    }

    @Test
    void passageToActive_blockedWhenChildIncomplete() {
        Question passage = question(1L, "PASSAGE", "DRAFT");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(passage));
        when(questionRepository.findByParentIdOrderById(1L)).thenReturn(List.of(question(2L, "MULTI_CHOICE", "DRAFT")));
        when(questionRepository.findIdsWithIncompleteInfo(List.of(2L))).thenReturn(List.of(2L));
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType("PASSAGE");
        form.setContent("Đoạn văn");
        form.setJlptLevel("N5");
        form.setSkillType("READING");
        form.setStatus("ACTIVE");

        assertRejected(form, "Câu hỏi con #2 chưa đủ cấp độ, kỹ năng hoặc đáp án nên không thể chuyển sang ACTIVE.", true);
    }

    // ── edits of questions already used in exams ──

    @Test
    void updateRejectsChangingQuestionType() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, "MULTI_CHOICE", "DRAFT")));
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType("PASSAGE");
        form.setContent("Đoạn văn");
        form.setStatus("DRAFT");

        assertRejected(form, "Không thể đổi loại câu hỏi sau khi đã tạo.", true);
    }

    @Test
    void updateRejectsLevelChangeOfQuestionUsedInExam() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        existing.setJlptLevel("N5");
        existing.setSkillType("VOCABULARY");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.countExamUsages(1L)).thenReturn(1L);

        assertRejected(choice(f -> { f.setJlptLevel("N1"); f.setSkillType("VOCABULARY"); }),
                "Câu hỏi đang nằm trong đề thi nên không đổi được cấp độ hoặc phần thi (Ngôn ngữ/Đọc/Nghe). Hãy gỡ khỏi đề trước.", true);
    }

    @Test
    void updateRejectsSkillChangeThatMovesQuestionToAnotherExamSection() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        existing.setJlptLevel("N5");
        existing.setSkillType("VOCABULARY");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.countExamUsages(1L)).thenReturn(2L);

        assertRejected(choice(f -> { f.setJlptLevel("N5"); f.setSkillType("READING"); }),
                "Câu hỏi đang nằm trong đề thi nên không đổi được cấp độ hoặc phần thi (Ngôn ngữ/Đọc/Nghe). Hãy gỡ khỏi đề trước.", true);
    }

    @Test
    void updateAllowsSkillChangeWithinTheSameExamSection() {
        Question existing = question(1L, "MULTI_CHOICE", "DRAFT");
        existing.setJlptLevel("N5");
        existing.setSkillType("VOCABULARY");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(existing)).thenReturn(existing);

        Question saved = service.update(1L, choice(f -> { f.setJlptLevel("N5"); f.setSkillType("GRAMMAR"); }));

        assertEquals("GRAMMAR", saved.getSkillType());
        verify(questionRepository, never()).countExamUsages(anyLong());
    }

    @Test
    void updatingPassageAudioCopiesItToChildren() {
        Question passage = question(1L, "PASSAGE", "DRAFT");
        passage.setSkillType("LISTENING");
        passage.setAudioUrl("/uploads/old.mp3");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(passage));
        when(questionRepository.save(passage)).thenReturn(passage);
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType("PASSAGE");
        form.setSkillType("LISTENING");
        form.setStatus("DRAFT");
        form.setAudioUrl("/uploads/new.mp3");

        service.update(1L, form);

        verify(questionRepository).copyAudioToChildren(1L, "/uploads/new.mp3", "/uploads/old.mp3");
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
