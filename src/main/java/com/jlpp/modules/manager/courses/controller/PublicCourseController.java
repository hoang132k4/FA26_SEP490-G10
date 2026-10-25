package com.jlpp.modules.manager.courses.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicPageController {

    @GetMapping("/")
    public String home() {
        return "pages/public/home";
    }

    @GetMapping("/about")
    public String about() {
        return "pages/public/about";
    }

    @GetMapping("/courses")
    public String courseList() {
        return "pages/public/courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id, Model model) {
        model.addAttribute("courseId", id);
        return "pages/public/course-detail";
    }

    @GetMapping("/mock-exam")
    public String mockExamIntro() {
        return "pages/public/mock-exam";
    }
}
