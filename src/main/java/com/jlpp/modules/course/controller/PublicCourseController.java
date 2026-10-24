package com.jlpp.modules.course.controller;

import com.jlpp.modules.course.service.PublicCatalogService;
import com.jlpp.common.util.CurrentUserSession;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.lesson.dto.LessonPreview;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class PublicCourseController {

    private final PublicCatalogService catalogService;
    private final CurrentUserSession currentUserSession;

    public PublicCourseController(PublicCatalogService catalogService, CurrentUserSession currentUserSession) {
        this.catalogService = catalogService;
        this.currentUserSession = currentUserSession;
    }

    @ModelAttribute("currentRole")
    public String currentRole(HttpSession session) {
        Object role = session.getAttribute("role");
        return role instanceof String roleName ? roleName : "GUEST";
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(HttpSession session) {
        return currentUserSession.isAuthenticated(session);
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredCourses", catalogService.getFeaturedCourses());
        return "pages/public/public-home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("featuredCourses", catalogService.getFeaturedCourses());
        return "pages/public/public-about";
    }

    @GetMapping("/courses")
    public String courseList(@RequestParam(required = false) String level,
                             @RequestParam(required = false) String q,
                             Model model) {
        String selectedLevel = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        String keyword = q == null ? "" : q.trim();
        List<Course> courses = catalogService.findCourses(selectedLevel, keyword);
        model.addAttribute("courses", courses);
        model.addAttribute("selectedLevel", selectedLevel);
        model.addAttribute("searchQuery", keyword);
        model.addAttribute("courseCount", courses.size());
        return "pages/public/public-courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id, Model model) {
        Course course = catalogService.findCourse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy khóa học"));
        List<LessonPreview> lessons = catalogService.findLessonPreviews(id);
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("lessonCount", lessons.size());
        return "pages/public/public-course-detail";
    }

    @GetMapping("/mock-exam")
    public String mockExamIntro(Model model) {
        model.addAttribute("mockExams", catalogService.getJlptMockExams());
        return "pages/public/public-JLPT-mock-exam";
    }

    @GetMapping("/mock-exam/{id}")
    public String mockExamDetails(@PathVariable Long id, Model model) {
        Exam exam = catalogService.findJlptMockExam(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy đề thi thử JLPT"));
        model.addAttribute("exam", exam);
        return "pages/public/public-JLPT-mock-exam";
    }
}
