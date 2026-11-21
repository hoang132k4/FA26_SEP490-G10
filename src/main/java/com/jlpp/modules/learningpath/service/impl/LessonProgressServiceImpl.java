package com.jlpp.modules.learningpath.service.impl;

import com.jlpp.modules.learningpath.dto.LearningPathDtos.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LessonProgressService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressServiceImpl implements LessonProgressService {

    private final LearningPathRepository learningPathRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final LessonRepository lessonRepository;
    private final CourseRegistrationService courseRegistrationService;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public LearningPathProgressDto getLessonProgress(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return new LearningPathProgressDto();
        }
        if (!courseRegistrationService.isUserEnrolled(userId, courseId)) {
            return new LearningPathProgressDto();
        }

        Optional<LearningPath> lpOpt = learningPathRepository
                .findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId)
                .or(() -> learningPathRepository.findFirstByUserIdOrderByScheduledDateDescIdDesc(userId));

        LearningPathProgressDto dto = new LearningPathProgressDto();
        if (lpOpt.isPresent() && courseId.equals(lpOpt.get().getCourseId())) {
            String json = lpOpt.get().getLessonProgress();
            log.info("getLessonProgress raw json from lpId={}: {}", lpOpt.get().getId(), json);
            dto = parseProgressDto(json);
        }

        // Populate legacy inferred count snapshot if not yet set and registration exists
        Optional<CourseRegistration> regOpt = courseRegistrationRepository.findByUserIdAndCourseId(userId, courseId);
        if (regOpt.isPresent()) {
            int currentPercent = regOpt.get().getProgressPercent() != null ? regOpt.get().getProgressPercent() : 0;
            int totalLessons = lessonRepository.countByCourseId(courseId);
            int inferredFromPercent = (totalLessons > 0 && currentPercent > 0)
                    ? (int) Math.round((currentPercent / 100.0) * totalLessons)
                    : 0;

            if (dto.getLegacyInferredCount() == null) {
                int legacyCount = Math.max(0, inferredFromPercent - dto.getLessons().size());
                dto.setLegacyInferredCount(legacyCount);
            } else if (dto.getLegacyInferredCount() < inferredFromPercent && dto.getLessons().isEmpty()) {
                dto.setLegacyInferredCount(inferredFromPercent);
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public boolean markLessonCompleted(Long userId, Long courseId, Long lessonId) {
        if (userId == null || courseId == null || lessonId == null) {
            log.warn("Invalid parameters to markLessonCompleted: userId={}, courseId={}, lessonId={}", userId, courseId, lessonId);
            return false;
        }

        // 1. Enrollment validation with Pessimistic Row Lock on CourseRegistration
        Optional<CourseRegistration> regOpt = courseRegistrationRepository.findForUpdateByUserIdAndCourseId(userId, courseId)
                .or(() -> courseRegistrationRepository.findByUserIdAndCourseId(userId, courseId));
        CourseRegistration reg;
        if (regOpt.isEmpty()) {
            Optional<CourseRegistration> freeRegistration =
                    courseRegistrationService.ensureFreeRegistrationForAccess(userId, courseId);
            if (freeRegistration.isEmpty()) {
                log.warn("Enrollment validation failed: userId={} is not enrolled in courseId={}", userId, courseId);
                return false;
            }
            reg = freeRegistration.get();
        } else {
            reg = regOpt.get();
        }
        if (!isAccessEligible(reg.getStatus())) {
            log.warn("Lesson completion denied for ineligible registration: userId={}, courseId={}, status={}",
                    userId, courseId, reg.getStatus());
            return false;
        }

        // 2. Course-to-Lesson ownership validation
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || lesson.getCourse() == null || !lesson.getCourse().getId().equals(courseId)) {
            log.warn("Course-to-lesson validation failed: lessonId={} does not belong to courseId={}", lessonId, courseId);
            return false;
        }

        // 3. Pessimistic Row Lock on LearningPath
        List<LearningPath> lps = learningPathRepository.findForUpdateByUserIdAndCourseId(userId, courseId);
        LearningPath lp = lps.isEmpty() ? null : lps.get(0);
        if (lp == null) {
            Optional<LearningPath> fallbackLp = learningPathRepository
                    .findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId)
                    .or(() -> learningPathRepository.findFirstByUserIdOrderByScheduledDateDescIdDesc(userId));
            lp = fallbackLp.filter(p -> courseId.equals(p.getCourseId())).orElse(null);
        }

        // Auto-provision initial LearningPath if none exists, while holding the CourseRegistration lock
        if (lp == null) {
            Course enrolledCourse = (reg.getCourse() != null) ? reg.getCourse() :
                    (courseRepository != null ? courseRepository.findById(courseId).orElse(null) : null);
            String targetLevel = (enrolledCourse != null && enrolledCourse.getLevel() != null)
                    ? enrolledCourse.getLevel()
                    : "N4";
            LocalDate startDate = reg.getRegisteredAt() != null
                    ? reg.getRegisteredAt().toLocalDate()
                    : LocalDate.now();

            lp = LearningPath.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .dailyHoursSetup(BigDecimal.valueOf(1.5))
                    .targetLevel(targetLevel)
                    .currentLevel("UNKNOWN")
                    .pathStatus("ACTIVE")
                    .scheduledDate(startDate)
                    .build();
            lp = learningPathRepository.saveAndFlush(lp);
            log.info("Auto-provisioned initial LearningPath id={} in markLessonCompleted for userId={}, courseId={}",
                    lp.getId(), userId, courseId);
        }

        // 4. Parse fresh progress JSON while holding the pessimistic locks
        LearningPathProgressDto progressDto = parseProgressDto(lp != null ? lp.getLessonProgress() : null);

        // 5. Freeze legacy progress count if not yet frozen
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        int totalLessons = courseLessons.size();

        int currentPercent = reg.getProgressPercent() != null ? reg.getProgressPercent() : 0;
        int inferredFromPercent = (totalLessons > 0 && currentPercent > 0)
                ? (int) Math.round((currentPercent / 100.0) * totalLessons)
                : 0;

        if (progressDto.getLegacyInferredCount() == null) {
            int legacyCount = Math.max(0, inferredFromPercent - progressDto.getLessons().size());
            progressDto.setLegacyInferredCount(legacyCount);
        } else if (progressDto.getLegacyInferredCount() < inferredFromPercent && progressDto.getLessons().isEmpty()) {
            progressDto.setLegacyInferredCount(inferredFromPercent);
        }

        // 6. Record verified completion
        String lessonKey = String.valueOf(lessonId);
        LessonProgressItemDto item = progressDto.getLessons().get(lessonKey);
        String nowIso = Instant.now().toString();

        if (item == null) {
            item = LessonProgressItemDto.builder()
                    .status("COMPLETED")
                    .startedAt(nowIso)
                    .completedAt(nowIso)
                    .build();
            progressDto.getLessons().put(lessonKey, item);
        } else {
            // Already existed - preserve original completedAt if present
            if (!"COMPLETED".equalsIgnoreCase(item.getStatus())) {
                item.setStatus("COMPLETED");
                if (item.getCompletedAt() == null) {
                    item.setCompletedAt(nowIso);
                }
            }
        }

        // 7. Save back to learning_paths
        if (lp != null) {
            try {
                String serialized = objectMapper.writeValueAsString(progressDto);
                lp.setLessonProgress(serialized);
                learningPathRepository.saveAndFlush(lp);
                log.info("Saved lesson_progress JSON for lpId={}, completedLessonId={}", lp.getId(), lessonId);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize LearningPathProgressDto: {}", e.getMessage());
            }
        }

        // 8. Authoritative calculation of CourseRegistration progress_percent (union of verified + frozen legacy)
        if (totalLessons > 0) {
            int legacyCount = progressDto.getLegacyInferredCount() != null ? progressDto.getLegacyInferredCount() : 0;
            Set<Long> legacyInferredLessonIds = new HashSet<>();
            if (legacyCount > 0) {
                int count = Math.min(legacyCount, courseLessons.size());
                for (int i = 0; i < count; i++) {
                    legacyInferredLessonIds.add(courseLessons.get(i).getId());
                }
            }

            long completedCount = courseLessons.stream()
                    .filter(l -> progressDto.isLessonCompleted(l.getId()) || legacyInferredLessonIds.contains(l.getId()))
                    .count();

            int calculatedPercent = Math.min(100, (int) Math.round((completedCount * 100.0) / totalLessons));
            int newPercent = Math.max(currentPercent, calculatedPercent);
            reg.setProgressPercent(newPercent);
            if (newPercent >= 100) {
                reg.setStatus("COMPLETED");
            }
            courseRegistrationRepository.saveAndFlush(reg);
            log.info("Authoritative sync courseRegistration progressPercent={} (completedCount={}/{}) for userId={}, courseId={}",
                    newPercent, completedCount, totalLessons, userId, courseId);
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getCompletedLessonIds(Long userId, Long courseId, List<Lesson> courseLessons, int progressPercent) {
        Set<Long> completedIds = new HashSet<>();
        if (userId == null || courseId == null || courseLessons == null || courseLessons.isEmpty()) {
            return completedIds;
        }
        if (!courseRegistrationService.isUserEnrolled(userId, courseId)) {
            return completedIds;
        }

        LearningPathProgressDto progressDto = getLessonProgress(userId, courseId);
        int totalLessons = courseLessons.size();

        // 1. Direct verified completions from JSONB
        for (Lesson l : courseLessons) {
            if (l != null && l.getId() != null && progressDto.isLessonCompleted(l.getId())) {
                completedIds.add(l.getId());
            }
        }

        // 2. Legacy inferred completions
        int legacyCount = progressDto.getLegacyInferredCount() != null
                ? progressDto.getLegacyInferredCount()
                : (progressDto.getLessons().isEmpty() && progressPercent > 0 && totalLessons > 0
                    ? (int) Math.round((progressPercent / 100.0) * totalLessons)
                    : 0);

        if (legacyCount > 0) {
            int count = Math.min(legacyCount, courseLessons.size());
            for (int i = 0; i < count; i++) {
                Lesson l = courseLessons.get(i);
                if (l != null && l.getId() != null) {
                    completedIds.add(l.getId());
                }
            }
        }

        return completedIds;
    }

    @Override
    public boolean isLessonCompleted(
            Long userId, Long courseId, Long lessonId,
            int lessonOrderIndex, int totalCourseLessons, int registrationProgressPercent) {

        if (userId == null || courseId == null || lessonId == null
                || !courseRegistrationService.isUserEnrolled(userId, courseId)) {
            return false;
        }
        LearningPathProgressDto progressDto = getLessonProgress(userId, courseId);
        if (progressDto.isLessonCompleted(lessonId)) {
            return true;
        }

        // Legacy compatibility fallback:
        int legacyCount = (progressDto.getLegacyInferredCount() != null && progressDto.getLegacyInferredCount() > 0)
                ? progressDto.getLegacyInferredCount()
                : (progressDto.getLessons().isEmpty() && registrationProgressPercent > 0 && totalCourseLessons > 0
                    ? (int) Math.round((registrationProgressPercent / 100.0) * totalCourseLessons)
                    : (progressDto.getLegacyInferredCount() != null ? progressDto.getLegacyInferredCount() : 0));

        if (legacyCount > 0) {
            // Check 1-based sequential position if valid (1 <= lessonOrderIndex <= totalCourseLessons)
            if (lessonOrderIndex >= 1 && lessonOrderIndex <= totalCourseLessons) {
                if (lessonOrderIndex <= legacyCount) {
                    return true;
                }
            } else if (courseId != null && lessonId != null) {
                // Support 0-based or non-contiguous order_index by checking the lesson's rank in sorted course lessons
                List<Lesson> sorted = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
                for (int i = 0; i < Math.min(legacyCount, sorted.size()); i++) {
                    if (sorted.get(i).getId().equals(lessonId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getCompletedLessonIds(Long userId, Long courseId) {
        Set<Long> completedIds = new HashSet<>();
        if (userId == null || courseId == null) {
            return completedIds;
        }
        if (!courseRegistrationService.isUserEnrolled(userId, courseId)) {
            return completedIds;
        }
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        Optional<CourseRegistration> regOpt = courseRegistrationRepository.findByUserIdAndCourseId(userId, courseId);
        int progressPercent = regOpt.map(r -> r.getProgressPercent() != null ? r.getProgressPercent() : 0).orElse(0);

        for (int i = 0; i < courseLessons.size(); i++) {
            Lesson l = courseLessons.get(i);
            if (isLessonCompleted(userId, courseId, l.getId(), i + 1, courseLessons.size(), progressPercent)) {
                completedIds.add(l.getId());
            }
        }
        return completedIds;
    }

    private boolean isAccessEligible(String status) {
        return CourseRegistration.STATUS_ACTIVE.equalsIgnoreCase(status)
                || CourseRegistration.STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    private LearningPathProgressDto parseProgressDto(String json) {
        if (json != null && !json.isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
                while (node != null && node.isTextual()) {
                    node = objectMapper.readTree(node.asText());
                }
                if (node != null && node.isObject()) {
                    return objectMapper.treeToValue(node, LearningPathProgressDto.class);
                } else if (node != null && node.isArray()) {
                    LearningPathProgressDto dto = new LearningPathProgressDto();
                    for (com.fasterxml.jackson.databind.JsonNode item : node) {
                        if (item.has("lessonId")) {
                            long lId = item.get("lessonId").asLong();
                            String completedAt = item.has("completedAt") ? item.get("completedAt").asText() : Instant.now().toString();
                            dto.getLessons().put(String.valueOf(lId), LessonProgressItemDto.builder()
                                    .status("COMPLETED")
                                    .completedAt(completedAt)
                                    .build());
                        }
                    }
                    return dto;
                }
            } catch (Exception e) {
                log.warn("Failed to parse lesson_progress JSON: {}", e.getMessage());
            }
        }
        return new LearningPathProgressDto();
    }
}

