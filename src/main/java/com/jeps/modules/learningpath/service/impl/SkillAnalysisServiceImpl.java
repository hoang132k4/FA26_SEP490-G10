package com.jeps.modules.learningpath.service.impl;

import com.jeps.modules.learningpath.dto.LearningPathDtos.*;

import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.exam.dto.ExamHistoryDto;
import com.jeps.modules.exam.dto.ExamResultResponse;
import com.jeps.modules.exam.dto.QuestionDto;
import com.jeps.modules.exam.entity.Exam;
import com.jeps.modules.exam.repository.ExamRepository;
import com.jeps.modules.exam.service.ExamService;
import com.jeps.modules.learningpath.service.SkillAnalysisService;
import com.jeps.modules.exam.service.AssessmentScoringService;
import com.jeps.modules.exam.service.impl.AssessmentScoringServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class SkillAnalysisServiceImpl implements SkillAnalysisService {

    private final ExamService examService;
    private final AssessmentScoringService scoringService;
    private final ExamRepository examRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public SkillAnalysisServiceImpl(ExamService examService) {
        this(examService, new AssessmentScoringServiceImpl(), null, null, null);
    }

    public SkillAnalysisServiceImpl(ExamService examService, AssessmentScoringService scoringService) {
        this(examService, scoringService, null, null, null);
    }

    public SkillAnalysisServiceImpl(ExamService examService, AssessmentScoringService scoringService, ExamRepository examRepository) {
        this(examService, scoringService, examRepository, null, null);
    }

    @Autowired
    public SkillAnalysisServiceImpl(ExamService examService, AssessmentScoringService scoringService,
                                   ExamRepository examRepository, CourseRepository courseRepository,
                                   LessonRepository lessonRepository) {
        this.examService = examService;
        this.scoringService = scoringService;
        this.examRepository = examRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    // Configurable heuristic thresholds
    public static final double WEAK_THRESHOLD = 50.0;
    public static final double STRONG_THRESHOLD = 70.0;

    @Override
    public SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId) {
        return analyzeLearnerSkills(userId, resultId, null, null);
    }

    @Override
    public SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId, Long courseId) {
        return analyzeLearnerSkills(userId, resultId, courseId, null);
    }

    @Override
    public SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId, Long courseId, String courseLevel) {
        if (userId == null) {
            return buildEmptyProfile(null, null);
        }

        String standardLevel = resolveCourseLevel(courseId, courseLevel);

        Long targetResultId = resultId;
        // Nếu resultId được truyền vào, kiểm tra xem nó có đúng level của khóa học không (tránh lấy nhầm kết quả của level khác)
        if (targetResultId != null && standardLevel != null) {
            try {
                ExamResultResponse checkResult = examService.getHistoricalResult(targetResultId, userId);
                if (checkResult != null) {
                    boolean matches = isExamMatchingLevel(checkResult.getExamId(), checkResult.getExamTitle(), checkResult.getJlptLevel(), courseId, standardLevel);
                    if (!matches) {
                        log.info("ResultId={} does not match level {}. Discarding and searching for level-matching placement result.", targetResultId, standardLevel);
                        targetResultId = null;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not verify historical resultId={} against level {}: {}", targetResultId, standardLevel, e.getMessage());
            }
        }

        // 2. Nếu chưa có targetResultId hợp lệ, tìm bài thi Đánh giá đầu vào (Placement Test) mới nhất CÙNG CẤP ĐỘ với khóa học
        if (targetResultId == null) {
            targetResultId = findLatestPlacementResultId(userId, courseId, standardLevel);
        }

        if (targetResultId == null) {
            log.info("No placement assessment record found matching level {} for userId={}, courseId={}. Returning standard/unknown profile.",
                    standardLevel, userId, courseId);
            return buildEmptyProfile(userId, null);
        }

        try {
            ExamResultResponse examResult = examService.getHistoricalResult(targetResultId, userId);
            if (examResult == null) {
                return buildEmptyProfile(userId, targetResultId);
            }
            return buildProfileFromResult(userId, targetResultId, examResult);
        } catch (Exception e) {
            log.warn("Could not load placement exam result for userId={}, resultId={}: {}",
                    userId, targetResultId, e.getMessage());
            return buildEmptyProfile(userId, targetResultId);
        }
    }

    @Override
    public SkillProfileDto analyzeAndRecalibrateSkills(Long userId, Long courseId, Long fallbackResultId) {
        return analyzeAndRecalibrateSkills(userId, courseId, null, fallbackResultId);
    }

    @Override
    public SkillProfileDto analyzeAndRecalibrateSkills(Long userId, Long courseId, String courseLevel, Long fallbackResultId) {
        if (userId == null) {
            return buildEmptyProfile(null, null);
        }

        String standardLevel = resolveCourseLevel(courseId, courseLevel);

        // 1. Tìm kết quả bài thi Cột mốc (Milestone Exam) gần nhất của học viên thuộc đúng khóa học / cấp độ
        Long latestMilestoneResultId = findLatestMilestoneResultId(userId, courseId, standardLevel);

        if (latestMilestoneResultId != null) {
            try {
                ExamResultResponse milestoneResult = examService.getHistoricalResult(latestMilestoneResultId, userId);
                if (milestoneResult != null) {
                    SkillProfileDto recalibratedProfile = buildProfileFromResult(userId, latestMilestoneResultId, milestoneResult);
                    recalibratedProfile.setRecalibratedFromMilestone(true);
                    recalibratedProfile.setMilestoneExamTitle(milestoneResult.getExamTitle() != null ? milestoneResult.getExamTitle() : "Bài kiểm tra Cột mốc");
                    recalibratedProfile.setMilestoneCompletionDate(milestoneResult.getCompletionDate());
                    recalibratedProfile.setMilestoneScore(milestoneResult.getPercentage() != null ? milestoneResult.getPercentage().intValue() : null);
                    recalibratedProfile.setRecalibrationNote(
                            String.format("Đã tái hiệu chỉnh theo Bài thi Cột mốc: %s",
                                    recalibratedProfile.getMilestoneExamTitle()));

                    // So sánh với điểm bài test đầu vào ban đầu (cùng cấp độ) để tính độ chênh lệch tiến bộ (accuracyDelta)
                    SkillProfileDto baselineProfile = analyzeLearnerSkills(userId, fallbackResultId, courseId, standardLevel);
                    if (baselineProfile != null && baselineProfile.isAssessmentTaken()) {
                        for (Map.Entry<String, SkillMetricDto> entry : recalibratedProfile.getSkills().entrySet()) {
                            String skillKey = entry.getKey();
                            SkillMetricDto metric = entry.getValue();
                            SkillMetricDto baseMetric = baselineProfile.getSkill(skillKey);
                            if (baseMetric != null && baseMetric.getAccuracyPercentage() != null && metric.getAccuracyPercentage() != null) {
                                metric.setPreviousAccuracy(baseMetric.getAccuracyPercentage());
                                BigDecimal delta = metric.getAccuracyPercentage().subtract(baseMetric.getAccuracyPercentage());
                                metric.setAccuracyDelta(delta);
                                metric.setImproved(delta.compareTo(BigDecimal.ZERO) > 0);
                            }
                        }
                    }

                    log.info("Successfully recalibrated skills from Milestone Exam resultId={} for userId={}, courseId={}, level={}",
                            latestMilestoneResultId, userId, courseId, standardLevel);
                    return recalibratedProfile;
                }
            } catch (Exception ex) {
                log.warn("Failed to load milestone exam resultId={} for recalibration: {}", latestMilestoneResultId, ex.getMessage());
            }
        }

        // 2. Nếu chưa có bài thi cột mốc nào cho khóa học này, fallback về phân tích bài test đầu vào (Placement Test) của ĐÚNG cấp độ
        return analyzeLearnerSkills(userId, fallbackResultId, courseId, standardLevel);
    }

    private Long findLatestMilestoneResultId(Long userId, Long courseId, String targetCourseLevel) {
        try {
            List<ExamHistoryDto> history = examService.getLearnerExamHistory(userId);
            if (history == null || history.isEmpty()) {
                return null;
            }

            String standardLevel = resolveCourseLevel(courseId, targetCourseLevel);

            // Thu thập danh sách examId của các bài thi Cột mốc thuộc khóa học này
            Set<Long> courseMilestoneExamIds = new HashSet<>();

            // 2a. Lấy từ các bài học cột mốc (milestone lessons) trong khóa học
            if (courseId != null && lessonRepository != null) {
                try {
                    List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
                    if (lessons != null) {
                        for (Lesson l : lessons) {
                            if (l.isMilestoneExam() && l.getExamId() != null) {
                                courseMilestoneExamIds.add(l.getExamId());
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Could not query milestone lessons for courseId={}: {}", courseId, ex.getMessage());
                }
            }

            // 2b. Lấy từ ExamRepository theo courseId
            if (courseId != null && examRepository != null) {
                try {
                    List<Exam> byCourse = examRepository.findByCourseIdAndExamTypeIgnoreCase(courseId, "MILESTONE");
                    if (byCourse != null) {
                        for (Exam e : byCourse) {
                            courseMilestoneExamIds.add(e.getExamId());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Could not query milestone exams by courseId={}: {}", courseId, ex.getMessage());
                }
            }

            // 2c. Lấy từ ExamRepository theo JLPT level của khóa học (ví dụ: "N3")
            if (standardLevel != null && !standardLevel.isBlank() && examRepository != null) {
                try {
                    List<Exam> byLevel = examRepository.findByExamTypeIgnoreCaseAndJlptLevelIgnoreCase("MILESTONE", standardLevel);
                    if (byLevel != null) {
                        for (Exam e : byLevel) {
                            courseMilestoneExamIds.add(e.getExamId());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Could not query milestone exams by level={}: {}", standardLevel, ex.getMessage());
                }
            }

            final String targetLevel = standardLevel;

            return history.stream()
                    .filter(h -> {
                        // Bắt buộc phải là bài thi Cột mốc
                        boolean isMilestone = "MILESTONE".equalsIgnoreCase(h.getExamType())
                                || (h.getExamTitle() != null && (
                                        h.getExamTitle().toLowerCase().contains("cột mốc") ||
                                        h.getExamTitle().toLowerCase().contains("milestone")));
                        if (!isMilestone) {
                            return false;
                        }

                        // Nếu bài thi nằm trong danh sách ExamId được xác định của khóa học này -> Chấp nhận
                        if (!courseMilestoneExamIds.isEmpty() && h.getExamId() != null && courseMilestoneExamIds.contains(h.getExamId())) {
                            return true;
                        }

                        // Nếu khóa học có cấp độ xác định (ví dụ N2, N3...)
                        if (targetLevel != null && !targetLevel.isBlank()) {
                            return isExamMatchingLevel(h.getExamId(), h.getExamTitle(), h.getJlptLevel(), courseId, targetLevel);
                        }

                        // Nếu không có courseId và không có targetLevel (chế độ không có ngữ cảnh khóa học)
                        return courseMilestoneExamIds.isEmpty();
                    })
                    .max(Comparator.comparing(ExamHistoryDto::getResultId))
                    .map(ExamHistoryDto::getResultId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Error finding latest milestone result for userId={}, courseId={}: {}", userId, courseId, e.getMessage());
            return null;
        }
    }

    private Long findLatestPlacementResultId(Long userId, Long courseId, String targetCourseLevel) {
        try {
            List<ExamHistoryDto> history = examService.getLearnerExamHistory(userId);
            if (history == null || history.isEmpty()) {
                return null;
            }

            String standardLevel = resolveCourseLevel(courseId, targetCourseLevel);

            return history.stream()
                    .filter(h -> {
                        boolean isPlacement = "PLACEMENT_TEST".equalsIgnoreCase(h.getExamType())
                                || (h.getExamTitle() != null && (
                                        h.getExamTitle().toLowerCase().contains("đánh giá năng lực") ||
                                        h.getExamTitle().toLowerCase().contains("placement")));
                        if (!isPlacement) {
                            return false;
                        }

                        // Nếu khóa học có cấp độ xác định (ví dụ N2): BẮT BUỘC bài thi phải thuộc cấp độ N2
                        if (standardLevel != null) {
                            return isExamMatchingLevel(h.getExamId(), h.getExamTitle(), h.getJlptLevel(), courseId, standardLevel);
                        }

                        return true;
                    })
                    .max(Comparator.comparing(ExamHistoryDto::getResultId))
                    .map(ExamHistoryDto::getResultId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Error finding latest placement result for userId={}, courseId={}, level={}: {}", userId, courseId, targetCourseLevel, e.getMessage());
            return null;
        }
    }

    private String extractStandardLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String upper = text.toUpperCase();
        for (String lvl : List.of("N1", "N2", "N3", "N4", "N5")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![A-Z0-9])" + lvl + "(?![A-Z0-9])").matcher(upper);
            if (m.find()) {
                return lvl;
            }
        }
        return null;
    }

    private String resolveCourseLevel(Long courseId, String explicitLevel) {
        String lvl = extractStandardLevel(explicitLevel);
        if (lvl != null) {
            return lvl;
        }
        if (courseId != null && courseRepository != null) {
            try {
                Course course = courseRepository.findById(courseId).orElse(null);
                if (course != null) {
                    lvl = extractStandardLevel(course.getLevel());
                    if (lvl != null) return lvl;
                    lvl = extractStandardLevel(course.getTitle());
                    if (lvl != null) return lvl;
                    lvl = extractStandardLevel(course.getCode());
                    if (lvl != null) return lvl;
                }
            } catch (Exception ex) {
                log.warn("Could not query Course for courseId={}: {}", courseId, ex.getMessage());
            }
        }
        return null;
    }

    private boolean isExamMatchingLevel(Long examId, String examTitle, String jlptLevel, Long courseId, String targetCourseLevel) {
        String standardTarget = extractStandardLevel(targetCourseLevel);
        if (standardTarget == null) {
            return true;
        }

        // 1. Kiểm tra qua jlptLevel nếu có trong kết quả / DTO
        String examLvl = extractStandardLevel(jlptLevel);
        if (examLvl != null) {
            return standardTarget.equalsIgnoreCase(examLvl);
        }

        // 2. Kiểm tra qua tiêu đề bài thi (Exam Title)
        String titleLvl = extractStandardLevel(examTitle);
        if (titleLvl != null) {
            return standardTarget.equalsIgnoreCase(titleLvl);
        }

        // 3. Kiểm tra qua Exam entity trong database (nếu có examRepository)
        if (examId != null && examRepository != null) {
            try {
                Exam exam = examRepository.findById(examId).orElse(null);
                if (exam != null) {
                    String entityLvl = extractStandardLevel(exam.getJlptLevel());
                    if (entityLvl != null) {
                        return standardTarget.equalsIgnoreCase(entityLvl);
                    }
                    String entityTitleLvl = extractStandardLevel(exam.getTitle() != null ? exam.getTitle() : exam.getExamTitle());
                    if (entityTitleLvl != null) {
                        return standardTarget.equalsIgnoreCase(entityTitleLvl);
                    }
                    if (exam.getCourseId() != null && courseId != null && exam.getCourseId().equals(courseId)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                log.warn("Error checking exam entity for examId={}: {}", examId, ex.getMessage());
            }
        }

        return false;
    }

    private SkillProfileDto buildProfileFromResult(Long userId, Long resultId, ExamResultResponse result) {
        Map<String, int[]> accumulators = new LinkedHashMap<>();
        // Keys: KANJI, VOCABULARY, GRAMMAR, READING, LISTENING
        accumulators.put("KANJI", new int[]{0, 0});       // [0]: correct, [1]: total
        accumulators.put("VOCABULARY", new int[]{0, 0});
        accumulators.put("GRAMMAR", new int[]{0, 0});
        accumulators.put("READING", new int[]{0, 0});
        accumulators.put("LISTENING", new int[]{0, 0});

        List<QuestionDto> questions = result.getQuestions();
        boolean hasGranularQuestions = questions != null && !questions.isEmpty();

        if (hasGranularQuestions) {
            for (QuestionDto q : questions) {
                String normalizedSkill = mapQuestionToSkill(q);
                int[] acc = accumulators.get(normalizedSkill);
                if (acc != null) {
                    acc[1]++;
                    if (Boolean.TRUE.equals(q.getIsUserCorrect())) {
                        acc[0]++;
                    }
                }
            }
        }

        // If granular questions did not map to anything, fallback to skillBreakdown in ExamResultResponse
        boolean anyGranularMeasured = accumulators.values().stream().anyMatch(a -> a[1] > 0);
        if (!anyGranularMeasured) {
            List<ExamResultResponse.SkillScoreDto> sourceBreakdown = (result.getCanonicalSkills() != null && !result.getCanonicalSkills().isEmpty())
                    ? result.getCanonicalSkills()
                    : result.getSkillBreakdown();

            if (sourceBreakdown != null) {
                for (ExamResultResponse.SkillScoreDto score : sourceBreakdown) {
                    if ("NOT_ASSESSED".equalsIgnoreCase(score.getStatus()) || score.getTotalQuestions() == 0) {
                        continue; // Do NOT populate accumulator for untested skills: keeps total = 0 -> UNKNOWN
                    }
                    String group = score.getSkillGroup();
                    int total = score.getTotalQuestions();
                    int correct = score.getCorrectCount();
                    if ("READING".equalsIgnoreCase(group)) {
                        accumulators.put("READING", new int[]{correct, total});
                    } else if ("LISTENING".equalsIgnoreCase(group)) {
                        accumulators.put("LISTENING", new int[]{correct, total});
                    } else if ("KANJI".equalsIgnoreCase(group)) {
                        accumulators.put("KANJI", new int[]{correct, total});
                    } else if ("VOCABULARY".equalsIgnoreCase(group)) {
                        accumulators.put("VOCABULARY", new int[]{correct, total});
                    } else if ("GRAMMAR".equalsIgnoreCase(group)) {
                        accumulators.put("GRAMMAR", new int[]{correct, total});
                    } else if ("VOCAB_GRAMMAR".equalsIgnoreCase(group)) {
                        // Compound group: distribute evenly or preserve as compound
                        accumulators.put("VOCABULARY", new int[]{correct, total});
                        accumulators.put("GRAMMAR", new int[]{correct, total});
                    }
                }
            }
        }

        Map<String, SkillMetricDto> skillMetrics = new LinkedHashMap<>();
        List<String> weakList = new ArrayList<>();
        List<String> mediumList = new ArrayList<>();
        List<String> strongList = new ArrayList<>();

        String weakestSkill = null;
        String strongestSkill = null;
        BigDecimal lowestPct = BigDecimal.valueOf(101);
        BigDecimal highestPct = BigDecimal.valueOf(-1);

        for (Map.Entry<String, int[]> entry : accumulators.entrySet()) {
            String skillKey = entry.getKey();
            int correct = entry.getValue()[0];
            int total = entry.getValue()[1];

            SkillProficiency prof;
            BigDecimal pct = null;
            boolean evidence = total > 0;

            if (evidence) {
                pct = BigDecimal.valueOf((correct * 100.0) / total).setScale(1, RoundingMode.HALF_UP);
                if (pct.doubleValue() < WEAK_THRESHOLD) {
                    prof = SkillProficiency.WEAK;
                    weakList.add(getDisplayName(skillKey));
                } else if (pct.doubleValue() >= STRONG_THRESHOLD) {
                    prof = SkillProficiency.STRONG;
                    strongList.add(getDisplayName(skillKey));
                } else {
                    prof = SkillProficiency.MEDIUM;
                    mediumList.add(getDisplayName(skillKey));
                }

                if (pct.compareTo(lowestPct) < 0) {
                    lowestPct = pct;
                    weakestSkill = getDisplayName(skillKey);
                }
                if (pct.compareTo(BigDecimal.ZERO) > 0 && pct.compareTo(highestPct) > 0) {
                    highestPct = pct;
                    strongestSkill = getDisplayName(skillKey);
                }
            } else {
                prof = SkillProficiency.UNKNOWN;
            }

            skillMetrics.put(skillKey, SkillMetricDto.builder()
                    .skillKey(skillKey)
                    .skillName(getDisplayName(skillKey))
                    .totalQuestions(total)
                    .correctCount(correct)
                    .accuracyPercentage(pct)
                    .proficiency(prof)
                    .evidenceAvailable(evidence)
                    .build());
        }

        BigDecimal overallAcc = result.getPercentage() != null
                ? result.getPercentage()
                : (result.getTotalQuestions() > 0
                    ? BigDecimal.valueOf((result.getCorrectCount() * 100.0) / result.getTotalQuestions()).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

        return SkillProfileDto.builder()
                .userId(userId)
                .resultId(resultId)
                .assessmentTitle(result.getExamTitle() != null ? result.getExamTitle() : "Bài đánh giá năng lực đầu vào")
                .jlptLevel(result.getJlptLevel())
                .overallAccuracy(overallAcc)
                .weakestSkillName(weakestSkill)
                .strongestSkillName(strongestSkill)
                .assessmentTaken(true)
                .skills(skillMetrics)
                .weakSkills(weakList)
                .mediumSkills(mediumList)
                .strongSkills(strongList)
                .build();
    }

    private String mapQuestionToSkill(QuestionDto q) {
        return scoringService.resolveCanonicalSkill(q);
    }

    private String getDisplayName(String skillKey) {
        return scoringService.getSkillDisplayName(skillKey);
    }

    private SkillProfileDto buildEmptyProfile(Long userId, Long resultId) {
        Map<String, SkillMetricDto> map = new LinkedHashMap<>();
        for (String key : List.of("KANJI", "VOCABULARY", "GRAMMAR", "READING", "LISTENING")) {
            map.put(key, SkillMetricDto.builder()
                    .skillKey(key)
                    .skillName(getDisplayName(key))
                    .totalQuestions(0)
                    .correctCount(0)
                    .accuracyPercentage(null)
                    .proficiency(SkillProficiency.UNKNOWN)
                    .evidenceAvailable(false)
                    .build());
        }

        return SkillProfileDto.builder()
                .userId(userId)
                .resultId(resultId)
                .assessmentTitle("Chưa thực hiện bài đánh giá")
                .overallAccuracy(BigDecimal.ZERO)
                .assessmentTaken(false)
                .skills(map)
                .weakSkills(Collections.emptyList())
                .mediumSkills(Collections.emptyList())
                .strongSkills(Collections.emptyList())
                .build();
    }
}

