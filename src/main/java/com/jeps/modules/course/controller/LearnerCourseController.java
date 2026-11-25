package com.jeps.modules.course.controller;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.dto.CourseFilterRequest;
import com.jeps.modules.course.dto.CourseRegistrationRequest;
import com.jeps.modules.course.dto.RecommendedCourseItem;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.entity.StudyMaterial;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.flashcard.entity.Flashcard;
import com.jeps.modules.flashcard.repository.FlashcardRepository;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonStudyView;
import com.jeps.modules.manager.lessons.service.LessonDetailService;
import com.jeps.modules.course.service.CourseRecommendationService;
import com.jeps.modules.course.service.CourseRegistrationService;
import com.jeps.modules.course.service.CourseService;
import com.jeps.modules.course.service.StudyMaterialService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import com.jeps.modules.course.service.CoursePriceCalculator;
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
    private final CoursePriceCalculator priceCalculator;
    private final CourseRepository courseRepository;
    private final CourseRegistrationService registrationService;
    private final CourseRecommendationService recommendationService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudyMaterialService studyMaterialService;
    private final FlashcardRepository flashcardRepository;
    private final com.jeps.modules.learningpath.service.LessonProgressService lessonProgressService;
    private final com.jeps.modules.exam.service.ExamService examService;
    private final LessonDetailService lessonDetailService;

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
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục");
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
            Set<Long> completedIds = lessonProgressService.getCompletedLessonIds(learner.getId(), activeCourse.getId());
            int progress = activeRegistration.getProgressPercent() != null ? activeRegistration.getProgressPercent() : 0;
            activeCourseCompletedLessons = Math.max(completedIds.size(), (int) Math.round((activeCourseLessonCount * progress) / 100.0));
            int calculatedPercent = (activeCourseLessonCount > 0) ? Math.min(100, (int) Math.round((activeCourseCompletedLessons * 100.0) / activeCourseLessonCount)) : 0;
            int effectiveProgress = Math.max(progress, calculatedPercent);
            if (activeRegistration.getProgressPercent() == null || activeRegistration.getProgressPercent() < effectiveProgress) {
                activeRegistration.setProgressPercent(effectiveProgress);
                if (effectiveProgress >= 100) {
                    activeRegistration.setStatus("COMPLETED");
                }
                courseRegistrationRepository.saveAndFlush(activeRegistration);
            }

            // Next lesson is the first uncompleted lesson
            nextLesson = null;
            for (Lesson l : lessons) {
                if (!completedIds.contains(l.getId())) {
                    nextLesson = l;
                    break;
                }
            }
            if (nextLesson == null && !lessons.isEmpty()) {
                int nextIdx = Math.min(activeCourseCompletedLessons, Math.max(0, activeCourseLessonCount - 1));
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
        if (!ensureCourseAccess(learner.getId(), course)) {
            return "redirect:/learning/courses/" + targetCourseId + "/register";
        }
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(targetCourseId);

        // Group lessons by Chapter / Section
        Map<String, List<Lesson>> groupedLessons = lessons.stream()
                .collect(Collectors.groupingBy(
                        l -> (l.getSectionName() != null && !l.getSectionName().isBlank()) ? l.getSectionName() : "Chương 1: Bài học căn bản",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        CourseRegistration registration = registrationService.getRegistration(learner.getId(), targetCourseId);
        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(learner.getId(), targetCourseId);
        int progress = (registration != null && registration.getProgressPercent() != null) ? registration.getProgressPercent() : 0;
        int completedCount = Math.max(completedLessonIds.size(), (int) Math.round((lessons.size() * progress) / 100.0));
        int calculatedPercent = lessons.isEmpty() ? 0 : Math.min(100, (int) Math.round((completedCount * 100.0) / lessons.size()));
        int effectiveProgress = Math.max(progress, calculatedPercent);

        if (registration != null && (registration.getProgressPercent() == null || registration.getProgressPercent() < effectiveProgress)) {
            registration.setProgressPercent(effectiveProgress);
            if (effectiveProgress >= 100) {
                registration.setStatus("COMPLETED");
            }
            courseRegistrationRepository.saveAndFlush(registration);
        }

        model.addAttribute("learner", learner);
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("groupedLessons", groupedLessons);
        model.addAttribute("registration", registration);
        model.addAttribute("progressPercent", effectiveProgress);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalCount", lessons.size());
        model.addAttribute("completedLessonIds", completedLessonIds);
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
            if (!ensureCourseAccess(learner.getId(), filterCourse)) {
                return "redirect:/learning/courses/" + courseId + "/register";
            }
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
        if (courseId != null && !courseId.equals(course.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        if (!ensureCourseAccess(learner.getId(), course)) {
            return "redirect:/learning/courses/" + course.getId() + "/register";
        }

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

        // Attached materials for this lesson (search by both lesson_id and id)
        List<StudyMaterial> attachedMaterials = new ArrayList<>(studyMaterialService.getMaterialsByLessonId(lesson.getId()));
        if (lesson.getLessonId() != null && !lesson.getLessonId().equals(lesson.getId())) {
            List<StudyMaterial> extraMats = studyMaterialService.getMaterialsByLessonId(lesson.getLessonId());
            for (StudyMaterial em : extraMats) {
                if (attachedMaterials.stream().noneMatch(m -> m.getMaterialId().equals(em.getMaterialId()))) {
                    attachedMaterials.add(em);
                }
            }
        }

        // Flashcards for this lesson from backend database
        List<Flashcard> lessonFlashcards = new ArrayList<>(flashcardRepository.findByLessonId(lesson.getId()));
        if (lesson.getLessonId() != null && !lesson.getLessonId().equals(lesson.getId())) {
            List<Flashcard> extraCards = flashcardRepository.findByLessonId(lesson.getLessonId());
            for (Flashcard ec : extraCards) {
                if (lessonFlashcards.stream().noneMatch(c -> c.getFlashcardId().equals(ec.getFlashcardId()))) {
                    lessonFlashcards.add(ec);
                }
            }
        }

        // Registration & progress
        CourseRegistration registration = registrationService.getRegistration(learner.getId(), course.getId());
        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(learner.getId(), course.getId());
        int progress = (registration != null && registration.getProgressPercent() != null) ? registration.getProgressPercent() : 0;
        int completedLessonsCount = Math.max(completedLessonIds.size(), (int) Math.round((courseLessons.size() * progress) / 100.0));
        int calculatedPercent = (courseLessons.size() > 0) ? Math.min(100, (int) Math.round((completedLessonsCount * 100.0) / courseLessons.size())) : 0;
        int effectiveProgress = Math.max(progress, calculatedPercent);

        if (registration != null && (registration.getProgressPercent() == null || registration.getProgressPercent() < effectiveProgress)) {
            registration.setProgressPercent(effectiveProgress);
            if (effectiveProgress >= 100) {
                registration.setStatus("COMPLETED");
            }
            courseRegistrationRepository.saveAndFlush(registration);
        }

        // Enforce Milestone exam gate: if there is an uncompleted Milestone lesson before this lesson, require taking it first
        for (int i = 0; i < currentIdx; i++) {
            Lesson prev = courseLessons.get(i);
            if (prev.isMilestoneExam()) {
                boolean prevDone = lessonProgressService.isLessonCompleted(
                        learner.getId(), course.getId(), prev.getId(), i + 1, courseLessons.size(), effectiveProgress);
                if (!prevDone) {
                    session.setAttribute("ACTIVE_MILESTONE_COURSE_ID", course.getId());
                    session.setAttribute("ACTIVE_MILESTONE_LESSON_ID", prev.getId());
                    return "redirect:" + examService.resolveMilestoneExamUrl(course.getId(), course.getLevel(), prev.getId());
                }
            }
        }

        // If the requested lesson itself is a Milestone exam, redirect directly to the Milestone exam page
        if (lesson.isMilestoneExam()) {
            session.setAttribute("ACTIVE_MILESTONE_COURSE_ID", course.getId());
            session.setAttribute("ACTIVE_MILESTONE_LESSON_ID", lesson.getId());
            return "redirect:" + examService.resolveMilestoneExamUrl(course.getId(), course.getLevel(), lesson.getId());
        }

        // Skill study view from LessonDetailService (Kanji, Grammar, Vocab, Reading, Listening details)
        LessonStudyView studyView = null;
        try {
            Long targetLessonId = (lesson.getLessonId() != null) ? lesson.getLessonId() : lesson.getId();
            studyView = lessonDetailService.buildLessonStudyView(targetLessonId);
            if (studyView == null && !targetLessonId.equals(lesson.getId())) {
                studyView = lessonDetailService.buildLessonStudyView(lesson.getId());
            }
        } catch (Exception exception) {
            log.warn("Could not build lesson study view for lesson {}", lessonId, exception);
        }

        model.addAttribute("learner", learner);
        model.addAttribute("lesson", lesson);
        model.addAttribute("studyView", studyView);
        model.addAttribute("course", course);
        model.addAttribute("courseLessons", courseLessons);
        model.addAttribute("groupedLessons", groupedLessons);
        model.addAttribute("currentLessonIndex", currentIdx + 1);
        model.addAttribute("totalLessonsCount", courseLessons.size());
        model.addAttribute("completedLessonsCount", completedLessonsCount);
        model.addAttribute("completedLessonIds", completedLessonIds);
        model.addAttribute("progressPercent", effectiveProgress);
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

        // Record granular lesson completion in Learning Path & synchronize course registration progress
        try {
            if (!lessonProgressService.markLessonCompleted(learner.getId(), course.getId(), lesson.getId())) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Bạn cần đăng ký và thanh toán khóa học trước khi tiếp tục.");
                return "redirect:/learning/courses/" + course.getId() + "/register";
            }
        } catch (Exception e) {
            log.warn("Could not mark granular lesson completion for userId={}, lessonId={}: {}",
                    learner.getId(), lesson.getId(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể cập nhật tiến độ bài học. Vui lòng thử lại.");
            return "redirect:/learning/lessons/" + lesson.getId();
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
     * Unified Course Catalog: Redirect to the unified /courses page
     */
    @GetMapping("/courses")
    public String learnerCourseList(
            @ModelAttribute("filter") CourseFilterRequest filter,
            Principal principal,
            HttpSession session,
            Model model) {
        String lvl = filter != null && filter.getLevel() != null && !"ALL".equalsIgnoreCase(filter.getLevel()) ? filter.getLevel() : "";
        String kw = filter != null && filter.getKeyword() != null ? filter.getKeyword() : "";
        if (!lvl.isBlank() && !kw.isBlank()) {
            return "redirect:/courses?level=" + lvl + "&q=" + kw;
        } else if (!lvl.isBlank()) {
            return "redirect:/courses?level=" + lvl;
        } else if (!kw.isBlank()) {
            return "redirect:/courses?q=" + kw;
        }
        return "redirect:/courses";
    }

    /**
     * Unified Course Detail Screen: Redirect to unified /courses/{id}
     */
    @GetMapping("/courses/{id}")
    public String learnerCourseDetail(@PathVariable Long id) {
        return "redirect:/courses/" + id;
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

        CourseRegistration existingRegistration =
                registrationService.getRegistration(learner.getId(), course.getId());
        if (registrationService.isUserEnrolled(learner.getId(), course.getId())) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Bạn đã đăng ký khóa học này trước đó. Dưới đây là thông tin chi tiết khóa học của bạn.");
            return "redirect:/courses/" + id;
        }

        if (existingRegistration != null) {
            if (CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(existingRegistration.getStatus())) {
                return "redirect:/checkout/" + id + "?registrationId=" + existingRegistration.getId();
            }
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Đăng ký khóa học hiện không thể tiếp tục. Vui lòng liên hệ hỗ trợ.");
            return "redirect:/courses/" + id;
        }

        java.math.BigDecimal finalPrice = priceCalculator.calculateFinalPrice(course);
        boolean freeCourse = finalPrice.signum() == 0;
        if (!model.containsAttribute("registrationRequest")) {
            CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                    .courseId(course.getId())
                    .fullName(learner.getFullName())
                    .email(learner.getEmail())
                    .phone(learner.getPhone())
                    .paymentMethod(freeCourse ? "FREE" : "SEPAY")
                    .termsAccepted(true)
                    .build();
            model.addAttribute("registrationRequest", request);
        }

        model.addAttribute("learner", learner);
        model.addAttribute("course", course);
        model.addAttribute("finalCoursePrice", finalPrice);
        model.addAttribute("isFreeCourse", freeCourse);
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
            java.math.BigDecimal finalPrice = priceCalculator.calculateFinalPrice(course);
            model.addAttribute("finalCoursePrice", finalPrice);
            model.addAttribute("isFreeCourse", finalPrice.signum() == 0);
            model.addAttribute("activeMenu", "courses");
            return "pages/learning/register-course";
        }

        try {
            java.math.BigDecimal finalPrice = priceCalculator.calculateFinalPrice(course);
            if (finalPrice.signum() == 0) {
                registrationService.registerCourse(learner.getId(), request);
            } else {
                CourseRegistration pendingRegistration =
                        registrationService.createPendingRegistration(learner.getId(), request);
                return "redirect:/checkout/" + id + "?registrationId=" + pendingRegistration.getId();
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Chúc mừng bạn đã đăng ký thành công khóa học: " + course.getTitle() + "! Bạn có thể bắt đầu bài học ngay bây giờ.");
            return "redirect:/courses/" + id;
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("warningMessage", ex.getMessage());
            return "redirect:/courses/" + id;
        } catch (Exception ex) {
            log.error("Failed to register course ID: {} for user ID: {}", id, learner.getId(), ex);
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký khóa học. Vui lòng thử lại sau.");
            model.addAttribute("learner", learner);
            model.addAttribute("course", course);
            java.math.BigDecimal finalPrice = priceCalculator.calculateFinalPrice(course);
            model.addAttribute("finalCoursePrice", finalPrice);
            model.addAttribute("isFreeCourse", finalPrice.signum() == 0);
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

        long inProgressCount = registrations.stream()
                .filter(r -> !"COMPLETED".equalsIgnoreCase(r.getStatus()) && (r.getProgressPercent() == null || r.getProgressPercent() < 100))
                .count();

        long completedCount = registrations.stream()
                .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()) || (r.getProgressPercent() != null && r.getProgressPercent() == 100))
                .count();

        model.addAttribute("learner", learner);
        model.addAttribute("registrations", registrations);
        model.addAttribute("totalCount", registrations.size());
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("activeMenu", "my-courses");
        return "pages/learning/my-courses";
    }

    private boolean ensureCourseAccess(Long userId, Course course) {
        if (registrationService.isUserEnrolled(userId, course.getId())) {
            return true;
        }
        return registrationService.ensureFreeRegistrationForAccess(userId, course.getId()).isPresent();
    }
}
