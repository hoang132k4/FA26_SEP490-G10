package com.jlpp.modules.course.controller;

import com.jlpp.common.util.CurrentUserSession;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.course.service.CourseService;
import com.jlpp.modules.course.service.PublicCatalogService;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PublicCourseController {

    private final PublicCatalogService catalogService;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final CurrentUserSession currentUserSession;

    @ModelAttribute("currentRole")
    public String currentRole(HttpSession session) {
        Object role = session.getAttribute("role");
        return role instanceof String roleName ? roleName : "GUEST";
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(HttpSession session) {
        return currentUserSession.isAuthenticated(session);
    }

    private User resolveCurrentUser(Principal principal, HttpSession session) {
        if (principal != null && principal.getName() != null) {
            String identifier = principal.getName().trim();
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> userRepository.findByUsername(identifier));
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }
        if (session != null) {
            Long userId = (Long) session.getAttribute("CURRENT_LEARNER_ID");
            if (userId != null) {
                return userRepository.findById(userId).orElse(null);
            }
            String email = (String) session.getAttribute("CURRENT_USER_EMAIL");
            if (email != null) {
                return userRepository.findByEmailIgnoreCase(email).orElse(null);
            }
        }
        return null;
    }

    /**
     * Homepage: Serves both Guest and Authenticated Learner
     * If Learner is logged in, delivers quick-study hub & active learning progress widget
     */
    @GetMapping("/")
    public String home(Principal principal, HttpSession session, Model model) {
        User currentUser = resolveCurrentUser(principal, session);
        List<Course> featuredCourses = catalogService.getFeaturedCourses();
        model.addAttribute("featuredCourses", featuredCourses);

        if (currentUser != null) {
            List<CourseRegistration> registrations = courseRegistrationRepository.findByUserIdWithCourse(currentUser.getId());
            Set<Long> enrolledCourseIds = registrations.stream()
                    .map(r -> r.getCourse().getId())
                    .collect(Collectors.toSet());

            CourseRegistration activeReg = registrations.stream()
                    .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                    .findFirst()
                    .orElse(registrations.isEmpty() ? null : registrations.get(0));

            Course activeCourse = null;
            Lesson nextLesson = null;
            int completedLessons = 0;
            int totalLessons = 0;

            if (activeReg != null) {
                activeCourse = activeReg.getCourse();
                List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(activeCourse.getId());
                totalLessons = lessons.size();
                int progress = activeReg.getProgressPercent() != null ? activeReg.getProgressPercent() : 0;
                completedLessons = (int) Math.round((totalLessons * progress) / 100.0);
                int nextIdx = Math.min(completedLessons, Math.max(0, totalLessons - 1));
                if (!lessons.isEmpty()) {
                    nextLesson = lessons.get(nextIdx);
                }
            }

            model.addAttribute("learner", currentUser);
            model.addAttribute("registrations", registrations);
            model.addAttribute("registeredCourseCount", registrations.size());
            model.addAttribute("activeRegistration", activeReg);
            model.addAttribute("activeCourse", activeCourse);
            model.addAttribute("nextLesson", nextLesson);
            model.addAttribute("completedLessons", completedLessons);
            model.addAttribute("totalLessons", totalLessons);
            model.addAttribute("enrolledCourseIds", enrolledCourseIds);
        }

        model.addAttribute("activeMenu", "home");
        return "pages/public/public-home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("featuredCourses", catalogService.getFeaturedCourses());
        model.addAttribute("activeMenu", "about");
        return "pages/public/public-about";
    }

    /**
     * Unified Course Catalog: For both Guest and Logged-in Learner
     */
    @GetMapping("/courses")
    public String courseList(@RequestParam(required = false) String level,
                             @RequestParam(required = false) String q,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        String selectedLevel = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        String keyword = q == null ? "" : q.trim();
        List<Course> courses = catalogService.findCourses(selectedLevel, keyword);

        User currentUser = resolveCurrentUser(principal, session);
        Set<Long> enrolledCourseIds = Set.of();
        if (currentUser != null) {
            List<CourseRegistration> registrations = courseRegistrationRepository.findByUserIdWithCourse(currentUser.getId());
            enrolledCourseIds = registrations.stream()
                    .map(r -> r.getCourse().getId())
                    .collect(Collectors.toSet());
            model.addAttribute("learner", currentUser);
            model.addAttribute("registeredCourseCount", registrations.size());
        }

        model.addAttribute("courses", courses);
        model.addAttribute("selectedLevel", selectedLevel);
        model.addAttribute("searchQuery", keyword);
        model.addAttribute("courseCount", courses.size());
        model.addAttribute("enrolledCourseIds", enrolledCourseIds);
        model.addAttribute("activeMenu", "courses");
        return "pages/public/public-courses";
    }

    /**
     * Unified Course Detail Screen: Serves both Guest and Logged-in Learner
     */
    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id,
                               Principal principal,
                               HttpSession session,
                               Model model) {
        Course course = catalogService.findCourse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy khóa học"));
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(id);
        List<Course> relatedCourses = courseService.getOtherCourses(id, course.getLevel(), 3);

        User currentUser = resolveCurrentUser(principal, session);
        boolean isEnrolled = false;
        CourseRegistration registration = null;

        if (currentUser != null) {
            isEnrolled = courseRegistrationRepository.existsByUserIdAndCourseId(currentUser.getId(), id);
            registration = courseRegistrationRepository.findByUserIdAndCourseId(currentUser.getId(), id).orElse(null);
            model.addAttribute("learner", currentUser);
        }

        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("lessonCount", lessons.size());
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("registration", registration);
        model.addAttribute("relatedCourses", relatedCourses);
        model.addAttribute("activeMenu", "courses");
        return "pages/learning/course-detail";
    }

    @GetMapping("/mock-exam")
    public String mockExamIntro(Model model) {
        model.addAttribute("mockExams", catalogService.getJlptMockExams());
        model.addAttribute("activeMenu", "mock-exam");
        return "pages/public/public-JLPT-mock-exam";
    }

    @GetMapping("/mock-exam/{id}")
    public String mockExamDetails(@PathVariable Long id, Model model) {
        Exam exam = catalogService.findJlptMockExam(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy đề thi thử JLPT"));
        model.addAttribute("exam", exam);
        model.addAttribute("activeMenu", "mock-exam");
        return "pages/public/public-JLPT-mock-exam";
    }
}
