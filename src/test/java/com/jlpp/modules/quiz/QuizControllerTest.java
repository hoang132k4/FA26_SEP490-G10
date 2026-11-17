package com.jlpp.modules.quiz;

import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.quiz.controller.QuizController;
import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.dto.QuizResultDto;
import com.jlpp.modules.quiz.entity.Exercise;
import com.jlpp.modules.quiz.service.QuizService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
@ActiveProfiles("test")
@WithMockUser(username = "learner1", roles = "LEARNER")
class QuizControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private QuizService quizService;
    @MockBean private LessonRepository lessonRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private com.jlpp.modules.notification.service.NotificationService notificationService;

    private Exercise exercise;

    @BeforeEach
    void setUp() {
        exercise = Exercise.builder()
                .exerciseId(1L)
                .lessonId(100L)
                .exerciseTitle("Quiz Hiragana")
                .totalQuestions(2)
                .passingScore(60)
                .status("PUBLISHED")
                .build();

        User user = User.builder().username("learner1").email("learner1@test.com").build();
        user.setId(5L);
        when(userRepository.findByEmailIgnoreCase("learner1")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("learner1")).thenReturn(Optional.of(user));
    }

    // ── DO QUIZ ──

    @Test
    void doQuiz_noParams_redirectsToDashboard() throws Exception {
        mockMvc.perform(get("/quiz/do"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/dashboard"));
    }

    @Test
    void doQuiz_byLessonId_returnsDoQuizView() throws Exception {
        when(quizService.getOrCreateExerciseForLesson(100L)).thenReturn(exercise);
        when(quizService.getQuestionsForExercise(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz/do").param("lessonId", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"))
                .andExpect(model().attributeExists("exercise", "questions", "totalQuestions"));
    }

    @Test
    void doQuiz_byExerciseId_returnsDoQuizView() throws Exception {
        when(quizService.getExercise(1L)).thenReturn(exercise);
        when(quizService.getQuestionsForExercise(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz/do").param("exerciseId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"))
                .andExpect(model().attribute("activeMenu", "my-courses"));
    }

    @Test
    void doQuiz_exerciseIdPrioritized_overLessonId() throws Exception {
        when(quizService.getExercise(1L)).thenReturn(exercise);
        when(quizService.getQuestionsForExercise(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz/do")
                        .param("exerciseId", "1")
                        .param("lessonId", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/do-quiz"));
    }

    @Test
    void doQuiz_withLesson_modelContainsCourse() throws Exception {
        Lesson lesson = Lesson.builder().title("Hiragana").build();
        lesson.setId(100L);
        Course course = Course.builder().build();
        course.setId(10L);
        lesson.setCourse(course);

        when(quizService.getOrCreateExerciseForLesson(100L)).thenReturn(exercise);
        when(quizService.getQuestionsForExercise(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

        mockMvc.perform(get("/quiz/do").param("lessonId", "100"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("lesson", "course"));
    }

    @Test
    void doQuiz_lessonIdNull_onExercise_courseNull() throws Exception {
        Exercise noLessonEx = Exercise.builder()
                .exerciseId(2L)
                .exerciseTitle("Orphan Quiz")
                .totalQuestions(0)
                .passingScore(60)
                .status("PUBLISHED")
                .build();

        when(quizService.getExercise(2L)).thenReturn(noLessonEx);
        when(quizService.getQuestionsForExercise(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/quiz/do").param("exerciseId", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("course"));
    }

    // ── SUBMIT QUIZ ──

    @Test
    void submitQuiz_success_redirectsToResult() throws Exception {
        QuizResultDto result = QuizResultDto.builder()
                .resultId(77L)
                .exerciseId(1L)
                .userId(5L)
                .build();

        when(quizService.submitQuiz(eq(1L), eq(5L), any())).thenReturn(result);

        mockMvc.perform(post("/quiz/submit").with(csrf())
                        .param("exerciseId", "1")
                        .param("answer_10", "A")
                        .param("answer_11", "B"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quiz/result/77"));
    }

    @Test
    void submitQuiz_parsesAnswerParams() throws Exception {
        QuizResultDto result = QuizResultDto.builder().resultId(77L).build();
        when(quizService.submitQuiz(eq(1L), eq(5L), any())).thenReturn(result);

        mockMvc.perform(post("/quiz/submit").with(csrf())
                        .param("exerciseId", "1")
                        .param("answer_10", "A")
                        .param("answer_20", "C")
                        .param("unrelated_param", "ignored"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quiz/result/77"));
    }

    @Test
    void submitQuiz_invalidAnswerKey_ignored() throws Exception {
        QuizResultDto result = QuizResultDto.builder().resultId(77L).build();
        when(quizService.submitQuiz(eq(1L), eq(5L), any())).thenReturn(result);

        mockMvc.perform(post("/quiz/submit").with(csrf())
                        .param("exerciseId", "1")
                        .param("answer_abc", "A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quiz/result/77"));
    }

    @Test
    void submitQuiz_noUser_redirectsToLogin() throws Exception {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/quiz/submit").with(csrf())
                        .param("exerciseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String url = result.getResponse().getRedirectedUrl();
                    assert url != null && url.contains("/auth/login");
                });
    }

    // ── VIEW RESULT ──

    @Test
    void viewResult_success_returnsViewWithModel() throws Exception {
        QuizResultDto result = QuizResultDto.builder()
                .resultId(77L)
                .exerciseId(1L)
                .userId(5L)
                .lessonId(100L)
                .correctCount(2)
                .totalQuestions(2)
                .percentage(new BigDecimal("100.0"))
                .passed(true)
                .resultStatus("PASSED")
                .submittedDate(ZonedDateTime.now())
                .questions(Collections.emptyList())
                .build();

        when(quizService.getResult(77L)).thenReturn(result);
        when(lessonRepository.findById(100L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz/result/77"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("activeMenu", "my-courses"));
    }

    @Test
    void viewResult_differentUser_throws403() throws Exception {
        QuizResultDto result = QuizResultDto.builder()
                .resultId(77L)
                .exerciseId(1L)
                .userId(999L) // different user
                .build();

        when(quizService.getResult(77L)).thenReturn(result);

        mockMvc.perform(get("/quiz/result/77"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewResult_noUser_redirectsToLogin() throws Exception {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/quiz/result/77"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String url = result.getResponse().getRedirectedUrl();
                    assert url != null && url.contains("/auth/login");
                });
    }

    @Test
    void viewResult_lessonNull_courseNull() throws Exception {
        QuizResultDto result = QuizResultDto.builder()
                .resultId(77L)
                .exerciseId(1L)
                .userId(5L)
                .build();

        when(quizService.getResult(77L)).thenReturn(result);

        mockMvc.perform(get("/quiz/result/77"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/quiz/result"));
    }
}
