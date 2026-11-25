package com.jeps.modules.learningpath.service.impl;

import com.jeps.modules.learningpath.dto.LearningPathDtos.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.exam.dto.ExamHistoryDto;
import com.jeps.modules.exam.service.ExamService;
import com.jeps.modules.learningpath.entity.LearningPath;
import com.jeps.modules.learningpath.repository.LearningPathRepository;
import com.jeps.modules.learningpath.service.LearningPathService;
import com.jeps.modules.learningpath.service.LessonProgressService;
import com.jeps.modules.learningpath.service.SkillAnalysisService;
import com.jeps.modules.learningpath.service.SkillFocusRecommendationService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import com.jeps.modules.course.service.CourseRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private final CourseRegistrationService courseRegistrationService;
    private final SkillAnalysisService skillAnalysisService;
    private final SkillFocusRecommendationService skillFocusRecommendationService;
    private final LessonProgressService lessonProgressService;
    private final ExamService examService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional
    public LearningPathResponse getLearningPath(String username) {
        return getLearningPath(username, null);
    }

    @Override
    @Transactional
    public LearningPathResponse getLearningPath(String username, Long courseId) {
        log.info("Fetching personalized skill-aware learning path for learner={}, courseId={}", username, courseId);
        User user = findUserByUsername(username);

        List<CourseRegistration> allRegistrations = courseRegistrationService.getUserRegistrations(user.getId());
        List<CourseRegistration> publishedRegistrations = allRegistrations.stream()
                .filter(r -> r.getCourse() != null && "PUBLISHED".equalsIgnoreCase(r.getCourse().getStatus()))
                .collect(Collectors.toList());

        if (publishedRegistrations.isEmpty()) {
            log.info("No published course registrations found for userId={}", user.getId());
            return LearningPathResponse.builder()
                    .hasPath(false)
                    .emptyStateMessage(
                            "Bạn chưa đăng ký khóa học nào đã được công bố. Hãy đăng ký khóa học để hệ thống thiết lập lộ trình học tập!")
                    .items(Collections.emptyList())
                    .build();
        }

        // Validate course enrollment if courseId is specified
        if (courseId != null) {
            boolean isRegistered = publishedRegistrations.stream()
                    .anyMatch(r -> r.getCourse().getId().equals(courseId));
            if (!isRegistered) {
                log.warn("Learner {} attempted to access learning path for un-enrolled courseId={}", username,
                        courseId);
                List<CourseOptionDto> courseOptions = publishedRegistrations.stream()
                        .map(r -> CourseOptionDto.builder()
                                .courseId(r.getCourse().getId())
                                .courseTitle(r.getCourse().getTitle())
                                .courseLevel(resolveLevelFromCourse(r.getCourse()))
                                .progressPercent(r.getProgressPercent() != null ? r.getProgressPercent() : 0)
                                .selected(false)
                                .build())
                        .collect(Collectors.toList());

                return LearningPathResponse.builder()
                        .hasPath(false)
                        .enrolledCourses(courseOptions)
                        .emptyStateMessage(
                                "Bạn chưa đăng ký khóa học này hoặc khóa học chưa được công bố. Vui lòng chọn một trong các khóa học bạn đã đăng ký.")
                        .items(Collections.emptyList())
                        .build();
            }
        }

        Optional<LearningPath> lpOpt = findPublishedLearningPathForUser(user.getId(), courseId);

        if (lpOpt.isEmpty()) {
            log.info("No learning path found or auto-provisioned for userId={}, courseId={}", user.getId(), courseId);
            return LearningPathResponse.builder()
                    .hasPath(false)
                    .emptyStateMessage("Chưa đủ dữ liệu để xây dựng lộ trình học tập cho khóa học đã chọn.")
                    .items(Collections.emptyList())
                    .build();
        }

        LearningPath lp = lpOpt.get();
        Course course = courseRepository.findById(lp.getCourseId()).orElse(null);
        if (course == null || !"PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            log.info("Course id={} for learning path id={} is not published (status={})",
                    lp.getCourseId(), lp.getId(), course != null ? course.getStatus() : "null");
            return LearningPathResponse.builder()
                    .hasPath(false)
                    .emptyStateMessage("Khóa học chưa được công bố.")
                    .items(Collections.emptyList())
                    .build();
        }

        String courseTitle = (course.getTitle() != null)
                ? course.getTitle()
                : "Khóa học #" + lp.getCourseId();
        String courseLevel = resolveLevelFromCourse(course);
        if (!"Chưa xác định".equalsIgnoreCase(courseLevel) && !courseLevel.equalsIgnoreCase(lp.getTargetLevel())) {
            lp.setTargetLevel(courseLevel);
            learningPathRepository.save(lp);
        }

        // Build enrolled course options for Course Switcher
        List<CourseOptionDto> enrolledCourses = publishedRegistrations.stream()
                .map(r -> CourseOptionDto.builder()
                        .courseId(r.getCourse().getId())
                        .courseTitle(r.getCourse().getTitle())
                        .courseLevel(resolveLevelFromCourse(r.getCourse()))
                        .progressPercent(r.getProgressPercent() != null ? r.getProgressPercent() : 0)
                        .selected(r.getCourse().getId().equals(lp.getCourseId()))
                        .build())
                .collect(Collectors.toList());

        // 1. Retrieve Lessons - Canonical sequence strictly preserved by order_index
        // ASC
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lp.getCourseId());
        Optional<CourseRegistration> regOpt = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), lp.getCourseId());

        int progressPercent = regOpt.map(CourseRegistration::getProgressPercent).orElse(0);
        int totalLessons = lessons.size();

        // 2. Skill Analysis & Dynamic Recalibration from latest Milestone Exam or
        // Placement Exam
        SkillProfileDto skillProfile = skillAnalysisService.analyzeAndRecalibrateSkills(user.getId(), lp.getCourseId(),
                courseLevel, lp.getResultId());

        // 3. Personalized Skill Focus & Resource Recommendations per Lesson (Preserving
        // order!)
        List<LessonSkillFocusDto> focusRecommendations = skillFocusRecommendationService.recommendFocusBatch(lessons,
                skillProfile);

        // 4. Progress Tracking (combines JSONB records with legacy fallback in 1 batch
        // query)
        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(user.getId(), lp.getCourseId(),
                lessons, progressPercent);
        int completedLessons = completedLessonIds.size();
        int remainingLessons = Math.max(0, totalLessons - completedLessons);

        LocalDate pathScheduledDate = lp.getScheduledDate();
        if (pathScheduledDate == null) {
            pathScheduledDate = lp.getBaselineStartDate() != null
                    ? lp.getBaselineStartDate()
                    : regOpt.map(r -> r.getRegisteredAt().toLocalDate()).orElse(LocalDate.now());
            lp.setScheduledDate(pathScheduledDate);
            learningPathRepository.save(lp);
        } else if (completedLessons == 0 && pathScheduledDate.isBefore(LocalDate.now())) {
            pathScheduledDate = LocalDate.now();
            lp.setScheduledDate(pathScheduledDate);
            learningPathRepository.save(lp);
        } else if (completedLessons > 0 && pathScheduledDate.isAfter(LocalDate.now())) {
            pathScheduledDate = regOpt.map(r -> r.getRegisteredAt().toLocalDate())
                    .orElse(LocalDate.now());
            lp.setScheduledDate(pathScheduledDate);
            learningPathRepository.save(lp);
        }

        BigDecimal dailyHours = lp.getDailyHoursSetup() != null
                ? lp.getDailyHoursSetup()
                : BigDecimal.valueOf(1.5);

        Set<DayOfWeek> studyDays = parseStudyDays(lp.getStudyDays(), null);

        // 4b. Parse Leave Records & Catch-up Mode
        List<LeaveRecordDto> leaveRecords = parseLeaveRecords(lp.getLeaveRecords());
        Set<LocalDate> leaveDates = extractLeaveDates(leaveRecords);
        LocalDate today = LocalDate.now();
        LeaveRecordDto activeLeave = leaveRecords.stream()
                .filter(r -> r.getStartDate() != null && r.getEndDate() != null &&
                        !today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate()))
                .findFirst()
                .orElse(null);

        boolean catchUpMode = Boolean.TRUE.equals(lp.getCatchUpMode());
        int catchUpExtraMinutes = (lp.getCatchUpExtraMinutes() != null && lp.getCatchUpExtraMinutes() > 0)
                ? lp.getCatchUpExtraMinutes()
                : 30;
        boolean catchUpIncludeWeekends = Boolean.TRUE.equals(lp.getCatchUpIncludeWeekends());

        if (catchUpMode && catchUpIncludeWeekends) {
            studyDays.add(DayOfWeek.SATURDAY);
            studyDays.add(DayOfWeek.SUNDAY);
        }

        int extraDailyMinutes = catchUpMode ? catchUpExtraMinutes : 0;
        int weeklyHours = Math.max(1, dailyHours.multiply(BigDecimal.valueOf(studyDays.size())).intValue());

        // Extract actual completion dates of completed lessons from lesson_progress
        // JSON
        Map<Long, LocalDate> lessonCompletionDates = new HashMap<>();
        LearningPathProgressDto progressDto = lessonProgressService.getLessonProgress(user.getId(), lp.getCourseId());
        if (progressDto != null && progressDto.getLessons() != null) {
            for (Map.Entry<String, LessonProgressItemDto> entry : progressDto.getLessons().entrySet()) {
                if (entry.getValue() != null && "COMPLETED".equalsIgnoreCase(entry.getValue().getStatus())
                        && entry.getValue().getCompletedAt() != null) {
                    try {
                        Long lid = Long.valueOf(entry.getKey());
                        LocalDate d = Instant.parse(entry.getValue().getCompletedAt())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        lessonCompletionDates.put(lid, d);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 5. Build Timeline Items with personalized durations & skill focus badges &
        // leave shifts
        List<LearningPathTimelineItemDto> timelineItems = buildTimelineItems(
                user.getId(), lp.getCourseId(), lessons, focusRecommendations,
                pathScheduledDate, dailyHours, studyDays, completedLessonIds, leaveDates, extraDailyMinutes,
                lessonCompletionDates);

        List<LearningPathWeekDto> weeks = groupItemsByWeek(timelineItems);

        String projectedEndDate = timelineItems.isEmpty()
                ? "Chưa xác định"
                : timelineItems.get(timelineItems.size() - 1).getPlannedDateFormatted();

        LocalDate finalPlannedDate = timelineItems.isEmpty() ? null
                : timelineItems.get(timelineItems.size() - 1).getPlannedDate();
        LocalDate targetExamDate = lp.getTargetExamDate();
        Long daysUntilExam = null;
        String examBufferDaysText = null;
        if (targetExamDate != null) {
            daysUntilExam = ChronoUnit.DAYS.between(LocalDate.now(), targetExamDate);
            if (finalPlannedDate != null) {
                long buffer = ChronoUnit.DAYS.between(finalPlannedDate, targetExamDate);
                if (buffer >= 14) {
                    examBufferDaysText = "Hoàn thành trước kỳ thi " + buffer
                            + " ngày (Rất an toàn, đủ thời gian luyện đề)";
                } else if (buffer >= 0) {
                    examBufferDaysText = "Hoàn thành sát kỳ thi (" + buffer + " ngày trước ngày thi)";
                } else {
                    examBufferDaysText = "Cảnh báo: Lộ trình trễ " + Math.abs(buffer) + " ngày so với kỳ thi!";
                }
            }
        }

        // Initialize Baseline Plan if not yet established (permanent baseline anchor)
        if (lp.getBaselineEndDate() == null && finalPlannedDate != null) {
            lp.setBaselineStartDate(lp.getBaselineStartDate() != null ? lp.getBaselineStartDate() : pathScheduledDate);
            lp.setBaselineEndDate(finalPlannedDate);
            lp.setBaselineDailyHours(lp.getBaselineDailyHours() != null ? lp.getBaselineDailyHours() : dailyHours);
            learningPathRepository.save(lp);
            log.info("Initialized permanent Baseline Plan for lpId={}: baselineStartDate={}, baselineEndDate={}",
                    lp.getId(), lp.getBaselineStartDate(), lp.getBaselineEndDate());
        }

        LocalDate baselineStart = lp.getBaselineStartDate() != null ? lp.getBaselineStartDate() : pathScheduledDate;
        LocalDate baselineEnd = lp.getBaselineEndDate() != null ? lp.getBaselineEndDate() : finalPlannedDate;
        Long baselineDaysVariance = null;
        String baselineVarianceShortText = null;
        String baselineVarianceText = null;
        String baselineVarianceBadgeClass = "bg-light text-secondary border";
        String planComparisonStatus = "ON_TRACK";

        boolean isAllCompleted = (totalLessons > 0 && completedLessons >= totalLessons) || progressPercent >= 100;
        if (isAllCompleted && !"COMPLETED".equalsIgnoreCase(lp.getPathStatus())) {
            lp.setPathStatus("COMPLETED");
            learningPathRepository.save(lp);
        }

        if (isAllCompleted) {
            // Xác định ngày hoàn thành thực tế của bài học cuối cùng
            LocalDate actualCompletionDate = null;
            try {
                if (progressDto != null && progressDto.getLessons() != null) {
                    for (LessonProgressItemDto item : progressDto.getLessons().values()) {
                        if (item != null && item.getCompletedAt() != null) {
                            try {
                                LocalDate d = Instant.parse(item.getCompletedAt())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate();
                                if (actualCompletionDate == null || d.isAfter(actualCompletionDate)) {
                                    actualCompletionDate = d;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (actualCompletionDate == null && regOpt.isPresent() && regOpt.get().getUpdatedAt() != null) {
                actualCompletionDate = regOpt.get().getUpdatedAt().toLocalDate();
            }
            if (actualCompletionDate == null) {
                actualCompletionDate = (finalPlannedDate != null && baselineEnd != null
                        && !finalPlannedDate.isAfter(baselineEnd))
                                ? finalPlannedDate
                                : (baselineEnd != null && !LocalDate.now().isAfter(baselineEnd) ? LocalDate.now()
                                        : finalPlannedDate);
            }

            if (actualCompletionDate != null) {
                finalPlannedDate = actualCompletionDate;
                projectedEndDate = actualCompletionDate.format(DATE_FORMATTER);
            }

            if (baselineEnd != null && actualCompletionDate != null) {
                baselineDaysVariance = ChronoUnit.DAYS.between(baselineEnd, actualCompletionDate);
                if (baselineDaysVariance < 0) {
                    baselineVarianceShortText = "-" + Math.abs(baselineDaysVariance) + " ngày";
                    baselineVarianceText = String.format("Đã hoàn thành sớm hơn %d ngày so với Kế hoạch ban đầu (%s)",
                            Math.abs(baselineDaysVariance), baselineEnd.format(DATE_FORMATTER));
                    baselineVarianceBadgeClass = "bg-success-subtle text-success border border-success-subtle";
                    planComparisonStatus = "AHEAD";
                } else if (baselineDaysVariance == 0) {
                    baselineVarianceShortText = "0 ngày";
                    baselineVarianceText = String.format("Đã hoàn thành đúng hạn Kế hoạch ban đầu (%s)",
                            baselineEnd.format(DATE_FORMATTER));
                    baselineVarianceBadgeClass = "bg-success-subtle text-success border border-success-subtle";
                    planComparisonStatus = "ON_TRACK";
                } else {
                    baselineVarianceShortText = "+" + baselineDaysVariance + " ngày";
                    baselineVarianceText = String.format("Đã hoàn thành trễ %d ngày so với Kế hoạch ban đầu (%s)",
                            baselineDaysVariance, baselineEnd.format(DATE_FORMATTER));
                    baselineVarianceBadgeClass = "bg-secondary-subtle text-secondary border";
                    planComparisonStatus = "DELAYED";
                }
            }
        } else if (baselineEnd != null && finalPlannedDate != null) {
            baselineDaysVariance = ChronoUnit.DAYS.between(baselineEnd, finalPlannedDate);
            if (baselineDaysVariance > 0) {
                baselineVarianceShortText = "+" + baselineDaysVariance + " ngày";
                baselineVarianceText = String.format("Trễ %d ngày so với Kế hoạch ban đầu (%s)",
                        baselineDaysVariance, baselineEnd.format(DATE_FORMATTER));
                baselineVarianceBadgeClass = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
                planComparisonStatus = "DELAYED";
            } else if (baselineDaysVariance < 0) {
                baselineVarianceShortText = "-" + Math.abs(baselineDaysVariance) + " ngày";
                baselineVarianceText = String.format("Sớm hơn %d ngày so với Kế hoạch ban đầu (%s)",
                        Math.abs(baselineDaysVariance), baselineEnd.format(DATE_FORMATTER));
                baselineVarianceBadgeClass = "bg-success-subtle text-success border border-success-subtle";
                planComparisonStatus = "AHEAD";
            } else {
                baselineVarianceShortText = "0 ngày";
                baselineVarianceText = String.format("Đúng tiến độ Kế hoạch ban đầu (%s)",
                        baselineEnd.format(DATE_FORMATTER));
                baselineVarianceBadgeClass = "bg-info-subtle text-info border border-info-subtle";
                planComparisonStatus = "ON_TRACK";
            }
        }

        int totalHours = course != null && course.getDurationHours() != null
                ? course.getDurationHours()
                : (int) Math.ceil(
                        focusRecommendations.stream().mapToInt(LessonSkillFocusDto::getRecommendedDurationMinutes).sum()
                                / 60.0);

        List<String> studyDaysList = studyDays.stream().map(Enum::name).collect(Collectors.toList());

        TodayFocusDto todayFocus = buildTodayFocus(user, lp, studyDays, timelineItems, completedLessons, totalLessons,
                activeLeave, catchUpMode, catchUpExtraMinutes);

        return LearningPathResponse.builder()
                .hasPath(true)
                .learningPathId(lp.getId())
                .courseId(lp.getCourseId())
                .courseTitle(courseTitle)
                .courseLevel(courseLevel)
                .targetLevel(lp.getTargetLevel())
                .currentLevel(lp.getCurrentLevel() != null ? lp.getCurrentLevel() : "Chưa xác định")
                .dailyHours(dailyHours)
                .dailyHoursFormatted(
                        dailyHours != null ? String.format(java.util.Locale.US, "%.2f giờ/ngày", dailyHours)
                                : "1.00 giờ/ngày")
                .weeklyHours(weeklyHours)
                .studyDays(studyDaysList)
                .studyDaysText(formatStudyDaysText(studyDays))
                .targetExamDate(targetExamDate)
                .targetExamDateFormatted(targetExamDate != null ? targetExamDate.format(DATE_FORMATTER) : null)
                .daysUntilExam(daysUntilExam)
                .examBufferDaysText(examBufferDaysText)
                .scheduleMode(lp.getScheduleMode() != null ? lp.getScheduleMode() : "CUSTOM")
                .startDateFormatted(pathScheduledDate != null ? pathScheduledDate.format(DATE_FORMATTER) : "--")
                .projectedEndDateFormatted(projectedEndDate)
                .baselineStartDate(baselineStart)
                .baselineStartDateFormatted(baselineStart != null ? baselineStart.format(DATE_FORMATTER) : "--")
                .baselineEndDate(baselineEnd)
                .baselineEndDateFormatted(baselineEnd != null ? baselineEnd.format(DATE_FORMATTER) : "--")
                .baselineDailyHours(lp.getBaselineDailyHours() != null ? lp.getBaselineDailyHours() : dailyHours)
                .baselineWeeklyHours(lp.calculateBaselineWeeklyHours(studyDays != null ? studyDays.size() : 7))
                .baselineDaysVariance(baselineDaysVariance)
                .baselineVarianceShortText(baselineVarianceShortText)
                .baselineVarianceText(baselineVarianceText)
                .baselineVarianceBadgeClass(baselineVarianceBadgeClass)
                .planComparisonStatus(planComparisonStatus)
                .progressPercent(progressPercent)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .remainingLessons(remainingLessons)
                .totalHours(totalHours)
                .pathStatus(lp.getPathStatus())
                .statusBadgeClass("ACTIVE".equalsIgnoreCase(lp.getPathStatus()) ? "bg-success" : "bg-secondary")
                .skillProfile(skillProfile)
                .todayFocus(todayFocus)
                .catchUpMode(catchUpMode)
                .catchUpExtraMinutes(catchUpExtraMinutes)
                .catchUpIncludeWeekends(catchUpIncludeWeekends)
                .catchUpStatusMessage(catchUpMode
                        ? String.format("Đang bật (+%d phút/ngày%s)", catchUpExtraMinutes,
                                catchUpIncludeWeekends ? ", bao gồm cuối tuần" : "")
                        : null)
                .currentlyOnLeave(activeLeave != null)
                .currentLeaveReason(activeLeave != null ? activeLeave.getReason() : null)
                .currentLeaveEndDateFormatted(activeLeave != null ? activeLeave.getEndDateFormatted() : null)
                .leaveRecords(leaveRecords)
                .enrolledCourses(enrolledCourses)
                .items(timelineItems)
                .weeks(weeks)
                .build();
    }

    @Override
    @Transactional
    public RecalibratePreviewResponse getRecalibratePreview(String username, RecalibrateRequest request) {
        log.info(
                "Calculating personalized recalibrate preview for learner={} with weeklyHours={}, dailyHours={}, studyDays={}, courseId={}",
                username, request != null ? request.getWeeklyHours() : null,
                request != null ? request.getDailyHours() : null,
                request != null ? request.getStudyDays() : null,
                request != null ? request.getCourseId() : null);
        User user = findUserByUsername(username);

        if (request != null && request.getStartDate() != null && request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu học dự kiến không được trước ngày hôm nay ("
                    + LocalDate.now().format(DATE_FORMATTER) + ").");
        }

        Long requestedCourseId = request != null ? request.getCourseId() : null;
        Optional<LearningPath> lpOpt = findPublishedLearningPathForUser(user.getId(), requestedCourseId);

        if (lpOpt.isEmpty()) {
            return RecalibratePreviewResponse.builder()
                    .hasPath(false)
                    .message("Chưa đủ dữ liệu để điều chỉnh lịch học hoặc bạn chưa đăng ký khóa học này.")
                    .rescheduledItems(Collections.emptyList())
                    .build();
        }

        LearningPath lp = lpOpt.get();
        Course course = courseRepository.findById(lp.getCourseId()).orElse(null);
        if (course == null || !"PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            return RecalibratePreviewResponse.builder()
                    .hasPath(false)
                    .message("Khóa học chưa được công bố.")
                    .rescheduledItems(Collections.emptyList())
                    .build();
        }
        String courseTitle = (course != null && course.getTitle() != null)
                ? course.getTitle()
                : "Khóa học #" + lp.getCourseId();
        String courseLevel = resolveLevelFromCourse(course);

        List<CourseRegistration> allRegistrations = courseRegistrationService.getUserRegistrations(user.getId());
        List<CourseOptionDto> courseOptions = allRegistrations.stream()
                .filter(r -> r.getCourse() != null && "PUBLISHED".equalsIgnoreCase(r.getCourse().getStatus()))
                .map(r -> CourseOptionDto.builder()
                        .courseId(r.getCourse().getId())
                        .courseTitle(r.getCourse().getTitle())
                        .courseLevel(resolveLevelFromCourse(r.getCourse()))
                        .progressPercent(r.getProgressPercent() != null ? r.getProgressPercent() : 0)
                        .selected(r.getCourse().getId().equals(lp.getCourseId()))
                        .build())
                .collect(Collectors.toList());

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lp.getCourseId());
        Optional<CourseRegistration> regOpt = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), lp.getCourseId());

        int progressPercent = regOpt.map(CourseRegistration::getProgressPercent).orElse(0);
        int totalLessons = lessons.size();

        SkillProfileDto skillProfile = skillAnalysisService.analyzeAndRecalibrateSkills(user.getId(), lp.getCourseId(),
                courseLevel, lp.getResultId());
        List<LessonSkillFocusDto> focusRecommendations = skillFocusRecommendationService.recommendFocusBatch(lessons,
                skillProfile);

        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(user.getId(), lp.getCourseId(),
                lessons, progressPercent);
        int completedLessons = completedLessonIds.size();

        if (totalLessons > 0 && completedLessons >= totalLessons) {
            return RecalibratePreviewResponse.builder()
                    .hasPath(false)
                    .message(
                            "Bạn đã hoàn thành 100% tất cả các bài học trong khóa học này. Lộ trình học tập đã hoàn tất xuất sắc!")
                    .rescheduledItems(Collections.emptyList())
                    .build();
        }

        LocalDate pathScheduledDate = lp.getScheduledDate();
        if (completedLessons > 0 && pathScheduledDate != null && pathScheduledDate.isAfter(LocalDate.now())) {
            pathScheduledDate = regOpt.map(r -> r.getRegisteredAt().toLocalDate())
                    .orElse(LocalDate.now().minusDays(1));
            lp.setScheduledDate(pathScheduledDate);
            learningPathRepository.save(lp);
        }

        Set<DayOfWeek> originalStudyDays = parseStudyDays(lp.getStudyDays(), null);
        BigDecimal originalDailyHours = lp.getDailyHoursSetup() != null
                ? lp.getDailyHoursSetup()
                : BigDecimal.valueOf(1.5);
        int originalWeeklyHours = Math.max(1,
                originalDailyHours.multiply(BigDecimal.valueOf(originalStudyDays.size())).intValue());

        Set<DayOfWeek> newStudyDays = parseStudyDays(lp.getStudyDays(),
                request != null ? request.getStudyDays() : null);

        BigDecimal newDailyHours;
        int newWeeklyHours;
        if (request != null && request.getDailyHours() != null
                && request.getDailyHours().compareTo(BigDecimal.ZERO) > 0) {
            newDailyHours = request.getDailyHours();
            newWeeklyHours = Math.max(1, newDailyHours.multiply(BigDecimal.valueOf(newStudyDays.size())).intValue());
        } else if (request != null && request.getWeeklyHours() != null && request.getWeeklyHours() > 0) {
            newWeeklyHours = request.getWeeklyHours();
            newDailyHours = BigDecimal.valueOf(newWeeklyHours)
                    .divide(BigDecimal.valueOf(newStudyDays.size()), 2, RoundingMode.HALF_UP);
        } else {
            newDailyHours = originalDailyHours;
            newWeeklyHours = Math.max(1, newDailyHours.multiply(BigDecimal.valueOf(newStudyDays.size())).intValue());
        }

        LocalDate newStartDate = (request != null && request.getStartDate() != null)
                ? request.getStartDate()
                : (pathScheduledDate != null && !pathScheduledDate.isBefore(LocalDate.now()) ? pathScheduledDate
                        : LocalDate.now());

        LocalDate targetExamDate = resolveTargetExamDate(
                request != null ? request.getJlptPeriod() : null,
                request != null ? request.getTargetExamDate() : null,
                lp.getTargetExamDate());

        List<LearningPathTimelineItemDto> originalItems = buildTimelineItems(
                user.getId(), lp.getCourseId(), lessons, focusRecommendations,
                pathScheduledDate, originalDailyHours, originalStudyDays, completedLessonIds);

        LocalDate originalEndDate = originalItems.isEmpty()
                ? LocalDate.now().plusMonths(2)
                : originalItems.get(originalItems.size() - 1).getPlannedDate();

        LocalDate baselineEnd = lp.getBaselineEndDate() != null ? lp.getBaselineEndDate() : originalEndDate;
        LocalDate baselineStart = lp.getBaselineStartDate() != null ? lp.getBaselineStartDate() : pathScheduledDate;

        List<LeaveRecordDto> leaveRecords = parseLeaveRecords(lp.getLeaveRecords());
        Set<LocalDate> leaveDates = extractLeaveDates(leaveRecords);

        List<LearningPathTimelineItemDto> rescheduledItems = rescheduleItems(
                originalItems, completedLessons, newDailyHours, newStudyDays, newStartDate, leaveDates);

        LocalDate newEndDate = rescheduledItems.isEmpty()
                ? LocalDate.now()
                : rescheduledItems.get(rescheduledItems.size() - 1).getPlannedDate();

        // Comparison against Current Plan (Kế hoạch hiện tại)
        long daysDiff = ChronoUnit.DAYS.between(originalEndDate, newEndDate);
        String daysDiffText;
        if (daysDiff < 0) {
            daysDiffText = String.format("Hoàn thành sớm hơn %d ngày so với Kế hoạch hiện tại", Math.abs(daysDiff));
        } else if (daysDiff > 0) {
            daysDiffText = String.format("Kéo dài thêm %d ngày so với Kế hoạch hiện tại", daysDiff);
        } else {
            daysDiffText = "Tiến độ tương đương Kế hoạch hiện tại";
        }

        // Comparison against Baseline Plan (Kế hoạch ban đầu cố định)
        Long daysDiffFromBaseline = null;
        String daysDiffFromBaselineText = null;
        if (baselineEnd != null) {
            daysDiffFromBaseline = ChronoUnit.DAYS.between(baselineEnd, newEndDate);
            if (daysDiffFromBaseline > 0) {
                daysDiffFromBaselineText = String.format("Kéo dài thêm %d ngày so với Kế hoạch ban đầu (%s)",
                        daysDiffFromBaseline, baselineEnd.format(DATE_FORMATTER));
            } else if (daysDiffFromBaseline < 0) {
                daysDiffFromBaselineText = String.format("Hoàn thành sớm hơn %d ngày so với Kế hoạch ban đầu (%s)",
                        Math.abs(daysDiffFromBaseline), baselineEnd.format(DATE_FORMATTER));
            } else {
                daysDiffFromBaselineText = String.format("Khớp ngày hoàn thành Kế hoạch ban đầu (%s)",
                        baselineEnd.format(DATE_FORMATTER));
            }
        }

        int remainingMinutes = 0;
        for (int i = 0; i < originalItems.size(); i++) {
            if (!"COMPLETED".equalsIgnoreCase(originalItems.get(i).getStatus())) {
                remainingMinutes += (originalItems.get(i).getRecommendedDurationMinutes() != null
                        ? originalItems.get(i).getRecommendedDurationMinutes()
                        : (originalItems.get(i).getDurationMinutes() != null ? originalItems.get(i).getDurationMinutes()
                                : 60));
            }
        }
        int remainingHours = Math.max(1, (int) Math.ceil(remainingMinutes / 60.0));

        // Group rescheduled items into actual calendar weeks
        List<LearningPathWeekDto> weeks = groupItemsByWeek(rescheduledItems);
        int weeksNeeded = Math.max(1, weeks.size());

        // JLPT Target Date analysis
        Long daysUntilExam = null;
        Long examBufferDays = null;
        String examBufferDaysText = null;
        String examReadinessStatus = "NO_EXAM";
        String examReadinessBadgeClass = "bg-secondary-subtle text-secondary border";
        BigDecimal recommendedDailyHoursForExam = null;

        if (targetExamDate != null) {
            daysUntilExam = ChronoUnit.DAYS.between(LocalDate.now(), targetExamDate);
            examBufferDays = ChronoUnit.DAYS.between(newEndDate, targetExamDate);

            if (examBufferDays >= 14) {
                examReadinessStatus = "ON_TRACK";
                examReadinessBadgeClass = "bg-success-subtle text-success border border-success-subtle";
                examBufferDaysText = "Hoàn thành trước kỳ thi " + examBufferDays
                        + " ngày (Rất an toàn, đủ thời gian luyện đề tổng hợp)";
            } else if (examBufferDays >= 0) {
                examReadinessStatus = "TIGHT";
                examReadinessBadgeClass = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
                examBufferDaysText = "Hoàn thành sát kỳ thi (" + examBufferDays
                        + " ngày trước ngày thi, nên duy trì kỷ luật)";
            } else {
                examReadinessStatus = "BEHIND";
                examReadinessBadgeClass = "bg-danger-subtle text-danger border border-danger-subtle";
                examBufferDaysText = "Cảnh báo: Lộ trình trễ " + Math.abs(examBufferDays)
                        + " ngày so với ngày thi! Cần tăng giờ học hoặc thêm ngày học.";
            }

            // Estimate recommended hours for exam target
            LocalDate examBufferTarget = targetExamDate.minusWeeks(1);
            long daysToTarget = ChronoUnit.DAYS.between(newStartDate != null ? newStartDate : LocalDate.now(),
                    examBufferTarget);
            long estimatedStudyDaysCount = Math.max(1, (daysToTarget * newStudyDays.size()) / 7);
            if (estimatedStudyDaysCount > 0 && remainingMinutes > 0) {
                double minPerDay = (double) remainingMinutes / estimatedStudyDaysCount;
                recommendedDailyHoursForExam = BigDecimal.valueOf(minPerDay / 60.0).setScale(1, RoundingMode.HALF_UP);
            }
        }

        String paceAnalysis = buildPaceAnalysis(newWeeklyHours, weeksNeeded, remainingHours, newStudyDays.size(),
                newDailyHours, targetExamDate, examBufferDaysText);

        List<String> studyDaysList = newStudyDays.stream().map(Enum::name).collect(Collectors.toList());

        return RecalibratePreviewResponse.builder()
                .hasPath(true)
                .courseId(lp.getCourseId())
                .courseTitle(courseTitle)
                .courseLevel(courseLevel)
                .enrolledCourses(courseOptions)
                .originalWeeklyHours(originalWeeklyHours)
                .newWeeklyHours(newWeeklyHours)
                .dailyHours(newDailyHours)
                .studyDays(studyDaysList)
                .studyDaysText(formatStudyDaysText(newStudyDays))
                .startDate(newStartDate)
                .startDateFormatted(newStartDate != null ? newStartDate.format(DATE_FORMATTER) : null)
                .targetExamDate(targetExamDate)
                .targetExamDateFormatted(targetExamDate != null ? targetExamDate.format(DATE_FORMATTER) : null)
                .daysUntilExam(daysUntilExam)
                .examBufferDays(examBufferDays)
                .examBufferDaysText(examBufferDaysText)
                .examReadinessStatus(examReadinessStatus)
                .examReadinessBadgeClass(examReadinessBadgeClass)
                .recommendedDailyHoursForExam(recommendedDailyHoursForExam)
                .scheduleMode(
                        request != null && request.getScheduleMode() != null ? request.getScheduleMode() : "CUSTOM")
                .baselineStartDate(baselineStart)
                .baselineStartDateFormatted(baselineStart != null ? baselineStart.format(DATE_FORMATTER) : null)
                .baselineEndDate(baselineEnd)
                .baselineEndDateFormatted(baselineEnd != null ? baselineEnd.format(DATE_FORMATTER) : null)
                .daysDiffFromBaseline(daysDiffFromBaseline)
                .daysDiffFromBaselineText(daysDiffFromBaselineText)
                .originalCompletionDate(originalEndDate.format(DATE_FORMATTER))
                .newCompletionDate(newEndDate.format(DATE_FORMATTER))
                .daysDifferenceText(daysDiffText)
                .remainingHours(remainingHours)
                .weeksNeeded(weeksNeeded)
                .paceAnalysisText(paceAnalysis)
                .rescheduledItems(rescheduledItems)
                .weeks(weeks)
                .build();
    }

    private List<LearningPathTimelineItemDto> buildTimelineItems(
            Long userId, Long courseId,
            List<Lesson> lessons, List<LessonSkillFocusDto> focusRecommendations,
            LocalDate startDate, BigDecimal dailyHours, Set<DayOfWeek> studyDays, Set<Long> completedLessonIds) {
        return buildTimelineItems(userId, courseId, lessons, focusRecommendations,
                startDate, dailyHours, studyDays, completedLessonIds, Collections.emptySet(), 0);
    }

    private List<LearningPathTimelineItemDto> buildTimelineItems(
            Long userId, Long courseId,
            List<Lesson> lessons, List<LessonSkillFocusDto> focusRecommendations,
            LocalDate startDate, BigDecimal dailyHours, Set<DayOfWeek> studyDays,
            Set<Long> completedLessonIds, Set<LocalDate> leaveDates, int extraDailyMinutes) {
        return buildTimelineItems(userId, courseId, lessons, focusRecommendations,
                startDate, dailyHours, studyDays, completedLessonIds, leaveDates, extraDailyMinutes,
                Collections.emptyMap());
    }

    private List<LearningPathTimelineItemDto> buildTimelineItems(
            Long userId, Long courseId,
            List<Lesson> lessons, List<LessonSkillFocusDto> focusRecommendations,
            LocalDate startDate, BigDecimal dailyHours, Set<DayOfWeek> studyDays,
            Set<Long> completedLessonIds, Set<LocalDate> leaveDates, int extraDailyMinutes,
            Map<Long, LocalDate> lessonCompletionDates) {

        if (lessons == null || lessons.isEmpty()) {
            return Collections.emptyList();
        }

        Set<DayOfWeek> effectiveStudyDays = (studyDays != null && !studyDays.isEmpty())
                ? EnumSet.copyOf(studyDays)
                : EnumSet.allOf(DayOfWeek.class);

        LocalDate referenceDate = LocalDate.now();
        LocalDate courseStart = (startDate != null) ? startDate : referenceDate;
        courseStart = findNextOrSameStudyDate(courseStart, effectiveStudyDays, leaveDates);
        LocalDate currentDate = courseStart;

        List<LearningPathTimelineItemDto> items = new ArrayList<>(lessons.size());
        int dailyMinutesLimit = dailyHours != null ? dailyHours.multiply(BigDecimal.valueOf(60)).intValue() : 90;
        if (dailyMinutesLimit <= 0)
            dailyMinutesLimit = 90;
        if (extraDailyMinutes > 0) {
            dailyMinutesLimit += extraDailyMinutes;
        }

        int accumulatedMinutesToday = 0;
        boolean hasSnappedToEffectiveStart = false;
        boolean firstInProgressEncountered = false;
        Map<Long, String> milestoneUrlCache = new HashMap<>();

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

            boolean isCompleted = completedLessonIds != null && completedLessonIds.contains(lesson.getId());
            boolean isOverflow = duration > dailyMinutesLimit;
            LocalDate itemDate;

            if (isCompleted && lessonCompletionDates != null && lessonCompletionDates.containsKey(lesson.getId())) {
                itemDate = lessonCompletionDates.get(lesson.getId());
            } else {
                // Snap incomplete items to effective start date (new start date or today)
                if (!isCompleted && !hasSnappedToEffectiveStart) {
                    hasSnappedToEffectiveStart = true;
                    LocalDate effectiveStart = (startDate != null && !startDate.isBefore(referenceDate))
                            ? startDate
                            : referenceDate;
                    if (currentDate.isBefore(effectiveStart)) {
                        currentDate = findNextOrSameStudyDate(effectiveStart, effectiveStudyDays, leaveDates);
                        accumulatedMinutesToday = 0;
                    }
                }

                accumulatedMinutesToday += duration;

                if (accumulatedMinutesToday > dailyMinutesLimit && accumulatedMinutesToday > duration) {
                    currentDate = findNextStudyDate(currentDate, effectiveStudyDays, leaveDates);
                    accumulatedMinutesToday = duration;
                }
                itemDate = currentDate;
            }

            long daysFromStart = ChronoUnit.DAYS.between(courseStart, itemDate);
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
                            ? milestoneUrlCache.computeIfAbsent(lesson.getId(),
                                    lid -> examService.resolveMilestoneExamUrl(courseId, courseLevel, lid))
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
                    .contentType(
                            isMilestone ? "EXAM" : (lesson.getLessonType() != null ? lesson.getLessonType() : "LESSON"))
                    .durationMinutes(baseDuration)
                    .durationFormatted(formatDuration(baseDuration))
                    .targetWeek(targetWeek)
                    .plannedDate(itemDate)
                    .plannedDateFormatted(itemDate.format(DATE_FORMATTER))
                    .dayOfWeekFormatted(formatDayOfWeekShort(itemDate.getDayOfWeek()))
                    .dateWithDayOfWeek(formatDateWithDayOfWeek(itemDate))
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

        // Post-processing: Normalize targetWeek across all items so week numbers are
        // strictly monotonic and consistent with dates
        LocalDate earliestDate = items.stream()
                .map(LearningPathTimelineItemDto::getPlannedDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(courseStart);

        for (LearningPathTimelineItemDto it : items) {
            if (it.getPlannedDate() != null) {
                long days = ChronoUnit.DAYS.between(earliestDate, it.getPlannedDate());
                it.setTargetWeek(Math.max(1, (int) (days / 7) + 1));
            }
        }

        return items;
    }

    private List<LearningPathWeekDto> groupItemsByWeek(List<LearningPathTimelineItemDto> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<LearningPathTimelineItemDto>> byWeek = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getTargetWeek() != null ? item.getTargetWeek() : 1,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<LearningPathWeekDto> weeks = new ArrayList<>();
        boolean currentWeekSet = false;

        for (Map.Entry<Integer, List<LearningPathTimelineItemDto>> entry : byWeek.entrySet()) {
            int weekNum = entry.getKey();
            List<LearningPathTimelineItemDto> weekItems = entry.getValue();

            LocalDate minDate = weekItems.stream()
                    .map(LearningPathTimelineItemDto::getPlannedDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            LocalDate maxDate = weekItems.stream()
                    .map(LearningPathTimelineItemDto::getPlannedDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            String startDateFormatted = minDate != null ? minDate.format(DATE_FORMATTER) : "N/A";
            String endDateFormatted = maxDate != null ? maxDate.format(DATE_FORMATTER) : "N/A";
            String dateRangeFormatted;
            if (minDate != null && maxDate != null) {
                if (minDate.equals(maxDate)) {
                    dateRangeFormatted = startDateFormatted;
                } else {
                    dateRangeFormatted = startDateFormatted + " - " + endDateFormatted;
                }
            } else {
                dateRangeFormatted = "Chưa xác định";
            }

            int total = weekItems.size();
            int completed = (int) weekItems.stream().filter(it -> "COMPLETED".equalsIgnoreCase(it.getStatus())).count();
            int inProgress = (int) weekItems.stream().filter(it -> "IN_PROGRESS".equalsIgnoreCase(it.getStatus()))
                    .count();

            String status;
            String statusDisplay;
            String statusBadgeClass;
            boolean isCurrentWeek = false;

            if (completed == total) {
                status = "COMPLETED";
                statusDisplay = "Đã hoàn thành";
                statusBadgeClass = "bg-success-subtle text-success border border-success-subtle";
            } else if (inProgress > 0 || (!currentWeekSet && completed < total)) {
                status = "IN_PROGRESS";
                statusDisplay = "Đang học";
                statusBadgeClass = "bg-primary-subtle text-primary border border-primary-subtle";
                isCurrentWeek = true;
                currentWeekSet = true;
            } else {
                status = "UPCOMING";
                statusDisplay = "Chưa học";
                statusBadgeClass = "bg-light text-secondary border";
            }

            int totalMinutes = weekItems.stream()
                    .mapToInt(it -> it.getRecommendedDurationMinutes() != null ? it.getRecommendedDurationMinutes()
                            : (it.getDurationMinutes() != null ? it.getDurationMinutes() : 60))
                    .sum();

            // Group lessons in this week by plannedDate to build day dropdowns
            Map<LocalDate, List<LearningPathTimelineItemDto>> byDate = weekItems.stream()
                    .filter(it -> it.getPlannedDate() != null)
                    .collect(Collectors.groupingBy(
                            LearningPathTimelineItemDto::getPlannedDate,
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<LearningPathDayDto> days = new ArrayList<>();
            int dayIdx = 1;
            boolean currentDaySetInWeek = false;

            for (Map.Entry<LocalDate, List<LearningPathTimelineItemDto>> dateEntry : byDate.entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<LearningPathTimelineItemDto> dayLessons = dateEntry.getValue();

                int dayTotalLessons = dayLessons.size();
                int dayCompleted = (int) dayLessons.stream().filter(it -> "COMPLETED".equalsIgnoreCase(it.getStatus()))
                        .count();
                int dayInProgress = (int) dayLessons.stream()
                        .filter(it -> "IN_PROGRESS".equalsIgnoreCase(it.getStatus())).count();

                String dayStatus;
                String dayStatusDisplay;
                String dayStatusBadgeClass;
                boolean isCurrentDay = false;

                if (dayCompleted == dayTotalLessons && dayTotalLessons > 0) {
                    dayStatus = "COMPLETED";
                    dayStatusDisplay = "Đã hoàn thành";
                    dayStatusBadgeClass = "bg-success-subtle text-success border border-success-subtle";
                } else if (dayInProgress > 0 || (!currentDaySetInWeek && dayCompleted < dayTotalLessons)) {
                    dayStatus = "IN_PROGRESS";
                    dayStatusDisplay = "Đang học";
                    dayStatusBadgeClass = "bg-primary-subtle text-primary border border-primary-subtle";
                    isCurrentDay = true;
                    currentDaySetInWeek = true;
                } else {
                    dayStatus = "UPCOMING";
                    dayStatusDisplay = "Chưa học";
                    dayStatusBadgeClass = "bg-light text-secondary border";
                }

                int dayMinutes = dayLessons.stream()
                        .mapToInt(it -> it.getRecommendedDurationMinutes() != null ? it.getRecommendedDurationMinutes()
                                : (it.getDurationMinutes() != null ? it.getDurationMinutes() : 60))
                        .sum();

                days.add(LearningPathDayDto.builder()
                        .dayIndex(dayIdx++)
                        .date(date)
                        .dateFormatted(date.format(DATE_FORMATTER))
                        .dayOfWeek(date.getDayOfWeek())
                        .dayOfWeekFormatted(formatDayOfWeekShort(date.getDayOfWeek()))
                        .fullDateDisplay(formatDateWithDayOfWeek(date))
                        .totalMinutes(dayMinutes)
                        .totalDurationFormatted(formatDuration(dayMinutes))
                        .totalLessons(dayTotalLessons)
                        .completedLessons(dayCompleted)
                        .status(dayStatus)
                        .statusDisplay(dayStatusDisplay)
                        .statusBadgeClass(dayStatusBadgeClass)
                        .isToday(LocalDate.now().equals(date))
                        .isCurrentDay(isCurrentDay)
                        .items(dayLessons)
                        .build());
            }

            if (!currentDaySetInWeek && isCurrentWeek && !days.isEmpty()) {
                days.get(0).setCurrentDay(true);
            }

            int weekProgressPercent = (total > 0) ? (int) Math.round((completed * 100.0) / total) : 0;

            weeks.add(LearningPathWeekDto.builder()
                    .weekNumber(weekNum)
                    .weekTitle("Tuần " + weekNum)
                    .startDate(minDate)
                    .endDate(maxDate)
                    .startDateFormatted(startDateFormatted)
                    .endDateFormatted(endDateFormatted)
                    .dateRangeFormatted(dateRangeFormatted)
                    .fullTitle("Tuần " + weekNum + " (" + dateRangeFormatted + ")")
                    .totalLessons(total)
                    .completedLessons(completed)
                    .inProgressLessons(inProgress)
                    .progressPercent(weekProgressPercent)
                    .status(status)
                    .statusDisplay(statusDisplay)
                    .statusBadgeClass(statusBadgeClass)
                    .isCurrentWeek(isCurrentWeek)
                    .totalMinutes(totalMinutes)
                    .totalDurationFormatted(formatDuration(totalMinutes))
                    .items(weekItems)
                    .days(days)
                    .build());
        }

        if (!currentWeekSet && !weeks.isEmpty()) {
            weeks.get(0).setCurrentWeek(true);
        }

        return weeks;
    }

    private List<LearningPathTimelineItemDto> rescheduleItems(
            List<LearningPathTimelineItemDto> originalItems, int completedCount,
            BigDecimal dailyHours, Set<DayOfWeek> studyDays, LocalDate newStartDate, Set<LocalDate> leaveDates) {

        if (studyDays == null || studyDays.isEmpty()) {
            studyDays = EnumSet.allOf(DayOfWeek.class);
        }

        List<LearningPathTimelineItemDto> result = new ArrayList<>();
        LocalDate referenceDate = LocalDate.now();
        LocalDate baseStartDate = (newStartDate != null && !newStartDate.isBefore(referenceDate))
                ? newStartDate
                : referenceDate;
        studyDays.add(baseStartDate.getDayOfWeek());
        LocalDate runningDate = baseStartDate;

        int dailyMinutesBudget = dailyHours != null ? dailyHours.multiply(BigDecimal.valueOf(60)).intValue() : 90;
        if (dailyMinutesBudget <= 0)
            dailyMinutesBudget = 90;
        int accumulatedMinutesToday = 0;

        for (int i = 0; i < originalItems.size(); i++) {
            LearningPathTimelineItemDto item = originalItems.get(i);

            // Hide already completed lessons in Step 2: only reschedule and display uncompleted lessons
            if ("COMPLETED".equalsIgnoreCase(item.getStatus())) {
                continue;
            }

            int duration = (item.getRecommendedDurationMinutes() != null
                    && item.getRecommendedDurationMinutes() > 0)
                            ? item.getRecommendedDurationMinutes()
                            : ((item.getDurationMinutes() != null && item.getDurationMinutes() > 0)
                                    ? item.getDurationMinutes()
                                    : 60);

            accumulatedMinutesToday += duration;
            if (accumulatedMinutesToday > dailyMinutesBudget && accumulatedMinutesToday > duration) {
                runningDate = findNextStudyDate(runningDate, studyDays, leaveDates);
                accumulatedMinutesToday = duration;
            }

            // Target week is calculated relative to baseStartDate (Week 1, Week 2, ...) of the new schedule
            long daysFromStart = ChronoUnit.DAYS.between(baseStartDate, runningDate);
            int targetWeek = Math.max(1, (int) (daysFromStart / 7) + 1);

                result.add(LearningPathTimelineItemDto.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .sectionName(item.getSectionName())
                        .contentType(item.getContentType())
                        .durationMinutes(item.getDurationMinutes())
                        .durationFormatted(item.getDurationFormatted() != null ? item.getDurationFormatted()
                                : formatDuration(item.getDurationMinutes()))
                        .targetWeek(targetWeek)
                        .plannedDate(runningDate)
                        .plannedDateFormatted(runningDate.format(DATE_FORMATTER))
                        .dayOfWeekFormatted(formatDayOfWeekShort(runningDate.getDayOfWeek()))
                        .dateWithDayOfWeek(formatDateWithDayOfWeek(runningDate))
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

        // Post-processing: Normalize targetWeek across all rescheduled items so week
        // numbers are strictly monotonic and consistent with dates
        LocalDate earliestRescheduledDate = result.stream()
                .map(LearningPathTimelineItemDto::getPlannedDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(baseStartDate);

        for (LearningPathTimelineItemDto it : result) {
            if (it.getPlannedDate() != null) {
                long days = ChronoUnit.DAYS.between(earliestRescheduledDate, it.getPlannedDate());
                it.setTargetWeek(Math.max(1, (int) (days / 7) + 1));
            }
        }

        return result;
    }

    private String formatDuration(int minutes) {
        if (minutes <= 0)
            return "1 giờ";
        int hours = minutes / 60;
        int remaining = minutes % 60;
        if (hours == 0)
            return remaining + " phút";
        if (remaining == 0)
            return hours + " giờ";
        return hours + " giờ " + remaining + " phút";
    }

    private String buildPaceAnalysis(int weeklyHours, int weeksNeeded, int remainingHours) {
        return buildPaceAnalysis(weeklyHours, weeksNeeded, remainingHours, 7, BigDecimal.valueOf(weeklyHours / 7.0),
                null, null);
    }

    private String buildPaceAnalysis(int weeklyHours, int weeksNeeded, int remainingHours,
            int daysPerWeek, BigDecimal dailyHours, LocalDate targetExamDate, String examBufferDaysText) {
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

        if (daysPerWeek > 0 && dailyHours != null) {
            sb.append(" Lịch học: ").append(daysPerWeek).append(" buổi/tuần, khoảng ")
                    .append(dailyHours).append(" giờ/buổi.");
        }

        if (targetExamDate != null && examBufferDaysText != null) {
            sb.append(" [Mục tiêu JLPT ").append(targetExamDate.format(DATE_FORMATTER)).append(": ")
                    .append(examBufferDaysText).append("]");
        }

        return sb.toString();
    }

    @Override
    @Transactional
    public void applyRecalibratedSchedule(String username, RecalibrateRequest request) {
        log.info("Applying recalibrated schedule for learner={} with weeklyHours={}, dailyHours={}, studyDays={}",
                username, request != null ? request.getWeeklyHours() : null,
                request != null ? request.getDailyHours() : null,
                request != null ? request.getStudyDays() : null);
        User user = findUserByUsername(username);

        if (request != null && request.getStartDate() != null && request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu học dự kiến không được trước ngày hôm nay ("
                    + LocalDate.now().format(DATE_FORMATTER) + ").");
        }

        Long requestedCourseId = request != null ? request.getCourseId() : null;
        Optional<LearningPath> lpOpt = findPublishedLearningPathForUser(user.getId(), requestedCourseId);

        if (lpOpt.isEmpty()) {
            throw new IllegalStateException(
                    "Không tìm thấy lộ trình học tập với khóa học đã đăng ký và xuất bản để cập nhật.");
        }

        LearningPath lp = lpOpt.get();

        // Validate: Không cho phép thiết lập lại lịch học khi người dùng đã học xong
        // tất cả các bài học
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lp.getCourseId());
        Optional<CourseRegistration> regOpt = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), lp.getCourseId());
        int progressPercent = regOpt.map(CourseRegistration::getProgressPercent).orElse(0);
        int totalLessons = lessons.size();
        Set<Long> completedLessonIds = lessonProgressService.getCompletedLessonIds(user.getId(), lp.getCourseId(),
                lessons, progressPercent);
        if (totalLessons > 0 && completedLessonIds.size() >= totalLessons) {
            throw new IllegalStateException(
                    "Bạn đã hoàn thành tất cả các bài học trong khóa học này. Không thể thiết lập lại lịch học cho lộ trình đã hoàn tất.");
        }

        if (request != null) {
            int daysCount = Math.max(1, (request.getStudyDays() != null && !request.getStudyDays().isEmpty())
                    ? request.getStudyDays().size()
                    : (lp.getStudyDays() != null && !lp.getStudyDays().isBlank()
                            ? parseStudyDays(lp.getStudyDays(), null).size()
                            : 7));

            if (request.getDailyHours() != null && request.getDailyHours().compareTo(BigDecimal.ZERO) > 0) {
                lp.setDailyHoursSetup(request.getDailyHours());
            } else if (request.getWeeklyHours() != null && request.getWeeklyHours() > 0) {
                BigDecimal newDailyHours = BigDecimal.valueOf(request.getWeeklyHours())
                        .divide(BigDecimal.valueOf(daysCount), 2, RoundingMode.HALF_UP);
                lp.setDailyHoursSetup(newDailyHours);
            }

            if (request.getStudyDays() != null && !request.getStudyDays().isEmpty()) {
                lp.setStudyDays(String.join(",", request.getStudyDays()));
            }

            if (request.getStartDate() != null) {
                lp.setScheduledDate(request.getStartDate());
            }

            if (request.getTargetExamDate() != null) {
                lp.setTargetExamDate(request.getTargetExamDate());
            } else if (request.getJlptPeriod() != null) {
                LocalDate resolved = resolveTargetExamDate(request.getJlptPeriod(), null, null);
                if (resolved != null) {
                    lp.setTargetExamDate(resolved);
                }
            }

            if (request.getScheduleMode() != null) {
                lp.setScheduleMode(request.getScheduleMode());
            }
        }

        learningPathRepository.save(lp);
        log.info(
                "Successfully updated learning path id={} for userId={}: dailyHours={}, studyDays={}, targetExamDate={}",
                lp.getId(), user.getId(), lp.getDailyHoursSetup(), lp.getStudyDays(), lp.getTargetExamDate());
    }

    private Optional<LearningPath> findPublishedLearningPathForUser(Long userId, Long preferredCourseId) {
        if (userId == null) {
            return Optional.empty();
        }

        List<CourseRegistration> registrations = courseRegistrationService.getUserRegistrations(userId);
        List<CourseRegistration> publishedRegs = registrations.stream()
                .filter(r -> r.getCourse() != null && "PUBLISHED".equalsIgnoreCase(r.getCourse().getStatus()))
                .collect(Collectors.toList());

        if (publishedRegs.isEmpty()) {
            return Optional.empty();
        }

        if (preferredCourseId != null) {
            // Must be one of the user's published registrations!
            Optional<CourseRegistration> matchReg = publishedRegs.stream()
                    .filter(r -> r.getCourse().getId().equals(preferredCourseId))
                    .findFirst();

            if (matchReg.isEmpty()) {
                // User is NOT registered for this course
                log.warn("User {} is not registered for courseId={}", userId, preferredCourseId);
                return Optional.empty();
            }

            // Check if existing LearningPath exists for this course
            Optional<LearningPath> existing = learningPathRepository
                    .findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, preferredCourseId);
            if (existing.isPresent()) {
                return existing;
            }

            // Auto-provision LearningPath for this registered course
            Course c = matchReg.get().getCourse();
            String targetLevel = resolveLevelFromCourse(c);
            LocalDate startDate = matchReg.get().getRegisteredAt() != null
                    ? matchReg.get().getRegisteredAt().toLocalDate()
                    : LocalDate.now();
            if (startDate.isBefore(LocalDate.now())) {
                startDate = LocalDate.now();
            }

            LearningPath newLp = LearningPath.builder()
                    .userId(userId)
                    .courseId(c.getId())
                    .dailyHoursSetup(BigDecimal.valueOf(1.5))
                    .targetLevel(targetLevel)
                    .currentLevel("UNKNOWN")
                    .pathStatus("ACTIVE")
                    .scheduledDate(startDate)
                    .build();

            return Optional.of(learningPathRepository.save(newLp));
        }

        // When preferredCourseId is null: Find the best matching registered course
        List<LearningPath> existingPaths = learningPathRepository.findByUserIdOrderByScheduledDateDescIdDesc(userId);
        for (LearningPath path : existingPaths) {
            if (path.getCourseId() != null) {
                boolean isEnrolledPublished = publishedRegs.stream()
                        .anyMatch(r -> r.getCourse().getId().equals(path.getCourseId()));
                if (isEnrolledPublished) {
                    return Optional.of(path);
                }
            }
        }

        // If no existing path matches registered courses, auto-provision for the first
        // registered course
        Course firstCourse = publishedRegs.get(0).getCourse();
        String targetLevel = resolveLevelFromCourse(firstCourse);
        LocalDate startDate = publishedRegs.get(0).getRegisteredAt() != null
                ? publishedRegs.get(0).getRegisteredAt().toLocalDate()
                : LocalDate.now();
        if (startDate.isBefore(LocalDate.now())) {
            startDate = LocalDate.now();
        }

        LearningPath newLp = LearningPath.builder()
                .userId(userId)
                .courseId(firstCourse.getId())
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel(targetLevel)
                .currentLevel("UNKNOWN")
                .pathStatus("ACTIVE")
                .scheduledDate(startDate)
                .build();

        return Optional.of(learningPathRepository.save(newLp));
    }

    // =========================================================================
    // HELPER METHODS FOR FLEXIBLE STUDY DAYS & JLPT TARGET DATE
    // =========================================================================

    private Set<DayOfWeek> parseStudyDays(String studyDaysStr, List<String> requestedList) {
        if (requestedList != null && !requestedList.isEmpty()) {
            Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
            for (String day : requestedList) {
                DayOfWeek dow = parseSingleDayOfWeek(day);
                if (dow != null)
                    result.add(dow);
            }
            if (!result.isEmpty())
                return result;
        }

        if (studyDaysStr != null && !studyDaysStr.isBlank()) {
            Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
            for (String token : studyDaysStr.split(",")) {
                DayOfWeek dow = parseSingleDayOfWeek(token.trim());
                if (dow != null)
                    result.add(dow);
            }
            if (!result.isEmpty())
                return result;
        }

        return EnumSet.allOf(DayOfWeek.class);
    }

    private DayOfWeek parseSingleDayOfWeek(String str) {
        if (str == null || str.isBlank())
            return null;
        String s = str.trim().toUpperCase(Locale.ROOT);
        try {
            return DayOfWeek.valueOf(s);
        } catch (IllegalArgumentException ignored) {
        }
        return switch (s) {
            case "2", "T2", "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "3", "T3", "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "4", "T4", "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "5", "T5", "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "6", "T6", "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "7", "T7", "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "8", "CN", "SUN", "SUNDAY", "1" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private String formatStudyDaysText(Set<DayOfWeek> studyDays) {
        if (studyDays == null || studyDays.size() == 7) {
            return "Tất cả các ngày (7 buổi/tuần)";
        }
        List<String> names = new ArrayList<>();
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (studyDays.contains(dow)) {
                names.add(formatDayOfWeekShort(dow));
            }
        }
        return String.join(", ", names) + " (" + studyDays.size() + " buổi/tuần)";
    }

    private String formatDayOfWeekShort(DayOfWeek dow) {
        if (dow == null)
            return "";
        return switch (dow) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private String formatDateWithDayOfWeek(LocalDate date) {
        if (date == null)
            return "Chưa xác định";
        return formatDayOfWeekShort(date.getDayOfWeek()) + ", " + date.format(DATE_FORMATTER);
    }

    private LocalDate findNextOrSameStudyDate(LocalDate date, Set<DayOfWeek> studyDays) {
        return findNextOrSameStudyDate(date, studyDays, Collections.emptySet());
    }

    private LocalDate findNextOrSameStudyDate(LocalDate date, Set<DayOfWeek> studyDays, Set<LocalDate> leaveDates) {
        if (date == null)
            return LocalDate.now();
        if (studyDays == null || studyDays.isEmpty()) {
            studyDays = EnumSet.allOf(DayOfWeek.class);
        }
        LocalDate d = date;
        for (int i = 0; i < 365; i++) {
            if (studyDays.contains(d.getDayOfWeek()) && (leaveDates == null || !leaveDates.contains(d))) {
                return d;
            }
            d = d.plusDays(1);
        }
        return date;
    }

    private LocalDate findNextStudyDate(LocalDate date, Set<DayOfWeek> studyDays) {
        return findNextStudyDate(date, studyDays, Collections.emptySet());
    }

    private LocalDate findNextStudyDate(LocalDate date, Set<DayOfWeek> studyDays, Set<LocalDate> leaveDates) {
        return findNextOrSameStudyDate(date.plusDays(1), studyDays, leaveDates);
    }

    private LocalDate resolveTargetExamDate(String jlptPeriod, LocalDate customExamDate, LocalDate fallbackDate) {
        if ("DEC_2026".equalsIgnoreCase(jlptPeriod)) {
            return LocalDate.of(2026, 12, 6);
        } else if ("JUL_2027".equalsIgnoreCase(jlptPeriod)) {
            return LocalDate.of(2027, 7, 4);
        } else if ("DEC_2027".equalsIgnoreCase(jlptPeriod)) {
            return LocalDate.of(2027, 12, 5);
        }
        if (customExamDate != null) {
            return customExamDate;
        }
        return fallbackDate;
    }

    private User findUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng: " + username));
    }

    private TodayFocusDto buildTodayFocus(User user, LearningPath lp, Set<DayOfWeek> studyDays,
            List<LearningPathTimelineItemDto> timelineItems,
            int completedCount, int totalCount,
            LeaveRecordDto activeLeave,
            boolean catchUpMode, int catchUpExtraMinutes) {
        LocalDate today = LocalDate.now();
        boolean onLeaveToday = activeLeave != null;
        boolean isStudyDay = studyDays != null && studyDays.contains(today.getDayOfWeek());
        boolean isCourseCompleted = totalCount > 0 && completedCount >= totalCount;

        // Calculate Study Streak (Chuỗi ngày học liên tục)
        Set<LocalDate> activeDates = extractActiveStudyDates(user.getId(), lp != null ? lp.getCourseId() : null);
        Set<LocalDate> allLeaveDates = extractLeaveDates(parseLeaveRecords(lp != null ? lp.getLeaveRecords() : null));

        LocalDate lastCompletionDate = null;
        for (LocalDate d : activeDates) {
            if (lastCompletionDate == null || d.isAfter(lastCompletionDate)) {
                lastCompletionDate = d;
            }
        }
        if (lastCompletionDate == null && lp != null && lp.getBaselineEndDate() != null) {
            lastCompletionDate = lp.getBaselineEndDate();
        }

        List<LearningPathTimelineItemDto> todayItems;
        if (isCourseCompleted && lastCompletionDate != null && lastCompletionDate.isBefore(today)) {
            // Khóa học đã hoàn thành vào ngày trước hôm nay -> hôm nay không có bài học nào
            todayItems = Collections.emptyList();
        } else {
            todayItems = timelineItems != null
                    ? timelineItems.stream()
                            .filter(it -> it.getPlannedDate() != null && it.getPlannedDate().equals(today))
                            .collect(Collectors.toList())
                    : Collections.emptyList();
        }

        boolean hasLessonsToday = !todayItems.isEmpty() && !onLeaveToday && !isCourseCompleted;
        int totalLessonsToday = hasLessonsToday ? todayItems.size() : 0;
        int completedLessonsToday = hasLessonsToday
                ? (int) todayItems.stream().filter(it -> "COMPLETED".equalsIgnoreCase(it.getStatus())).count()
                : 0;
        int remainingLessonsToday = Math.max(0, totalLessonsToday - completedLessonsToday);
        int totalMinutesToday = hasLessonsToday
                ? todayItems.stream()
                        .mapToInt(it -> it.getRecommendedDurationMinutes() != null ? it.getRecommendedDurationMinutes()
                                : (it.getDurationMinutes() != null ? it.getDurationMinutes() : 60))
                        .sum()
                : 0;

        int progressPercentToday = totalLessonsToday > 0
                ? (int) Math.round((completedLessonsToday * 100.0) / totalLessonsToday)
                : (isCourseCompleted ? 100 : (isStudyDay ? 0 : 100));

        boolean allCompletedToday = isCourseCompleted
                || (totalLessonsToday > 0 && completedLessonsToday == totalLessonsToday);

        String nextLessonTitle = null;
        String nextLessonUrl = null;
        String nextLessonBadge = null;
        String nextLessonBadgeClass = null;
        String actionButtonText = "Học bài tiếp theo";
        String statusMessage;

        if (isCourseCompleted) {
            hasLessonsToday = false;
            actionButtonText = "Ôn tập lại bài học";
            nextLessonTitle = (timelineItems != null && !timelineItems.isEmpty()) ? timelineItems.get(0).getTitle()
                    : null;
            nextLessonUrl = (timelineItems != null && !timelineItems.isEmpty()) ? timelineItems.get(0).getActionUrl()
                    : "/learning-path";
            nextLessonBadge = "Hoàn thành 100%";
            nextLessonBadgeClass = "bg-success text-white";

            if (lastCompletionDate != null && lastCompletionDate.isBefore(today)) {
                statusMessage = String.format(
                        "Bạn đã hoàn thành 100%% lộ trình học tập của khóa học (vào ngày %s). Hiện tại không có bài học mới cần làm. Bạn có thể tự do ôn lại bài học hoặc làm bài thi thử JLPT!",
                        lastCompletionDate.format(DATE_FORMATTER));
            } else if (lastCompletionDate != null && lastCompletionDate.equals(today)) {
                statusMessage = "Chúc mừng! Bạn đã hoàn thành xuất sắc toàn bộ khóa học trong ngày hôm nay!";
            } else {
                statusMessage = "Chúc mừng bạn đã hoàn thành 100% lộ trình học tập của khóa học! Bạn có thể tự do ôn lại bài học bất cứ lúc nào.";
            }
        } else if (onLeaveToday) {
            hasLessonsToday = false;
            statusMessage = "Bạn đang trong kỳ báo nghỉ ("
                    + (activeLeave.getReason() != null ? activeLeave.getReason() : "Việc bận cá nhân")
                    + ", đến " + activeLeave.getEndDateFormatted()
                    + "). Các bài học đã tự động dời sang các buổi tiếp theo. Hãy an tâm nghỉ ngơi!";
            actionButtonText = "Học bài tiếp theo";
            LearningPathTimelineItemDto nextUpcoming = timelineItems != null
                    ? timelineItems.stream()
                            .filter(it -> !"COMPLETED".equalsIgnoreCase(it.getStatus()))
                            .findFirst()
                            .orElse(null)
                    : null;
            if (nextUpcoming != null) {
                nextLessonTitle = nextUpcoming.getTitle();
                nextLessonUrl = nextUpcoming.getActionUrl();
                nextLessonBadge = nextUpcoming.getFocusBadgeText();
                nextLessonBadgeClass = nextUpcoming.getFocusBadgeClass();
            }
        } else if (hasLessonsToday) {
            LearningPathTimelineItemDto pendingToday = todayItems.stream()
                    .filter(it -> !"COMPLETED".equalsIgnoreCase(it.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (pendingToday != null) {
                nextLessonTitle = pendingToday.getTitle();
                nextLessonUrl = pendingToday.getActionUrl();
                nextLessonBadge = pendingToday.getFocusBadgeText();
                nextLessonBadgeClass = pendingToday.getFocusBadgeClass();
                actionButtonText = pendingToday.getActionText() != null ? pendingToday.getActionText()
                        : "Bắt đầu học ngay";
                if (completedLessonsToday > 0) {
                    statusMessage = String.format(
                            "Bạn đã hoàn thành %d/%d bài học hôm nay. Tiếp tục học để hoàn tất mục tiêu ngày!",
                            completedLessonsToday, totalLessonsToday);
                } else {
                    statusMessage = String.format(
                            "Hôm nay bạn có %d bài học cần hoàn thành (khoảng %d phút). Hãy bắt đầu ngay nhé!",
                            totalLessonsToday, totalMinutesToday);
                }
            } else {
                nextLessonTitle = todayItems.get(todayItems.size() - 1).getTitle();
                nextLessonUrl = todayItems.get(todayItems.size() - 1).getActionUrl();
                actionButtonText = "Ôn tập lại";
                statusMessage = "Xuất sắc! Bạn đã hoàn thành toàn bộ mục tiêu học tập của ngày hôm nay!";
            }
        } else {
            LearningPathTimelineItemDto nextUpcoming = timelineItems != null
                    ? timelineItems.stream()
                            .filter(it -> !"COMPLETED".equalsIgnoreCase(it.getStatus()))
                            .findFirst()
                            .orElse(null)
                    : null;

            if (nextUpcoming != null) {
                nextLessonTitle = nextUpcoming.getTitle();
                nextLessonUrl = nextUpcoming.getActionUrl();
                nextLessonBadge = nextUpcoming.getFocusBadgeText();
                nextLessonBadgeClass = nextUpcoming.getFocusBadgeClass();
                actionButtonText = "Học trước bài tiếp theo";
                if (isStudyDay) {
                    statusMessage = "Bạn đã hoàn thành các bài học trước đó. Bạn có thể học trước bài của buổi tiếp theo ("
                            + nextUpcoming.getDateWithDayOfWeek() + ")!";
                } else {
                    statusMessage = "Hôm nay là ngày nghỉ theo lịch của bạn. Bạn có thể nghỉ ngơi hoặc học trước bài của buổi kế tiếp ("
                            + nextUpcoming.getDateWithDayOfWeek() + ")!";
                }
            } else {
                actionButtonText = "Xem lại lộ trình";
                nextLessonUrl = "/learning-path";
                statusMessage = "Chúc mừng bạn đã hoàn thành tất cả các bài học trong lộ trình học tập!";
            }
        }

        boolean streakActiveToday = activeDates.contains(today);

        // Chuỗi ngày học tính toán theo quy tắc:
        // 1. Nếu hôm nay đã học -> tính từ hôm nay.
        // 2. Nếu hôm nay chưa học:
        //    - Đang nghỉ phép: bảo lưu chuỗi, kiểm tra từ hôm qua.
        //    - Khóa học đã hoàn thành: kiểm tra từ ngày hoàn thành cuối cùng.
        //    - Ngày nghỉ theo lịch học cá nhân (ví dụ T7, CN của lịch 2-4-6): kiểm tra từ hôm qua.
        //    - Ngày học theo lịch nhưng chưa học: kiểm tra từ hôm qua để giữ chuỗi chờ học trong ngày.
        LocalDate checkDate = streakActiveToday ? today
                : (isCourseCompleted && lastCompletionDate != null ? lastCompletionDate : today.minusDays(1));

        int streakDays = 0;
        int maxLookbackDays = 1000;
        while (checkDate != null && maxLookbackDays-- > 0) {
            if (activeDates.contains(checkDate)) {
                streakDays++;
                checkDate = checkDate.minusDays(1);
            } else if (allLeaveDates.contains(checkDate)) {
                // Ngày nghỉ phép được phê duyệt -> bắc cầu bảo lưu chuỗi, không ngắt
                checkDate = checkDate.minusDays(1);
            } else if (studyDays != null && !studyDays.isEmpty() && !studyDays.contains(checkDate.getDayOfWeek())) {
                // Ngày nghỉ theo lịch học cá nhân -> bắc cầu ngày nghỉ theo kế hoạch
                checkDate = checkDate.minusDays(1);
            } else {
                // Rơi vào ngày học theo lịch mà không học và không có đơn báo nghỉ -> đứt chuỗi
                break;
            }
        }

        String streakMessage;
        String streakBadgeClass;
        if (isCourseCompleted) {
            if (streakActiveToday) {
                streakMessage = "Đã học hôm nay! Bạn đã hoàn thành toàn bộ lộ trình khóa học 🏆";
                streakBadgeClass = "text-success";
            } else {
                streakMessage = "Khóa học đã hoàn thành xuất sắc! Chuỗi (" + streakDays + " ngày) được lưu danh 🏆";
                streakBadgeClass = "text-success";
            }
        } else if (onLeaveToday) {
            streakMessage = "Đang nghỉ phép! Chuỗi (" + streakDays + " ngày) được bảo lưu an toàn 🛡️";
            streakBadgeClass = "text-primary";
        } else if (streakActiveToday) {
            streakMessage = "Đã học hôm nay! Chuỗi ngày học an toàn";
            streakBadgeClass = "text-success";
        } else if (!isStudyDay && streakDays > 0) {
            streakMessage = "Hôm nay là ngày nghỉ theo lịch! Chuỗi (" + streakDays + " ngày) an toàn 🌿";
            streakBadgeClass = "text-primary";
        } else if (streakDays > 0) {
            streakMessage = "Học bài hôm nay để duy trì chuỗi 🔥";
            streakBadgeClass = "text-danger";
        } else {
            streakMessage = "Học bài hôm nay để kích hoạt chuỗi 🔥";
            streakBadgeClass = "text-warning-emphasis";
        }

        return TodayFocusDto.builder()
                .todayFormatted(formatDateWithDayOfWeek(today))
                .studyDayToday(isStudyDay && !onLeaveToday && !isCourseCompleted)
                .courseCompleted(isCourseCompleted)
                .courseCompletedDateFormatted(
                        lastCompletionDate != null ? lastCompletionDate.format(DATE_FORMATTER) : null)
                .hasLessonsToday(hasLessonsToday)
                .totalLessonsToday(totalLessonsToday)
                .completedLessonsToday(completedLessonsToday)
                .remainingLessonsToday(remainingLessonsToday)
                .totalMinutesToday(totalMinutesToday)
                .progressPercent(progressPercentToday)
                .allCompletedToday(allCompletedToday)
                .nextLessonTitle(nextLessonTitle)
                .nextLessonUrl(nextLessonUrl)
                .nextLessonBadge(nextLessonBadge)
                .nextLessonBadgeClass(nextLessonBadgeClass)
                .actionButtonText(actionButtonText)
                .statusMessage(statusMessage)
                .streakDays(streakDays)
                .streakActiveToday(streakActiveToday || onLeaveToday || (!isStudyDay && streakDays > 0) || isCourseCompleted)
                .streakMessage(streakMessage)
                .streakBadgeClass(streakBadgeClass)
                .onLeaveToday(onLeaveToday)
                .leaveReason(activeLeave != null ? activeLeave.getReason() : null)
                .leaveEndDateFormatted(activeLeave != null ? activeLeave.getEndDateFormatted() : null)
                .streakFrozen(onLeaveToday)
                .catchUpActive(catchUpMode && !isCourseCompleted)
                .catchUpExtraMinutes(catchUpExtraMinutes)
                .todayLessons(hasLessonsToday ? todayItems : Collections.emptyList())
                .build();
    }

    private Set<LocalDate> extractActiveStudyDates(Long userId, Long courseId) {
        Set<LocalDate> dates = new HashSet<>();
        if (userId == null) {
            return dates;
        }

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");

        // 1. Gather active study dates across all registered learning paths of this user
        try {
            List<LearningPath> userPaths = learningPathRepository.findByUserIdOrderByScheduledDateDescIdDesc(userId);
            for (LearningPath p : userPaths) {
                if (p.getCourseId() != null) {
                    LearningPathProgressDto progressDto = lessonProgressService.getLessonProgress(userId,
                            p.getCourseId());
                    if (progressDto != null && progressDto.getLessons() != null) {
                        for (LessonProgressItemDto item : progressDto.getLessons().values()) {
                            if ("COMPLETED".equalsIgnoreCase(item.getStatus()) && item.getCompletedAt() != null) {
                                try {
                                    LocalDate d = Instant.parse(item.getCompletedAt())
                                            .atZone(vnZone)
                                            .toLocalDate();
                                    dates.add(d);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Gather active exam / test completion dates (support both dd/MM/yyyy and dd/MM/yyyy HH:mm)
        if (examService != null) {
            try {
                List<ExamHistoryDto> history = examService.getLearnerExamHistory(userId);
                if (history != null) {
                    for (ExamHistoryDto h : history) {
                        if (h.getFormattedDate() != null && !h.getFormattedDate().isBlank() && !"N/A".equalsIgnoreCase(h.getFormattedDate())) {
                            try {
                                String raw = h.getFormattedDate().trim();
                                if (raw.length() >= 10) {
                                    LocalDate d = LocalDate.parse(raw.substring(0, 10), DATE_FORMATTER);
                                    dates.add(d);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Fallback for legacy completed lessons without JSON timestamp
        if (dates.isEmpty() && courseId != null) {
            try {
                Optional<CourseRegistration> regOpt = courseRegistrationRepository.findByUserIdAndCourseId(userId, courseId);
                if (regOpt.isPresent() && regOpt.get().getProgressPercent() != null && regOpt.get().getProgressPercent() > 0) {
                    LocalDate baseDate = regOpt.get().getUpdatedAt() != null ? regOpt.get().getUpdatedAt().toLocalDate()
                            : (regOpt.get().getRegisteredAt() != null ? regOpt.get().getRegisteredAt().toLocalDate() : null);
                    if (baseDate != null) {
                        dates.add(baseDate);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return dates;
    }

    @Override
    @Transactional
    public void submitLeaveOfAbsence(String username, SubmitLeaveRequest request) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc nghỉ không được để trống.");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu nghỉ không được sau ngày kết thúc nghỉ.");
        }
        if (request.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày kết thúc nghỉ không được ở trong quá khứ.");
        }

        User user = findUserByUsername(username);
        Optional<LearningPath> lpOpt = findPublishedLearningPathForUser(user.getId(), request.getCourseId());
        if (lpOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy lộ trình học tập để ghi nhận báo nghỉ.");
        }

        LearningPath lp = lpOpt.get();
        List<LeaveRecordDto> records = parseLeaveRecords(lp.getLeaveRecords());

        String leaveId = UUID.randomUUID().toString();
        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        LocalDate today = LocalDate.now();

        LeaveRecordDto newRecord = LeaveRecordDto.builder()
                .id(leaveId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startDateFormatted(request.getStartDate().format(DATE_FORMATTER))
                .endDateFormatted(request.getEndDate().format(DATE_FORMATTER))
                .reason(request.getReason() != null && !request.getReason().isBlank() ? request.getReason().trim()
                        : null)
                .totalDays(totalDays)
                .activeNow(!today.isBefore(request.getStartDate()) && !today.isAfter(request.getEndDate()))
                .upcoming(request.getStartDate().isAfter(today))
                .build();

        records.add(newRecord);
        records.sort((a, b) -> b.getStartDate().compareTo(a.getStartDate()));

        lp.setLeaveRecords(serializeLeaveRecords(records));
        learningPathRepository.save(lp);
        log.info("Learner '{}' registered leave of absence from {} to {} for courseId={}",
                username, request.getStartDate(), request.getEndDate(), lp.getCourseId());
    }

    @Override
    @Transactional
    public void cancelLeaveOfAbsence(String username, Long courseId, String leaveId) {
        if (leaveId == null || leaveId.isBlank()) {
            throw new IllegalArgumentException("Mã đợt nghỉ không hợp lệ.");
        }

        User user = findUserByUsername(username);
        Optional<LearningPath> lpOpt = findPublishedLearningPathForUser(user.getId(), courseId);
        if (lpOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy lộ trình học tập.");
        }

        LearningPath lp = lpOpt.get();
        List<LeaveRecordDto> records = parseLeaveRecords(lp.getLeaveRecords());
        records.removeIf(r -> leaveId.equals(r.getId()));

        lp.setLeaveRecords(serializeLeaveRecords(records));
        learningPathRepository.save(lp);
        log.info("Learner '{}' cancelled leave of absence id={} for courseId={}", username, leaveId, lp.getCourseId());
    }

    @Override
    @Transactional
    public void toggleCatchUpMode(String username, ToggleCatchUpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu điều chỉnh chế độ bù bài không hợp lệ.");
        }

        User user = findUserByUsername(username);
        Optional<LearningPath> lpOpt = findPublishedLearningPathForUser(user.getId(), request.getCourseId());
        if (lpOpt.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy lộ trình học tập.");
        }

        LearningPath lp = lpOpt.get();
        boolean enable = Boolean.TRUE.equals(request.getEnable());
        lp.setCatchUpMode(enable);
        if (enable) {
            int extra = (request.getExtraMinutes() != null && request.getExtraMinutes() > 0) ? request.getExtraMinutes()
                    : 30;
            lp.setCatchUpExtraMinutes(extra);
            lp.setCatchUpIncludeWeekends(Boolean.TRUE.equals(request.getIncludeWeekends()));
        }

        learningPathRepository.save(lp);
        log.info("Learner '{}' set catch-up mode to {} (extraMinutes={}, weekends={}) for courseId={}",
                username, enable, lp.getCatchUpExtraMinutes(), lp.getCatchUpIncludeWeekends(), lp.getCourseId());
    }

    private List<LeaveRecordDto> parseLeaveRecords(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            LocalDate today = LocalDate.now();
            List<LeaveRecordDto> list = objectMapper.readValue(json, new TypeReference<List<LeaveRecordDto>>() {
            });
            for (LeaveRecordDto r : list) {
                if (r.getStartDate() != null) {
                    r.setStartDateFormatted(r.getStartDate().format(DATE_FORMATTER));
                }
                if (r.getEndDate() != null) {
                    r.setEndDateFormatted(r.getEndDate().format(DATE_FORMATTER));
                }
                if (r.getStartDate() != null && r.getEndDate() != null) {
                    r.setTotalDays((int) ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()) + 1);
                    r.setActiveNow(!today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate()));
                    r.setUpcoming(r.getStartDate().isAfter(today));
                }
            }
            return list;
        } catch (Exception ex) {
            log.warn("Failed to parse leave_records JSON: {}", json, ex);
            return new ArrayList<>();
        }
    }

    private String serializeLeaveRecords(List<LeaveRecordDto> records) {
        if (records == null || records.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(records);
        } catch (Exception ex) {
            log.error("Failed to serialize leave_records: {}", records, ex);
            return "[]";
        }
    }

    private Set<LocalDate> extractLeaveDates(List<LeaveRecordDto> records) {
        Set<LocalDate> dates = new HashSet<>();
        if (records == null)
            return dates;
        for (LeaveRecordDto r : records) {
            if (r.getStartDate() != null && r.getEndDate() != null && !r.getStartDate().isAfter(r.getEndDate())) {
                LocalDate cur = r.getStartDate();
                while (!cur.isAfter(r.getEndDate())) {
                    dates.add(cur);
                    cur = cur.plusDays(1);
                }
            }
        }
        return dates;
    }
    private String resolveLevelFromCourse(Course c) {
        if (c == null) {
            return "Chưa xác định";
        }
        if (c.getLevel() != null && !c.getLevel().isBlank()) {
            return c.getLevel().trim().toUpperCase();
        }
        if (c.getTitle() != null) {
            String titleUpper = c.getTitle().toUpperCase();
            for (String lvl : List.of("N1", "N2", "N3", "N4", "N5")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![A-Z0-9])" + lvl + "(?![A-Z0-9])").matcher(titleUpper);
                if (m.find()) {
                    return lvl;
                }
            }
        }
        return "Chưa xác định";
    }
}
