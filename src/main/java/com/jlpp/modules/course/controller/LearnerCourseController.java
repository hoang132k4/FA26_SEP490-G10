package com.jlpp.modules.course.controller;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.dto.CourseFilterRequest;
import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.entity.StudyMaterial;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import com.jlpp.modules.course.service.CourseRecommendationService;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.course.service.CourseService;
import com.jlpp.modules.course.service.StudyMaterialService;
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

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearnerCourseController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseRegistrationService registrationService;
    private final CourseRecommendationService recommendationService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudyMaterialService studyMaterialService;
    private final FlashcardRepository flashcardRepository;

    /**
     * Helper to get active learner (from Security Principal, Session, or default Supabase account)
     */
    private User getCurrentLearner(Principal principal, HttpSession session) {
        if (principal != null) {
            String name = principal.getName();
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(name)
                    .or(() -> userRepository.findByUsername(name));
            if (userOpt.isPresent()) {
                session.setAttribute("CURRENT_LEARNER_ID", userOpt.get().getId());
                return userOpt.get();
            }
        }
        Long userId = (Long) session.getAttribute("CURRENT_LEARNER_ID");
        if (userId != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
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
     * UC: View Learner Dashboard
     */
    @GetMapping("/dashboard")
    public String learnerDashboard(Principal principal, HttpSession session, Model model) {
        User learner = getCurrentLearner(principal, session);
        List<CourseRegistration> registrations = registrationService.getUserRegistrations(learner.getId());

        // Find currently active course
        CourseRegistration activeRegistration = registrations.stream()
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                .findFirst()
                .orElse(registrations.isEmpty() ? null : registrations.get(0));

        Course activeCourse = null;
        Lesson nextLesson = null;
        int activeCourseLessonCount = 0;
        int activeCourseCompletedLessons = 0;

        if (activeRegistration != null) {
            activeCourse = activeRegistration.getCourse();
            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(activeCourse.getId());
            activeCourseLessonCount = lessons.size();
            int progress = activeRegistration.getProgressPercent() != null ? activeRegistration.getProgressPercent() : 0;
            activeCourseCompletedLessons = (int) Math.round((activeCourseLessonCount * progress) / 100.0);

            // Next lesson is the first uncompleted lesson
            int nextIdx = Math.min(activeCourseCompletedLessons, Math.max(0, activeCourseLessonCount - 1));
            if (!lessons.isEmpty()) {
                nextLesson = lessons.get(nextIdx);
            }
        }

        long completedCourses = registrations.stream()
                .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                .count();

        // Sample lessons for exploration
        List<Lesson> exploreLessons = List.of();
        if (activeCourse != null) {
            exploreLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(activeCourse.getId())
                    .stream().limit(6).toList();
        }

        model.addAttribute("learner", learner);
        model.addAttribute("registrations", registrations);
        model.addAttribute("activeRegistration", activeRegistration);
        model.addAttribute("activeCourse", activeCourse);
        model.addAttribute("nextLesson", nextLesson);
        model.addAttribute("activeCourseLessonCount", activeCourseLessonCount);
        model.addAttribute("activeCourseCompletedLessons", activeCourseCompletedLessons);
        model.addAttribute("completedCourses", completedCourses);
        model.addAttribute("totalRegisteredCourses", registrations.size());
        model.addAttribute("exploreLessons", exploreLessons);
        model.addAttribute("activeMenu", "learner-dashboard");
        return "pages/learning/dashboard";
    }

    /**
     * UC: View Lesson List
     */
    @GetMapping({"/lessons", "/courses/{courseId}/lessons"})
    public String viewLessonList(
            @PathVariable(required = false) Long courseId,
            Principal principal,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(principal, session);
        List<CourseRegistration> registrations = registrationService.getUserRegistrations(learner.getId());

        Long targetCourseId = courseId;
        if (targetCourseId == null) {
            if (!registrations.isEmpty()) {
                targetCourseId = registrations.get(0).getCourse().getId();
            } else {
                List<Course> activeCourses = courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
                if (!activeCourses.isEmpty()) {
                    targetCourseId = activeCourses.get(0).getId();
                } else {
                    List<Course> allCourses = courseRepository.findAll();
                    if (!allCourses.isEmpty()) {
                        targetCourseId = allCourses.get(0).getId();
                    } else {
                        return "redirect:/learning/courses";
                    }
                }
            }
        }

        Course course = courseService.getCourseById(targetCourseId);
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(targetCourseId);

        // Group lessons by Chapter / Section
        Map<String, List<Lesson>> groupedLessons = lessons.stream()
                .collect(Collectors.groupingBy(
                        l -> (l.getSectionName() != null && !l.getSectionName().isBlank()) ? l.getSectionName() : "Chương 1: Bài học căn bản",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        CourseRegistration registration = registrationService.getRegistration(learner.getId(), targetCourseId);
        int progress = (registration != null && registration.getProgressPercent() != null) ? registration.getProgressPercent() : 0;
        int completedCount = (int) Math.round((lessons.size() * progress) / 100.0);

        model.addAttribute("learner", learner);
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("groupedLessons", groupedLessons);
        model.addAttribute("registration", registration);
        model.addAttribute("progressPercent", progress);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalCount", lessons.size());
        model.addAttribute("activeMenu", "my-courses");
        return "pages/learning/lesson-list";
    }

    /**
     * UC: Study Materials
     */
    @GetMapping({"/materials", "/courses/{courseId}/materials"})
    public String viewStudyMaterials(
            @PathVariable(required = false) Long courseId,
            @RequestParam(required = false, defaultValue = "ALL") String level,
            @RequestParam(required = false, defaultValue = "ALL") String skill,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) String keyword,
            Principal principal,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(principal, session);
        List<StudyMaterial> materials;
        Course filterCourse = null;

        if (courseId != null) {
            filterCourse = courseService.getCourseById(courseId);
            materials = studyMaterialService.getMaterialsByCourseId(courseId);
        } else {
            materials = studyMaterialService.searchMaterials(level, skill, type, keyword);
        }

        long audioCount = materials.stream().filter(m -> "AUDIO".equalsIgnoreCase(m.getMaterialType())).count();
        long pdfCount = materials.stream().filter(m -> "PDF".equalsIgnoreCase(m.getMaterialType())).count();
        long docCount = materials.stream().filter(m -> "DOCS".equalsIgnoreCase(m.getMaterialType()) || "SLIDE".equalsIgnoreCase(m.getMaterialType())).count();

        model.addAttribute("learner", learner);
        model.addAttribute("materials", materials);
        model.addAttribute("filterCourse", filterCourse);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedType", type);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("totalCount", materials.size());
        model.addAttribute("audioCount", audioCount);
        model.addAttribute("pdfCount", pdfCount);
        model.addAttribute("docCount", docCount);
        model.addAttribute("activeMenu", "listening");
        return "pages/learning/materials";
    }

    /**
     * UC: Study Lesson
     */
    @GetMapping({"/lessons/{lessonId}", "/courses/{courseId}/lessons/{lessonId}"})
    public String studyLesson(
            @PathVariable Long lessonId,
            @PathVariable(required = false) Long courseId,
            Principal principal,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(principal, session);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học có ID: " + lessonId));
        Course course = lesson.getCourse();

        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        Map<String, List<Lesson>> groupedLessons = courseLessons.stream()
                .collect(Collectors.groupingBy(
                        l -> (l.getSectionName() != null && !l.getSectionName().isBlank()) ? l.getSectionName() : "Chương 1: Bài học căn bản",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Find prev and next lesson
        int currentIdx = -1;
        for (int i = 0; i < courseLessons.size(); i++) {
            if (courseLessons.get(i).getId().equals(lesson.getId())) {
                currentIdx = i;
                break;
            }
        }
        Lesson prevLesson = (currentIdx > 0) ? courseLessons.get(currentIdx - 1) : null;
        Lesson nextLesson = (currentIdx >= 0 && currentIdx < courseLessons.size() - 1) ? courseLessons.get(currentIdx + 1) : null;

        // Attached materials for this lesson
        Long lid = lesson.getLessonId() != null ? lesson.getLessonId() : lesson.getId();
        List<StudyMaterial> attachedMaterials = studyMaterialService.getMaterialsByLessonId(lid);

        // Flashcards for this lesson from backend database
        List<Flashcard> lessonFlashcards = flashcardRepository.findByLessonId(lesson.getId());

        // Registration & progress
        CourseRegistration registration = registrationService.getRegistration(learner.getId(), course.getId());
        int progress = (registration != null && registration.getProgressPercent() != null) ? registration.getProgressPercent() : 0;
        int completedLessonsCount = (int) Math.round((courseLessons.size() * progress) / 100.0);

        model.addAttribute("learner", learner);
        model.addAttribute("lesson", lesson);
        model.addAttribute("course", course);
        model.addAttribute("courseLessons", courseLessons);
        model.addAttribute("groupedLessons", groupedLessons);
        model.addAttribute("currentLessonIndex", currentIdx + 1);
        model.addAttribute("totalLessonsCount", courseLessons.size());
        model.addAttribute("completedLessonsCount", completedLessonsCount);
        model.addAttribute("prevLesson", prevLesson);
        model.addAttribute("nextLesson", nextLesson);
        model.addAttribute("attachedMaterials", attachedMaterials);
        model.addAttribute("lessonFlashcards", lessonFlashcards);
        model.addAttribute("registration", registration);
        model.addAttribute("activeMenu", "my-courses");
        return "pages/learning/lesson-study";
    }

    /**
     * Mark Lesson as Completed
     */
    @PostMapping("/lessons/{lessonId}/complete")
    public String completeLesson(
            @PathVariable Long lessonId,
            Principal principal,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(principal, session);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học có ID: " + lessonId));
        Course course = lesson.getCourse();

        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        int currentIdx = -1;
        for (int i = 0; i < courseLessons.size(); i++) {
            if (courseLessons.get(i).getId().equals(lesson.getId())) {
                currentIdx = i;
                break;
            }
        }

        CourseRegistration registration = registrationService.getRegistration(learner.getId(), course.getId());
        if (registration != null) {
            int newCompletedCount = Math.max(
                    (int) Math.round((courseLessons.size() * (registration.getProgressPercent() != null ? registration.getProgressPercent() : 0)) / 100.0),
                    currentIdx + 1
            );
            int newPercent = Math.min(100, (int) Math.round((newCompletedCount * 100.0) / courseLessons.size()));
            registration.setProgressPercent(newPercent);
            if (newPercent >= 100) {
                registration.setStatus("COMPLETED");
            }
            courseRegistrationRepository.save(registration);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Chúc mừng bạn đã hoàn thành bài học: " + lesson.getTitle() + "!");

        if (currentIdx >= 0 && currentIdx < courseLessons.size() - 1) {
            Lesson next = courseLessons.get(currentIdx + 1);
            return "redirect:/learning/lessons/" + next.getId();
        } else {
            return "redirect:/learning/courses/" + course.getId() + "/lessons";
        }
    }

    /**
     * UC-04: View Course List (Learner Interface)
     */
    @GetMapping("/courses")
    public String learnerCourseList(
            @ModelAttribute("filter") CourseFilterRequest filter,
            Principal principal,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(principal, session);
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
            Principal principal,
            HttpSession session,
            Model model) {

        User learner = getCurrentLearner(principal, session);
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
    public String viewRecommendedCourses(Principal principal, HttpSession session, Model model) {
        User learner = getCurrentLearner(principal, session);
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
            Principal principal,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(principal, session);
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
            Principal principal,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(principal, session);
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
    public String myCourses(Principal principal, HttpSession session, Model model) {
        User learner = getCurrentLearner(principal, session);
        List<CourseRegistration> registrations = registrationService.getUserRegistrations(learner.getId());

        model.addAttribute("learner", learner);
        model.addAttribute("registrations", registrations);
        model.addAttribute("activeMenu", "my-courses");
        return "pages/learning/my-courses";
    }
}
