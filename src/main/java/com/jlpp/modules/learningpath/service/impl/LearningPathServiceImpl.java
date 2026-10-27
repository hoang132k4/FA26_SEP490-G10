package com.jlpp.modules.learningpath.service.impl;

import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.learningpath.dto.LearningPathResponse;
import com.jlpp.modules.learningpath.dto.LearningPathTimelineItemDto;
import com.jlpp.modules.learningpath.dto.RecalibratePreviewResponse;
import com.jlpp.modules.learningpath.dto.RecalibrateRequest;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LearningPathService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public LearningPathResponse getLearningPath(String username) {
        log.info("Fetching learning path for learner={}", username);
        User user = findUserByUsername(username);

        Optional<LearningPath> lpOpt = learningPathRepository
                .findFirstByUserIdOrderByScheduledDateDescIdDesc(user.getId());

        if (lpOpt.isEmpty()) {
            log.info("No learning path found in database for userId={}", user.getId());
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

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lp.getCourseId());
        Optional<CourseRegistration> regOpt = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), lp.getCourseId());

        int progressPercent = regOpt.map(CourseRegistration::getProgressPercent).orElse(0);
        int totalLessons = lessons.size();
        int completedLessons = totalLessons > 0
                ? (int) Math.round((progressPercent / 100.0) * totalLessons)
                : 0;
        int remainingLessons = Math.max(0, totalLessons - completedLessons);

        BigDecimal dailyHours = lp.getDailyHoursSetup() != null
                ? lp.getDailyHoursSetup()
                : BigDecimal.valueOf(1.5);
        int weeklyHours = Math.max(1, dailyHours.multiply(BigDecimal.valueOf(7)).intValue());

        List<LearningPathTimelineItemDto> timelineItems = buildTimelineItems(
                lessons, completedLessons, lp.getScheduledDate(), dailyHours);

        String projectedEndDate = timelineItems.isEmpty()
                ? "Chưa xác định"
                : timelineItems.get(timelineItems.size() - 1).getPlannedDateFormatted();

        int totalHours = course != null && course.getDurationHours() != null
                ? course.getDurationHours()
                : (int) Math.ceil(lessons.stream().mapToInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 30).sum() / 60.0);

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
                .startDateFormatted(lp.getScheduledDate() != null ? lp.getScheduledDate().format(DATE_FORMATTER) : "N/A")
                .projectedEndDateFormatted(projectedEndDate)
                .progressPercent(progressPercent)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .remainingLessons(remainingLessons)
                .totalHours(totalHours)
                .pathStatus(lp.getPathStatus())
                .statusBadgeClass("ACTIVE".equalsIgnoreCase(lp.getPathStatus()) ? "bg-success" : "bg-secondary")
                .items(timelineItems)
                .build();
    }

    @Override
    public RecalibratePreviewResponse getRecalibratePreview(String username, RecalibrateRequest request) {
        log.info("Calculating recalibrate preview for learner={} with weeklyHours={}", username, request.getWeeklyHours());
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
        int completedLessons = totalLessons > 0
                ? (int) Math.round((progressPercent / 100.0) * totalLessons)
                : 0;

        BigDecimal originalDailyHours = lp.getDailyHoursSetup() != null
                ? lp.getDailyHoursSetup()
                : BigDecimal.valueOf(1.5);
        int originalWeeklyHours = Math.max(1, originalDailyHours.multiply(BigDecimal.valueOf(7)).intValue());
        int newWeeklyHours = (request != null && request.getWeeklyHours() != null && request.getWeeklyHours() > 0)
                ? request.getWeeklyHours()
                : originalWeeklyHours;

        List<LearningPathTimelineItemDto> originalItems = buildTimelineItems(
                lessons, completedLessons, lp.getScheduledDate(), originalDailyHours);

        LocalDate originalEndDate = originalItems.isEmpty()
                ? LocalDate.now().plusMonths(2)
                : originalItems.get(originalItems.size() - 1).getPlannedDate();

        LocalDate simStartDate = (request != null && request.getStartDate() != null)
                ? request.getStartDate()
                : LocalDate.now();

        List<LearningPathTimelineItemDto> rescheduledItems = rescheduleItems(
                originalItems, completedLessons, simStartDate, newWeeklyHours);

        LocalDate newEndDate = rescheduledItems.isEmpty()
                ? simStartDate
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
        for (int i = completedLessons; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            remainingMinutes += (l.getDurationMinutes() != null ? l.getDurationMinutes() : 30);
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
            List<Lesson> lessons, int completedCount, LocalDate startDate, BigDecimal dailyHours) {

        if (lessons == null || lessons.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate currentDate = startDate != null ? startDate : LocalDate.now();
        List<LearningPathTimelineItemDto> items = new ArrayList<>();
        int dailyMinutesLimit = dailyHours.multiply(BigDecimal.valueOf(60)).intValue();
        if (dailyMinutesLimit <= 0) dailyMinutesLimit = 90;

        int accumulatedMinutesToday = 0;
        int targetWeek = 1;
        LocalDate weekStart = currentDate;

        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            int duration = lesson.getDurationMinutes() != null ? lesson.getDurationMinutes() : 30;

            accumulatedMinutesToday += duration;
            if (accumulatedMinutesToday > dailyMinutesLimit) {
                currentDate = currentDate.plusDays(1);
                accumulatedMinutesToday = duration;
            }

            long daysFromStart = ChronoUnit.DAYS.between(weekStart, currentDate);
            targetWeek = (int) (daysFromStart / 7) + 1;

            String status;
            String statusDisplay;
            String badgeClass;
            String actionUrl = "/courses";
            String actionText = "Xem bài học";

            if (i < completedCount) {
                status = "COMPLETED";
                statusDisplay = "Đã hoàn thành";
                badgeClass = "bg-success";
                actionText = "Ôn lại";
            } else if (i == completedCount) {
                status = "IN_PROGRESS";
                statusDisplay = "Đang học";
                badgeClass = "bg-primary";
                actionText = "Học tiếp";
            } else {
                status = "UPCOMING";
                statusDisplay = "Chưa học";
                badgeClass = "bg-secondary";
                actionText = "Bắt đầu học";
            }

            String sectionName = lesson.getSectionName() != null
                    ? lesson.getSectionName()
                    : "Chương " + ((i / 5) + 1);

            items.add(LearningPathTimelineItemDto.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .sectionName(sectionName)
                    .contentType(lesson.getLessonType() != null ? lesson.getLessonType() : "LESSON")
                    .durationMinutes(duration)
                    .targetWeek(targetWeek)
                    .plannedDate(currentDate)
                    .plannedDateFormatted(currentDate.format(DATE_FORMATTER))
                    .status(status)
                    .statusDisplay(statusDisplay)
                    .statusBadgeClass(badgeClass)
                    .actionUrl(actionUrl)
                    .actionText(actionText)
                    .orderIndex(i + 1)
                    .build());
        }

        return items;
    }

    private List<LearningPathTimelineItemDto> rescheduleItems(
            List<LearningPathTimelineItemDto> originalItems, int completedCount,
            LocalDate simStartDate, int newWeeklyHours) {

        List<LearningPathTimelineItemDto> result = new ArrayList<>();
        LocalDate runningDate = simStartDate;
        int dailyMinutesBudget = Math.max(30, (newWeeklyHours * 60) / 7);
        int accumulatedMinutesToday = 0;
        int currentWeek = 1;
        LocalDate weekStart = runningDate;

        for (int i = 0; i < originalItems.size(); i++) {
            LearningPathTimelineItemDto item = originalItems.get(i);

            if (i < completedCount) {
                // Completed items keep their original historical status and date
                result.add(item);
            } else {
                int duration = item.getDurationMinutes() != null ? item.getDurationMinutes() : 30;
                accumulatedMinutesToday += duration;
                if (accumulatedMinutesToday > dailyMinutesBudget) {
                    runningDate = runningDate.plusDays(1);
                    accumulatedMinutesToday = duration;
                }

                long daysFromStart = ChronoUnit.DAYS.between(weekStart, runningDate);
                currentWeek = (int) (daysFromStart / 7) + 1;

                result.add(LearningPathTimelineItemDto.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .sectionName(item.getSectionName())
                        .contentType(item.getContentType())
                        .durationMinutes(duration)
                        .targetWeek(currentWeek)
                        .plannedDate(runningDate)
                        .plannedDateFormatted(runningDate.format(DATE_FORMATTER))
                        .status(item.getStatus())
                        .statusDisplay(item.getStatusDisplay())
                        .statusBadgeClass(item.getStatusBadgeClass())
                        .actionUrl(item.getActionUrl())
                        .actionText(item.getActionText())
                        .orderIndex(item.getOrderIndex())
                        .build());
            }
        }

        return result;
    }

    private String buildPaceAnalysis(int weeklyHours, int weeksNeeded, int remainingHours) {
        StringBuilder sb = new StringBuilder();
        if (weeklyHours >= 15) {
            sb.append("Cường độ Tăng tốc (Intensive): Với ").append(weeklyHours)
                    .append(" giờ/tuần, bạn sẽ sớm hoàn thành khối lượng bài học còn lại (khoảng ")
                    .append(remainingHours).append(" giờ học) trong ")
                    .append(weeksNeeded).append(" tuần tới. Phù hợp cho mục tiêu thi JLPT gấp.");
        } else if (weeklyHours >= 8) {
            sb.append("Cường độ Tiêu chuẩn (Standard): Phân bổ ").append(weeklyHours)
                    .append(" giờ/tuần là nhịp độ cân bằng và bền vững nhất để tiếp thu sâu kiến thức ngữ pháp và luyện nghe đều đặn trong ")
                    .append(weeksNeeded).append(" tuần.");
        } else {
            sb.append("Cường độ Nhẹ nhàng (Light): Với ").append(weeklyHours)
                    .append(" giờ/tuần, lộ trình sẽ kéo dài khoảng ").append(weeksNeeded)
                    .append(" tuần để bạn học tập thong thả, không bị áp lực thời gian.");
        }
        return sb.toString();
    }

    private User findUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng: " + username));
    }
}
