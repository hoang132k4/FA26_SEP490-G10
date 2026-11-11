package com.jlpp.modules.learningpath.service.impl;

import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.learningpath.dto.SkillMetricDto;
import com.jlpp.modules.learningpath.dto.SkillProfileDto;
import com.jlpp.modules.learningpath.dto.SkillProficiency;
import com.jlpp.modules.learningpath.service.SkillAnalysisService;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import com.jlpp.modules.exam.service.impl.AssessmentScoringServiceImpl;
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

    public SkillAnalysisServiceImpl(ExamService examService) {
        this(examService, new AssessmentScoringServiceImpl());
    }

    @Autowired
    public SkillAnalysisServiceImpl(ExamService examService, AssessmentScoringService scoringService) {
        this.examService = examService;
        this.scoringService = scoringService;
    }

    // Configurable heuristic thresholds
    public static final double WEAK_THRESHOLD = 50.0;
    public static final double STRONG_THRESHOLD = 70.0;

    @Override
    public SkillProfileDto analyzeLearnerSkills(Long userId, Long resultId) {
        if (userId == null) {
            return buildEmptyProfile(null, null);
        }

        Long targetResultId = resultId;
        if (targetResultId == null) {
            targetResultId = findLatestPlacementResultId(userId);
        }

        if (targetResultId == null) {
            log.info("No placement assessment record found for userId={}. Returning UNKNOWN profile.", userId);
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

    private Long findLatestPlacementResultId(Long userId) {
        try {
            List<ExamHistoryDto> history = examService.getLearnerExamHistory(userId);
            if (history == null || history.isEmpty()) {
                return null;
            }

            return history.stream()
                    .filter(h -> "PLACEMENT_TEST".equalsIgnoreCase(h.getExamType())
                            || (h.getExamTitle() != null && h.getExamTitle().toLowerCase().contains("đánh giá năng lực")))
                    .max(Comparator.comparing(ExamHistoryDto::getResultId))
                    .map(ExamHistoryDto::getResultId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Error finding latest placement result for userId={}: {}", userId, e.getMessage());
            return null;
        }
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

