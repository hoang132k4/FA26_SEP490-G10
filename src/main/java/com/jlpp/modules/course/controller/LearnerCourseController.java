package com.jlpp.modules.course.controller;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.dto.CourseFilterRequest;
import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.service.CourseRecommendationService;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.course.service.CourseService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearnerCourseController {

    private final CourseService courseService;
    private final CourseRegistrationService registrationService;
    private final CourseRecommendationService recommendationService;
    private final UserRepository userRepository;

    /**
     * Helper to get or mock the active learner
     */
    private User getCurrentLearner(HttpSession session) {
        Long userId = (Long) session.getAttribute("CURRENT_LEARNER_ID");
        if (userId != null) {
            return userRepository.findById(userId).orElseGet(this::getOrCreateDefaultLearner);
        }
        User defaultLearner = getOrCreateDefaultLearner();
        session.setAttribute("CURRENT_LEARNER_ID", defaultLearner.getId());
        return defaultLearner;
    }

    private User getOrCreateDefaultLearner() {
        return userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner@jeps.edu.vn")
                        .username("learner")
                        .fullName("Nguyễn Văn Học Viên")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));
    }

    /**
     * UC-04: View Course List (Learner Interface)
     */
    @GetMapping("/courses")
    public String learnerCourseList(
            @ModelAttribute("filter") CourseFilterRequest filter,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(session);
        Page<Course> coursePage = courseService.getCourses(filter);

        model.addAttribute("learner", learner);
        model.addAttribute("coursePage", coursePage);
        model.addAttribute("courses", coursePage.getContent());
        model.addAttribute("filter", filter);
        model.addAttribute("levels", List.of("ALL", "N5", "N4", "N3", "N2", "N1"));
        model.addAttribute("activeMenu", "courses");
        return "pages/learning/course-list";
    }

    /**
     * UC-05: View Course Detail (Learner)
     */
    @GetMapping("/courses/{id}")
    public String learnerCourseDetail(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(session);
        Course course = courseService.getCourseById(id);
        boolean isEnrolled = registrationService.isUserEnrolled(learner.getId(), course.getId());
        CourseRegistration registration = registrationService.getRegistration(learner.getId(), course.getId());
        List<Course> relatedCourses = courseService.getOtherCourses(id, course.getLevel(), 3);

        model.addAttribute("learner", learner);
        model.addAttribute("course", course);
        model.addAttribute("lessons", course.getLessons());
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("registration", registration);
        model.addAttribute("relatedCourses", relatedCourses);
        model.addAttribute("activeMenu", "courses");
        return "pages/learning/course-detail";
    }

    /**
     * UC-18: View Recommended Course
     */
    @GetMapping("/courses/recommended")
    public String viewRecommendedCourses(HttpSession session, Model model) {
        User learner = getCurrentLearner(session);
        List<RecommendedCourseItem> recommendedItems = recommendationService.getRecommendationsForLearner(learner.getId());
        List<Course> otherCourses = recommendationService.getOtherAvailableCourses(learner.getId());

        model.addAttribute("learner", learner);
        model.addAttribute("recommendedItems", recommendedItems);
        model.addAttribute("otherCourses", otherCourses);
        model.addAttribute("levels", List.of("N5", "N4", "N3", "N2", "N1"));
        model.addAttribute("activeMenu", "recommended");
        return "pages/learning/recommended-courses";
    }

    /**
     * UC-19: Register for Course Screen
     */
    @GetMapping("/courses/{id}/register")
    public String registerCoursePage(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(session);
        Course course = courseService.getCourseById(id);

        if (registrationService.isUserEnrolled(learner.getId(), course.getId())) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Bạn đã đăng ký khóa học này trước đó. Dưới đây là thông tin chi tiết khóa học của bạn.");
            return "redirect:/learning/courses/" + id;
        }

        if (!model.containsAttribute("registrationRequest")) {
            CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                    .courseId(course.getId())
                    .fullName(learner.getFullName())
                    .email(learner.getEmail())
                    .phone(learner.getPhone())
                    .paymentMethod(course.getPrice() == 0 ? "FREE" : "VNPAY")
                    .termsAccepted(true)
                    .build();
            model.addAttribute("registrationRequest", request);
        }

        model.addAttribute("learner", learner);
        model.addAttribute("course", course);
        model.addAttribute("activeMenu", "courses");
        return "pages/learning/register-course";
    }

    /**
     * UC-19: Process Register for Course
     */
    @PostMapping("/courses/{id}/register")
    public String handleRegisterCourse(
            @PathVariable Long id,
            @Valid @ModelAttribute("registrationRequest") CourseRegistrationRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(session);
        Course course = courseService.getCourseById(id);
        request.setCourseId(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("learner", learner);
            model.addAttribute("course", course);
            model.addAttribute("activeMenu", "courses");
            return "pages/learning/register-course";
        }

        try {
            registrationService.registerCourse(learner.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Chúc mừng bạn đã đăng ký thành công khóa học: " + course.getTitle() + "! Bạn có thể bắt đầu bài học ngay bây giờ.");
            return "redirect:/learning/courses/" + id;
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("warningMessage", ex.getMessage());
            return "redirect:/learning/courses/" + id;
        } catch (Exception ex) {
            log.error("Failed to register course ID: {} for user ID: {}", id, learner.getId(), ex);
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký khóa học. Vui lòng thử lại sau.");
            model.addAttribute("learner", learner);
            model.addAttribute("course", course);
            model.addAttribute("activeMenu", "courses");
            return "pages/learning/register-course";
        }
    }

    /**
     * Learner My Courses
     */
    @GetMapping("/my-courses")
    public String myCourses(HttpSession session, Model model) {
        User learner = getCurrentLearner(session);
        List<CourseRegistration> registrations = registrationService.getUserRegistrations(learner.getId());

        model.addAttribute("learner", learner);
        model.addAttribute("registrations", registrations);
        model.addAttribute("activeMenu", "my-courses");
        return "pages/learning/my-courses";
    }
}
