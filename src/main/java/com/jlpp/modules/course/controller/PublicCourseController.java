package com.jlpp.modules.course.controller;

import com.jlpp.modules.course.dto.CourseFilterRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredCourses", courseService.getFeaturedCourses());
        return "pages/public/home";
    }

    @GetMapping("/about")
    public String about() {
        return "pages/public/about";
    }

    @GetMapping("/mock-exam")
    public String mockExamIntro() {
        return "pages/public/mock-exam";
    }

    /**
     * UC-04: View Course List (Public / Guest)
     */
    @GetMapping("/courses")
    public String courseList(@ModelAttribute("filter") CourseFilterRequest filter, Model model) {
        if (filter == null) {
            filter = new CourseFilterRequest();
        }
        Page<Course> coursePage = courseService.getCourses(filter);
        model.addAttribute("coursePage", coursePage);
        model.addAttribute("courses", coursePage.getContent());
        model.addAttribute("filter", filter);
        model.addAttribute("levels", List.of("ALL", "N5", "N4", "N3", "N2", "N1"));
        return "pages/public/courses";
    }

    /**
     * UC-05: View Course Details (Public / Guest)
     */
    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        List<Course> relatedCourses = courseService.getOtherCourses(id, course.getLevel(), 3);
        
        model.addAttribute("course", course);
        model.addAttribute("lessons", course.getLessons());
        model.addAttribute("relatedCourses", relatedCourses);
        model.addAttribute("isLearner", false);
        model.addAttribute("isEnrolled", false);
        return "pages/public/course-detail";
    }
}
