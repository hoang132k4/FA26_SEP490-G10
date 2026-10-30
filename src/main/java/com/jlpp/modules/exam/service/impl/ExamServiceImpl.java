package com.jlpp.modules.exam.service.impl;

import com.jlpp.modules.exam.dto.*;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.reader.LearnerExamResultReader;
import com.jlpp.modules.exam.reader.LearnerQuestionReader;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.exam.writer.LearnerExamResultWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final LearnerQuestionReader questionReader;
    private final LearnerExamResultWriter resultWriter;
    private final LearnerExamResultReader resultReader;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Double-submit deduplication cache (5s window per user + exam + answers hash)
    private static class SubmissionRecord {
        final long timestamp;
        final ExamResultResponse response;

        SubmissionRecord(long timestamp, ExamResultResponse response) {
            this.timestamp = timestamp;
            this.response = response;
        }
    }

    private final Map<String, SubmissionRecord> recentSubmissions = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public List<ExamCardDto> getLearnerExams(String jlptLevel) {
        List<Exam> exams;
        if (jlptLevel == null || jlptLevel.trim().isEmpty() || "ALL".equalsIgnoreCase(jlptLevel.trim())) {
            exams = examRepository.findByExamTypeIgnoreCase("JLPT_MOCK");
        } else {
            exams = examRepository.findByExamTypeIgnoreCaseAndJlptLevelIgnoreCase("JLPT_MOCK", jlptLevel.trim());
        }

        List<ExamCardDto> result = new ArrayList<>();
        for (Exam exam : exams) {
            long count = questionReader.countQuestionsByExamId(exam.getExamId());
            result.add(ExamCardDto.builder()
                    .examId(exam.getExamId())
                    .examTitle(exam.getExamTitle() != null ? exam.getExamTitle() : exam.getTitle())
                    .description(exam.getDescription())
                    .jlptLevel(exam.getJlptLevel())
                    .durationMinutes(exam.getDurationMinutes())
                    .totalQuestions((int) count)
                    .passingScore(exam.getPassingScore())
                    .status(exam.getStatus())
                    .ready(count > 0)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamTakeResponse getExamForTaking(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề thi JLPT"));

        List<QuestionDto> questions = questionReader.findQuestionsForTaking(examId);

        List<QuestionDto> vocabGrammar = new ArrayList<>();
        List<QuestionDto> reading = new ArrayList<>();
        List<QuestionDto> listening = new ArrayList<>();
        int totalPoints = 0;

        for (QuestionDto q : questions) {
            int points = q.getPoints() != null ? q.getPoints() : 2;
            totalPoints += points;

            String skillGroup = q.getSkillGroup() != null ? q.getSkillGroup() : "VOCAB_GRAMMAR";
            switch (skillGroup) {
                case "VOCAB_GRAMMAR" -> vocabGrammar.add(q);
                case "READING" -> reading.add(q);
                case "LISTENING" -> listening.add(q);
                default -> vocabGrammar.add(q);
            }
        }

        return ExamTakeResponse.builder()
                .examId(exam.getExamId())
                .examTitle(exam.getExamTitle() != null ? exam.getExamTitle() : exam.getTitle())
                .description(exam.getDescription())
                .examType(exam.getExamType())
                .jlptLevel(exam.getJlptLevel())
                .durationMinutes(exam.getDurationMinutes())
                .totalPoints(totalPoints)
                .passingScore(exam.getPassingScore())
                .totalQuestions(questions.size())
                .submitUrl("/exams/" + exam.getExamId() + "/submit")
                .questions(questions)
                .vocabGrammarQuestions(vocabGrammar)
                .readingQuestions(reading)
                .listeningQuestions(listening)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamTakeResponse getPlacementExamForTaking() {
        return getPlacementExamForTaking(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamTakeResponse getPlacementExamForTaking(String assignedLevel) {
        List<Exam> list = examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST");
        if (list == null || list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hiện chưa có bài đánh giá đầu vào nào trong hệ thống.");
        }

        Exam selected = null;
        if (assignedLevel != null && !assignedLevel.trim().isEmpty()) {
            for (Exam e : list) {
                if (assignedLevel.equalsIgnoreCase(e.getJlptLevel())) {
                    selected = e;
                    break;
                }
            }
        }
        if (selected == null) {
            selected = list.get(0);
        }

        long count = questionReader.countQuestionsByExamId(selected.getExamId());
        if (count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài đánh giá đầu vào hiện chưa có câu hỏi nào để làm bài.");
        }

        return getExamForTaking(selected.getExamId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamCardDto> getMilestoneExams(Long courseId) {
        List<Exam> exams = courseId != null
                ? examRepository.findByCourseIdAndExamTypeIgnoreCase(courseId, "MILESTONE")
                : examRepository.findByExamTypeIgnoreCase("MILESTONE");

        List<ExamCardDto> result = new ArrayList<>();
        for (Exam exam : exams) {
            long count = questionReader.countQuestionsByExamId(exam.getExamId());
            result.add(ExamCardDto.builder()
                    .examId(exam.getExamId())
                    .examTitle(exam.getExamTitle() != null ? exam.getExamTitle() : exam.getTitle())
                    .description(exam.getDescription())
                    .jlptLevel(exam.getJlptLevel())
                    .durationMinutes(exam.getDurationMinutes())
                    .totalQuestions((int) count)
                    .passingScore(exam.getPassingScore())
                    .status(exam.getStatus())
                    .ready(count > 0)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamTakeResponse getMilestoneExamForTaking(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài kiểm tra cột mốc"));

        if (!"MILESTONE".equalsIgnoreCase(exam.getExamType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề thi này không phải là bài kiểm tra cột mốc.");
        }

        long count = questionReader.countQuestionsByExamId(examId);
        if (count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài kiểm tra cột mốc này chưa có câu hỏi.");
        }

        return getExamForTaking(examId);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResultResponse evaluateSubmission(Long examId, Map<String, String> formParams, Integer timeSpentSeconds) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề thi"));

        List<QuestionDto> questions = questionReader.findQuestionsForEvaluation(examId);

        int totalPossibleScore = 0;
        int earnedScore = 0;
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;

        // Skill accumulators: [0]: earnedPoints, [1]: totalPoints, [2]: correctCount, [3]: totalQuestions
        Map<String, int[]> skillStats = new LinkedHashMap<>();
        skillStats.put("VOCAB_GRAMMAR", new int[]{0, 0, 0, 0});
        skillStats.put("READING", new int[]{0, 0, 0, 0});
        skillStats.put("LISTENING", new int[]{0, 0, 0, 0});

        List<QuestionDto> reviewQuestions = new ArrayList<>();

        for (QuestionDto q : questions) {
            int qPoints = q.getPoints() != null ? q.getPoints() : 2;
            totalPossibleScore += qPoints;

            String skillGroup = q.getSkillGroup() != null ? q.getSkillGroup() : "VOCAB_GRAMMAR";
            int[] stats = skillStats.computeIfAbsent(skillGroup, k -> new int[]{0, 0, 0, 0});
            stats[1] += qPoints; // total points
            stats[3] += 1;       // total questions

            String userOptionParam = formParams != null ? formParams.get("answer_" + q.getId()) : null;
            Long userOptionId = null;
            if (userOptionParam != null && !userOptionParam.trim().isEmpty()) {
                try {
                    userOptionId = Long.parseLong(userOptionParam.trim());
                } catch (NumberFormatException ignored) {}
            }

            OptionDto correctOption = null;
            if (q.getOptions() != null) {
                for (OptionDto opt : q.getOptions()) {
                    if (Boolean.TRUE.equals(opt.getCorrect())) {
                        correctOption = opt;
                        break;
                    }
                }
            }

            boolean isCorrect = false;
            if (userOptionId != null) {
                if (correctOption != null && userOptionId.equals(correctOption.getId())) {
                    isCorrect = true;
                    earnedScore += qPoints;
                    correctCount++;
                    stats[0] += qPoints;
                    stats[2] += 1;
                } else {
                    incorrectCount++;
                }
            } else {
                unansweredCount++;
            }

            reviewQuestions.add(QuestionDto.builder()
                    .id(q.getId())
                    .points(qPoints)
                    .category(q.getCategory())
                    .skillGroup(skillGroup)
                    .skillGroupLabel(q.getSkillGroupLabel())
                    .content(q.getContent())
                    .audioUrl(q.getAudioUrl())
                    .explanation(q.getExplanation())
                    .orderIndex(q.getOrderIndex())
                    .userAnswerId(userOptionId)
                    .isUserCorrect(isCorrect)
                    .options(q.getOptions())
                    .build());
        }

        BigDecimal percentage = BigDecimal.ZERO;
        if (totalPossibleScore > 0) {
            percentage = BigDecimal.valueOf((earnedScore * 100.0) / totalPossibleScore)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        boolean isPlacement = "PLACEMENT_TEST".equalsIgnoreCase(exam.getExamType());
        boolean isMilestone = "MILESTONE".equalsIgnoreCase(exam.getExamType());

        int passingScore = exam.getPassingScore() != null ? exam.getPassingScore() : (isMilestone ? 50 : 35);
        boolean passed = isPlacement || (earnedScore >= passingScore);

        int safeTime = timeSpentSeconds != null && timeSpentSeconds >= 0 ? timeSpentSeconds : 0;
        int minutes = safeTime / 60;
        int seconds = safeTime % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        // Build skill breakdown
        List<ExamResultResponse.SkillScoreDto> breakdown = new ArrayList<>();
        String strongestSkill = null;
        String weakestSkill = null;
        BigDecimal maxPct = BigDecimal.valueOf(-1);
        BigDecimal minPct = BigDecimal.valueOf(101);

        for (Map.Entry<String, int[]> entry : skillStats.entrySet()) {
            String group = entry.getKey();
            int[] s = entry.getValue();
            if (s[3] == 0) continue; // Skip skill groups with no questions in this exam

            BigDecimal skillPct = BigDecimal.ZERO;
            if (s[1] > 0) {
                skillPct = BigDecimal.valueOf((s[0] * 100.0) / s[1]).setScale(1, RoundingMode.HALF_UP);
            }

            String skillName = switch (group) {
                case "VOCAB_GRAMMAR" -> "Kiến thức ngôn ngữ (Từ vựng • Ngữ pháp • Kanji)";
                case "READING" -> "Đọc hiểu (Reading)";
                case "LISTENING" -> "Nghe hiểu (Listening)";
                default -> "Kiến thức tổng hợp";
            };

            breakdown.add(ExamResultResponse.SkillScoreDto.builder()
                    .skillGroup(group)
                    .skillName(skillName)
                    .earnedPoints(s[0])
                    .totalPoints(s[1])
                    .correctCount(s[2])
                    .totalQuestions(s[3])
                    .percentage(skillPct)
                    .build());

            if (skillPct.compareTo(maxPct) > 0) {
                maxPct = skillPct;
                strongestSkill = skillName;
            }
            if (skillPct.compareTo(minPct) < 0) {
                minPct = skillPct;
                weakestSkill = skillName;
            }
        }

        String aiFeedback = generateConstructiveFeedback(exam.getExamType(), passed, percentage, strongestSkill, weakestSkill, exam.getJlptLevel());

        return ExamResultResponse.builder()
                .examId(exam.getExamId())
                .examTitle(exam.getExamTitle() != null ? exam.getExamTitle() : exam.getTitle())
                .examType(exam.getExamType())
                .jlptLevel(exam.getJlptLevel())
                .score(earnedScore)
                .totalPossibleScore(totalPossibleScore)
                .percentage(percentage)
                .passed(passed)
                .passingScore(isPlacement ? null : passingScore)
                .timeSpentSeconds(safeTime)
                .formattedTimeSpent(formattedTime)
                .correctCount(correctCount)
                .incorrectCount(incorrectCount)
                .unansweredCount(unansweredCount)
                .totalQuestions(questions.size())
                .strongestSkill(strongestSkill)
                .weakestSkill(weakestSkill)
                .aiFeedback(aiFeedback)
                .persistentSaved(false)
                .detailsAvailable(true)
                .statusNotice(resolveStatusNotice(exam.getExamType(), passed))
                .skillBreakdown(breakdown)
                .questions(reviewQuestions)
                .build();
    }

    @Override
    @Transactional
    public ExamResultResponse evaluateAndSaveSubmission(Long examId, Long userId, Map<String, String> formParams, Integer timeSpentSeconds) {
        String dedupKey = null;
        if (userId != null && examId != null) {
            dedupKey = userId + "_" + examId + "_" + (formParams != null ? formParams.hashCode() : 0);
            SubmissionRecord existing = recentSubmissions.get(dedupKey);
            if (existing != null && (System.currentTimeMillis() - existing.timestamp) < 5000) {
                log.info("Deduplicated submission for user {} and exam {}", userId, examId);
                return existing.response;
            }
        }

        ExamResultResponse result = evaluateSubmission(examId, formParams, timeSpentSeconds);
        String nowFormatted = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        result.setCompletionDate(nowFormatted);

        if (userId != null) {
            try {
                boolean isPassed = Boolean.TRUE.equals(result.getPassed());
                // Supabase constraint exam_results_result_status_check only allows 'PASSED' or 'FAILED'
                String resultStatus = isPassed ? "PASSED" : "FAILED";

                Long resultId = resultWriter.saveExamResultSummary(
                        userId,
                        examId,
                        result.getScore(),
                        result.getPercentage(),
                        isPassed,
                        resultStatus,
                        timeSpentSeconds
                );
                result.setResultId(resultId);
                result.setPersistentSaved(true);
                result.setDetailsAvailable(true); // Active in current session

                // Persist question-level attempt details into Supabase exam_result_details
                resultWriter.saveExamResultDetails(resultId, result.getQuestions());
                result.setStatusNotice("Kết quả bài thi và chi tiết từng câu hỏi đã được lưu trữ vĩnh viễn vào hệ thống Supabase (Mã kết quả: #" + resultId + ").");

                if (dedupKey != null) {
                    recentSubmissions.put(dedupKey, new SubmissionRecord(System.currentTimeMillis(), result));
                    if (recentSubmissions.size() > 100) {
                        long cutoff = System.currentTimeMillis() - 30_000;
                        recentSubmissions.entrySet().removeIf(e -> e.getValue().timestamp < cutoff);
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to persist exam result to Supabase for userId {} and examId {}: {}", userId, examId, ex.getMessage(), ex);
                try {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                } catch (NoTransactionException ignored) {
                    // Ignored in non-transactional test environments
                }
                result.setResultId(null);
                result.setPersistentSaved(false);
                result.setDetailsAvailable(false);
                result.setStatusNotice("Kết quả bài thi được tính toán trong phiên làm việc. Chưa thể ghi nhận kết quả dài hạn do lỗi kết nối cơ sở dữ liệu.");
            }
        } else {
            result.setPersistentSaved(false);
            result.setStatusNotice("Bạn đang làm bài với tư cách khách. Kết quả này chỉ hiển thị trong phiên làm việc hiện tại và chưa được lưu vào lịch sử tài khoản.");
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamHistoryDto> getLearnerExamHistory(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return resultReader.findExamHistoryByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResultResponse getHistoricalResult(Long resultId, Long userId) {
        if (resultId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin yêu cầu không hợp lệ.");
        }

        Optional<ExamResultResponse> opt = resultReader.findExamResultByIdAndUserId(resultId, userId);
        if (opt.isPresent()) {
            ExamResultResponse res = opt.get();
            if (res.getExamId() != null) {
                List<QuestionDto> questions = questionReader.findQuestionsForEvaluation(res.getExamId());
                Map<Long, String> selectedAnswers = resultReader.findSelectedOptionsByResultId(resultId);
                int totalPoints = 0;
                for (QuestionDto q : questions) {
                    if (q.getPoints() != null) totalPoints += q.getPoints();
                    String selectedOpt = selectedAnswers.get(q.getId());
                    if (selectedOpt != null && !selectedOpt.isBlank()) {
                        try {
                            Long optId = Long.parseLong(selectedOpt.trim());
                            q.setUserAnswerId(optId);
                            boolean correct = q.getOptions() != null && q.getOptions().stream()
                                    .anyMatch(o -> o.getId().equals(optId) && Boolean.TRUE.equals(o.getCorrect()));
                            q.setIsUserCorrect(correct);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                res.setQuestions(questions);
                res.setTotalPossibleScore(totalPoints);
                res.setDetailsAvailable(!questions.isEmpty());
                res.setStatusNotice("Kết quả và chi tiết câu hỏi bài thi được tải từ cơ sở dữ liệu Supabase.");
            }
            return res;
        }

        // Kiểm tra xem kết quả có tồn tại cho người dùng khác không để phân biệt 403 Forbidden vs 404 Not Found
        if (resultReader.existsById(resultId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập kết quả bài thi này.");
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kết quả bài thi.");
    }

    private String resolveStatusNotice(String examType, boolean passed) {
        if (examType == null) return "Kết quả bài thi đã được ghi nhận.";
        return switch (examType.toUpperCase()) {
            case "PLACEMENT_TEST" -> "Đánh giá năng lực đầu vào hoàn tất. Hệ thống ghi nhận điểm số thực tế để làm cơ sở tư vấn học tập.";
            case "MILESTONE" -> passed
                    ? "Chúc mừng bạn đã hoàn thành bài kiểm tra cột mốc! Bạn đủ điều kiện tiếp tục các nội dung tiếp theo."
                    : "Bạn chưa đạt điểm yêu cầu cho bài kiểm tra cột mốc này. Hãy xem lại kiến thức và ôn tập thêm.";
            case "JLPT_MOCK" -> "Kết quả bài thi thử JLPT đã được tính toán từ dữ liệu đề thi Supabase.";
            default -> "Kết quả bài thi đã được ghi nhận.";
        };
    }

    private String generateConstructiveFeedback(String examType, boolean passed, BigDecimal percentage, String strongest, String weakest, String level) {
        StringBuilder sb = new StringBuilder();

        if ("PLACEMENT_TEST".equalsIgnoreCase(examType)) {
            sb.append("Bạn đã hoàn thành bài kiểm tra đánh giá năng lực đầu vào với tỷ lệ chính xác ").append(percentage).append("%. ");
            sb.append("Điểm số thực tế này phản ánh năng lực hiện tại của bạn. ");
            if (strongest != null) {
                sb.append("Thế mạnh của bạn nằm ở: ").append(strongest).append(". ");
            }
            if (weakest != null) {
                sb.append("Khu vực cần bổ trợ thêm là: ").append(weakest).append(". ");
            }
            sb.append("Dựa trên kết quả này, bạn có thể lựa chọn các khóa học phù hợp với trình độ mục tiêu.");
            return sb.toString();
        }

        if (passed) {
            sb.append("Chúc mừng bạn đã đạt yêu cầu bài thi ").append(level != null ? level : "").append("! ");
            sb.append("Bạn đạt tỷ lệ chính xác ").append(percentage).append("%. ");
        } else {
            sb.append("Bạn chưa đạt điểm sàn bài thi ").append(level != null ? level : "").append(" lần này. ");
            sb.append("Điểm số của bạn đạt ").append(percentage).append("% tổng điểm. ");
        }

        if (strongest != null) {
            sb.append("Kỹ năng nổi bật nhất của bạn là: ").append(strongest).append(". ");
        }
        if (weakest != null) {
            sb.append("Kỹ năng cần tập trung ôn tập thêm là: ").append(weakest).append(". ");
        }
        sb.append("Hãy xem lại phần giải thích chi tiết bên dưới cho từng câu hỏi để củng cố kiến thức.");
        return sb.toString();
    }
}
