package com.jeps.modules.course.controller;

import com.jeps.common.util.CurrentUserSession;
import com.jeps.modules.course.dto.CourseRegistrationRequest;
import com.jeps.modules.course.dto.RecommendedCourseItem;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.entity.StudyMaterial;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearnerCourseController {

    private final CourseService courseService;
    private final CoursePriceCalculator priceCalculator;
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
     * Helper to get active learner from Security Principal or Session.
     */
    private User getCurrentLearner(Principal principal, HttpSession session) {
        User user = CurrentUserSession.resolveSessionUser(principal, session, userRepository, true);
        if (user != null) {
            return user;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục");
    }

    private int computeCompletedCount(int recordedCompleted, CourseRegistration registration, int totalLessons) {
        int progress = (registration != null && registration.getProgressPercent() != null)
                ? registration.getProgressPercent()
                : 0;
        return Math.max(recordedCompleted, (int) Math.round((totalLessons * progress) / 100.0));
    }

    private int syncRegistrationProgress(CourseRegistration registration, int completedCount, int totalLessons) {
        int progress = (registration != null && registration.getProgressPercent() != null)
                ? registration.getProgressPercent()
                : 0;
        int calculatedPercent = totalLessons > 0
                ? Math.min(100, (int) Math.round((completedCount * 100.0) / totalLessons))
                : 0;
        int effectiveProgress = Math.max(progress, calculatedPercent);
        if (registration != null && (registration.getProgressPercent() == null
                || registration.getProgressPercent() < effectiveProgress)) {
            registration.setProgressPercent(effectiveProgress);
            if (effectiveProgress >= 100) {
                registration.setStatus("COMPLETED");
            }
            courseRegistrationRepository.saveAndFlush(registration);
        }
        return effectiveProgress;
    }

    /**
     * UC: View Learner Dashboard
     */
    @GetMapping("/dashboard")
    public String learnerDashboard(Principal principal, HttpSession session) {
        getCurrentLearner(principal, session);
        return "redirect:/learning/my-courses";
    }

    /**
     * UC: View Lesson List
     */
    @GetMapping({ "/lessons", "/courses/{courseId}/lessons" })
    public String viewLessonList(
            @PathVariable(required = false) Long courseId,
            Principal principal,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(principal, session);
        List<CourseRegistration> registrations = registrationService.getUserRegistrations(learner.getId());

        Long targetCourseId = courseId;
        if (targetCourseId == null) {
            if (!registrations.isEmpty()) {
                targetCourseId = registrations.get(0).getCourse().getId();
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Bạn chưa đăng ký khóa học nào. Vui lòng chọn và đăng ký một khóa học để bắt đầu học tập!");
                return "redirect:/learning/courses";
            }
        }

        Course course = courseService.getCourseById(targetCourseId);
        if (!ensureCourseAccess(learner.getId(), course)) {
            CourseRegistration registration = registrationService.getRegistration(learner.getId(), targetCourseId);
            if (registration != null
                    && CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Khóa học này đang chờ thanh toán. Vui lòng hoàn tất thanh toán để tiếp tục!");
                return "redirect:/learning/courses/" + targetCourseId + "/register";
            }
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Bạn chưa đăng ký khóa học này. Vui lòng đăng ký để truy cập lộ trình bài học!");
            return "redirect:/courses/" + targetCourseId;
        }
        List<Lesson> lessons = courseService.getCourseLessons(targetCourseId).stream()
                .filter(l -> !"INACTIVE".equalsIgnoreCase(l.getStatus()))
                .toList();

        // Group lessons by Chapter / Section
        Map<String, List<Lesson>> groupedLessons = courseService.groupLessonsBySection(lessons);

        final Long finalCourseId = targetCourseId;
        CourseRegistration registration = registrations.stream()
                .filter(r -> r.getCourse() != null && r.getCourse().getId().equals(finalCourseId))
                .findFirst()
                .orElseGet(() -> registrationService.getRegistration(learner.getId(), finalCourseId));
        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(learner.getId(), targetCourseId);
        int completedCount = computeCompletedCount(completedLessonIds.size(), registration, lessons.size());
        int effectiveProgress = syncRegistrationProgress(registration, completedCount, lessons.size());

        com.jeps.modules.exam.entity.Exam midtermExam = examService.getCourseExamsByType(targetCourseId, "MIDTERM")
                .stream().findFirst().orElse(null);
        com.jeps.modules.exam.entity.Exam finalExam = examService.getCourseExamsByType(targetCourseId, "FINAL").stream()
                .findFirst().orElse(null);

        Set<Long> unlockedExamIds = new HashSet<>();
        Map<Long, com.jeps.modules.exam.dto.ExamPrerequisiteResult> examPrereqMap = new HashMap<>();
        CourseViewHelper.populateExamPrerequisites(
                examService,
                learner.getId(),
                lessons,
                midtermExam,
                finalExam,
                unlockedExamIds,
                examPrereqMap);

        model.addAttribute("learner", learner);
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("groupedLessons", groupedLessons);
        model.addAttribute("registration", registration);
        model.addAttribute("progressPercent", effectiveProgress);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalCount", lessons.size());
        model.addAttribute("completedLessonIds", completedLessonIds);
        model.addAttribute("midtermExam", midtermExam);
        model.addAttribute("finalExam", finalExam);
        model.addAttribute("unlockedExamIds", unlockedExamIds);
        model.addAttribute("examPrereqMap", examPrereqMap);
        model.addAttribute("activeMenu", "my-courses");
        return "pages/learning/lesson-list";
    }

    /**
     * UC: Study Materials
     */
    @GetMapping({ "/materials", "/courses/{courseId}/materials" })
    public String viewStudyMaterials(
            @PathVariable(required = false) Long courseId,
            @RequestParam(required = false, defaultValue = "ALL") String level,
            @RequestParam(required = false, defaultValue = "ALL") String skill,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) String keyword,
            Principal principal,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(principal, session);
        List<StudyMaterial> materials;
        Course filterCourse = null;

        if (courseId != null) {
            boolean isEnrolled = registrationService.isUserEnrolled(learner.getId(), courseId);
            if (!isEnrolled) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Bạn chưa đăng ký khóa học này để truy cập kho tài liệu.");
                return "redirect:/courses/" + courseId;
            }
            filterCourse = courseService.getCourseById(courseId);
            if (!ensureCourseAccess(learner.getId(), filterCourse)) {
                return "redirect:/learning/courses/" + courseId + "/register";
            }
            materials = studyMaterialService.getMaterialsByCourseId(courseId);
        } else {
            List<StudyMaterial> searched = studyMaterialService.searchMaterials(level, skill, type, keyword);
            Set<Long> matLessonIds = searched.stream()
                    .map(StudyMaterial::getLessonId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            Map<Long, Lesson> matLessonMap = matLessonIds.isEmpty()
                    ? Collections.emptyMap()
                    : lessonRepository.findAllById(matLessonIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Lesson::getId, l -> l, (a, b) -> a));
            Set<Long> enrolledIds = registrationService.getUserRegistrations(learner.getId()).stream()
                    .map(r -> r.getCourse() != null ? r.getCourse().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            materials = searched.stream()
                    .filter(m -> {
                        if (m.getLessonId() == null) return true;
                        Lesson l = matLessonMap.get(m.getLessonId());
                        if (l == null || "INACTIVE".equalsIgnoreCase(l.getStatus())) return false;
                        Course c = l.getCourse();
                        if (c == null || !isCoursePublicStatus(c)) return false;
                        return l.isFreePreview() || enrolledIds.contains(c.getId());
                    })
                    .toList();
        }

        long audioCount = materials.stream().filter(m -> "AUDIO".equalsIgnoreCase(m.getMaterialType())).count();
        long pdfCount = materials.stream().filter(m -> "PDF".equalsIgnoreCase(m.getMaterialType())).count();
        long docCount = materials.stream().filter(
                m -> "DOCS".equalsIgnoreCase(m.getMaterialType()) || "SLIDE".equalsIgnoreCase(m.getMaterialType()))
                .count();

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
    @GetMapping({ "/lessons/{lessonId}", "/courses/{courseId}/lessons/{lessonId}" })
    public String studyLesson(
            @PathVariable Long lessonId,
            @PathVariable(required = false) Long courseId,
            Principal principal,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getCurrentLearner(principal, session);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học có ID: " + lessonId));
        Course course = (lesson.getCourse() != null)
                ? courseService.getCourseById(lesson.getCourse().getId())
                : null;
        if (course == null && lesson.getCourse() != null) {
            course = lesson.getCourse();
        }
        if (course == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        if (courseId != null && !courseId.equals(course.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        if ("INACTIVE".equalsIgnoreCase(lesson.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bài học này hiện đang tạm ẩn.");
            return "redirect:/learning/courses/" + course.getId() + "/lessons";
        }

        // Chặn quyền truy cập bài học nếu học viên chưa đăng ký khóa học (ngoại trừ bài học cho phép học thử)
        boolean isEnrolled = registrationService.isUserEnrolled(learner.getId(), course.getId());
        if (!isEnrolled) {
            CourseRegistration registration = course != null
                    ? registrationService.getRegistration(learner.getId(), course.getId())
                    : null;
            if (registration != null
                    && CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Khóa học này đang chờ thanh toán. Vui lòng hoàn tất thanh toán để tiếp tục!");
                return "redirect:/learning/courses/" + course.getId() + "/register";
            }
            boolean isFree = lesson.isFreePreview();
            if (!isFree) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Bạn chưa đăng ký khóa học này. Vui lòng đăng ký khóa học để truy cập bài học!");
                return "redirect:/courses/" + course.getId();
            }
        }

        List<Lesson> courseLessons = courseService.getCourseLessons(course.getId()).stream()
                .filter(l -> !"INACTIVE".equalsIgnoreCase(l.getStatus()))
                .toList();
        Map<String, List<Lesson>> groupedLessons = courseService.groupLessonsBySection(courseLessons);

        // Find prev and next lesson
        int currentIdx = -1;
        for (int i = 0; i < courseLessons.size(); i++) {
            if (courseLessons.get(i).getId().equals(lesson.getId())) {
                currentIdx = i;
                break;
            }
        }
        Lesson prevLesson = (currentIdx > 0) ? courseLessons.get(currentIdx - 1) : null;
        Lesson nextLesson = (currentIdx >= 0 && currentIdx < courseLessons.size() - 1)
                ? courseLessons.get(currentIdx + 1)
                : null;

        // Attached materials for this lesson (single indexed query)
        List<StudyMaterial> attachedMaterials = studyMaterialService.getMaterialsByLessonId(lesson.getId());

        // Flashcards for this lesson from backend database
        List<Flashcard> lessonFlashcards = flashcardRepository.findByLessonId(lesson.getId());

        // Registration & progress
        CourseRegistration registration = registrationService.getRegistration(learner.getId(), course.getId());
        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(learner.getId(), course.getId());
        int completedLessonsCount = computeCompletedCount(completedLessonIds.size(), registration, courseLessons.size());
        int effectiveProgress = syncRegistrationProgress(registration, completedLessonsCount, courseLessons.size());

        // Enforce Milestone exam gate: if there is an uncompleted Milestone lesson
        // before this lesson, require taking it first
        for (int i = 0; i < currentIdx; i++) {
            Lesson prev = courseLessons.get(i);
            if (prev.isMilestoneExam()) {
                boolean prevDone = completedLessonIds.contains(prev.getId());
                if (!prevDone) {
                    session.setAttribute("ACTIVE_MILESTONE_COURSE_ID", course.getId());
                    session.setAttribute("ACTIVE_MILESTONE_LESSON_ID", prev.getId());
                    return "redirect:"
                            + examService.resolveMilestoneExamUrl(course.getId(), course.getLevel(), prev.getId());
                }
            }
        }

        // If the requested lesson itself is an Exam or Milestone exam, check
        // prerequisites before allowing entry
        if (lesson.isExam() || lesson.isMilestoneExam()) {
            Long targetExamId = lesson.getExamId();
            if (targetExamId != null) {
                var prereq = examService.checkExamPrerequisites(targetExamId, learner.getId());
                if (prereq != null && !prereq.isUnlocked()) {
                    redirectAttributes.addFlashAttribute("errorMessage", prereq.getMessage());
                    return "redirect:/learning/courses/" + course.getId() + "/lessons";
                }
                return "redirect:/exams/" + targetExamId + "/start";
            }
            session.setAttribute("ACTIVE_MILESTONE_COURSE_ID", course.getId());
            session.setAttribute("ACTIVE_MILESTONE_LESSON_ID", lesson.getId());
            return "redirect:" + examService.resolveMilestoneExamUrl(course.getId(), course.getLevel(), lesson.getId());
        }

        // Skill study view from LessonDetailService (Kanji, Grammar, Vocab, Reading,
        // Listening details)
        LessonStudyView studyView = null;
        try {
            studyView = lessonDetailService.buildLessonStudyView(lesson.getId());
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
        Course course = (lesson.getCourse() != null)
                ? courseService.getCourseById(lesson.getCourse().getId())
                : null;
        if (course == null && lesson.getCourse() != null) {
            course = lesson.getCourse();
        }

        // Chặn quyền hoàn thành bài học nếu học viên chưa đăng ký khóa học
        boolean isEnrolled = (course != null) && registrationService.isUserEnrolled(learner.getId(), course.getId());
        if (!isEnrolled) {
            CourseRegistration registration = course != null
                    ? registrationService.getRegistration(learner.getId(), course.getId())
                    : null;
            if (registration != null
                    && CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Khóa học này đang chờ thanh toán. Vui lòng hoàn tất thanh toán để tiếp tục!");
                return "redirect:/learning/courses/" + course.getId() + "/register";
            }
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Bạn chưa đăng ký khóa học này. Vui lòng đăng ký khóa học để ghi nhận tiến độ!");
            return "redirect:/courses/" + (course != null ? course.getId() : 1);
        }

        List<Lesson> courseLessons = courseService.getCourseLessons(course.getId());
        int currentIdx = -1;
        for (int i = 0; i < courseLessons.size(); i++) {
            if (courseLessons.get(i).getId().equals(lesson.getId())) {
                currentIdx = i;
                break;
            }
        }

        // Record granular lesson completion in Learning Path & synchronize course
        // registration progress
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

        redirectAttributes.addFlashAttribute("successMessage",
                "Chúc mừng bạn đã hoàn thành bài học: " + lesson.getTitle() + "!");

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
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
        String lvl = (level != null && !"ALL".equalsIgnoreCase(level)) ? level : "";
        String kw = keyword != null ? keyword : "";
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
        com.jeps.modules.course.dto.ExamRecommendationDto examRecommendation = null;
        try {
            var history = examService.getLearnerExamHistory(learner.getId());
            if (history != null) {
                var latestPlacement = history.stream()
                        .filter(h -> "PLACEMENT_TEST".equalsIgnoreCase(h.getExamType()))
                        .findFirst()
                        .orElse(null);
                if (latestPlacement != null && latestPlacement.getResultId() != null) {
                    var fullResult = examService.getHistoricalResult(latestPlacement.getResultId(), learner.getId());
                    if (fullResult != null) {
                        examRecommendation = recommendationService.getRecommendationsFromExamResult(
                                fullResult, learner.getId(), learner.getTargetJlptLevel(), "NO", fullResult.getJlptLevel());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not load placement-based recommendation for learner {}: {}", learner.getId(), ex.getMessage());
        }

        List<RecommendedCourseItem> recommendedItems = (examRecommendation != null
                && examRecommendation.getAllRecommendations() != null
                && !examRecommendation.getAllRecommendations().isEmpty())
                ? examRecommendation.getAllRecommendations()
                : recommendationService.getRecommendationsForLearner(learner.getId());
        List<Course> otherCourses = recommendationService.getOtherAvailableCourses(learner.getId());

        model.addAttribute("learner", learner);
        model.addAttribute("examRecommendation", examRecommendation);
        model.addAttribute("recommendedItems", recommendedItems);
        model.addAttribute("otherCourses", otherCourses);
        model.addAttribute("levels", List.of("N5", "N4", "N3", "N2", "N1"));
        model.addAttribute("activeMenu", "recommended");
        return "pages/learning/recommended-courses";
    }

    private boolean isCoursePublicStatus(Course course) {
        if (course == null || course.getStatus() == null) {
            return false;
        }
        String status = course.getStatus().trim().toUpperCase(Locale.ROOT);
        return "PUBLISHED".equals(status) || "ACTIVE".equals(status) || "PUBLIC".equals(status);
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
        Course course;
        try {
            course = courseService.getCourseById(id);
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khóa học yêu cầu.");
            return "redirect:/courses";
        }

        if (!isCoursePublicStatus(course)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Khóa học này hiện chưa được xuất bản hoặc đã lưu trữ, không thể đăng ký.");
            return "redirect:/courses";
        }

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
                    "Chúc mừng bạn đã đăng ký thành công khóa học: " + course.getTitle()
                            + "! Bạn có thể bắt đầu bài học ngay bây giờ.");
            return "redirect:/courses/" + id;
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("warningMessage", ex.getMessage());
            return isCoursePublicStatus(course) ? "redirect:/courses/" + id : "redirect:/courses";
        } catch (Exception ex) {
            log.error("Failed to register course ID: {} for user ID: {}", id, learner.getId(), ex);
            model.addAttribute("errorMessage",
                    "Đã có lỗi xảy ra trong quá trình đăng ký khóa học. Vui lòng thử lại sau.");
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
                .filter(r -> !"COMPLETED".equalsIgnoreCase(r.getStatus())
                        && (r.getProgressPercent() == null || r.getProgressPercent() < 100))
                .count();

        long completedCount = registrations.stream()
                .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus())
                        || (r.getProgressPercent() != null && r.getProgressPercent() == 100))
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
