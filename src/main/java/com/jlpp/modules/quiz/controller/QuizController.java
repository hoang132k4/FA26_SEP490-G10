package com.jlpp.modules.quiz.controller;

import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.dto.QuizResultDto;
import com.jlpp.modules.quiz.entity.Exercise;
import com.jlpp.modules.quiz.service.QuizService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    /**
     * Start doing quiz / exercise for a lesson
     */
    private Long resolveUserId(CustomUserDetails userDetails, Principal principal, HttpSession session) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        if (principal != null) {
            String name = principal.getName();
            return userRepository.findByEmailIgnoreCase(name)
                    .or(() -> userRepository.findByUsername(name))
                    .map(User::getId)
                    .orElse(null);
        }
        if (session != null) {
            Object id = session.getAttribute("CURRENT_LEARNER_ID");
            if (id instanceof Long l) return l;
        }
        return null;
    }

    @GetMapping("/do")
    public String doQuiz(
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) Long exerciseId,
            Model model
    ) {
        if (lessonId == null && exerciseId == null) {
            return "redirect:/learning/dashboard";
        }

        Exercise exercise;
        if (exerciseId != null) {
            exercise = quizService.getOrCreateExerciseForLesson(exerciseId);
        } else {
            exercise = quizService.getOrCreateExerciseForLesson(lessonId);
        }

        List<QuizQuestionDto> questions = quizService.getQuestionsForExercise(exercise.getExerciseId());
        Lesson lesson = (exercise.getLessonId() != null) ? lessonRepository.findById(exercise.getLessonId()).orElse(null) : null;
        Course course = (lesson != null) ? lesson.getCourse() : null;

        model.addAttribute("exercise", exercise);
        model.addAttribute("questions", questions);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("lesson", lesson);
        model.addAttribute("course", course);
        model.addAttribute("activeMenu", "my-courses");

        return "pages/quiz/do-quiz";
    }

    /**
     * Submit quiz answers and grade
     */
    @PostMapping("/submit")
    public String submitQuiz(
            @RequestParam("exerciseId") Long exerciseId,
            @RequestParam Map<String, String> allParams,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Principal principal,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = resolveUserId(userDetails, principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/quiz/do?exerciseId=" + exerciseId;
        }

        // Parse answers from inputs named: "answer_{questionId}"
        Map<Long, String> answers = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("answer_")) {
                try {
                    Long qId = Long.parseLong(key.substring(7));
                    answers.put(qId, entry.getValue());
                } catch (NumberFormatException ignored) {}
            }
        }

        log.info("Submitting quiz for exerciseId: {}, userId: {}, answers count: {}", exerciseId, userId, answers.size());
        QuizResultDto result = quizService.submitQuiz(exerciseId, userId, answers);
        return "redirect:/quiz/result/" + result.getResultId();
    }

    /**
     * View quiz result and question review with explanations
     */
    @GetMapping("/result/{resultId}")
    public String viewResult(@PathVariable Long resultId,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        Long userId = resolveUserId(userDetails, principal, session);
        if (userId == null) {
            return "redirect:/auth/login?continue=/quiz/result/" + resultId;
        }

        QuizResultDto result = quizService.getResult(resultId);
        if (!userId.equals(result.getUserId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền xem kết quả này.");
        }

        Lesson lesson = (result.getLessonId() != null) ? lessonRepository.findById(result.getLessonId()).orElse(null) : null;
        Course course = (lesson != null) ? lesson.getCourse() : null;

        model.addAttribute("result", result);
        model.addAttribute("lesson", lesson);
        model.addAttribute("course", course);
        model.addAttribute("activeMenu", "my-courses");

        return "pages/quiz/result";
    }
}
