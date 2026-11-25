package com.jeps.modules.quiz;

import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.quiz.dto.QuizQuestionDto;
import com.jeps.modules.quiz.dto.QuizResultDto;
import com.jeps.modules.quiz.entity.*;
import com.jeps.modules.quiz.repository.*;
import com.jeps.modules.quiz.service.QuizServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuizServiceUnitTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private QuizQuestionRepository questionRepository;

    @Mock
    private ExerciseQuestionRepository exerciseQuestionRepository;

    @Mock
    private ExerciseResultRepository exerciseResultRepository;

    @Mock
    private ExerciseResultDetailRepository exerciseResultDetailRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private QuestionBankRepository questionBankRepository;

    private QuizServiceImpl quizService;

    private final Long lessonId = 101L;
    private final Long exerciseId = 201L;
    private final Long userId = 55L;

    @BeforeEach
    void setUp() {
        quizService = new QuizServiceImpl(
                exerciseRepository,
                questionRepository,
                exerciseQuestionRepository,
                exerciseResultRepository,
                exerciseResultDetailRepository,
                lessonRepository,
                questionBankRepository
        );
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson: Trả về bài tập hiện có nếu đã tồn tại và có câu hỏi")
    void testGetOrCreateExerciseForLesson_ReturnsExisting() {
        Exercise existing = Exercise.builder()
                .exerciseId(exerciseId)
                .lessonId(lessonId)
                .exerciseTitle("Bài tập hiện có")
                .passingScore(60)
                .build();

        ExerciseQuestion eq = ExerciseQuestion.builder()
                .exerciseId(exerciseId)
                .questionId(1L)
                .questionOrder(1)
                .build();

        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(Optional.of(existing));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exerciseId)).thenReturn(List.of(eq));

        Exercise result = quizService.getOrCreateExerciseForLesson(lessonId);

        assertThat(result).isNotNull();
        assertThat(result.getExerciseId()).isEqualTo(exerciseId);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateExerciseForLesson: Tự động khởi tạo bài tập mới nếu bài học chưa có bài tập")
    void testGetOrCreateExerciseForLesson_CreatesNewWhenNotFound() {
        Lesson lesson = Lesson.builder().title("Ngữ pháp N4").build();
        lesson.setId(lessonId);

        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(Optional.empty());
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            e.setExerciseId(exerciseId);
            return e;
        });
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(999L);
            return q;
        });

        Exercise result = quizService.getOrCreateExerciseForLesson(lessonId);

        assertThat(result).isNotNull();
        assertThat(result.getExerciseTitle()).contains("Ngữ pháp N4");
        assertThat(result.getPassingScore()).isEqualTo(60);
        verify(exerciseRepository, atLeastOnce()).save(any(Exercise.class));
    }

    @Test
    @DisplayName("getQuestionsForExercise: Đọc danh sách câu hỏi theo đúng thứ tự câu và ánh xạ các lựa chọn")
    void testGetQuestionsForExercise_Success() {
        ExerciseQuestion eq1 = ExerciseQuestion.builder().exerciseId(exerciseId).questionId(10L).questionOrder(1).build();
        ExerciseQuestion eq2 = ExerciseQuestion.builder().exerciseId(exerciseId).questionId(20L).questionOrder(2).build();

        Question q1 = Question.builder()
                .content("Câu hỏi 1").explanation("Giải thích 1").jlptLevel("N5")
                .options("[{\"key\":\"A\",\"text\":\"Đáp án A\"},{\"key\":\"B\",\"text\":\"Đáp án B\"}]")
                .answerKey("[\"A\"]")
                .build();
        q1.setId(10L);
        Question q2 = Question.builder().content("Câu hỏi 2").explanation("Giải thích 2").jlptLevel("N5").build();
        q2.setId(20L);

        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exerciseId)).thenReturn(List.of(eq1, eq2));
        when(questionRepository.findByIdIn(List.of(10L, 20L))).thenReturn(List.of(q1, q2));
        when(questionBankRepository.findByQuestionIdIn(anyList())).thenReturn(List.of());

        List<QuizQuestionDto> questions = quizService.getQuestionsForExercise(exerciseId);

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).getContent()).isEqualTo("Câu hỏi 1");
        assertThat(questions.get(0).getOptions()).hasSize(2);
        assertThat(questions.get(0).getOptions().get(0).getLabel()).isEqualTo("A");
        assertThat(questions.get(0).getOptions().get(0).getIsCorrect()).isTrue();
    }

    @Test
    @DisplayName("submitQuiz: Đúng 2/2 câu -> Đạt 100%, trạng thái PASSED và điểm số 20")
    void testSubmitQuiz_AllCorrect_Passed() {
        Exercise exercise = Exercise.builder().exerciseId(exerciseId).lessonId(lessonId).passingScore(60).build();

        ExerciseQuestion eq1 = ExerciseQuestion.builder().exerciseId(exerciseId).questionId(1L).questionOrder(1).build();
        ExerciseQuestion eq2 = ExerciseQuestion.builder().exerciseId(exerciseId).questionId(2L).questionOrder(2).build();

        Question q1 = Question.builder().content("Q1")
                .options("[{\"key\":\"A\",\"text\":\"Đúng\"},{\"key\":\"B\",\"text\":\"Sai\"}]")
                .answerKey("[\"A\"]").build();
        q1.setId(1L);
        Question q2 = Question.builder().content("Q2")
                .options("[{\"key\":\"A\",\"text\":\"Sai\"},{\"key\":\"B\",\"text\":\"Đúng\"}]")
                .answerKey("[\"B\"]").build();
        q2.setId(2L);

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exerciseId)).thenReturn(List.of(eq1, eq2));
        when(questionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(q1, q2));
        when(questionBankRepository.findByQuestionIdIn(anyList())).thenReturn(List.of());
        when(exerciseResultRepository.countByExerciseIdAndUserId(exerciseId, userId)).thenReturn(0L);

        when(exerciseResultRepository.save(any(ExerciseResult.class))).thenAnswer(inv -> {
            ExerciseResult er = inv.getArgument(0);
            er.setExerciseResultId(999L);
            return er;
        });

        // Nộp đáp án: Câu 1 chọn A, Câu 2 chọn B
        Map<Long, String> answers = Map.of(1L, "A", 2L, "B");
        QuizResultDto result = quizService.submitQuiz(exerciseId, userId, answers);

        assertThat(result).isNotNull();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.getResultStatus()).isEqualTo("PASSED");
        assertThat(result.getScore()).isEqualTo(20);
        assertThat(result.getPercentage()).isEqualTo(BigDecimal.valueOf(100.0).setScale(1));
        assertThat(result.getCorrectCount()).isEqualTo(2);

        verify(exerciseResultDetailRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("submitQuiz: Đúng 1/2 câu (50%) < Điểm chuẩn (60%) -> Trạng thái FAILED")
    void testSubmitQuiz_BelowPassingThreshold_Failed() {
        Exercise exercise = Exercise.builder().exerciseId(exerciseId).lessonId(lessonId).passingScore(60).build();

        ExerciseQuestion eq1 = ExerciseQuestion.builder().exerciseId(exerciseId).questionId(1L).questionOrder(1).build();
        ExerciseQuestion eq2 = ExerciseQuestion.builder().exerciseId(exerciseId).questionId(2L).questionOrder(2).build();

        Question q1 = Question.builder().content("Q1")
                .options("[{\"key\":\"A\",\"text\":\"Đúng\"},{\"key\":\"B\",\"text\":\"Sai\"}]")
                .answerKey("[\"A\"]").build();
        q1.setId(1L);
        Question q2 = Question.builder().content("Q2")
                .options("[{\"key\":\"A\",\"text\":\"Sai\"},{\"key\":\"B\",\"text\":\"Đúng\"}]")
                .answerKey("[\"B\"]").build();
        q2.setId(2L);

        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exerciseId)).thenReturn(List.of(eq1, eq2));
        when(questionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(q1, q2));
        when(questionBankRepository.findByQuestionIdIn(anyList())).thenReturn(List.of());
        when(exerciseResultRepository.countByExerciseIdAndUserId(exerciseId, userId)).thenReturn(1L);

        when(exerciseResultRepository.save(any(ExerciseResult.class))).thenAnswer(inv -> {
            ExerciseResult er = inv.getArgument(0);
            er.setExerciseResultId(1000L);
            return er;
        });

        // Nộp đáp án: Câu 1 chọn A (đúng), Câu 2 chọn C (sai)
        Map<Long, String> answers = Map.of(1L, "A", 2L, "C");
        QuizResultDto result = quizService.submitQuiz(exerciseId, userId, answers);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getResultStatus()).isEqualTo("FAILED");
        assertThat(result.getPercentage()).isEqualTo(BigDecimal.valueOf(50.0).setScale(1));
        assertThat(result.getCorrectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getResult: Trả về kết quả bài làm chi tiết kèm phản hồi giải thích")
    void testGetResult_Success() {
        ExerciseResult saved = ExerciseResult.builder()
                .exerciseResultId(888L)
                .exerciseId(exerciseId)
                .userId(userId)
                .score(20)
                .percentage(BigDecimal.valueOf(100.0))
                .resultStatus("PASSED")
                .feedback("Rất tốt!")
                .submittedDate(ZonedDateTime.now())
                .build();

        ExerciseResultDetail detail = ExerciseResultDetail.builder()
                .exerciseResultId(888L)
                .questionId(1L)
                .selectedOption("A")
                .isCorrect(true)
                .build();

        Question q = Question.builder().content("Câu 1").explanation("Giải thích chi tiết")
                .options("[{\"key\":\"A\",\"text\":\"Đáp án A\"}]")
                .answerKey("[\"A\"]").build();
        q.setId(1L);

        when(exerciseResultRepository.findById(888L)).thenReturn(Optional.of(saved));
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(Exercise.builder().exerciseTitle("Test").build()));
        when(exerciseResultDetailRepository.findByExerciseResultId(888L)).thenReturn(List.of(detail));
        when(questionRepository.findByIdIn(List.of(1L))).thenReturn(List.of(q));
        when(questionBankRepository.findByQuestionIdIn(anyList())).thenReturn(List.of());

        QuizResultDto dto = quizService.getResult(888L);

        assertThat(dto).isNotNull();
        assertThat(dto.getResultId()).isEqualTo(888L);
        assertThat(dto.isPassed()).isTrue();
        assertThat(dto.getQuestions()).hasSize(1);
        assertThat(dto.getQuestions().get(0).getExplanation()).isEqualTo("Giải thích chi tiết");
    }

    @Test
    @DisplayName("getResult: ID không tồn tại sẽ ném IllegalArgumentException")
    void testGetResult_NotFound_ThrowsException() {
        when(exerciseResultRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.getResult(99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy kết quả");
    }
}
