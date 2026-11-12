package com.jlpp.modules.exam.service.impl;

import com.jlpp.modules.exam.dto.*;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.reader.LearnerExamResultReader;
import com.jlpp.modules.exam.reader.LearnerQuestionReader;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.exam.service.JlptExamStructure;
import com.jlpp.modules.exam.writer.LearnerExamResultWriter;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final LearnerQuestionReader questionReader;
    private final LearnerExamResultWriter resultWriter;
    private final LearnerExamResultReader resultReader;
    private final AssessmentScoringService scoringService;

    public ExamServiceImpl(ExamRepository examRepository,
                           LearnerQuestionReader questionReader,
                           LearnerExamResultWriter resultWriter,
                           LearnerExamResultReader resultReader) {
        this(examRepository, questionReader, resultWriter, resultReader, new AssessmentScoringServiceImpl());
    }

    @Autowired
    public ExamServiceImpl(ExamRepository examRepository,
                           LearnerQuestionReader questionReader,
                           LearnerExamResultWriter resultWriter,
                           LearnerExamResultReader resultReader,
                           AssessmentScoringService scoringService) {
        this.examRepository = examRepository;
        this.questionReader = questionReader;
        this.resultWriter = resultWriter;
        this.resultReader = resultReader;
        this.scoringService = scoringService;
    }

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
        int totalPoints = 180; // thang JLPT cố định

        for (QuestionDto q : questions) {
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

        if ((exams == null || exams.isEmpty()) && courseId != null) {
            exams = examRepository.findByExamTypeIgnoreCase("MILESTONE");
        }
        if (exams == null) {
            exams = Collections.emptyList();
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
    public String resolveMilestoneExamUrl(Long courseId, String courseLevel, Long lessonId) {
        List<Exam> candidates = new ArrayList<>();
        if (courseId != null) {
            List<Exam> byCourse = examRepository.findByCourseIdAndExamTypeIgnoreCase(courseId, "MILESTONE");
            if (byCourse != null && !byCourse.isEmpty()) {
                candidates.addAll(byCourse);
            }
        }
        if (candidates.isEmpty() && courseLevel != null && !courseLevel.isBlank()) {
            List<Exam> byLevel = examRepository.findByExamTypeIgnoreCaseAndJlptLevelIgnoreCase("MILESTONE", courseLevel.trim());
            if (byLevel != null && !byLevel.isEmpty()) {
                candidates.addAll(byLevel);
            }
        }
        if (candidates.isEmpty()) {
            List<Exam> allMilestones = examRepository.findByExamTypeIgnoreCase("MILESTONE");
            if (allMilestones != null && !allMilestones.isEmpty()) {
                candidates.addAll(allMilestones);
            }
        }

        Exam selected = null;
        for (Exam exam : candidates) {
            if (exam != null && exam.getExamId() != null && questionReader.countQuestionsByExamId(exam.getExamId()) > 0) {
                selected = exam;
                break;
            }
        }
        if (selected == null && !candidates.isEmpty()) {
            selected = candidates.get(0);
        }

        StringBuilder query = new StringBuilder();
        if (courseId != null) {
            query.append("?courseId=").append(courseId);
            if (lessonId != null) {
                query.append("&lessonId=").append(lessonId);
            }
        } else if (lessonId != null) {
            query.append("?lessonId=").append(lessonId);
        }

        if (selected != null && selected.getExamId() != null) {
            return "/exams/milestone/" + selected.getExamId() + query;
        }
        return "/exams/milestones" + query;
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

        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;

        Map<String, Integer> correctMap = new HashMap<>();
        Map<String, Integer> totalMap = new HashMap<>();

        for (QuestionDto q : questions) {
            String skillGroup = q.getSkillGroup() != null ? q.getSkillGroup() : "VOCAB_GRAMMAR";
            totalMap.put(skillGroup, totalMap.getOrDefault(skillGroup, 0) + 1);

            String userOptionParam = extractUserOptionParam(formParams, q.getId());
            OptionDto selectedOption = null;
            Long userOptionId = null;

            if (userOptionParam != null && !userOptionParam.trim().isEmpty()) {
                try {
                    userOptionId = Long.parseLong(userOptionParam.trim());
                    if (q.getOptions() != null) {
                        for (OptionDto opt : q.getOptions()) {
                            if (opt.getId() != null && opt.getId().equals(userOptionId)) {
                                selectedOption = opt;
                                break;
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            boolean isCorrect = false;
            boolean isUnanswered = (userOptionParam == null || userOptionParam.trim().isEmpty() || selectedOption == null);

            if (isUnanswered) {
                unansweredCount++;
            } else {
                if (Boolean.TRUE.equals(selectedOption.getCorrect())) {
                    isCorrect = true;
                    correctCount++;
                    correctMap.put(skillGroup, correctMap.getOrDefault(skillGroup, 0) + 1);
                } else {
                    incorrectCount++;
                }
            }

            q.setUserAnswerId(userOptionId);
            q.setIsUserCorrect(isCorrect);
        }

        JlptExamStructure jlpt = JlptExamStructure.of(exam.getJlptLevel());

        // Xây nhóm chấm
        record Group(String name, int max, String... skills) {
            int correct(Map<String, Integer> correctMap) {
                int sum = 0;
                for (String s : skills) sum += correctMap.getOrDefault(s, 0);
                return sum;
            }
            int total(Map<String, Integer> totalMap) {
                int sum = 0;
                for (String s : skills) sum += totalMap.getOrDefault(s, 0);
                return sum;
            }
        }

        List<Group> groups = jlpt.languageReadingCombined()
            ? List.of(new Group("Ngôn ngữ + Đọc hiểu", 120, "VOCAB_GRAMMAR", "READING"),
                      new Group("Nghe hiểu", 60, "LISTENING"))
            : List.of(new Group("Kiến thức ngôn ngữ", 60, "VOCAB_GRAMMAR"),
                      new Group("Đọc hiểu", 60, "READING"),
                      new Group("Nghe hiểu", 60, "LISTENING"));

        // Bỏ nhóm không có câu nào (đề milestone thiếu phần)
        List<Group> present = groups.stream().filter(g -> g.total(totalMap) > 0).toList();
        int presentMax = present.stream().mapToInt(Group::max).sum();
        if (presentMax == 0) {
            presentMax = JlptExamStructure.TOTAL_SCORE;
        }

        int score = 0;
        boolean allAboveMin = true;
        List<ExamResultResponse.SkillScoreDto> breakdown = new ArrayList<>();
        String failedMinSection = null;
        int failedMinEarned = 0;
        int failedMinRequired = 0;

        for (Group g : present) {
            int max = g.max() * JlptExamStructure.TOTAL_SCORE / presentMax;  // chia lại nếu thiếu nhóm
            int earned = g.total(totalMap) > 0
                    ? (int) Math.round(g.correct(correctMap) * (double) max / g.total(totalMap))
                    : 0;
            int min = (int) Math.ceil(max * JlptExamStructure.SECTION_MIN_SCORE / 60.0);
            score += earned;
            if (earned < min) {
                allAboveMin = false;
                if (failedMinSection == null) {
                    failedMinSection = g.name();
                    failedMinEarned = earned;
                    failedMinRequired = min;
                }
            }

            BigDecimal pct = BigDecimal.valueOf(max > 0 ? earned * 100.0 / max : 0)
                    .setScale(1, RoundingMode.HALF_UP);

            breakdown.add(ExamResultResponse.SkillScoreDto.builder()
                    .skillGroup(String.join("+", g.skills()))
                    .skillName(g.name())
                    .earnedPoints(earned)
                    .totalPoints(max)
                    .correctCount(g.correct(correctMap))
                    .totalQuestions(g.total(totalMap))
                    .percentage(pct)
                    .build());
        }

        int totalPossibleScore = 180;  // LUÔN là 180
        BigDecimal percentage = BigDecimal.valueOf(score * 100.0 / 180).setScale(1, RoundingMode.HALF_UP);

        boolean isPlacement = "PLACEMENT_TEST".equalsIgnoreCase(exam.getExamType());
        boolean isMilestone = "MILESTONE".equalsIgnoreCase(exam.getExamType());
        int defaultPassing = defaultPassingScore(exam.getJlptLevel(), isMilestone);
        int passingScore = exam.getPassingScore() != null ? exam.getPassingScore() : defaultPassing;
        boolean passed = isPlacement || (score >= passingScore && allAboveMin);

        // strongestSkill / weakestSkill: phân tích chính xác theo tỷ lệ % và các kỹ năng thực tế của bài thi
        SkillAnalysisResult skillAnalysis = analyzeSkills(breakdown);
        String strongestSkill = skillAnalysis.strongestSkill();
        String weakestSkill = skillAnalysis.weakestSkill();

        int safeTime = timeSpentSeconds != null && timeSpentSeconds >= 0 ? timeSpentSeconds : 0;
        int minutes = safeTime / 60;
        int seconds = safeTime % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        String feedback = generateConstructiveFeedback(exam.getExamType(), passed, percentage,
                strongestSkill, weakestSkill, exam.getJlptLevel(),
                failedMinSection, failedMinEarned, failedMinRequired, score, passingScore);

        return ExamResultResponse.builder()
                .examId(exam.getExamId())
                .examTitle(exam.getExamTitle() != null ? exam.getExamTitle() : exam.getTitle())
                .examType(exam.getExamType())
                .jlptLevel(exam.getJlptLevel())
                .score(score)
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
                .aiFeedback(feedback)
                .recommendation(generateRecommendation(exam.getExamType(), passed, weakestSkill))
                .persistentSaved(false)
                .detailsAvailable(true)
                .statusNotice(resolveStatusNotice(exam.getExamType(), passed))
                .skillBreakdown(breakdown)
                .questions(questions)
                .build();
    }

    @Override
    @Transactional
    public ExamResultResponse evaluateAndSaveSubmission(Long examId, Long userId, Map<String, String> formParams, Integer timeSpentSeconds) {
        String dedupKey = null;
        if (userId != null && examId != null) {
            dedupKey = userId + "_" + examId + "_" + (formParams != null ? new TreeMap<>(formParams).toString().hashCode() : 0);
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
                    long now = System.currentTimeMillis();
                    recentSubmissions.put(dedupKey, new SubmissionRecord(now, result));
                    long cutoff = now - 30_000;
                    recentSubmissions.entrySet().removeIf(e -> e.getValue().timestamp < cutoff);
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

                res.setTotalPossibleScore(180);
                int correctCount = 0;
                int incorrectCount = 0;
                int unansweredCount = 0;

                Map<String, Integer> correctMap = new HashMap<>();
                Map<String, Integer> totalMap = new HashMap<>();

                for (QuestionDto q : questions) {
                    String skillGroup = q.getSkillGroup() != null ? q.getSkillGroup() : "VOCAB_GRAMMAR";
                    totalMap.put(skillGroup, totalMap.getOrDefault(skillGroup, 0) + 1);

                    String selectedOpt = (selectedAnswers != null) ? selectedAnswers.get(q.getId()) : null;
                    if (selectedOpt != null && !selectedOpt.isBlank()) {
                        try {
                            Long optId = Long.parseLong(selectedOpt.trim());
                            q.setUserAnswerId(optId);
                            boolean correct = q.getOptions() != null && q.getOptions().stream()
                                    .anyMatch(o -> o.getId().equals(optId) && Boolean.TRUE.equals(o.getCorrect()));
                            q.setIsUserCorrect(correct);
                            if (correct) {
                                correctCount++;
                                correctMap.put(skillGroup, correctMap.getOrDefault(skillGroup, 0) + 1);
                            } else {
                                incorrectCount++;
                            }
                        } catch (NumberFormatException ignored) {
                            unansweredCount++;
                        }
                    } else {
                        unansweredCount++;
                    }
                }

                res.setQuestions(questions);
                res.setTotalQuestions(questions.size());
                res.setCorrectCount(correctCount);
                res.setIncorrectCount(incorrectCount);
                res.setUnansweredCount(unansweredCount);

                // Build JLPT breakdown for historical view
                JlptExamStructure jlpt = JlptExamStructure.of(res.getJlptLevel());
                record Group(String name, int max, String... skills) {
                    int correct(Map<String, Integer> correctMap) {
                        int sum = 0;
                        for (String s : skills) sum += correctMap.getOrDefault(s, 0);
                        return sum;
                    }
                    int total(Map<String, Integer> totalMap) {
                        int sum = 0;
                        for (String s : skills) sum += totalMap.getOrDefault(s, 0);
                        return sum;
                    }
                }

                List<Group> groups = jlpt.languageReadingCombined()
                        ? List.of(new Group("Ngôn ngữ + Đọc hiểu", 120, "VOCAB_GRAMMAR", "READING"),
                                  new Group("Nghe hiểu", 60, "LISTENING"))
                        : List.of(new Group("Kiến thức ngôn ngữ", 60, "VOCAB_GRAMMAR"),
                                  new Group("Đọc hiểu", 60, "READING"),
                                  new Group("Nghe hiểu", 60, "LISTENING"));

                List<Group> present = groups.stream().filter(g -> g.total(totalMap) > 0).toList();
                int presentMax = present.stream().mapToInt(Group::max).sum();
                if (presentMax == 0) presentMax = JlptExamStructure.TOTAL_SCORE;

                List<ExamResultResponse.SkillScoreDto> breakdown = new ArrayList<>();
                String failedMinSection = null;
                int failedMinEarned = 0;
                int failedMinRequired = 0;

                for (Group g : present) {
                    int max = g.max() * JlptExamStructure.TOTAL_SCORE / presentMax;
                    int minScore = (int) Math.ceil(max * (double) JlptExamStructure.SECTION_MIN_SCORE / 60.0);
                    int earned = g.total(totalMap) > 0 ? (int) Math.round(g.correct(correctMap) * (double) max / g.total(totalMap)) : 0;
                    if (earned < minScore && failedMinSection == null) {
                        failedMinSection = g.name();
                        failedMinEarned = earned;
                        failedMinRequired = minScore;
                    }
                    BigDecimal pct = BigDecimal.valueOf(max > 0 ? earned * 100.0 / max : 0).setScale(1, RoundingMode.HALF_UP);
                    breakdown.add(ExamResultResponse.SkillScoreDto.builder()
                            .skillGroup(String.join("+", g.skills()))
                            .skillName(g.name())
                            .earnedPoints(earned)
                            .totalPoints(max)
                            .correctCount(g.correct(correctMap))
                            .totalQuestions(g.total(totalMap))
                            .percentage(pct)
                            .build());
                }

                SkillAnalysisResult skillAnalysis = analyzeSkills(breakdown);
                String strongestSkill = skillAnalysis.strongestSkill();
                String weakestSkill = skillAnalysis.weakestSkill();

                int histScore = res.getScore() != null ? res.getScore() : 0;
                BigDecimal histPct = res.getPercentage() != null ? res.getPercentage() : BigDecimal.ZERO;
                boolean histPassed = Boolean.TRUE.equals(res.getPassed());
                boolean isMilestone = "MILESTONE".equalsIgnoreCase(res.getExamType());
                int histPassingScore = res.getPassingScore() != null
                        ? res.getPassingScore()
                        : defaultPassingScore(res.getJlptLevel(), isMilestone);

                res.setStrongestSkill(strongestSkill);
                res.setWeakestSkill(weakestSkill);
                res.setAiFeedback(generateConstructiveFeedback(res.getExamType(), histPassed, histPct,
                        strongestSkill, weakestSkill, res.getJlptLevel(),
                        failedMinSection, failedMinEarned, failedMinRequired, histScore, histPassingScore));
                res.setRecommendation(generateRecommendation(res.getExamType(), histPassed, weakestSkill));
                res.setSkillBreakdown(breakdown);
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

    private int defaultPassingScore(String jlptLevel, boolean isMilestone) {
        if (isMilestone) return 50;
        return JlptExamStructure.of(jlptLevel).passingScore();
    }

    private String extractUserOptionParam(Map<String, String> formParams, Long questionId) {
        if (formParams == null || questionId == null) {
            return null;
        }
        String[] possibleKeys = {
                "answer_" + questionId,
                "answer[" + questionId + "]",
                "q_" + questionId,
                "question_" + questionId,
                String.valueOf(questionId)
        };
        for (String key : possibleKeys) {
            if (formParams.containsKey(key)) {
                return formParams.get(key);
            }
        }
        return null;
    }

    private String generateRecommendation(String examType, boolean passed, String weakest) {
        if ("PLACEMENT_TEST".equalsIgnoreCase(examType)) {
            return "Xem lộ trình học tập gợi ý dựa trên kết quả đánh giá năng lực.";
        }
        if (passed) {
            return "Tiếp tục duy trì phong độ và luyện tập thêm các đề thi cấp độ cao hơn.";
        }
        if (weakest != null) {
            return "Tập trung ôn luyện thêm kỹ năng " + weakest + " trước khi thi lại.";
        }
        return "Ôn tập lại các phần kiến thức chưa đạt và thử sức lại với đề thi này.";
    }

    private String generateConstructiveFeedback(String examType, boolean passed, BigDecimal percentage,
                                                String strongest, String weakest, String level,
                                                String failedMinSection, int failedMinEarned, int failedMinRequired,
                                                int score, int passingScore) {
        StringBuilder sb = new StringBuilder();

        if ("PLACEMENT_TEST".equalsIgnoreCase(examType)) {
            sb.append("Bạn đã hoàn thành bài kiểm tra đánh giá năng lực đầu vào với tỷ lệ chính xác ").append(percentage).append("%. ");
            sb.append("Điểm số thực tế này phản ánh năng lực hiện tại của bạn. ");
            if (strongest != null) {
                sb.append("Thế mạnh của bạn nằm ở: ").append(strongest).append(". ");
            } else {
                sb.append("Hiện chưa có câu trả lời đúng nào để xác định thế mạnh nổi bật. ");
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
            if (failedMinSection != null && score >= passingScore) {
                sb.append("Bạn chưa đạt điểm sàn bài thi ").append(level != null ? level : "").append(" lần này. ");
                sb.append("Tổng điểm của bạn đạt ").append(score).append("/180 (đủ điểm đỗ ").append(passingScore)
                  .append("), nhưng phần ").append(failedMinSection).append(" chỉ đạt ")
                  .append(failedMinEarned).append(" điểm, chưa đạt mức tối thiểu ")
                  .append(failedMinRequired).append(" điểm.");
                return sb.toString();
            } else {
                sb.append("Bạn chưa đạt điểm sàn bài thi ").append(level != null ? level : "").append(" lần này. ");
                sb.append("Điểm số của bạn đạt ").append(percentage).append("% tổng điểm. ");
            }
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

    private SkillAnalysisResult analyzeSkills(List<ExamResultResponse.SkillScoreDto> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return new SkillAnalysisResult(null, null);
        }

        // 1. Tìm maxPct (Kỹ năng tốt nhất)
        BigDecimal maxPct = BigDecimal.ZERO;
        for (ExamResultResponse.SkillScoreDto skill : breakdown) {
            BigDecimal p = skill.getPercentage() != null ? skill.getPercentage() : BigDecimal.ZERO;
            if (p.compareTo(maxPct) > 0) {
                maxPct = p;
            }
        }

        String strongestSkill = null;
        if (maxPct.compareTo(BigDecimal.ZERO) > 0) {
            List<String> strongList = new ArrayList<>();
            for (ExamResultResponse.SkillScoreDto skill : breakdown) {
                BigDecimal p = skill.getPercentage() != null ? skill.getPercentage() : BigDecimal.ZERO;
                if (p.compareTo(maxPct) == 0) {
                    strongList.add(skill.getSkillName());
                }
            }
            strongestSkill = String.join(", ", strongList);
        }

        // 2. Tìm minPct (Kỹ năng thấp nhất)
        BigDecimal minPct = BigDecimal.valueOf(101);
        for (ExamResultResponse.SkillScoreDto skill : breakdown) {
            BigDecimal p = skill.getPercentage() != null ? skill.getPercentage() : BigDecimal.ZERO;
            if (p.compareTo(minPct) < 0) {
                minPct = p;
            }
        }

        String weakestSkill = null;
        // Nếu tất cả các kỹ năng đều đạt >= 75%: không có kỹ năng yếu
        if (minPct.compareTo(BigDecimal.valueOf(75)) >= 0) {
            weakestSkill = null;
        } else {
            List<String> weakList = new ArrayList<>();
            for (ExamResultResponse.SkillScoreDto skill : breakdown) {
                BigDecimal p = skill.getPercentage() != null ? skill.getPercentage() : BigDecimal.ZERO;
                if (p.compareTo(minPct) == 0) {
                    weakList.add(skill.getSkillName());
                }
            }

            // Nếu TẤT CẢ các kỹ năng đều có điểm bằng nhau và < 75% (đặc biệt khi đều 0%)
            if (weakList.size() == breakdown.size() && breakdown.size() > 1) {
                weakestSkill = "Toàn diện các kỹ năng (" + String.join(", ", weakList) + ")";
            } else {
                weakestSkill = String.join(", ", weakList);
            }
        }

        return new SkillAnalysisResult(strongestSkill, weakestSkill);
    }

    private record SkillAnalysisResult(String strongestSkill, String weakestSkill) {}
}
