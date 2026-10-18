package com.jlpp.modules.exam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ExamController {

    @GetMapping("/exams")
    public String takeExamPage() {
        return "pages/exam/take-exam";
    }

    @GetMapping("/exams/result/{id}")
    public String examResultPage(@PathVariable Long id, Model model) {
        model.addAttribute("examId", id);
        return "pages/exam/result";
    }

    @GetMapping("/exams/history")
    public String examHistoryPage() {
        return "pages/exam/history-list";
    }

    @GetMapping("/admin/exams")
    public String adminExamList() {
        return "pages/admin/exam/list";
    }
}
