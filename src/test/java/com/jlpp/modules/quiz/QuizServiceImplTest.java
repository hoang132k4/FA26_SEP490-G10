package com.jlpp.modules.quiz;

import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.dto.QuizResultDto;
import com.jlpp.modules.quiz.entity.*;
import com.jlpp.modules.quiz.repository.*;
import com.jlpp.modules.quiz.service.QuizServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceImplTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private QuizQuestionRepository questionRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private ExerciseQuestionRepository exerciseQuestionRepository;
    @Mock private ExerciseResultRepository exerciseResultRepository;
    @Mock private ExerciseResultDetailRepository exerciseResultDetailRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private QuestionBankRepository questionBankRepository;

    @InjectMocks
    private QuizServiceImpl quizService;

    private Exercise exercise;

    @BeforeEach
    void setUp() {
        exercise = Exercise.builder()
                .exerciseId(1L)
                .lessonId(100L)
                .exerciseTitle("Bài tập trắc nghiệm: Hiragana")
                .totalQuestions(2)
                .passingScore(60)
                .status("PUBLISHED")
                .build();
    }

    // ── getOrCreateExerciseForLesson ──

    @Test
    @DisplayName("getOrCreateExerciseForLesson - null lessonId ném IllegalArgumentException")
    void getOrCreate_nullLessonId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> quizService.getOrCreateExerciseForLesson(null));
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson - exercise đã tồn tại và có câu hỏi → trả về luôn")
    void getOrCreate_existingWithQuestions_returnsImmediately() {
        when(exerciseRepository.findByLessonId(100L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(new ExerciseQuestion(1L, 10L, 1)));

        Exercise result = quizService.getOrCreateExerciseForLesson(100L);

        assertEquals(1L, result.getExerciseId());
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson - exercise tồn tại nhưng chưa có câu hỏi → seed")
    void getOrCreate_existingWithoutQuestions_seeds() {
        when(exerciseRepository.findByLessonId(100L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L))
                .thenReturn(Collections.emptyList());
        // seed calls findByLessonId again internally
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());
        when(exerciseRepository.findByLessonId(100L)).thenReturn(Optional.of(exercise));
        // seed finds exercise already exists with no questions, creates seeds
        when(questionRepository.save(any())).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(99L);
            return q;
        });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Exercise result = quizService.getOrCreateExerciseForLesson(100L);

        assertEquals(1L, result.getExerciseId());
    }

    // ── getExercise ──

    @Test
    @DisplayName("getExercise - id tồn tại → trả về exercise")
    void getExercise_found() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));

        Exercise result = quizService.getExercise(1L);

        assertEquals("Bài tập trắc nghiệm: Hiragana", result.getExerciseTitle());
    }

    @Test
    @DisplayName("getExercise - id không tồn tại → 404")
    void getExercise_notFound_throws404() {
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> quizService.getExercise(999L));
        assertEquals(404, ex.getStatusCode().value());
    }

    // ── getQuestionsForExercise ──

    @Test
    @DisplayName("getQuestionsForExercise - không có câu hỏi → empty list")
    void getQuestions_empty() {
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L))
                .thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getQuestionsForExercise - có câu hỏi với option → trả đúng DTO")
    void getQuestions_withQuestionAndOptions() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Nội dung?").category("Grammar").jlptLevel("N4").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đáp án A").isCorrect(true).build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals(1, result.size());
        assertEquals("Nội dung?", result.get(0).getContent());
        assertEquals(1, result.get(0).getOptions().size());
        assertEquals("A", result.get(0).getOptions().get(0).getLabel());
    }

    @Test
    @DisplayName("getQuestionsForExercise - question null nhưng có questionBank → fallback nội dung")
    void getQuestions_fallbackToQuestionBank() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        QuestionBank qb = QuestionBank.builder().questionId(10L).questionText("Từ bank").category("Vocab").jlptLevel("N3").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(List.of(qb));
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals(1, result.size());
        assertEquals("Từ bank", result.get(0).getContent());
    }

    // ── submitQuiz ──

    @Test
    @DisplayName("submitQuiz - exercise không tồn tại → ném exception")
    void submitQuiz_exerciseNotFound_throws() {
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> quizService.submitQuiz(999L, 1L, Map.of()));
    }

    @Test
    @DisplayName("submitQuiz - trả lời đúng hết → passed = true, percentage = 100")
    void submitQuiz_allCorrect_passes() {
        ExerciseQuestion eq1 = new ExerciseQuestion(1L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(1L, 11L, 2);
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        QuestionOption opt1 = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();
        QuestionOption opt2 = QuestionOption.builder().id(51L).questionId(11L).label("B").optionText("Đ2").isCorrect(true).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq1, eq2));
        when(questionRepository.findByIdIn(List.of(10L, 11L))).thenReturn(List.of(q1, q2));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L)))
                .thenReturn(List.of(opt1, opt2));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> {
            ExerciseResult r = inv.getArgument(0);
            r.setExerciseResultId(77L);
            return r;
        });
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Hiragana")));

        Map<Long, String> answers = Map.of(10L, "A", 11L, "B");
        QuizResultDto result = quizService.submitQuiz(1L, 5L, answers);

        assertTrue(result.isPassed());
        assertEquals(2, result.getCorrectCount());
        assertEquals(new BigDecimal("100.0"), result.getPercentage());
        assertEquals("PASSED", result.getResultStatus());
    }

    @Test
    @DisplayName("submitQuiz - trả lời sai hết → failed, percentage = 0")
    void submitQuiz_allWrong_fails() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption optCorrect = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đúng").isCorrect(true).build();
        QuestionOption optWrong = QuestionOption.builder().id(51L).questionId(10L).label("B").optionText("Sai").isCorrect(false).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L)))
                .thenReturn(List.of(optCorrect, optWrong));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> {
            ExerciseResult r = inv.getArgument(0);
            r.setExerciseResultId(78L);
            return r;
        });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "B"));

        assertFalse(result.isPassed());
        assertEquals(0, result.getCorrectCount());
        assertEquals("FAILED", result.getResultStatus());
    }

    @Test
    @DisplayName("submitQuiz - null answers map → tất cả unanswered")
    void submitQuiz_nullAnswers_allUnanswered() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L)))
                .thenReturn(List.of(opt));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, 5L)).thenReturn(2L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> {
            ExerciseResult r = inv.getArgument(0);
            r.setExerciseResultId(79L);
            return r;
        });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(1L, 5L, null);

        assertEquals(0, result.getCorrectCount());
        assertFalse(result.isPassed());
    }

    // ── getResult ──

    @Test
    @DisplayName("getResult - resultId không tồn tại → ném exception")
    void getResult_notFound_throws() {
        when(exerciseResultRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> quizService.getResult(999L));
    }

    @Test
    @DisplayName("getResult - kết quả tồn tại → trả đúng review data")
    void getResult_found_returnsFullDto() {
        ExerciseResult savedResult = ExerciseResult.builder()
                .exerciseResultId(77L)
                .exerciseId(1L)
                .userId(5L)
                .attemptNumber(1)
                .score(10)
                .percentage(new BigDecimal("100.0"))
                .resultStatus("PASSED")
                .feedback("Xuất sắc!")
                .submittedDate(ZonedDateTime.now())
                .build();

        ExerciseResultDetail detail = ExerciseResultDetail.builder()
                .exerciseResultId(77L)
                .questionId(10L)
                .selectedOption("A")
                .isCorrect(true)
                .build();

        Question q = Question.builder().id(10L).content("Q1").category("Grammar").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();

        when(exerciseResultRepository.findById(77L)).thenReturn(Optional.of(savedResult));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Hiragana")));
        when(exerciseResultDetailRepository.findByExerciseResultId(77L)).thenReturn(List.of(detail));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));

        QuizResultDto result = quizService.getResult(77L);

        assertEquals(77L, result.getResultId());
        assertTrue(result.isPassed());
        assertEquals(1, result.getCorrectCount());
        assertEquals("Hiragana", result.getLessonTitle());
        assertEquals(1, result.getQuestions().size());
        assertTrue(result.getQuestions().get(0).isUserCorrect());
    }

    // ── getOrCreateExerciseForLesson — new exercise creation ──

    @Test
    @DisplayName("getOrCreateExerciseForLesson - không có exercise → tạo mới và seed")
    void getOrCreate_noExistingExercise_createsNewAndSeeds() {
        when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty());
        when(lessonRepository.findById(200L)).thenReturn(Optional.of(lesson(200L, "Katakana")));
        when(exerciseRepository.save(any())).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            e.setExerciseId(50L);
            return e;
        });
        // seed path: findByLessonId returns the newly saved exercise
        when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(Exercise.builder().exerciseId(50L).lessonId(200L).totalQuestions(5).passingScore(60).status("PUBLISHED").build()));
        // seed internals
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(50L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(99L);
            return q;
        });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Exercise result = quizService.getOrCreateExerciseForLesson(200L);

        assertNotNull(result);
        verify(exerciseRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson - có lesson → title chứa tên lesson")
    void getOrCreate_noExistingExercise_withLesson_usesLessonTitle() {
        when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty());
        when(lessonRepository.findById(200L)).thenReturn(Optional.of(lesson(200L, "Katakana")));
        List<Exercise> saved = new ArrayList<>();
        when(exerciseRepository.save(any())).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            if (e.getExerciseId() == null) e.setExerciseId(50L);
            saved.add(e);
            return e;
        });
        // seed calls findByLessonId again — returns the newly created exercise
        when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty());
        lenient().when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty());
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(50L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Exercise result = quizService.getOrCreateExerciseForLesson(200L);

        assertTrue(result.getExerciseTitle().contains("Katakana"));
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson - lesson không tìm thấy → fallback title chứa lessonId")
    void getOrCreate_noExistingExercise_lessonNotFound_usesDefault() {
        when(exerciseRepository.findByLessonId(300L)).thenReturn(Optional.empty());
        when(lessonRepository.findById(300L)).thenReturn(Optional.empty());
        when(exerciseRepository.save(any())).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            if (e.getExerciseId() == null) e.setExerciseId(60L);
            return e;
        });
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(60L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Exercise result = quizService.getOrCreateExerciseForLesson(300L);

        assertTrue(result.getExerciseTitle().contains("300"));
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson - exercise mới tạo có đúng defaults (totalQ=5, passingScore=60, PUBLISHED)")
    void getOrCreate_exerciseSavedWithCorrectDefaults() {
        when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty());
        when(lessonRepository.findById(200L)).thenReturn(Optional.of(lesson(200L, "Test")));
        List<Exercise> captured = new ArrayList<>();
        when(exerciseRepository.save(any())).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            captured.add(Exercise.builder()
                    .exerciseId(50L).lessonId(e.getLessonId())
                    .exerciseTitle(e.getExerciseTitle())
                    .totalQuestions(e.getTotalQuestions())
                    .passingScore(e.getPassingScore())
                    .status(e.getStatus()).build());
            if (e.getExerciseId() == null) e.setExerciseId(50L);
            return e;
        });
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(50L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quizService.getOrCreateExerciseForLesson(200L);

        Exercise first = captured.get(0);
        assertEquals(5, first.getTotalQuestions());
        assertEquals(60, first.getPassingScore());
        assertEquals("PUBLISHED", first.getStatus());
    }

    // ── getQuestionsForExercise — additional tests ──

    @Test
    @DisplayName("getQuestionsForExercise - cả question lẫn bank null → bị skip")
    void getQuestions_bothQuestionAndBankNull_skipped() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getQuestionsForExercise - 3 câu hỏi → trả về đúng thứ tự")
    void getQuestions_multipleQuestionsOrdered() {
        ExerciseQuestion eq1 = new ExerciseQuestion(1L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(1L, 11L, 2);
        ExerciseQuestion eq3 = new ExerciseQuestion(1L, 12L, 3);
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        Question q3 = Question.builder().id(12L).content("Q3").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq1, eq2, eq3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getOrderIndex());
        assertEquals(2, result.get(1).getOrderIndex());
        assertEquals(3, result.get(2).getOrderIndex());
        assertEquals("Q1", result.get(0).getContent());
        assertEquals("Q3", result.get(2).getContent());
    }

    @Test
    @DisplayName("getQuestionsForExercise - audioUrl lấy từ Question")
    void getQuestions_audioUrlFromQuestion() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").audioUrl("/audio/q1.mp3").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals("/audio/q1.mp3", result.get(0).getAudioUrl());
    }

    @Test
    @DisplayName("getQuestionsForExercise - audioUrl fallback sang QuestionBank")
    void getQuestions_audioUrlFallbackToBank() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").audioUrl(null).build();
        QuestionBank qb = QuestionBank.builder().questionId(10L).questionText("Q1 bank").audioUrl("/audio/bank.mp3").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(List.of(qb));
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals("/audio/bank.mp3", result.get(0).getAudioUrl());
    }

    @Test
    @DisplayName("getQuestionsForExercise - options được map đúng label")
    void getQuestions_optionsSortedByLabel() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption optA = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Opt A").isCorrect(true).build();
        QuestionOption optB = QuestionOption.builder().id(51L).questionId(10L).label("B").optionText("Opt B").isCorrect(false).build();
        QuestionOption optC = QuestionOption.builder().id(52L).questionId(10L).label("C").optionText("Opt C").isCorrect(false).build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(optA, optB, optC));

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals(3, result.get(0).getOptions().size());
        assertEquals("A", result.get(0).getOptions().get(0).getLabel());
        assertEquals("B", result.get(0).getOptions().get(1).getLabel());
        assertEquals("C", result.get(0).getOptions().get(2).getLabel());
    }

    @Test
    @DisplayName("getQuestionsForExercise - explanation lấy từ Question")
    void getQuestions_explanationFromQuestion() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").explanation("Giải thích từ question").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals("Giải thích từ question", result.get(0).getExplanation());
    }

    @Test
    @DisplayName("getQuestionsForExercise - explanation fallback sang QuestionBank")
    void getQuestions_explanationFallbackToBank() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").explanation(null).build();
        QuestionBank qb = QuestionBank.builder().questionId(10L).questionText("Q1").explanation("Giải thích từ bank").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(List.of(qb));
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> result = quizService.getQuestionsForExercise(1L);

        assertEquals("Giải thích từ bank", result.get(0).getExplanation());
    }

    // ── submitQuiz — additional tests ──

    @Test
    @DisplayName("submitQuiz - 1/3 đúng (33%) < 60% → FAILED")
    void submitQuiz_partialCorrect_belowThreshold_fails() {
        Exercise ex3 = Exercise.builder().exerciseId(2L).lessonId(100L).totalQuestions(3).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq1 = new ExerciseQuestion(2L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(2L, 11L, 2);
        ExerciseQuestion eq3 = new ExerciseQuestion(2L, 12L, 3);
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        Question q3 = Question.builder().id(12L).content("Q3").build();
        QuestionOption opt1 = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();
        QuestionOption opt2 = QuestionOption.builder().id(51L).questionId(11L).label("B").optionText("Đ2").isCorrect(true).build();
        QuestionOption opt3 = QuestionOption.builder().id(52L).questionId(12L).label("C").optionText("Đ3").isCorrect(true).build();

        when(exerciseRepository.findById(2L)).thenReturn(Optional.of(ex3));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(2L)).thenReturn(List.of(eq1, eq2, eq3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(List.of(opt1, opt2, opt3));
        when(exerciseResultRepository.countByExerciseIdAndUserId(2L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(80L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(2L, 5L, Map.of(10L, "A", 11L, "X", 12L, "X"));

        assertFalse(result.isPassed());
        assertEquals(1, result.getCorrectCount());
        assertEquals("FAILED", result.getResultStatus());
    }

    @Test
    @DisplayName("submitQuiz - 2/3 đúng (67%) >= 60% → PASSED")
    void submitQuiz_partialCorrect_aboveThreshold_passes() {
        Exercise ex3 = Exercise.builder().exerciseId(2L).lessonId(100L).totalQuestions(3).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq1 = new ExerciseQuestion(2L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(2L, 11L, 2);
        ExerciseQuestion eq3 = new ExerciseQuestion(2L, 12L, 3);
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        Question q3 = Question.builder().id(12L).content("Q3").build();
        QuestionOption opt1 = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();
        QuestionOption opt2 = QuestionOption.builder().id(51L).questionId(11L).label("B").optionText("Đ2").isCorrect(true).build();
        QuestionOption opt3 = QuestionOption.builder().id(52L).questionId(12L).label("C").optionText("Đ3").isCorrect(true).build();

        when(exerciseRepository.findById(2L)).thenReturn(Optional.of(ex3));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(2L)).thenReturn(List.of(eq1, eq2, eq3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(List.of(opt1, opt2, opt3));
        when(exerciseResultRepository.countByExerciseIdAndUserId(2L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(81L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(2L, 5L, Map.of(10L, "A", 11L, "B", 12L, "X"));

        assertTrue(result.isPassed());
        assertEquals(2, result.getCorrectCount());
        assertEquals("PASSED", result.getResultStatus());
    }

    @Test
    @DisplayName("submitQuiz - đúng 60% → PASSED (boundary)")
    void submitQuiz_exactlyAtThreshold_passes() {
        Exercise ex5 = Exercise.builder().exerciseId(3L).lessonId(100L).totalQuestions(5).passingScore(60).status("PUBLISHED").build();
        List<ExerciseQuestion> eqs = new ArrayList<>();
        List<Question> qs = new ArrayList<>();
        List<QuestionOption> opts = new ArrayList<>();
        Map<Long, String> answers = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            long qId = 10L + i;
            eqs.add(new ExerciseQuestion(3L, qId, i + 1));
            qs.add(Question.builder().id(qId).content("Q" + i).build());
            opts.add(QuestionOption.builder().id(50L + i).questionId(qId).label("A").optionText("Đ").isCorrect(true).build());
            if (i < 3) answers.put(qId, "A"); // 3/5 = 60%
        }
        List<Long> qIds = List.of(10L, 11L, 12L, 13L, 14L);

        when(exerciseRepository.findById(3L)).thenReturn(Optional.of(ex5));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(3L)).thenReturn(eqs);
        when(questionRepository.findByIdIn(qIds)).thenReturn(qs);
        when(questionBankRepository.findByQuestionIdIn(qIds)).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(qIds)).thenReturn(opts);
        when(exerciseResultRepository.countByExerciseIdAndUserId(3L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(82L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(3L, 5L, answers);

        assertTrue(result.isPassed());
        assertEquals(3, result.getCorrectCount());
        assertEquals(new BigDecimal("60.0"), result.getPercentage());
    }

    @Test
    @DisplayName("submitQuiz - match bằng optionId → correct")
    void submitQuiz_matchByOptionId_isCorrect() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "50"));

        assertEquals(1, result.getCorrectCount());
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("submitQuiz - match bằng optionText → correct")
    void submitQuiz_matchByOptionText_isCorrect() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "Đáp án đúng"));

        assertEquals(1, result.getCorrectCount());
    }

    @Test
    @DisplayName("submitQuiz - match case insensitive label ('a' matches 'A')")
    void submitQuiz_matchByCaseInsensitiveLabel() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "a"));

        assertEquals(1, result.getCorrectCount());
    }

    @Test
    @DisplayName("submitQuiz - empty string answer → not correct")
    void submitQuiz_emptyAnswerString_notCorrect() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, ""));

        assertEquals(0, result.getCorrectCount());
    }

    @Test
    @DisplayName("submitQuiz - whitespace-only answer → trimmed to empty → not correct")
    void submitQuiz_whitespaceOnlyAnswer_trimmedToEmpty() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "   "));

        assertEquals(0, result.getCorrectCount());
    }

    @Test
    @DisplayName("submitQuiz - 3 câu đúng → score = 30")
    void submitQuiz_scoreIs10PerCorrect() {
        Exercise ex3 = Exercise.builder().exerciseId(2L).lessonId(100L).totalQuestions(3).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq1 = new ExerciseQuestion(2L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(2L, 11L, 2);
        ExerciseQuestion eq3 = new ExerciseQuestion(2L, 12L, 3);
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        Question q3 = Question.builder().id(12L).content("Q3").build();
        QuestionOption opt1 = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();
        QuestionOption opt2 = QuestionOption.builder().id(51L).questionId(11L).label("A").optionText("Đ2").isCorrect(true).build();
        QuestionOption opt3 = QuestionOption.builder().id(52L).questionId(12L).label("A").optionText("Đ3").isCorrect(true).build();

        when(exerciseRepository.findById(2L)).thenReturn(Optional.of(ex3));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(2L)).thenReturn(List.of(eq1, eq2, eq3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(List.of(opt1, opt2, opt3));
        when(exerciseResultRepository.countByExerciseIdAndUserId(2L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(83L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(2L, 5L, Map.of(10L, "A", 11L, "A", 12L, "A"));

        assertEquals(30, result.getScore());
    }

    @Test
    @DisplayName("submitQuiz - 1/3 đúng → percentage = 33.3 (HALF_UP)")
    void submitQuiz_percentageRoundedHalfUp() {
        Exercise ex3 = Exercise.builder().exerciseId(2L).lessonId(100L).totalQuestions(3).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq1 = new ExerciseQuestion(2L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(2L, 11L, 2);
        ExerciseQuestion eq3 = new ExerciseQuestion(2L, 12L, 3);
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        Question q3 = Question.builder().id(12L).content("Q3").build();
        QuestionOption opt1 = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ1").isCorrect(true).build();
        QuestionOption opt2 = QuestionOption.builder().id(51L).questionId(11L).label("B").optionText("Đ2").isCorrect(true).build();
        QuestionOption opt3 = QuestionOption.builder().id(52L).questionId(12L).label("C").optionText("Đ3").isCorrect(true).build();

        when(exerciseRepository.findById(2L)).thenReturn(Optional.of(ex3));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(2L)).thenReturn(List.of(eq1, eq2, eq3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(List.of(opt1, opt2, opt3));
        when(exerciseResultRepository.countByExerciseIdAndUserId(2L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(84L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(2L, 5L, Map.of(10L, "A", 11L, "X", 12L, "X"));

        assertEquals(new BigDecimal("33.3"), result.getPercentage());
    }

    @Test
    @DisplayName("submitQuiz - failed → feedback chứa threshold percent")
    void submitQuiz_feedbackContainsThresholdWhenFailed() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "WRONG"));

        assertFalse(result.isPassed());
        assertTrue(result.getFeedback().contains("60"));
    }

    @Test
    @DisplayName("submitQuiz - passed → feedback tích cực")
    void submitQuiz_feedbackIsPositiveWhenPassed() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "A"));

        assertTrue(result.isPassed());
        assertTrue(result.getFeedback().contains("Xuất sắc"));
    }

    @Test
    @DisplayName("submitQuiz - attemptNumber = prior count + 1")
    void submitQuiz_attemptNumberIncrementsFromPriorAttempts() {
        Exercise ex1 = Exercise.builder().exerciseId(1L).lessonId(100L).totalQuestions(1).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ").isCorrect(true).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(ex1));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, 5L)).thenReturn(2L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> {
            ExerciseResult r = inv.getArgument(0);
            assertEquals(3, r.getAttemptNumber());
            r.setExerciseResultId(85L);
            return r;
        });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        quizService.submitQuiz(1L, 5L, Map.of(10L, "A"));
    }

    @Test
    @DisplayName("submitQuiz - không có option nào isCorrect → correctLabel mặc định 'A'")
    void submitQuiz_noCorrectOptionDefined_defaultsToA() {
        Exercise ex1 = Exercise.builder().exerciseId(1L).lessonId(100L).totalQuestions(1).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption optNoCorrect = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Opt").isCorrect(false).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(ex1));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(optNoCorrect));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(86L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "A"));

        assertEquals("A", result.getQuestions().get(0).getCorrectLabel());
        assertEquals(0, result.getCorrectCount());
    }

    @Test
    @DisplayName("submitQuiz - unanswered question → review DTO selectedLabel null, detail entity 'NONE'")
    void submitQuiz_unansweredQuestion_detailHasNONE() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of());

        assertNull(result.getQuestions().get(0).getUserSelectedLabel());
        verify(exerciseResultDetailRepository).saveAll(argThat(details -> {
            List<ExerciseResultDetail> list = new ArrayList<>();
            details.forEach(list::add);
            return list.size() == 1 && "NONE".equals(list.get(0).getSelectedOption());
        }));
    }

    @Test
    @DisplayName("submitQuiz - lessonId null → lessonTitle = 'Bài học'")
    void submitQuiz_lessonIdNull_lessonTitleDefault() {
        Exercise exNoLesson = Exercise.builder().exerciseId(4L).lessonId(null).totalQuestions(1).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq = new ExerciseQuestion(4L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ").isCorrect(true).build();

        when(exerciseRepository.findById(4L)).thenReturn(Optional.of(exNoLesson));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(4L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));
        when(exerciseResultRepository.countByExerciseIdAndUserId(4L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(87L); return r; });

        QuizResultDto result = quizService.submitQuiz(4L, 5L, Map.of(10L, "A"));

        assertEquals("Bài học", result.getLessonTitle());
    }

    @Test
    @DisplayName("submitQuiz - 0 câu hỏi → percentage = ZERO")
    void submitQuiz_zeroQuestions_percentageZero() {
        Exercise exEmpty = Exercise.builder().exerciseId(5L).lessonId(100L).totalQuestions(0).passingScore(60).status("PUBLISHED").build();

        when(exerciseRepository.findById(5L)).thenReturn(Optional.of(exEmpty));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(5L)).thenReturn(Collections.emptyList());
        when(questionRepository.findByIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(exerciseResultRepository.countByExerciseIdAndUserId(5L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(88L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(5L, 5L, Map.of());

        assertEquals(BigDecimal.ZERO, result.getPercentage());
        assertEquals(0, result.getCorrectCount());
    }

    @Test
    @DisplayName("submitQuiz - passingScore null → threshold mặc định 60")
    void submitQuiz_passingScoreNull_defaultsTo60() {
        Exercise exNullScore = Exercise.builder().exerciseId(6L).lessonId(100L).totalQuestions(1).passingScore(null).status("PUBLISHED").build();
        ExerciseQuestion eq = new ExerciseQuestion(6L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đ").isCorrect(true).build();

        when(exerciseRepository.findById(6L)).thenReturn(Optional.of(exNullScore));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(6L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));
        when(exerciseResultRepository.countByExerciseIdAndUserId(6L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(89L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        QuizResultDto result = quizService.submitQuiz(6L, 5L, Map.of(10L, "A"));

        assertTrue(result.isPassed());
        assertTrue(result.getFeedback().contains("Xuất sắc") || result.getFeedback().contains("60"));
    }

    @Test
    @DisplayName("submitQuiz - DTO trả về đầy đủ tất cả các field")
    void submitQuiz_returnedDtoHasAllFields() {
        stubSingleQuestionSubmit();

        QuizResultDto result = quizService.submitQuiz(1L, 5L, Map.of(10L, "A"));

        assertNotNull(result.getResultId());
        assertEquals(1L, result.getExerciseId());
        assertEquals(5L, result.getUserId());
        assertNotNull(result.getSubmittedDate());
        assertNotNull(result.getQuestions());
        assertFalse(result.getQuestions().isEmpty());
        assertNotNull(result.getResultStatus());
        assertNotNull(result.getFeedback());
    }

    // ── getResult — additional tests ──

    @Test
    @DisplayName("getResult - exercise null → fallback exerciseTitle và lessonTitle")
    void getResult_exerciseNull_fallbackDefaults() {
        ExerciseResult savedResult = ExerciseResult.builder()
                .exerciseResultId(77L).exerciseId(999L).userId(5L).attemptNumber(1)
                .score(10).percentage(new BigDecimal("100.0")).resultStatus("PASSED").feedback("OK")
                .submittedDate(ZonedDateTime.now()).build();

        when(exerciseResultRepository.findById(77L)).thenReturn(Optional.of(savedResult));
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());
        when(exerciseResultDetailRepository.findByExerciseResultId(77L)).thenReturn(Collections.emptyList());
        when(questionRepository.findByIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(Collections.emptyList())).thenReturn(Collections.emptyList());

        QuizResultDto result = quizService.getResult(77L);

        assertEquals("Bài tập trắc nghiệm", result.getExerciseTitle());
        assertEquals("Bài học", result.getLessonTitle());
    }

    @Test
    @DisplayName("getResult - lesson null → lessonTitle = 'Bài học'")
    void getResult_lessonNull_fallbackTitle() {
        ExerciseResult savedResult = ExerciseResult.builder()
                .exerciseResultId(77L).exerciseId(1L).userId(5L).attemptNumber(1)
                .score(10).percentage(new BigDecimal("100.0")).resultStatus("PASSED").feedback("OK")
                .submittedDate(ZonedDateTime.now()).build();

        when(exerciseResultRepository.findById(77L)).thenReturn(Optional.of(savedResult));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());
        when(exerciseResultDetailRepository.findByExerciseResultId(77L)).thenReturn(Collections.emptyList());
        when(questionRepository.findByIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(Collections.emptyList())).thenReturn(Collections.emptyList());

        QuizResultDto result = quizService.getResult(77L);

        assertEquals("Bài học", result.getLessonTitle());
    }

    @Test
    @DisplayName("getResult - correctCount được tính từ details.isCorrect")
    void getResult_correctCountFromDetails() {
        ExerciseResult savedResult = ExerciseResult.builder()
                .exerciseResultId(77L).exerciseId(1L).userId(5L).score(20)
                .percentage(new BigDecimal("66.7")).resultStatus("PASSED").feedback("OK")
                .submittedDate(ZonedDateTime.now()).build();
        ExerciseResultDetail d1 = ExerciseResultDetail.builder().exerciseResultId(77L).questionId(10L).selectedOption("A").isCorrect(true).build();
        ExerciseResultDetail d2 = ExerciseResultDetail.builder().exerciseResultId(77L).questionId(11L).selectedOption("B").isCorrect(true).build();
        ExerciseResultDetail d3 = ExerciseResultDetail.builder().exerciseResultId(77L).questionId(12L).selectedOption("C").isCorrect(false).build();
        Question q1 = Question.builder().id(10L).content("Q1").build();
        Question q2 = Question.builder().id(11L).content("Q2").build();
        Question q3 = Question.builder().id(12L).content("Q3").build();

        when(exerciseResultRepository.findById(77L)).thenReturn(Optional.of(savedResult));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Bài")));
        when(exerciseResultDetailRepository.findByExerciseResultId(77L)).thenReturn(List.of(d1, d2, d3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());

        QuizResultDto result = quizService.getResult(77L);

        assertEquals(2, result.getCorrectCount());
        assertEquals(3, result.getQuestions().size());
    }

    @Test
    @DisplayName("getResult - PASSED string → passed=true, FAILED → false")
    void getResult_passedStatusFromResultString() {
        ExerciseResult failedResult = ExerciseResult.builder()
                .exerciseResultId(78L).exerciseId(1L).userId(5L).score(0)
                .percentage(BigDecimal.ZERO).resultStatus("FAILED").feedback("Thử lại")
                .submittedDate(ZonedDateTime.now()).build();

        when(exerciseResultRepository.findById(78L)).thenReturn(Optional.of(failedResult));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Bài")));
        when(exerciseResultDetailRepository.findByExerciseResultId(78L)).thenReturn(Collections.emptyList());
        when(questionRepository.findByIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(Collections.emptyList())).thenReturn(Collections.emptyList());

        QuizResultDto result = quizService.getResult(78L);

        assertFalse(result.isPassed());
        assertEquals("FAILED", result.getResultStatus());
    }

    // ── seedQuizQuestionsForLesson — additional tests ──

    @Test
    @DisplayName("seed - lesson null → mặc định level N4")
    void seed_lessonNull_defaultsToN4() {
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());
        Exercise seedExercise = Exercise.builder().exerciseId(70L).lessonId(999L).totalQuestions(5).passingScore(60).status("PUBLISHED").build();
        when(exerciseRepository.findByLessonId(999L)).thenReturn(Optional.of(seedExercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(70L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); assertEquals("N4", q.getJlptLevel()); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quizService.seedQuizQuestionsForLesson(999L);

        verify(questionRepository, times(5)).save(any());
    }

    @Test
    @DisplayName("seed - exercise đã có câu hỏi → return ngay, không seed")
    void seed_exerciseAlreadyHasQuestions_returnsImmediately() {
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Bài")));
        when(exerciseRepository.findByLessonId(100L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(new ExerciseQuestion(1L, 10L, 1)));

        quizService.seedQuizQuestionsForLesson(100L);

        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("seed - không có exercise → tạo mới rồi seed")
    void seed_createsExerciseWhenNoneExists() {
        when(lessonRepository.findById(400L)).thenReturn(Optional.of(lesson(400L, "Bài mới")));
        Exercise newEx = Exercise.builder().exerciseId(80L).lessonId(400L).totalQuestions(5).passingScore(60).status("PUBLISHED").build();
        when(exerciseRepository.findByLessonId(400L)).thenReturn(Optional.empty());
        when(exerciseRepository.save(any())).thenReturn(newEx);
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(80L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quizService.seedQuizQuestionsForLesson(400L);

        verify(exerciseRepository, atLeastOnce()).save(any());
        verify(questionRepository, times(5)).save(any());
    }

    @Test
    @DisplayName("seed - lưu 5 question + 5 questionBank + 20 options + 5 exerciseQuestion")
    void seed_savesQuestionBankAndOptionsForEachSeed() {
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());
        when(exerciseRepository.findByLessonId(100L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quizService.seedQuizQuestionsForLesson(100L);

        verify(questionRepository, times(5)).save(any());
        verify(questionBankRepository, times(5)).save(any());
        verify(questionOptionRepository, times(20)).save(any()); // 5 questions * 4 options each
        verify(exerciseQuestionRepository, times(5)).save(any());
    }

    @Test
    @DisplayName("seed - cập nhật exercise.totalQuestions = seeds.size()")
    void seed_setsExerciseTotalQuestions() {
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());
        when(exerciseRepository.findByLessonId(100L)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { Question q = inv.getArgument(0); q.setId(99L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.save(any())).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            assertEquals(5, e.getTotalQuestions());
            return e;
        });

        quizService.seedQuizQuestionsForLesson(100L);

        verify(exerciseRepository).save(any());
    }

    // ── NEW: additional edge cases ──

    @Test
    @DisplayName("submitQuiz - null userId → result vẫn được lưu với userId null")
    void submitQuiz_nullUserId_savedWithNullUserId() {
        Exercise ex1 = Exercise.builder().exerciseId(1L).lessonId(100L).totalQuestions(1).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        com.jlpp.modules.quiz.entity.Question q = com.jlpp.modules.quiz.entity.Question.builder().id(10L).content("Q1").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đáp án đúng").isCorrect(true).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(ex1));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, null)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> {
            ExerciseResult r = inv.getArgument(0);
            r.setExerciseResultId(90L);
            return r;
        });
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Hiragana")));

        QuizResultDto result = quizService.submitQuiz(1L, null, Map.of(10L, "A"));

        assertNull(result.getUserId());
        verify(exerciseResultRepository).save(any());
    }

    @Test
    @DisplayName("submitQuiz - nhiều câu hỏi nhưng answers map thiếu → chỉ matched là correct")
    void submitQuiz_partialAnswers_onlyMatchedCounted() {
        Exercise ex = Exercise.builder().exerciseId(2L).lessonId(100L).totalQuestions(3).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq1 = new ExerciseQuestion(2L, 10L, 1);
        ExerciseQuestion eq2 = new ExerciseQuestion(2L, 11L, 2);
        ExerciseQuestion eq3 = new ExerciseQuestion(2L, 12L, 3);

        com.jlpp.modules.quiz.entity.Question q1 = com.jlpp.modules.quiz.entity.Question.builder().id(10L).content("Q1").build();
        com.jlpp.modules.quiz.entity.Question q2 = com.jlpp.modules.quiz.entity.Question.builder().id(11L).content("Q2").build();
        com.jlpp.modules.quiz.entity.Question q3 = com.jlpp.modules.quiz.entity.Question.builder().id(12L).content("Q3").build();

        QuestionOption opt1 = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Yes").isCorrect(true).build();
        QuestionOption opt2 = QuestionOption.builder().id(51L).questionId(11L).label("B").optionText("No").isCorrect(true).build();
        QuestionOption opt3 = QuestionOption.builder().id(52L).questionId(12L).label("C").optionText("Maybe").isCorrect(true).build();

        when(exerciseRepository.findById(2L)).thenReturn(Optional.of(ex));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(2L)).thenReturn(List.of(eq1, eq2, eq3));
        when(questionRepository.findByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(q1, q2, q3));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L, 11L, 12L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L, 11L, 12L))).thenReturn(List.of(opt1, opt2, opt3));
        when(exerciseResultRepository.countByExerciseIdAndUserId(2L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> { ExerciseResult r = inv.getArgument(0); r.setExerciseResultId(91L); return r; });
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Test")));

        QuizResultDto result = quizService.submitQuiz(2L, 5L, Map.of(10L, "A"));

        assertEquals(1, result.getCorrectCount());
        assertEquals(3, result.getTotalQuestions());
    }

    @Test
    @DisplayName("getResult - score null → trả 0")
    void getResult_scoreNull_returnsZero() {
        ExerciseResult er = ExerciseResult.builder()
                .exerciseResultId(50L).exerciseId(1L).userId(5L)
                .score(null).percentage(null).resultStatus("FAILED").build();
        when(exerciseResultRepository.findById(50L)).thenReturn(Optional.of(er));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseResultDetailRepository.findByExerciseResultId(50L)).thenReturn(Collections.emptyList());
        when(questionRepository.findByIdIn(any())).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(any())).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(any())).thenReturn(Collections.emptyList());
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Hiragana")));

        QuizResultDto result = quizService.getResult(50L);

        assertEquals(0, result.getScore());
        assertEquals(BigDecimal.ZERO, result.getPercentage());
    }

    @Test
    @DisplayName("getResult - resultStatus null → passed = false")
    void getResult_statusNull_passedFalse() {
        ExerciseResult er = ExerciseResult.builder()
                .exerciseResultId(51L).exerciseId(1L).userId(5L)
                .score(50).percentage(BigDecimal.valueOf(50)).resultStatus(null).build();
        when(exerciseResultRepository.findById(51L)).thenReturn(Optional.of(er));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseResultDetailRepository.findByExerciseResultId(51L)).thenReturn(Collections.emptyList());
        when(questionRepository.findByIdIn(any())).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(any())).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(any())).thenReturn(Collections.emptyList());
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Hiragana")));

        QuizResultDto result = quizService.getResult(51L);

        assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("getQuestionsForExercise - category/jlptLevel fallback từ QuestionBank")
    void getQuestions_categoryFallback_fromQuestionBank() {
        ExerciseQuestion eq = new ExerciseQuestion(1L, 20L, 1);
        QuestionBank qb = QuestionBank.builder().questionId(20L).questionText("QB content").category("Kanji").jlptLevel("N3").build();

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(20L))).thenReturn(Collections.emptyList());
        when(questionBankRepository.findByQuestionIdIn(List.of(20L))).thenReturn(List.of(qb));
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(20L))).thenReturn(Collections.emptyList());

        List<QuizQuestionDto> dtos = quizService.getQuestionsForExercise(1L);

        assertEquals(1, dtos.size());
        assertEquals("Kanji", dtos.get(0).getCategory());
        assertEquals("N3", dtos.get(0).getJlptLevel());
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson - exercise chưa tồn tại → tạo mới")
    void getOrCreate_noExisting_createsNew() {
        when(exerciseRepository.findByLessonId(200L)).thenReturn(Optional.empty());
        when(lessonRepository.findById(200L)).thenReturn(Optional.of(lesson(200L, "Lesson 2")));
        when(exerciseRepository.save(any())).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            e.setExerciseId(99L);
            return e;
        });
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(99L)).thenReturn(Collections.emptyList());
        when(questionRepository.save(any())).thenAnswer(inv -> { com.jlpp.modules.quiz.entity.Question q = inv.getArgument(0); q.setId(50L); return q; });
        when(questionBankRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(questionOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Exercise result = quizService.getOrCreateExerciseForLesson(200L);

        assertEquals(99L, result.getExerciseId());
        verify(exerciseRepository, atLeastOnce()).save(any());
    }

    // ── shared helper for single-question submit tests ──

    private void stubSingleQuestionSubmit() {
        Exercise ex1 = Exercise.builder().exerciseId(1L).lessonId(100L).totalQuestions(1).passingScore(60).status("PUBLISHED").build();
        ExerciseQuestion eq = new ExerciseQuestion(1L, 10L, 1);
        Question q = Question.builder().id(10L).content("Q1").build();
        QuestionOption opt = QuestionOption.builder().id(50L).questionId(10L).label("A").optionText("Đáp án đúng").isCorrect(true).build();

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(ex1));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(1L)).thenReturn(List.of(eq));
        when(questionRepository.findByIdIn(List.of(10L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(List.of(10L))).thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(List.of(10L))).thenReturn(List.of(opt));
        when(exerciseResultRepository.countByExerciseIdAndUserId(1L, 5L)).thenReturn(0L);
        when(exerciseResultRepository.save(any())).thenAnswer(inv -> {
            ExerciseResult r = inv.getArgument(0);
            r.setExerciseResultId(90L);
            return r;
        });
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson(100L, "Hiragana")));
    }

    // ── helpers ──

    private static Lesson lesson(Long id, String title) {
        Lesson l = Lesson.builder().title(title).build();
        l.setId(id);
        return l;
    }
}
