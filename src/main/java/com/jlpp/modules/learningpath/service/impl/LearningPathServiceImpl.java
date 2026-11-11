package com.jlpp.modules.learningpath.service.impl;

import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.learningpath.dto.*;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LearningPathService;
import com.jlpp.modules.learningpath.service.LessonProgressService;
import com.jlpp.modules.learningpath.service.SkillAnalysisService;
import com.jlpp.modules.learningpath.service.SkillFocusRecommendationService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final SkillAnalysisService skillAnalysisService;
    private final SkillFocusRecommendationService skillFocusRecommendationService;
    private final LessonProgressService lessonProgressService;
    private final ExamService examService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional
    public LearningPathResponse getLearningPath(String username) {
        log.info("Fetching personalized skill-aware learning path for learner={}", username);
        User user = findUserByUsername(username);

        Optional<LearningPath> lpOpt = learningPathRepository
                .findFirstByUserIdOrderByScheduledDateDescIdDesc(user.getId());

        // Auto-provisioning fallback: If user has a registered course but no learning_paths entry, auto-create it
        if (lpOpt.isEmpty()) {
            List<CourseRegistration> registrations = courseRegistrationRepository.findByUserIdWithCourse(user.getId());
            if (!registrations.isEmpty()) {
                CourseRegistration reg = registrations.get(0);
                Course enrolledCourse = reg.getCourse();
                String targetLevel = (enrolledCourse != null && enrolledCourse.getLevel() != null)
                        ? enrolledCourse.getLevel()
                        : "N4";
                LocalDate startDate = reg.getRegisteredAt() != null
                        ? reg.getRegisteredAt().toLocalDate()
                        : LocalDate.now();

                LearningPath autoCreated = LearningPath.builder()
                        .userId(user.getId())
                        .courseId(reg.getCourse().getId())
                        .dailyHoursSetup(BigDecimal.valueOf(1.5))
                        .targetLevel(targetLevel)
                        .currentLevel("UNKNOWN")
                        .pathStatus("ACTIVE")
                        .scheduledDate(startDate)
                        .build();

                autoCreated = learningPathRepository.save(autoCreated);
                log.info("Auto-provisioned initial LearningPath id={} for enrolled learner userId={}, courseId={}",
                        autoCreated.getId(), user.getId(), reg.getCourse().getId());
                lpOpt = Optional.of(autoCreated);
            }
        }

        if (lpOpt.isEmpty()) {
            log.info("No learning path or course registration found for userId={}", user.getId());
            return LearningPathResponse.builder()
                    .hasPath(false)
                    .emptyStateMessage("Bạn chưa có lộ trình học tập.")
                    .items(Collections.emptyList())
                    .build();
        }

        LearningPath lp = lpOpt.get();
        Course course = courseRepository.findById(lp.getCourseId()).orElse(null);
        String courseTitle = (course != null && course.getTitle() != null)
                ? course.getTitle()
                : "Khóa học #" + lp.getCourseId();
        String courseLevel = (course != null && course.getLevel() != null)
                ? course.getLevel()
                : lp.getTargetLevel();

        // 1. Retrieve Lessons - Canonical sequence strictly preserved by order_index ASC
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lp.getCourseId());
        Optional<CourseRegistration> regOpt = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), lp.getCourseId());

        int progressPercent = regOpt.map(CourseRegistration::getProgressPercent).orElse(0);
        int totalLessons = lessons.size();

        // 2. Skill Analysis from real placement exam results
        SkillProfileDto skillProfile = skillAnalysisService.analyzeLearnerSkills(user.getId(), lp.getResultId());

        // 3. Personalized Skill Focus & Resource Recommendations per Lesson (Preserving order!)
        List<LessonSkillFocusDto> focusRecommendations = skillFocusRecommendationService.recommendFocusBatch(lessons, skillProfile);

        // 4. Progress Tracking (combines JSONB records with legacy fallback)
        int completedLessons = 0;
        for (int i = 0; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            if (lessonProgressService.isLessonCompleted(user.getId(), lp.getCourseId(), l.getId(), i + 1, totalLessons, progressPercent)) {
                completedLessons++;
            }
        }
        int remainingLessons = Math.max(0, totalLessons - completedLessons);

        LocalDate pathScheduledDate = lp.getScheduledDate();
        if (completedLessons > 0 && pathScheduledDate != null && pathScheduledDate.isAfter(LocalDate.now())) {
            pathScheduledDate = regOpt.map(r -> r.getRegisteredAt().toLocalDate())
                    .orElse(LocalDate.now().minusDays(1));
            lp.setScheduledDate(pathScheduledDate);
            learningPathRepository.save(lp);
        }

        BigDecimal dailyHours = lp.getDailyHoursSetup() != null
                ? lp.getDailyHoursSetup()
                : BigDecimal.valueOf(1.5);
        int weeklyHours = Math.max(1, dailyHours.multiply(BigDecimal.valueOf(7)).intValue());

        // 5. Build Timeline Items with personalized durations & skill focus badges
        List<LearningPathTimelineItemDto> timelineItems = buildTimelineItems(
                user.getId(), lp.getCourseId(), lessons, focusRecommendations,
                pathScheduledDate, dailyHours, progressPercent);

        String projectedEndDate = timelineItems.isEmpty()
                ? "Chưa xác định"
                : timelineItems.get(timelineItems.size() - 1).getPlannedDateFormatted();

        int totalHours = course != null && course.getDurationHours() != null
                ? course.getDurationHours()
                : (int) Math.ceil(focusRecommendations.stream().mapToInt(LessonSkillFocusDto::getRecommendedDurationMinutes).sum() / 60.0);

        return LearningPathResponse.builder()
                .hasPath(true)
                .learningPathId(lp.getId())
                .courseId(lp.getCourseId())
                .courseTitle(courseTitle)
                .courseLevel(courseLevel)
                .targetLevel(lp.getTargetLevel())
                .currentLevel(lp.getCurrentLevel() != null ? lp.getCurrentLevel() : "Chưa xác định")
                .dailyHours(dailyHours)
                .weeklyHours(weeklyHours)
                .startDateFormatted(pathScheduledDate != null ? pathScheduledDate.format(DATE_FORMATTER) : "N/A")
                .projectedEndDateFormatted(projectedEndDate)
                .progressPercent(progressPercent)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .remainingLessons(remainingLessons)
                .totalHours(totalHours)
                .pathStatus(lp.getPathStatus())
                .statusBadgeClass("ACTIVE".equalsIgnoreCase(lp.getPathStatus()) ? "bg-success" : "bg-secondary")
                .skillProfile(skillProfile)
                .items(timelineItems)
                .build();
    }

    @Override
    @Transactional
    public RecalibratePreviewResponse getRecalibratePreview(String username, RecalibrateRequest request) {
        log.info("Calculating personalized recalibrate preview for learner={} with weeklyHours={}",
                username, request != null ? request.getWeeklyHours() : null);
        User user = findUserByUsername(username);

        Optional<LearningPath> lpOpt = learningPathRepository
                .findFirstByUserIdOrderByScheduledDateDescIdDesc(user.getId());

        if (lpOpt.isEmpty()) {
            return RecalibratePreviewResponse.builder()
                    .hasPath(false)
                    .message("Chưa đủ dữ liệu để điều chỉnh lịch học.")
                    .rescheduledItems(Collections.emptyList())
                    .build();
        }

        LearningPath lp = lpOpt.get();
        Course course = courseRepository.findById(lp.getCourseId()).orElse(null);
        String courseTitle = (course != null && course.getTitle() != null)
                ? course.getTitle()
                : "Khóa học #" + lp.getCourseId();

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lp.getCourseId());
        Optional<CourseRegistration> regOpt = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), lp.getCourseId());

        int progressPercent = regOpt.map(CourseRegistration::getProgressPercent).orElse(0);
        int totalLessons = lessons.size();

        SkillProfileDto skillProfile = skillAnalysisService.analyzeLearnerSkills(user.getId(), lp.getResultId());
        List<LessonSkillFocusDto> focusRecommendations = skillFocusRecommendationService.recommendFocusBatch(lessons, skillProfile);

        int completedLessons = 0;
        for (int i = 0; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            if (lessonProgressService.isLessonCompleted(user.getId(), lp.getCourseId(), l.getId(), i + 1, totalLessons, progressPercent)) {
                completedLessons++;
            }
        }

        LocalDate pathScheduledDate = lp.getScheduledDate();
        if (completedLessons > 0 && pathScheduledDate != null && pathScheduledDate.isAfter(LocalDate.now())) {
            pathScheduledDate = regOpt.map(r -> r.getRegisteredAt().toLocalDate())
                    .orElse(LocalDate.now().minusDays(1));
            lp.setScheduledDate(pathScheduledDate);
            learningPathRepository.save(lp);
        }

        BigDecimal originalDailyHours = lp.getDailyHoursSetup() != null
                ? lp.getDailyHoursSetup()
                : BigDecimal.valueOf(1.5);
        int originalWeeklyHours = Math.max(1, originalDailyHours.multiply(BigDecimal.valueOf(7)).intValue());
        int newWeeklyHours = (request != null && request.getWeeklyHours() != null && request.getWeeklyHours() > 0)
                ? request.getWeeklyHours()
                : originalWeeklyHours;

        List<LearningPathTimelineItemDto> originalItems = buildTimelineItems(
                user.getId(), lp.getCourseId(), lessons, focusRecommendations,
                pathScheduledDate, originalDailyHours, progressPercent);

        LocalDate originalEndDate = originalItems.isEmpty()
                ? LocalDate.now().plusMonths(2)
                : originalItems.get(originalItems.size() - 1).getPlannedDate();

        List<LearningPathTimelineItemDto> rescheduledItems = rescheduleItems(
                originalItems, completedLessons, newWeeklyHours);

        LocalDate newEndDate = rescheduledItems.isEmpty()
                ? LocalDate.now()
                : rescheduledItems.get(rescheduledItems.size() - 1).getPlannedDate();

        long daysDiff = ChronoUnit.DAYS.between(originalEndDate, newEndDate);
        String daysDiffText;
        if (daysDiff < 0) {
            daysDiffText = String.format("Hoàn thành sớm hơn %d ngày", Math.abs(daysDiff));
        } else if (daysDiff > 0) {
            daysDiffText = String.format("Kéo dài thêm %d ngày", daysDiff);
        } else {
            daysDiffText = "Tiến độ tương đương kế hoạch ban đầu";
        }

        int remainingMinutes = 0;
        for (int i = 0; i < originalItems.size(); i++) {
            if (!"COMPLETED".equalsIgnoreCase(originalItems.get(i).getStatus())) {
                remainingMinutes += (originalItems.get(i).getRecommendedDurationMinutes() != null
                        ? originalItems.get(i).getRecommendedDurationMinutes()
                        : (originalItems.get(i).getDurationMinutes() != null ? originalItems.get(i).getDurationMinutes() : 60));
            }
        }
        int remainingHours = Math.max(1, (int) Math.ceil(remainingMinutes / 60.0));
        int weeksNeeded = Math.max(1, (int) Math.ceil((double) remainingHours / newWeeklyHours));

        String paceAnalysis = buildPaceAnalysis(newWeeklyHours, weeksNeeded, remainingHours);

        return RecalibratePreviewResponse.builder()
                .hasPath(true)
                .courseTitle(courseTitle)
                .originalWeeklyHours(originalWeeklyHours)
                .newWeeklyHours(newWeeklyHours)
                .originalCompletionDate(originalEndDate.format(DATE_FORMATTER))
                .newCompletionDate(newEndDate.format(DATE_FORMATTER))
                .daysDifferenceText(daysDiffText)
                .remainingHours(remainingHours)
                .weeksNeeded(weeksNeeded)
                .paceAnalysisText(paceAnalysis)
                .rescheduledItems(rescheduledItems)
                .build();
    }

    private List<LearningPathTimelineItemDto> buildTimelineItems(
            Long userId, Long courseId,
            List<Lesson> lessons, List<LessonSkillFocusDto> focusRecommendations,
            LocalDate startDate, BigDecimal dailyHours, int progressPercent) {

        if (lessons == null || lessons.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate referenceDate = LocalDate.now();
        LocalDate courseStart = startDate != null ? startDate : referenceDate;
        LocalDate currentDate = courseStart;

        List<LearningPathTimelineItemDto> items = new ArrayList<>(lessons.size());
        int dailyMinutesLimit = dailyHours.multiply(BigDecimal.valueOf(60)).intValue();
        if (dailyMinutesLimit <= 0) dailyMinutesLimit = 90;

        int accumulatedMinutesToday = 0;
        boolean firstInProgressEncountered = false;

        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            LessonSkillFocusDto focus = (focusRecommendations != null && i < focusRecommendations.size())
                    ? focusRecommendations.get(i)
                    : null;

            int baseDuration = (lesson.getDurationMinutes() != null && lesson.getDurationMinutes() > 0)
                    ? lesson.getDurationMinutes()
                    : 60;

            int duration = (focus != null && focus.getRecommendedDurationMinutes() > 0)
                    ? focus.getRecommendedDurationMinutes()
                    : baseDuration;

            boolean isCompleted = lessonProgressService.isLessonCompleted(
                    userId, courseId, lesson.getId(), i + 1, lessons.size(), progressPercent);

            // Snap incomplete items to today if we were in the past
            if (!isCompleted && !firstInProgressEncountered && currentDate.isBefore(referenceDate)) {
                currentDate = referenceDate;
                accumulatedMinutesToday = 0;
            }

            accumulatedMinutesToday += duration;
            boolean isOverflow = duration > dailyMinutesLimit;

            if (accumulatedMinutesToday > dailyMinutesLimit && accumulatedMinutesToday > duration) {
                currentDate = currentDate.plusDays(1);
                accumulatedMinutesToday = duration;
            }

            long daysFromStart = ChronoUnit.DAYS.between(courseStart, currentDate);
            int targetWeek = Math.max(1, (int) (daysFromStart / 7) + 1);

            boolean isMilestone = lesson.isMilestoneExam();
            String courseLevel = (lesson.getCourse() != null && lesson.getCourse().getLevel() != null)
                    ? lesson.getCourse().getLevel()
                    : null;

            String status;
            String statusDisplay;
            String badgeClass;
            String actionUrl = isMilestone
                    ? (examService != null
                        ? examService.resolveMilestoneExamUrl(courseId, courseLevel, lesson.getId())
                        : "/exams/milestones?courseId=" + courseId)
                    : "/learning/lessons/" + lesson.getId();
            String actionText;

            if (isCompleted) {
                status = "COMPLETED";
                statusDisplay = "Đã hoàn thành";
                badgeClass = "bg-success";
                actionText = isMilestone ? "Làm lại" : "Ôn lại";
            } else if (!firstInProgressEncountered) {
                status = "IN_PROGRESS";
                statusDisplay = isMilestone ? "Đến hạn kiểm tra" : "Đang học";
                badgeClass = isMilestone ? "bg-warning text-dark" : "bg-primary";
                actionText = isMilestone ? "Làm bài" : "Học tiếp";
                firstInProgressEncountered = true;
            } else {
                status = "UPCOMING";
                statusDisplay = "Chưa học";
                badgeClass = "bg-secondary";
                actionText = isMilestone ? "Làm bài" : "Bắt đầu học";
            }

            String sectionName = lesson.getSectionName() != null
                    ? lesson.getSectionName()
                    : "Chương " + ((i / 5) + 1);

            items.add(LearningPathTimelineItemDto.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .sectionName(sectionName)
                    .contentType(isMilestone ? "EXAM" : (lesson.getLessonType() != null ? lesson.getLessonType() : "LESSON"))
                    .durationMinutes(baseDuration)
                    .durationFormatted(formatDuration(baseDuration))
                    .targetWeek(targetWeek)
                    .plannedDate(currentDate)
                    .plannedDateFormatted(currentDate.format(DATE_FORMATTER))
                    .status(status)
                    .statusDisplay(statusDisplay)
                    .statusBadgeClass(badgeClass)
                    .actionUrl(actionUrl)
                    .actionText(actionText)
                    .orderIndex(i + 1)
                    // Skill-Aware Personalization Fields
                    .skillType(focus != null ? focus.getRawSkillType() : lesson.getSkillType())
                    .skillDisplayName(focus != null ? focus.getSkillDisplayName() : "Kiến thức chung")
                    .skillProficiency(focus != null ? focus.getProficiency() : SkillProficiency.UNKNOWN)
                    .baseDurationMinutes(baseDuration)
                    .recommendedDurationMinutes(duration)
                    .recommendedDurationFormatted(formatDuration(duration))
                    .focusBadgeText(focus != null ? focus.getFocusBadgeText() : "Thời lượng chuẩn")
                    .focusBadgeClass(focus != null ? focus.getFocusBadgeClass() : "bg-secondary text-white")
                    .recommendationReason(focus != null ? focus.getRecommendationReason() : null)
                    .reviewResourcesSummary(focus != null ? focus.getReviewResourcesSummary() : null)
                    .hasReviewResources(focus != null && focus.isHasReviewResources())
                    .isOverflowDay(isOverflow)
                    .build());
        }

        return items;
    }

    private List<LearningPathTimelineItemDto> rescheduleItems(
            List<LearningPathTimelineItemDto> originalItems, int completedCount, int newWeeklyHours) {

        List<LearningPathTimelineItemDto> result = new ArrayList<>();
        LocalDate referenceDate = LocalDate.now();
        LocalDate runningDate = referenceDate;

        LocalDate courseStartDate = originalItems.isEmpty()
                ? referenceDate
                : originalItems.get(0).getPlannedDate();

        int dailyMinutesBudget = Math.max(30, (newWeeklyHours * 60) / 7);
        int accumulatedMinutesToday = 0;

        for (int i = 0; i < originalItems.size(); i++) {
            LearningPathTimelineItemDto item = originalItems.get(i);

            if ("COMPLETED".equalsIgnoreCase(item.getStatus()) || i < completedCount) {
                // Completed items keep their original historical status, date and week
                result.add(item);
                if (item.getPlannedDate() != null && item.getPlannedDate().isAfter(runningDate)) {
                    runningDate = item.getPlannedDate();
                }
            } else {
                int duration = (item.getRecommendedDurationMinutes() != null && item.getRecommendedDurationMinutes() > 0)
                        ? item.getRecommendedDurationMinutes()
                        : ((item.getDurationMinutes() != null && item.getDurationMinutes() > 0) ? item.getDurationMinutes() : 60);

                accumulatedMinutesToday += duration;
                if (accumulatedMinutesToday > dailyMinutesBudget && accumulatedMinutesToday > duration) {
                    runningDate = runningDate.plusDays(1);
                    accumulatedMinutesToday = duration;
                }

                long daysFromStart = ChronoUnit.DAYS.between(courseStartDate, runningDate);
                int targetWeek = Math.max(1, (int) (daysFromStart / 7) + 1);

                result.add(LearningPathTimelineItemDto.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .sectionName(item.getSectionName())
                        .contentType(item.getContentType())
                        .durationMinutes(item.getDurationMinutes())
                        .durationFormatted(item.getDurationFormatted() != null ? item.getDurationFormatted() : formatDuration(item.getDurationMinutes()))
                        .targetWeek(targetWeek)
                        .plannedDate(runningDate)
                        .plannedDateFormatted(runningDate.format(DATE_FORMATTER))
                        .status(item.getStatus())
                        .statusDisplay(item.getStatusDisplay())
                        .statusBadgeClass(item.getStatusBadgeClass())
                        .actionUrl(item.getActionUrl())
                        .actionText(item.getActionText())
                        .orderIndex(item.getOrderIndex())
                        // Preserve personalization metadata
                        .skillType(item.getSkillType())
                        .skillDisplayName(item.getSkillDisplayName())
                        .skillProficiency(item.getSkillProficiency())
                        .baseDurationMinutes(item.getBaseDurationMinutes())
                        .recommendedDurationMinutes(item.getRecommendedDurationMinutes())
                        .recommendedDurationFormatted(item.getRecommendedDurationFormatted())
                        .focusBadgeText(item.getFocusBadgeText())
                        .focusBadgeClass(item.getFocusBadgeClass())
                        .recommendationReason(item.getRecommendationReason())
                        .reviewResourcesSummary(item.getReviewResourcesSummary())
                        .hasReviewResources(item.isHasReviewResources())
                        .isOverflowDay(duration > dailyMinutesBudget)
                        .build());
            }
        }

        return result;
    }

    private String formatDuration(int minutes) {
        if (minutes <= 0) return "1 giờ";
        int hours = minutes / 60;
        int remaining = minutes % 60;
        if (hours == 0) return remaining + " phút";
        if (remaining == 0) return hours + " giờ";
        return hours + " giờ " + remaining + " phút";
    }

    private String buildPaceAnalysis(int weeklyHours, int weeksNeeded, int remainingHours) {
        StringBuilder sb = new StringBuilder();
        if (weeklyHours >= 15) {
            sb.append("Cường độ Tăng tốc (Intensive): Với ").append(weeklyHours)
                    .append(" giờ/tuần, bạn sẽ hoàn thành khối lượng bài học còn lại (khoảng ")
                    .append(remainingHours).append(" giờ theo gợi ý kỹ năng cá nhân) trong ")
                    .append(weeksNeeded).append(" tuần tới. Phù hợp cho mục tiêu thi JLPT gấp.");
        } else if (weeklyHours >= 8) {
            sb.append("Cường độ Tiêu chuẩn (Standard): Phân bổ ").append(weeklyHours)
                    .append(" giờ/tuần là nhịp độ cân bằng và bền vững nhất để củng cố kỹ năng còn yếu và ôn tập đều đặn trong ")
                    .append(weeksNeeded).append(" tuần.");
        } else {
            sb.append("Cường độ Nhẹ nhàng (Light): Với ").append(weeklyHours)
                    .append(" giờ/tuần, lộ trình sẽ kéo dài khoảng ").append(weeksNeeded)
                    .append(" tuần để bạn học tập thong thả, không bị áp lực thời gian.");
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public void applyRecalibratedSchedule(String username, RecalibrateRequest request) {
        log.info("Applying recalibrated schedule for learner={} with weeklyHours={}",
                username, request != null ? request.getWeeklyHours() : null);
        User user = findUserByUsername(username);

        Optional<LearningPath> lpOpt = learningPathRepository
                .findFirstByUserIdOrderByScheduledDateDescIdDesc(user.getId());

        if (lpOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy lộ trình học tập để cập nhật.");
        }

        LearningPath lp = lpOpt.get();

        if (request != null && request.getWeeklyHours() != null && request.getWeeklyHours() > 0) {
            BigDecimal newDailyHours = BigDecimal.valueOf(request.getWeeklyHours())
                    .divide(BigDecimal.valueOf(7), 2, java.math.RoundingMode.HALF_UP);
            lp.setDailyHoursSetup(newDailyHours);
        }

        if (request != null && request.getStartDate() != null) {
            lp.setScheduledDate(request.getStartDate());
        }

        learningPathRepository.save(lp);
        log.info("Successfully updated learning path id={} for userId={}: dailyHours={}",
                lp.getId(), user.getId(), lp.getDailyHoursSetup());
    }

    private User findUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng: " + username));
    }
}
