package com.jlpp.modules.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.jlpp.modules.course.dto.CourseOption;
import com.jlpp.modules.course.service.CourseService;

/**
 * Manager area pages. Lesson management lives in
 * {@link com.jlpp.modules.lesson.controller.ManagerLessonController}.
 */
@Controller
@RequestMapping("/manager")
public class ManagerController {

    private static final Logger log = LoggerFactory.getLogger(ManagerController.class);

    private final CourseService courseService;

    public ManagerController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public String managerHome() {
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/courses")
    public String courses(Model model) {
        List<Map<String, Object>> courses;
        try {
            courses = courseService.findCourses().stream()
                    .map(this::toCourseView)
                    .toList();
        } catch (DataAccessException | TransactionException exception) {
            courses = List.of();
            model.addAttribute("courseError",
                    "Không kết nối được Supabase. Kiểm tra file .env và thông tin Session Pooler rồi tải lại.");
        }
        model.addAttribute("courses", courses);
        model.addAttribute("courseCount", courses.size());
        model.addAttribute("courseForm", model.containsAttribute("courseForm")
                ? model.getAttribute("courseForm") : null);
        return "pages/manager/course/manager-courses";
    }

    @GetMapping("/courses/new")
    public String newCourse(Model model) {
        model.addAttribute("courseForm", Map.of("id", "", "name", "", "level", "N5", "status", "PUBLISHED"));
        return courses(model);
    }

    @GetMapping("/courses/{courseId}/edit")
    public String editCourse(@PathVariable long courseId, Model model, RedirectAttributes redirectAttributes) {
        return courseService.findCourse(courseId).map(course -> {
            model.addAttribute("courseForm", Map.of("id", course.id(), "name", course.title(),
                    "level", course.level(), "status", normalizedCourseStatus(course.status())));
            return courses(model);
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("courseError", "Không tìm thấy khóa học cần sửa.");
            return "redirect:/manager/courses";
        });
    }

    @PostMapping("/courses")
    public String createCourse(@RequestParam String name, @RequestParam String level,
                               @RequestParam String status, RedirectAttributes redirectAttributes) {
        try {
            courseService.createCourse(clean(name), validLevel(level), validStatus(status));
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã thêm khóa học.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("courseError", exception.getMessage());
        } catch (DataAccessException exception) {
            log.error("Could not create course", exception);
            redirectAttributes.addFlashAttribute("courseError", "Không thể hoàn tất thao tác thêm khóa học. Kiểm tra log ứng dụng để biết chi tiết.");
        }
        return "redirect:/manager/courses";
    }

    @PostMapping("/courses/{courseId}")
    public String updateCourse(@PathVariable long courseId, @RequestParam String name,
                               @RequestParam String level, @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            courseService.updateCourse(courseId, clean(name), validLevel(level), validStatus(status));
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã cập nhật khóa học.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("courseError", exception.getMessage());
        } catch (DataAccessException exception) {
            log.error("Could not update course {}", courseId, exception);
            redirectAttributes.addFlashAttribute("courseError", "Không thể hoàn tất thao tác cập nhật khóa học. Kiểm tra log ứng dụng để biết chi tiết.");
        }
        return "redirect:/manager/courses";
    }

    private String clean(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException("Tên khóa học không được để trống và tối đa 255 ký tự.");
        }
        return value;
    }

    private String validLevel(String level) {
        String value = level == null ? "" : level.trim().toUpperCase();
        if (!List.of("N5", "N4", "N3", "N2", "N1").contains(value)) {
            throw new IllegalArgumentException("Vui lòng chọn cấp độ JLPT hợp lệ.");
        }
        return value;
    }

    private String validStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(value)) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái hợp lệ.");
        }
        return value;
    }

    @GetMapping("/flashcards")
    public String flashcards() {
        return "pages/manager/course/manager-flashcards";
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

    private Map<String, Object> toCourseView(CourseOption row) {
        Map<String, Object> course = new LinkedHashMap<>();
        course.put("id", row.id());
        course.put("name", row.title());
        course.put("level", row.level());
        course.put("description", "-");
        course.put("status", row.status());
        course.put("statusLabel", courseStatusLabel(row.status()));
        course.put("statusFilterValue", normalizedCourseStatus(row.status()));
        course.put("lessonCount", row.lessonCount());
        course.put("createdAt", "-");
        return course;
    }

    private String courseStatusLabel(String status) {
        if ("PUBLISHED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)) {
            return "Published";
        }
        if ("UNPUBLISHED".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
            return "Unpublished";
        }
        if ("DRAFT".equalsIgnoreCase(status)) {
            return "Unpublished";
        }
        if ("ARCHIVED".equalsIgnoreCase(status)) {
            return "Archived";
        }
        return status;
    }

    private String normalizedCourseStatus(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "published";
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return "DRAFT";
        }
        if ("UNPUBLISHED".equalsIgnoreCase(status)) {
            return "DRAFT";
        }
        return status == null ? null : status.toUpperCase();
    }
}
