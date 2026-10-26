package com.jlpp.modules.exam.controller;

import com.jlpp.common.util.CurrentUserSession;
import com.jlpp.modules.course.service.PublicCatalogService;
import com.jlpp.modules.exam.entity.Exam;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class ExamController {

    private final PublicCatalogService catalogService;
    private final CurrentUserSession currentUserSession;

    public ExamController(PublicCatalogService catalogService, CurrentUserSession currentUserSession) {
        this.catalogService = catalogService;
        this.currentUserSession = currentUserSession;
    }

    @GetMapping("/exams")
    public String takeExamPage() {
        return "redirect:/mock-exam";
    }

    @GetMapping("/exams/{id}/start")
    public String startExam(@PathVariable Long id, HttpSession session, Model model) {
        if (!currentUserSession.isAuthenticated(session)) {
            return "redirect:/auth/register?continue=/exams/" + id + "/start";
        }

        Exam exam = catalogService.findJlptMockExam(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy đề thi thử JLPT"));
        model.addAttribute("exam", exam);
        return "pages/exam/take-exam";
    }

    @GetMapping("/exams/result/{id}")
    public String examResultPage(@PathVariable Long id, Model model, HttpSession session) {
        if (!currentUserSession.isAuthenticated(session)) {
            return "redirect:/auth/register?continue=/exams/result/" + id;
        }
        model.addAttribute("examId", id);
        return "pages/exam/result";
    }

    @GetMapping("/exams/history")
    public String examHistoryPage(HttpSession session) {
        if (!currentUserSession.isAuthenticated(session)) {
            return "redirect:/auth/register?continue=/exams/history";
        }
        return "pages/exam/history-list";
    }

    @GetMapping("/admin/exams")
    public String adminExamList() {
        return "pages/admin/exam/list";
    }
}
