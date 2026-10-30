package com.jlpp.modules.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Manager area pages. Lesson management lives in
 * {@link com.jlpp.modules.manager.lessons.controller.ManagerLessonController}.
 */
@Controller
@RequestMapping("/manager")
public class ManagerController {

    @GetMapping
    public String managerHome() {
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/reports")
    public String reports() {
        return "pages/manager/report/manager-reports";
    }

    @GetMapping("/questions")
    public String questions() {
        return "pages/manager/question/list";
    }

    @GetMapping("/quizzes")
    public String quizzes() {
        return "pages/manager/quiz/list";
    }

    @GetMapping("/exams")
    public String exams() {
        return "pages/manager/exam/list";
    }

    @GetMapping("/flashcards")
    public String flashcards() {
        return "pages/manager/flashcard/list";
    }
}
