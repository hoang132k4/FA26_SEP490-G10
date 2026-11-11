package com.jlpp.modules.exam.service.impl;

import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.dto.ScoringEvaluationResult;
import com.jlpp.modules.exam.dto.SingleQuestionScoreResult;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class AssessmentScoringServiceImpl implements AssessmentScoringService {

    public static final int DEFAULT_QUESTION_POINTS = 2;
    public static final List<String> CANONICAL_SKILLS = List.of(
            "KANJI",
            "VOCABULARY",
            "GRAMMAR",
            "READING",
            "LISTENING"
    );

    @Override
    public ScoringEvaluationResult scoreQuestions(List<QuestionDto> questions,
                                                  Map<String, String> formParams,
                                                  Exam examContext) {
        String examType = examContext != null ? examContext.getExamType() : null;
        String jlptLevel = examContext != null ? examContext.getJlptLevel() : null;
        Integer customPassingScore = examContext != null ? examContext.getPassingScore() : null;

        return scoreQuestions(questions, formParams, examType, jlptLevel, customPassingScore);
    }

    @Override
    public ScoringEvaluationResult scoreQuestions(List<QuestionDto> questions,
                                                  Map<String, String> formParams,
                                                  String examType,
                                                  String jlptLevel,
                                                  Integer customPassingScore) {
        if (questions == null || questions.isEmpty()) {
            return buildEmptyResult(examType, jlptLevel, customPassingScore);
        }

        int totalPossibleScore = 0;
        int earnedScore = 0;
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;

        List<QuestionDto> reviewQuestions = new ArrayList<>();

        for (QuestionDto q : questions) {
            int qPoints = (q.getPoints() != null && q.getPoints() > 0) ? q.getPoints() : DEFAULT_QUESTION_POINTS;
            totalPossibleScore += qPoints;

            String userOptionParam = extractUserOptionParam(formParams, q.getId());
            SingleQuestionScoreResult single = scoreSingleQuestion(q, userOptionParam);

            if (single.isCorrect()) {
                earnedScore += qPoints;
                correctCount++;
            } else if (single.isUnanswered()) {
                unansweredCount++;
            } else {
                incorrectCount++;
            }

            reviewQuestions.add(QuestionDto.builder()
                    .id(q.getId())
                    .points(qPoints)
                    .category(q.getCategory())
                    .skillGroup(q.getSkillGroup())
                    .skillGroupLabel(q.getSkillGroupLabel())
                    .content(q.getContent())
                    .audioUrl(q.getAudioUrl())
                    .explanation(q.getExplanation())
                    .orderIndex(q.getOrderIndex())
                    .userAnswerId(single.getSelectedOptionId())
                    .isUserCorrect(single.isCorrect())
                    .options(q.getOptions())
                    .build());
        }

        BigDecimal percentage = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        if (totalPossibleScore > 0) {
            percentage = BigDecimal.valueOf((earnedScore * 100.0) / totalPossibleScore)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        boolean isPlacement = "PLACEMENT_TEST".equalsIgnoreCase(examType);
        boolean isMilestone = "MILESTONE".equalsIgnoreCase(examType);

        boolean passed;
        Integer passingScore;

        if (isPlacement) {
            passed = true;
            passingScore = null;
        } else if (isMilestone) {
            // Milestone Exam Evaluation
            if (totalPossibleScore <= 0) {
                // Zero-maximum-score exams cannot pass under any circumstances
                passed = false;
                passingScore = resolveMilestonePassingThreshold(totalPossibleScore, customPassingScore);
            } else if (earnedScore >= totalPossibleScore) {
                // Full marks always pass on any valid Milestone configuration
                passed = true;
                passingScore = resolveMilestonePassingThreshold(totalPossibleScore, customPassingScore);
            } else {
                // Determine unambiguous effective passing score (raw score or calibrated percentage)
                int effectivePassingScore = resolveMilestonePassingThreshold(totalPossibleScore, customPassingScore);
                // Unambiguous decision: compare integer earnedScore against effectivePassingScore
                // DO NOT compare rounded display percentage to avoid display rounding contamination (e.g. 49.96% rounding to 50.0%)
                passed = (earnedScore >= effectivePassingScore);
                passingScore = effectivePassingScore;
            }
        } else {
            // JLPT_MOCK and other exams: passingScore is raw marks out of total score
            int threshold = resolvePassingScore(examType, jlptLevel, customPassingScore);
            passed = (totalPossibleScore > 0) && (
                    (earnedScore >= totalPossibleScore) || (earnedScore >= threshold)
            );
            passingScore = threshold;
        }

        List<ExamResultResponse.SkillScoreDto> assessedSkills = calculateSkillBreakdown(reviewQuestions, false);
        List<ExamResultResponse.SkillScoreDto> allCanonicalSkills = calculateSkillBreakdown(reviewQuestions, true);

        // Identify strongest and weakest skills from assessed skills
        String strongestSkill = null;
        String weakestSkill = null;
        BigDecimal maxPct = BigDecimal.valueOf(-1);
        BigDecimal minPct = BigDecimal.valueOf(101);

        for (ExamResultResponse.SkillScoreDto skill : assessedSkills) {
            BigDecimal p = skill.getPercentage() != null ? skill.getPercentage() : BigDecimal.ZERO;
            if (p.compareTo(maxPct) > 0) {
                maxPct = p;
                strongestSkill = skill.getSkillName();
            }
            if (p.compareTo(minPct) < 0) {
                minPct = p;
                weakestSkill = skill.getSkillName();
            }
        }

        if (strongestSkill == null && !assessedSkills.isEmpty()) {
            strongestSkill = assessedSkills.get(0).getSkillName();
        }
        if (weakestSkill == null && !assessedSkills.isEmpty()) {
            weakestSkill = assessedSkills.get(0).getSkillName();
        }

        String level = jlptLevel != null ? jlptLevel : "N4";
        String aiFeedback = generateAiFeedback(examType, passed, earnedScore, totalPossibleScore, passingScore, percentage, strongestSkill, weakestSkill, level);
        String recommendation = generateRecommendation(examType, passed, weakestSkill, level);

        return ScoringEvaluationResult.builder()
                .earnedScore(earnedScore)
                .totalPossibleScore(totalPossibleScore)
                .percentage(percentage)
                .passed(passed)
                .passingScore(isPlacement ? null : passingScore)
                .correctCount(correctCount)
                .incorrectCount(incorrectCount)
                .unansweredCount(unansweredCount)
                .totalQuestions(questions.size())
                .strongestSkill(strongestSkill)
                .weakestSkill(weakestSkill)
                .aiFeedback(aiFeedback)
                .recommendation(recommendation)
                .skillBreakdown(assessedSkills)
                .canonicalSkills(allCanonicalSkills)
                .scoredQuestions(reviewQuestions)
                .build();
    }

    @Override
    public SingleQuestionScoreResult scoreSingleQuestion(QuestionDto question, String rawUserAnswer) {
        if (question == null) {
            return SingleQuestionScoreResult.builder()
                    .correct(false)
                    .unanswered(true)
                    .weight(DEFAULT_QUESTION_POINTS)
                    .earnedScore(0)
                    .build();
        }

        int weight = (question.getPoints() != null && question.getPoints() > 0)
                ? question.getPoints()
                : DEFAULT_QUESTION_POINTS;

        String canonicalSkill = resolveCanonicalSkill(question);

        Long selectedOptionId = null;
        if (rawUserAnswer != null && !rawUserAnswer.trim().isEmpty()) {
            try {
                selectedOptionId = Long.parseLong(rawUserAnswer.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (selectedOptionId == null) {
            return SingleQuestionScoreResult.builder()
                    .questionId(question.getId())
                    .selectedOptionId(null)
                    .correct(false)
                    .unanswered(true)
                    .weight(weight)
                    .earnedScore(0)
                    .canonicalSkill(canonicalSkill)
                    .build();
        }

        OptionDto correctOption = null;
        if (question.getOptions() != null) {
            for (OptionDto opt : question.getOptions()) {
                if (Boolean.TRUE.equals(opt.getCorrect())) {
                    correctOption = opt;
                    break;
                }
            }
        }

        boolean isCorrect = correctOption != null && selectedOptionId.equals(correctOption.getId());

        return SingleQuestionScoreResult.builder()
                .questionId(question.getId())
                .selectedOptionId(selectedOptionId)
                .correct(isCorrect)
                .unanswered(false)
                .weight(weight)
                .earnedScore(isCorrect ? weight : 0)
                .canonicalSkill(canonicalSkill)
                .build();
    }

    @Override
    public String resolveCanonicalSkill(QuestionDto question) {
        if (question == null) {
            return "VOCABULARY";
        }

        // 1. Authoritative SkillGroup metadata (exact canonical match takes highest precedence)
        String sg = question.getSkillGroup();
        if (sg != null) {
            String sgUpper = sg.trim().toUpperCase();
            if ("KANJI".equals(sgUpper)) return "KANJI";
            if ("VOCABULARY".equals(sgUpper)) return "VOCABULARY";
            if ("GRAMMAR".equals(sgUpper)) return "GRAMMAR";
            if ("READING".equals(sgUpper)) return "READING";
            if ("LISTENING".equals(sgUpper)) return "LISTENING";
        }

        // 2. Authoritative Category metadata
        String cat = question.getCategory() != null ? question.getCategory().toLowerCase().trim() : "";
        if (!cat.isEmpty()) {
            if (cat.contains("hán tự") || cat.contains("kanji") || cat.contains("chữ hán") || cat.contains("han tu")) {
                return "KANJI";
            }
            if (cat.contains("ngữ pháp") || cat.contains("bunpou") || cat.contains("grammar") || cat.contains("kính ngữ")) {
                return "GRAMMAR";
            }
            if (cat.contains("từ vựng") || cat.contains("goi") || cat.contains("vocab")) {
                return "VOCABULARY";
            }
            if (cat.contains("đọc") || cat.contains("doc") || cat.contains("dokkai") || cat.contains("reading")) {
                return "READING";
            }
            if (cat.contains("nghe") || cat.contains("choukai") || cat.contains("listening")) {
                return "LISTENING";
            }
        }

        // 3. Structured SkillGroupLabel metadata
        String sgl = question.getSkillGroupLabel() != null ? question.getSkillGroupLabel().toLowerCase().trim() : "";
        if (!sgl.isEmpty()) {
            if (sgl.contains("hán tự") || sgl.contains("kanji")) return "KANJI";
            if (sgl.contains("ngữ pháp") || sgl.contains("grammar")) return "GRAMMAR";
            if (sgl.contains("đọc") || sgl.contains("reading")) return "READING";
            if (sgl.contains("nghe") || sgl.contains("listening")) return "LISTENING";
            if (sgl.contains("từ vựng") || sgl.contains("vocab")) return "VOCABULARY";
        }

        // 4. Secondary heuristics: ONLY fallback to audioUrl and content keywords if metadata is absent
        String audio = question.getAudioUrl();
        if (audio != null && !audio.trim().isEmpty()) {
            return "LISTENING";
        }

        String content = question.getContent() != null ? question.getContent().toLowerCase() : "";
        if (content.contains("choukai") || content.contains("nghe hiểu") || content.contains("bài nghe")) {
            return "LISTENING";
        }
        if (content.contains("dokkai") || content.contains("đọc hiểu") || content.contains("đoạn văn")) {
            return "READING";
        }
        if (content.contains("hán tự") || content.contains("chữ hán")) {
            return "KANJI";
        }
        if (content.contains("bunpou") || content.contains("ngữ pháp")) {
            return "GRAMMAR";
        }
        if (content.contains("từ vựng") || content.contains("chọn từ")) {
            return "VOCABULARY";
        }

        return "VOCABULARY";
    }

    @Override
    public String getSkillDisplayName(String skillKey) {
        if (skillKey == null) return "Kiến thức tổng hợp";
        return switch (skillKey.toUpperCase()) {
            case "KANJI" -> "Chữ Hán (Kanji)";
            case "VOCABULARY" -> "Từ vựng (Vocabulary)";
            case "GRAMMAR" -> "Ngữ pháp (Grammar)";
            case "READING" -> "Đọc hiểu (Reading)";
            case "LISTENING" -> "Nghe hiểu (Listening)";
            case "VOCAB_GRAMMAR" -> "Kiến thức ngôn ngữ (Từ vựng • Ngữ pháp)";
            default -> skillKey;
        };
    }

    @Override
    public List<ExamResultResponse.SkillScoreDto> calculateSkillBreakdown(List<QuestionDto> scoredQuestions, boolean includeUntested) {
        Map<String, int[]> statsMap = new LinkedHashMap<>();
        for (String skill : CANONICAL_SKILLS) {
            statsMap.put(skill, new int[]{0, 0, 0, 0}); // [0]: earnedPoints, [1]: totalPoints, [2]: correctCount, [3]: totalQuestions
        }

        if (scoredQuestions != null) {
            for (QuestionDto q : scoredQuestions) {
                String skill = resolveCanonicalSkill(q);
                int points = (q.getPoints() != null && q.getPoints() > 0) ? q.getPoints() : DEFAULT_QUESTION_POINTS;
                boolean isCorrect = Boolean.TRUE.equals(q.getIsUserCorrect());

                int[] stat = statsMap.computeIfAbsent(skill, k -> new int[]{0, 0, 0, 0});
                stat[1] += points;
                stat[3] += 1;
                if (isCorrect) {
                    stat[0] += points;
                    stat[2] += 1;
                }
            }
        }

        List<ExamResultResponse.SkillScoreDto> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : statsMap.entrySet()) {
            String skillKey = entry.getKey();
            int[] s = entry.getValue();

            boolean isAssessed = s[3] > 0;
            if (!isAssessed && !includeUntested) {
                continue;
            }

            BigDecimal pct = null;
            if (s[1] > 0) {
                pct = BigDecimal.valueOf((s[0] * 100.0) / s[1]).setScale(1, RoundingMode.HALF_UP);
            }

            result.add(ExamResultResponse.SkillScoreDto.builder()
                    .skillGroup(skillKey)
                    .skillName(getSkillDisplayName(skillKey))
                    .earnedPoints(s[0])
                    .totalPoints(s[1])
                    .correctCount(s[2])
                    .totalQuestions(s[3])
                    .percentage(pct)
                    .status(isAssessed ? "ASSESSED" : "NOT_ASSESSED")
                    .build());
        }

        return result;
    }

    @Override
    public int resolvePassingScore(String examType, String jlptLevel, Integer customPassingScore) {
        if (customPassingScore != null && customPassingScore > 0) {
            return customPassingScore;
        }

        if ("MILESTONE".equalsIgnoreCase(examType)) {
            return 50;
        }

        return switch (jlptLevel != null ? jlptLevel.toUpperCase() : "") {
            case "N5" -> 80;
            case "N4" -> 90;
            case "N3" -> 95;
            case "N2" -> 90;
            case "N1" -> 100;
            default -> 80;
        };
    }

    /**
     * Resolves the unambiguous required passing score for a Milestone Assessment.
     * Respects raw score thresholds when valid, handles uncalibrated percentage defaults when exceeding max score,
     * and rejects/normalizes invalid thresholds.
     */
    public int resolveMilestonePassingThreshold(int totalPossibleScore, Integer customPassingScore) {
        if (totalPossibleScore <= 0) {
            return (customPassingScore != null && customPassingScore > 0) ? customPassingScore : 50;
        }

        // Case 1: Unspecified or non-positive threshold -> fallback to standard 50%
        if (customPassingScore == null || customPassingScore <= 0) {
            return (int) Math.ceil((totalPossibleScore * 50.0) / 100.0);
        }

        int threshold = customPassingScore;

        // Case 2: Configured threshold is a valid raw score within (0, totalPossibleScore]
        if (threshold <= totalPossibleScore) {
            return threshold;
        }

        // Case 3: Threshold exceeds totalPossibleScore (e.g., legacy default threshold 50 on a 40-point exam)
        // If threshold <= 100, it represents a percentage standard (e.g. 50%)
        if (threshold <= 100) {
            return (int) Math.ceil((totalPossibleScore * threshold) / 100.0);
        }

        // Case 4: Completely invalid threshold (> 100 and > totalPossibleScore) -> clamp to totalPossibleScore
        return totalPossibleScore;
    }

    private String extractUserOptionParam(Map<String, String> formParams, Long questionId) {
        if (formParams == null || questionId == null) {
            return null;
        }
        String param = formParams.get("answer_" + questionId);
        if (param == null || param.trim().isEmpty()) {
            param = formParams.get(String.valueOf(questionId));
        }
        return param;
    }

    private ScoringEvaluationResult buildEmptyResult(String examType, String jlptLevel, Integer customPassingScore) {
        boolean isPlacement = "PLACEMENT_TEST".equalsIgnoreCase(examType);
        int passingScore = resolvePassingScore(examType, jlptLevel, customPassingScore);
        List<ExamResultResponse.SkillScoreDto> allUntested = calculateSkillBreakdown(Collections.emptyList(), true);

        return ScoringEvaluationResult.builder()
                .earnedScore(0)
                .totalPossibleScore(0)
                .percentage(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP))
                .passed(isPlacement)
                .passingScore(isPlacement ? null : passingScore)
                .correctCount(0)
                .incorrectCount(0)
                .unansweredCount(0)
                .totalQuestions(0)
                .strongestSkill(null)
                .weakestSkill(null)
                .aiFeedback("Đề thi chưa có câu hỏi nào để chấm điểm.")
                .recommendation("Vui lòng liên hệ quản trị viên để cập nhật ngân hàng câu hỏi.")
                .skillBreakdown(Collections.emptyList())
                .canonicalSkills(allUntested)
                .scoredQuestions(Collections.emptyList())
                .build();
    }

    private String generateAiFeedback(String examType,
                                       boolean passed,
                                       int earnedScore,
                                       int totalPossibleScore,
                                       Integer passingScore,
                                       BigDecimal percentage,
                                       String strongestSkill,
                                       String weakestSkill,
                                       String level) {
        if ("PLACEMENT_TEST".equalsIgnoreCase(examType)) {
            return "Bạn đã hoàn thành bài kiểm tra đánh giá năng lực đầu vào với tỷ lệ chính xác " + percentage + "% (" + earnedScore + "/" + totalPossibleScore + " điểm). "
                    + "Điểm số thực tế này phản ánh năng lực hiện tại của bạn. "
                    + (strongestSkill != null ? "Thế mạnh của bạn: " + strongestSkill + ". " : "")
                    + (weakestSkill != null ? "Khu vực cần bổ trợ: " + weakestSkill + ". " : "")
                    + "Kết quả này được dùng làm cơ sở thiết lập lộ trình học tập cá nhân hóa.";
        }

        if ("MILESTONE".equalsIgnoreCase(examType)) {
            int displayPassing = (passingScore != null) ? passingScore : 50;
            if (passed) {
                return "Chúc mừng bạn đã vượt qua bài kiểm tra cột mốc (" + earnedScore + "/" + totalPossibleScore + " điểm, điểm đạt yêu cầu: " + displayPassing + " điểm, độ chính xác " + percentage + "%). Bạn đã nắm vững các kiến thức trọng tâm của giai đoạn này.";
            } else {
                return "Kết quả hiện tại chưa đạt điểm sàn bài kiểm tra cột mốc (" + earnedScore + "/" + totalPossibleScore + " điểm, điểm đạt yêu cầu: " + displayPassing + " điểm). Bạn cần tập trung ôn tập kỹ năng " + (weakestSkill != null ? weakestSkill.toLowerCase() : "trọng tâm") + " trước khi chuyển sang giai đoạn tiếp theo.";
            }
        }

        // Default: JLPT_MOCK and other exams
        if (passed) {
            return "Chúc mừng bạn đã đạt điểm sàn bài thi JLPT " + level + " (" + earnedScore + "/" + totalPossibleScore + " điểm). Bạn đã nắm vững các kiến thức trọng tâm.";
        } else {
            return "Kết quả hiện tại chưa đạt điểm sàn JLPT " + level + " (" + earnedScore + "/" + totalPossibleScore + " điểm). Bạn cần tập trung bổ sung kiến thức " + (weakestSkill != null ? weakestSkill.toLowerCase() : "từ vựng") + " và luyện tập lại các thể động từ cơ bản.";
        }
    }

    private String generateRecommendation(String examType,
                                          boolean passed,
                                          String weakestSkill,
                                          String level) {
        if ("PLACEMENT_TEST".equalsIgnoreCase(examType)) {
            return "Dựa trên kết quả đánh giá, hệ thống đề xuất lộ trình học tập cá nhân hóa tập trung củng cố kỹ năng " + (weakestSkill != null ? weakestSkill.toLowerCase() : "căn bản") + ".";
        }

        if ("MILESTONE".equalsIgnoreCase(examType)) {
            if (passed) {
                return "Tiếp tục học các bài học tiếp theo theo lộ trình cá nhân hóa của khóa học.";
            } else {
                return "Xem lại các bài học và bài tập ôn tập thuộc giai đoạn này rồi thực hiện lại bài kiểm tra cột mốc.";
            }
        }

        if (!passed) {
            return "Ôn tập giáo trình Minnano Nihongo Bài " + ("N5".equalsIgnoreCase(level) ? "1 - 25" : "26 - 35") + " và thi thử lại sau 2 tuần.";
        } else {
            return "Tiếp tục duy trì cường độ học tập và thử sức với các bài thi mô phỏng tiếp theo.";
        }
    }
}

