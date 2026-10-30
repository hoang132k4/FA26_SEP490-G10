package com.jlpp.modules.quiz.service;

import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.quiz.dto.QuestionOptionDto;
import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.dto.QuizQuestionReviewDto;
import com.jlpp.modules.quiz.dto.QuizResultDto;
import com.jlpp.modules.quiz.entity.*;
import com.jlpp.modules.quiz.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final ExerciseRepository exerciseRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final ExerciseResultDetailRepository exerciseResultDetailRepository;
    private final LessonRepository lessonRepository;
    private final QuestionBankRepository questionBankRepository;

    @Override
    @Transactional
    public Exercise getOrCreateExerciseForLesson(Long lessonId) {
        if (lessonId == null) {
            throw new IllegalArgumentException("lessonId không được để trống.");
        }

        Optional<Exercise> existing = exerciseRepository.findByLessonId(lessonId);
        if (existing.isPresent()) {
            Exercise ex = existing.get();
            List<ExerciseQuestion> eqs = exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(ex.getExerciseId());
            if (eqs.isEmpty()) {
                seedQuizQuestionsForLesson(lessonId);
            }
            return ex;
        }

        // Create new Exercise record for this lesson
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        String title = (lesson != null)
                ? "Bài tập trắc nghiệm: " + lesson.getTitle()
                : "Bài tập trắc nghiệm bài #" + lessonId;

        Exercise newExercise = Exercise.builder()
                .lessonId(lessonId)
                .exerciseTitle(title)
                .totalQuestions(5)
                .passingScore(60)
                .status("PUBLISHED")
                .build();

        Exercise saved = exerciseRepository.save(newExercise);
        seedQuizQuestionsForLesson(lessonId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestionDto> getQuestionsForExercise(Long exerciseId) {
        List<ExerciseQuestion> eqs = exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exerciseId);
        if (eqs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> qIds = eqs.stream().map(ExerciseQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionRepository.findByIdIn(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, QuestionBank> qbMap = questionBankRepository.findByQuestionIdIn(qIds).stream()
                .collect(Collectors.toMap(QuestionBank::getQuestionId, q -> q));

        List<QuestionOption> allOptions = questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(qIds);
        Map<Long, List<QuestionOption>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        List<QuizQuestionDto> result = new ArrayList<>();
        int order = 1;
        for (ExerciseQuestion eq : eqs) {
            Long qId = eq.getQuestionId();
            Question q = questionMap.get(qId);
            QuestionBank qb = qbMap.get(qId);
            if (q == null && qb == null) continue;

            String content = (q != null && q.getContent() != null) ? q.getContent() : (qb != null ? qb.getQuestionText() : "");
            String explanation = (q != null && q.getExplanation() != null) ? q.getExplanation() : (qb != null ? qb.getExplanation() : "");
            String category = (q != null && q.getCategory() != null) ? q.getCategory() : (qb != null ? qb.getCategory() : "");
            String jlptLevel = (q != null && q.getJlptLevel() != null) ? q.getJlptLevel() : (qb != null ? qb.getJlptLevel() : "");
            String audioUrl = (q != null && q.getAudioUrl() != null) ? q.getAudioUrl() : (qb != null ? qb.getAudioUrl() : null);

            List<QuestionOptionDto> optDtos = optionMap.getOrDefault(qId, Collections.emptyList()).stream()
                    .map(o -> QuestionOptionDto.builder()
                            .id(o.getId())
                            .label(o.getLabel())
                            .optionText(o.getOptionText())
                            .isCorrect(o.getIsCorrect())
                            .build())
                    .collect(Collectors.toList());

            result.add(QuizQuestionDto.builder()
                    .questionId(qId)
                    .orderIndex(order++)
                    .content(content)
                    .explanation(explanation)
                    .category(category)
                    .jlptLevel(jlptLevel)
                    .audioUrl(audioUrl)
                    .options(optDtos)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public QuizResultDto submitQuiz(Long exerciseId, Long userId, Map<Long, String> answers) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập ID: " + exerciseId));

        List<ExerciseQuestion> eqs = exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exerciseId);
        List<Long> qIds = eqs.stream().map(ExerciseQuestion::getQuestionId).collect(Collectors.toList());

        Map<Long, Question> questionMap = questionRepository.findByIdIn(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, QuestionBank> qbMap = questionBankRepository.findByQuestionIdIn(qIds).stream()
                .collect(Collectors.toMap(QuestionBank::getQuestionId, q -> q));

        List<QuestionOption> allOptions = questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(qIds);
        Map<Long, List<QuestionOption>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        int totalQuestions = eqs.size();
        int correctCount = 0;
        List<QuizQuestionReviewDto> reviews = new ArrayList<>();
        List<ExerciseResultDetail> detailsToSave = new ArrayList<>();

        int order = 1;
        for (ExerciseQuestion eq : eqs) {
            Long qId = eq.getQuestionId();
            Question q = questionMap.get(qId);
            QuestionBank qb = qbMap.get(qId);
            if (q == null && qb == null) continue;

            String content = (q != null && q.getContent() != null) ? q.getContent() : (qb != null ? qb.getQuestionText() : "");
            String explanation = (q != null && q.getExplanation() != null) ? q.getExplanation() : (qb != null ? qb.getExplanation() : "");
            String category = (q != null && q.getCategory() != null) ? q.getCategory() : (qb != null ? qb.getCategory() : "");
            String jlptLevel = (q != null && q.getJlptLevel() != null) ? q.getJlptLevel() : (qb != null ? qb.getJlptLevel() : "");

            List<QuestionOption> opts = optionMap.getOrDefault(qId, Collections.emptyList());
            String selected = answers != null ? answers.get(qId) : null;
            if (selected != null) selected = selected.trim();

            QuestionOption correctOpt = opts.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).findFirst().orElse(null);
            String correctLabel = (correctOpt != null) ? correctOpt.getLabel() : "A";

            boolean isCorrect = false;
            if (selected != null && correctOpt != null) {
                // Match either label ("A" vs "A") or option id or label prefix ("1." vs "1.")
                isCorrect = selected.equalsIgnoreCase(correctOpt.getLabel())
                        || selected.equalsIgnoreCase(String.valueOf(correctOpt.getId()))
                        || selected.equalsIgnoreCase(correctOpt.getOptionText());
            }

            if (isCorrect) correctCount++;

            List<QuestionOptionDto> optDtos = opts.stream()
                    .map(o -> QuestionOptionDto.builder()
                            .id(o.getId())
                            .label(o.getLabel())
                            .optionText(o.getOptionText())
                            .isCorrect(o.getIsCorrect())
                            .build())
                    .collect(Collectors.toList());

            reviews.add(QuizQuestionReviewDto.builder()
                    .questionId(qId)
                    .orderIndex(order++)
                    .content(content)
                    .explanation(explanation)
                    .category(category)
                    .jlptLevel(jlptLevel)
                    .options(optDtos)
                    .userSelectedLabel(selected)
                    .correctLabel(correctLabel)
                    .isUserCorrect(isCorrect)
                    .build());
        }

        int score = correctCount * 10;
        BigDecimal percentage = (totalQuestions > 0)
                ? BigDecimal.valueOf((correctCount * 100.0) / totalQuestions).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int passingThreshold = exercise.getPassingScore() != null ? exercise.getPassingScore() : 60;
        boolean passed = percentage.doubleValue() >= passingThreshold;
        String status = passed ? "PASSED" : "FAILED";

        String feedback = passed
                ? "Xuất sắc! Bạn đã vượt qua bài trắc nghiệm với điểm số ấn tượng. Hãy tiếp tục phát huy ở các bài học tiếp theo!"
                : "Bạn chưa đạt điểm chuẩn tối thiểu (" + passingThreshold + "%). Hãy xem lại các câu giải thích bên dưới và thử làm lại nhé!";

        // Save Attempt to exercise_results
        ExerciseResult exResult = ExerciseResult.builder()
                .exerciseId(exerciseId)
                .userId(userId != null ? userId : 1L)
                .attemptNumber(1)
                .score(score)
                .percentage(percentage)
                .resultStatus(status)
                .submittedDate(ZonedDateTime.now())
                .feedback(feedback)
                .build();

        ExerciseResult savedResult = exerciseResultRepository.save(exResult);

        // Save Details
        for (QuizQuestionReviewDto rev : reviews) {
            ExerciseResultDetail detail = ExerciseResultDetail.builder()
                    .exerciseResultId(savedResult.getExerciseResultId())
                    .questionId(rev.getQuestionId())
                    .selectedOption(rev.getUserSelectedLabel() != null ? rev.getUserSelectedLabel() : "NONE")
                    .isCorrect(rev.isUserCorrect())
                    .build();
            detailsToSave.add(detail);
        }
        exerciseResultDetailRepository.saveAll(detailsToSave);

        Lesson lesson = exercise.getLessonId() != null ? lessonRepository.findById(exercise.getLessonId()).orElse(null) : null;

        return QuizResultDto.builder()
                .resultId(savedResult.getExerciseResultId())
                .exerciseId(exerciseId)
                .lessonId(exercise.getLessonId())
                .exerciseTitle(exercise.getExerciseTitle())
                .lessonTitle(lesson != null ? lesson.getTitle() : "Bài học")
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .score(score)
                .percentage(percentage)
                .passed(passed)
                .resultStatus(status)
                .feedback(feedback)
                .submittedDate(savedResult.getSubmittedDate())
                .questions(reviews)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResultDto getResult(Long resultId) {
        ExerciseResult result = exerciseResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả làm bài ID: " + resultId));

        Exercise exercise = exerciseRepository.findById(result.getExerciseId()).orElse(null);
        Lesson lesson = (exercise != null && exercise.getLessonId() != null)
                ? lessonRepository.findById(exercise.getLessonId()).orElse(null) : null;

        List<ExerciseResultDetail> details = exerciseResultDetailRepository.findByExerciseResultId(resultId);
        List<Long> qIds = details.stream().map(ExerciseResultDetail::getQuestionId).collect(Collectors.toList());

        Map<Long, Question> qMap = questionRepository.findByIdIn(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, QuestionBank> qbMap = questionBankRepository.findByQuestionIdIn(qIds).stream()
                .collect(Collectors.toMap(QuestionBank::getQuestionId, q -> q));

        List<QuestionOption> allOpts = questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscLabelAsc(qIds);
        Map<Long, List<QuestionOption>> optMap = allOpts.stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        List<QuizQuestionReviewDto> reviews = new ArrayList<>();
        int order = 1;
        int correctCount = 0;
        for (ExerciseResultDetail d : details) {
            Long qId = d.getQuestionId();
            Question q = qMap.get(qId);
            QuestionBank qb = qbMap.get(qId);
            if (q == null && qb == null) continue;

            if (Boolean.TRUE.equals(d.getIsCorrect())) correctCount++;

            String content = (q != null && q.getContent() != null) ? q.getContent() : (qb != null ? qb.getQuestionText() : "");
            String explanation = (q != null && q.getExplanation() != null) ? q.getExplanation() : (qb != null ? qb.getExplanation() : "");
            String category = (q != null && q.getCategory() != null) ? q.getCategory() : (qb != null ? qb.getCategory() : "");
            String jlptLevel = (q != null && q.getJlptLevel() != null) ? q.getJlptLevel() : (qb != null ? qb.getJlptLevel() : "");

            List<QuestionOption> opts = optMap.getOrDefault(qId, Collections.emptyList());
            QuestionOption correctOpt = opts.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).findFirst().orElse(null);

            List<QuestionOptionDto> optDtos = opts.stream()
                    .map(o -> QuestionOptionDto.builder()
                            .id(o.getId())
                            .label(o.getLabel())
                            .optionText(o.getOptionText())
                            .isCorrect(o.getIsCorrect())
                            .build())
                    .collect(Collectors.toList());

            reviews.add(QuizQuestionReviewDto.builder()
                    .questionId(qId)
                    .orderIndex(order++)
                    .content(content)
                    .explanation(explanation)
                    .category(category)
                    .jlptLevel(jlptLevel)
                    .options(optDtos)
                    .userSelectedLabel(d.getSelectedOption())
                    .correctLabel(correctOpt != null ? correctOpt.getLabel() : "A")
                    .isUserCorrect(Boolean.TRUE.equals(d.getIsCorrect()))
                    .build());
        }

        return QuizResultDto.builder()
                .resultId(result.getExerciseResultId())
                .exerciseId(result.getExerciseId())
                .lessonId(exercise != null ? exercise.getLessonId() : null)
                .exerciseTitle(exercise != null ? exercise.getExerciseTitle() : "Bài tập trắc nghiệm")
                .lessonTitle(lesson != null ? lesson.getTitle() : "Bài học")
                .totalQuestions(details.size())
                .correctCount(correctCount)
                .score(result.getScore() != null ? result.getScore() : 0)
                .percentage(result.getPercentage() != null ? result.getPercentage() : BigDecimal.ZERO)
                .passed("PASSED".equalsIgnoreCase(result.getResultStatus()))
                .resultStatus(result.getResultStatus())
                .feedback(result.getFeedback())
                .submittedDate(result.getSubmittedDate())
                .questions(reviews)
                .build();
    }

    @Override
    @Transactional
    public void seedQuizQuestionsForLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        String level = (lesson != null && lesson.getCourse() != null && lesson.getCourse().getLevel() != null)
                ? lesson.getCourse().getLevel() : "N4";

        Exercise exercise = exerciseRepository.findByLessonId(lessonId).orElseGet(() -> {
            Exercise e = Exercise.builder()
                    .lessonId(lessonId)
                    .exerciseTitle(lesson != null ? "Trắc nghiệm: " + lesson.getTitle() : "Bài tập trắc nghiệm #" + lessonId)
                    .totalQuestions(5)
                    .passingScore(60)
                    .status("PUBLISHED")
                    .build();
            return exerciseRepository.save(e);
        });

        // Check if exercise already has questions linked
        List<ExerciseQuestion> existingEqs = exerciseQuestionRepository.findByExerciseIdOrderByQuestionOrderAsc(exercise.getExerciseId());
        if (!existingEqs.isEmpty()) {
            return;
        }

        // Create authentic questions tailored to this lesson
        List<QuestionSeed> seeds = generateSeedsForLesson(lessonId, level, lesson != null ? lesson.getTitle() : "");
        int order = 1;
        for (QuestionSeed seed : seeds) {
            String correctKey = seed.options.stream().filter(o -> o.isCorrect).map(o -> o.label).findFirst().orElse("A");
            Question q = Question.builder()
                    .content(seed.content)
                    .explanation(seed.explanation)
                    .category(seed.category)
                    .jlptLevel(level)
                    .questionType("SINGLE_CHOICE")
                    .difficulty("MEDIUM")
                    .status("DRAFT")
                    .options("[{\"key\": \"A\", \"text\": \"\"}, {\"key\": \"B\", \"text\": \"\"}, {\"key\": \"C\", \"text\": \"\"}, {\"key\": \"D\", \"text\": \"\"}]")
                    .answerKey("[\"" + correctKey + "\"]")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Question savedQ = questionRepository.save(q);

            QuestionBank qb = QuestionBank.builder()
                    .questionId(savedQ.getId())
                    .questionText(seed.content)
                    .questionType("SINGLE_CHOICE")
                    .category(seed.category)
                    .jlptLevel(level)
                    .difficultyLevel("MEDIUM")
                    .correctAnswer(correctKey)
                    .explanation(seed.explanation)
                    .status("ACTIVE")
                    .build();
            questionBankRepository.save(qb);

            for (OptionSeed optSeed : seed.options) {
                QuestionOption opt = QuestionOption.builder()
                        .questionId(savedQ.getId())
                        .label(optSeed.label)
                        .optionText(optSeed.text)
                        .isCorrect(optSeed.isCorrect)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                questionOptionRepository.save(opt);
            }

            exerciseQuestionRepository.save(new ExerciseQuestion(exercise.getExerciseId(), savedQ.getId(), order++));
        }

        exercise.setTotalQuestions(seeds.size());
        exerciseRepository.save(exercise);
        log.info("Seeded {} quiz questions for lesson {}", seeds.size(), lessonId);
    }

    private List<QuestionSeed> generateSeedsForLesson(Long lessonId, String level, String title) {
        List<QuestionSeed> list = new ArrayList<>();

        if ("N2".equalsIgnoreCase(level) || (title != null && title.contains("N2"))) {
            // N2 specific questions based on Onbin rules and vocabulary
            list.add(new QuestionSeed(
                    "どんな困難があっても、チーム全員の努力によって必ず____できると信じている。",
                    "Từ vựng 克服 (こくふく - Khắc phục) mang ý nghĩa vượt qua khó khăn, trở ngại bằng ý chí và nỗ lực.",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "克服（こくふく）", true),
                            new OptionSeed("B", "妥協（だきょう）", false),
                            new OptionSeed("C", "曖昧（あいまい）", false),
                            new OptionSeed("D", "慎重（しんちょう）", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "会議での彼の発言は論点が____で、賛成なのか反対なのかよく分からなかった。",
                    "曖昧（あいまい）: Mơ hồ, không rõ ràng, lập lờ nước đôi.",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "顕著", false),
                            new OptionSeed("B", "曖昧", true),
                            new OptionSeed("C", "把握", false),
                            new OptionSeed("D", "克服", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "現状の課題を正確に____しない限り、適切な解決策を見出すことはできない。",
                    "把握（はあく）: Nắm bắt, thấu suốt tường tận tình hình thực tế.",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "妥協", false),
                            new OptionSeed("B", "把握", true),
                            new OptionSeed("C", "慎重", false),
                            new OptionSeed("D", "顕著", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "景気回復の兆しがデータ上に____に現れ始めた。",
                    "顕著（けんちょ）: Rõ rệt, nổi bật, đáng chú ý (thường đi với phó từ 顕著に現れる).",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "曖昧", false),
                            new OptionSeed("B", "克服", false),
                            new OptionSeed("C", "顕著", true),
                            new OptionSeed("D", "把握", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "将来の進路を選択するにあたっては、様々な可能性を考慮して____に判断すべきだ。",
                    "慎重（しんちょう）: Cẩn trọng, dè dặt, kỹ lưỡng (慎重に判断する: phán đoán cẩn trọng).",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "慎重", true),
                            new OptionSeed("B", "妥協", false),
                            new OptionSeed("C", "克服", false),
                            new OptionSeed("D", "曖昧", false)
                    )
            ));
        } else {
            // N4 / N5 standard questions
            list.add(new QuestionSeed(
                    "来週の旅行の____はもう全部終わりましたか。",
                    "準備（じゅんび - Chuẩn bị): Đi cùng danh từ 旅行の準備 (chuẩn bị cho chuyến du lịch).",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "準備（じゅんび）", true),
                            new OptionSeed("B", "約束（やくそく）", false),
                            new OptionSeed("C", "案内（あんない）", false),
                            new OptionSeed("D", "習慣（しゅうかん）", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "今度の土曜日に友達と映画を見に行く____があります。",
                    "約束（やくそく - Hẹn, lời hứa): 約束がある mang nghĩa 'có cuộc hẹn với ai'.",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "経験", false),
                            new OptionSeed("B", "約束", true),
                            new OptionSeed("C", "案内", false),
                            new OptionSeed("D", "説明", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "先生が東京の有名な場所を親切に____してくれました。",
                    "案内する（あんないする - Hướng dẫn, dẫn đường cho người khác).",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "案内", true),
                            new OptionSeed("B", "準備", false),
                            new OptionSeed("C", "複雑", false),
                            new OptionSeed("D", "簡単", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "この文法のルールは少し____ですが、毎日練習すればすぐ覚えられます。",
                    "複雑（ふくざつ - Phức tạp, rắc rối), đối nghĩa với 簡単（かんたん - Đơn giản).",
                    "Ngữ pháp (Bunpou)",
                    List.of(
                            new OptionSeed("A", "簡単", false),
                            new OptionSeed("B", "複雑", true),
                            new OptionSeed("C", "案内", false),
                            new OptionSeed("D", "習慣", false)
                    )
            ));

            list.add(new QuestionSeed(
                    "先生が文法のポイントを分かりやすく丁寧に____してくれました。",
                    "説明する（せつめいする - Giải thích, thuyết minh rõ ràng).",
                    "Từ vựng (Goi)",
                    List.of(
                            new OptionSeed("A", "説明", true),
                            new OptionSeed("B", "準備", false),
                            new OptionSeed("C", "約束", false),
                            new OptionSeed("D", "経験", false)
                    )
            ));
        }

        return list;
    }

    private static class QuestionSeed {
        String content;
        String explanation;
        String category;
        List<OptionSeed> options;

        QuestionSeed(String content, String explanation, String category, List<OptionSeed> options) {
            this.content = content;
            this.explanation = explanation;
            this.category = category;
            this.options = options;
        }
    }

    private static class OptionSeed {
        String label;
        String text;
        boolean isCorrect;

        OptionSeed(String label, String text, boolean isCorrect) {
            this.label = label;
            this.text = text;
            this.isCorrect = isCorrect;
        }
    }
}
