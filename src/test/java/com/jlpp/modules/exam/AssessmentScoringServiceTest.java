package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.dto.ScoringEvaluationResult;
import com.jlpp.modules.exam.dto.SingleQuestionScoreResult;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import com.jlpp.modules.exam.service.impl.AssessmentScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentScoringServiceTest {

    private AssessmentScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new AssessmentScoringServiceImpl();
    }

    @Test
    @DisplayName("Single Question - Correct Answer earns full question points")
    void testSingleQuestion_Correct() {
        QuestionDto question = QuestionDto.builder()
                .id(1L)
                .points(5)
                .category("Từ vựng (Goi)")
                .options(List.of(
                        OptionDto.builder().id(101L).text("A").correct(true).build(),
                        OptionDto.builder().id(102L).text("B").correct(false).build()
                ))
                .build();

        SingleQuestionScoreResult result = scoringService.scoreSingleQuestion(question, "101");

        assertTrue(result.isCorrect());
        assertFalse(result.isUnanswered());
        assertEquals(5, result.getWeight());
        assertEquals(5, result.getEarnedScore());
        assertEquals("VOCABULARY", result.getCanonicalSkill());
    }

    @Test
    @DisplayName("Single Question - Incorrect Answer earns 0 points and is marked incorrect")
    void testSingleQuestion_Incorrect() {
        QuestionDto question = QuestionDto.builder()
                .id(2L)
                .points(10)
                .category("Ngữ pháp (Bunpou)")
                .options(List.of(
                        OptionDto.builder().id(201L).text("A").correct(true).build(),
                        OptionDto.builder().id(202L).text("B").correct(false).build()
                ))
                .build();

        SingleQuestionScoreResult result = scoringService.scoreSingleQuestion(question, "202");

        assertFalse(result.isCorrect());
        assertFalse(result.isUnanswered());
        assertEquals(10, result.getWeight());
        assertEquals(0, result.getEarnedScore());
        assertEquals("GRAMMAR", result.getCanonicalSkill());
    }

    @Test
    @DisplayName("Single Question - Unanswered or blank input earns 0 points without exceptions")
    void testSingleQuestion_Unanswered() {
        QuestionDto question = QuestionDto.builder()
                .id(3L)
                .points(null) // Should default to 2
                .category("Hán tự (Kanji)")
                .options(List.of(
                        OptionDto.builder().id(301L).text("A").correct(true).build()
                ))
                .build();

        SingleQuestionScoreResult resultNull = scoringService.scoreSingleQuestion(question, null);
        assertTrue(resultNull.isUnanswered());
        assertFalse(resultNull.isCorrect());
        assertEquals(2, resultNull.getWeight());
        assertEquals(0, resultNull.getEarnedScore());

        SingleQuestionScoreResult resultBlank = scoringService.scoreSingleQuestion(question, "   ");
        assertTrue(resultBlank.isUnanswered());
        assertEquals(0, resultBlank.getEarnedScore());

        SingleQuestionScoreResult resultMalformed = scoringService.scoreSingleQuestion(question, "not-a-number");
        assertTrue(resultMalformed.isUnanswered());
        assertEquals(0, resultMalformed.getEarnedScore());
    }

    @Test
    @DisplayName("Canonical Skill Mapping - Resolves 5 canonical skills correctly")
    void testSkillMapping() {
        // Listening with audio
        QuestionDto qAudio = QuestionDto.builder().id(1L).audioUrl("https://jlpp.test/sound.mp3").build();
        assertEquals("LISTENING", scoringService.resolveCanonicalSkill(qAudio));

        // Listening by keyword
        QuestionDto qListen = QuestionDto.builder().id(2L).category("Nghe hiểu (Choukai)").build();
        assertEquals("LISTENING", scoringService.resolveCanonicalSkill(qListen));

        // Reading
        QuestionDto qRead = QuestionDto.builder().id(3L).category("Đọc hiểu văn bản ngắn").build();
        assertEquals("READING", scoringService.resolveCanonicalSkill(qRead));

        // Kanji
        QuestionDto qKanji = QuestionDto.builder().id(4L).category("Hán tự N4").build();
        assertEquals("KANJI", scoringService.resolveCanonicalSkill(qKanji));

        // Grammar
        QuestionDto qGrammar = QuestionDto.builder().id(5L).category("Ngữ pháp và kính ngữ").build();
        assertEquals("GRAMMAR", scoringService.resolveCanonicalSkill(qGrammar));

        // Vocabulary
        QuestionDto qVocab = QuestionDto.builder().id(6L).category("Từ vựng chủ đề gia đình").build();
        assertEquals("VOCABULARY", scoringService.resolveCanonicalSkill(qVocab));
    }

    @Test
    @DisplayName("Assessment Evaluation - Mixed answers, weighting, and rounding")
    void testOverallScoring_MixedAnswers() {
        // 3 Questions:
        // Q1: Kanji, weight 10, correct
        // Q2: Vocab, weight 10, incorrect
        // Q3: Reading, weight 10, unanswered
        QuestionDto q1 = QuestionDto.builder()
                .id(1L).points(10).category("Hán tự (Kanji)")
                .options(List.of(OptionDto.builder().id(11L).correct(true).build()))
                .build();
        QuestionDto q2 = QuestionDto.builder()
                .id(2L).points(10).category("Từ vựng (Goi)")
                .options(List.of(
                        OptionDto.builder().id(21L).correct(true).build(),
                        OptionDto.builder().id(22L).correct(false).build()))
                .build();
        QuestionDto q3 = QuestionDto.builder()
                .id(3L).points(10).category("Đọc hiểu (Dokkai)")
                .options(List.of(OptionDto.builder().id(31L).correct(true).build()))
                .build();

        Map<String, String> answers = Map.of(
                "answer_1", "11", // correct (10 pts)
                "answer_2", "22"  // wrong (0 pts)
                // Q3 omitted -> unanswered (0 pts)
        );

        ScoringEvaluationResult result = scoringService.scoreQuestions(
                List.of(q1, q2, q3),
                answers,
                "MILESTONE",
                "N4",
                null
        );

        assertEquals(10, result.getEarnedScore());
        assertEquals(30, result.getTotalPossibleScore());
        // 10 / 30 * 100 = 33.333... -> 33.3
        assertEquals(new BigDecimal("33.3"), result.getPercentage());
        assertEquals(1, result.getCorrectCount());
        assertEquals(1, result.getIncorrectCount());
        assertEquals(1, result.getUnansweredCount());
        assertEquals(3, result.getTotalQuestions());

        // Milestone default passing score is 50%. 50% of 30 pts = 15 pts. 10 < 15 -> passed is false
        assertFalse(result.isPassed());
        assertEquals(15, result.getPassingScore());
    }

    @Test
    @DisplayName("Untested Skills Handling - Explicit NOT_ASSESSED status with null percentage")
    void testUntestedSkills_Handling() {
        // Exam only contains Vocabulary questions
        QuestionDto q1 = QuestionDto.builder()
                .id(1L).points(20).category("Từ vựng (Goi)")
                .options(List.of(OptionDto.builder().id(101L).correct(true).build()))
                .build();

        Map<String, String> answers = Map.of("answer_1", "101");

        ScoringEvaluationResult result = scoringService.scoreQuestions(
                List.of(q1),
                answers,
                "MILESTONE",
                "N5",
                null
        );

        // Assessed breakdown should contain only the tested skill (VOCABULARY)
        assertEquals(1, result.getSkillBreakdown().size());
        ExamResultResponse.SkillScoreDto vocabAssessed = result.getSkillBreakdown().get(0);
        assertEquals("VOCABULARY", vocabAssessed.getSkillGroup());
        assertEquals(20, vocabAssessed.getEarnedPoints());
        assertEquals(20, vocabAssessed.getTotalPoints());
        assertEquals(new BigDecimal("100.0"), vocabAssessed.getPercentage());
        assertEquals("ASSESSED", vocabAssessed.getStatus());

        // Canonical skills must contain all 5 skills
        assertEquals(5, result.getCanonicalSkills().size());
        Optional<ExamResultResponse.SkillScoreDto> kanji = result.getCanonicalSkills().stream()
                .filter(s -> "KANJI".equals(s.getSkillGroup()))
                .findFirst();
        assertTrue(kanji.isPresent());
        assertEquals("NOT_ASSESSED", kanji.get().getStatus());
        assertEquals(0, kanji.get().getTotalQuestions());
        assertEquals(0, kanji.get().getTotalPoints());
        assertEquals(0, kanji.get().getEarnedPoints());
        assertNull(kanji.get().getPercentage()); // Untested skills must have null percentage, not 0.0
    }

    @Test
    @DisplayName("Purpose Separation - Placement Assessment always passed = true with null passing score")
    void testPlacementPurposeSeparation() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).points(10).category("Từ vựng")
                .options(List.of(OptionDto.builder().id(10L).correct(true).build()))
                .build();

        // 0 points earned in Placement test
        ScoringEvaluationResult result = scoringService.scoreQuestions(
                List.of(q),
                Collections.emptyMap(),
                "PLACEMENT_TEST",
                "N3",
                null
        );

        assertEquals(0, result.getEarnedScore());
        assertEquals(10, result.getTotalPossibleScore());
        // Diagnostic test: never fails learner
        assertTrue(result.isPassed());
        assertNull(result.getPassingScore());
        assertTrue(result.getAiFeedback().contains("đánh giá năng lực đầu vào"));
    }

    @Test
    @DisplayName("Risk 1 Validation: 20 Questions x 2 Points, passingScore = 50. Full marks MUST PASS.")
    void testMilestonePassingScore_20Questions2Points_PassingScore50() {
        // Create 20 questions worth 2 points each -> Total 40 points
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> fullMarkAnswers = new HashMap<>();
        Map<String, String> halfMarkAnswers = new HashMap<>();
        Map<String, String> failMarkAnswers = new HashMap<>();

        for (int i = 1; i <= 20; i++) {
            long qId = i;
            long correctOptId = i * 10L + 1;
            long wrongOptId = i * 10L + 2;

            questions.add(QuestionDto.builder()
                    .id(qId)
                    .points(2)
                    .category("Ngữ pháp N4")
                    .options(List.of(
                            OptionDto.builder().id(correctOptId).text("Correct").correct(true).build(),
                            OptionDto.builder().id(wrongOptId).text("Wrong").correct(false).build()
                    ))
                    .build());

            fullMarkAnswers.put("answer_" + qId, String.valueOf(correctOptId));
            if (i <= 10) {
                halfMarkAnswers.put("answer_" + qId, String.valueOf(correctOptId));
            } else {
                halfMarkAnswers.put("answer_" + qId, String.valueOf(wrongOptId));
            }
            if (i <= 9) {
                failMarkAnswers.put("answer_" + qId, String.valueOf(correctOptId));
            } else {
                failMarkAnswers.put("answer_" + qId, String.valueOf(wrongOptId));
            }
        }

        // Full marks: 40/40 points (100%) with passingScore threshold = 50 (%)
        ScoringEvaluationResult fullMarkResult = scoringService.scoreQuestions(
                questions,
                fullMarkAnswers,
                "MILESTONE",
                "N4",
                50
        );

        assertEquals(40, fullMarkResult.getEarnedScore());
        assertEquals(40, fullMarkResult.getTotalPossibleScore());
        assertEquals(new BigDecimal("100.0"), fullMarkResult.getPercentage());
        assertTrue(fullMarkResult.isPassed(), "Full marks (40/40) MUST NOT fail when passingScore=50 is treated as a percentage");
        assertEquals(20, fullMarkResult.getPassingScore(), "Passing score must be resolved to required score (50% of 40 = 20 pts)");

        // Exactly 50%: 20/40 points (10 correct) -> MUST PASS
        ScoringEvaluationResult halfMarkResult = scoringService.scoreQuestions(
                questions,
                halfMarkAnswers,
                "MILESTONE",
                "N4",
                50
        );
        assertEquals(20, halfMarkResult.getEarnedScore());
        assertEquals(new BigDecimal("50.0"), halfMarkResult.getPercentage());
        assertTrue(halfMarkResult.isPassed(), "50% score (20/40) must pass when passingScore=50");
        assertEquals(20, halfMarkResult.getPassingScore());

        // Under 50%: 18/40 points (9 correct, 45%) -> MUST FAIL
        ScoringEvaluationResult failResult = scoringService.scoreQuestions(
                questions,
                failMarkAnswers,
                "MILESTONE",
                "N4",
                50
        );
        assertEquals(18, failResult.getEarnedScore());
        assertEquals(new BigDecimal("45.0"), failResult.getPercentage());
        assertFalse(failResult.isPassed(), "45% score (18/40) must fail when passingScore=50");
        assertEquals(20, failResult.getPassingScore());
    }

    @Test
    @DisplayName("Risk 2 Validation: Mixed content question prefers authoritative metadata over heuristics")
    void testMixedContentQuestion_PrefersAuthoritativeCategoryOverContentKeywords() {
        // Question 1: Category is "Ngữ pháp (Bunpou)" but content contains "nghe nhạc (choukai)"
        QuestionDto grammarWithListeningText = QuestionDto.builder()
                .id(1L)
                .category("Ngữ pháp (Bunpou)")
                .content("Hãy chọn cấu trúc đúng: Tôi thích nghe (choukai) nhạc Nhật Bản.")
                .build();
        assertEquals("GRAMMAR", scoringService.resolveCanonicalSkill(grammarWithListeningText));

        // Question 2: Category is "Đọc hiểu (Dokkai)" but content mentions "audio choukai"
        QuestionDto readingWithAudioKeyword = QuestionDto.builder()
                .id(2L)
                .category("Đọc hiểu ngắn")
                .content("Bài văn mô tả về một file audio nghe choukai.")
                .build();
        assertEquals("READING", scoringService.resolveCanonicalSkill(readingWithAudioKeyword));

        // Question 3: SkillGroup is explicitly "VOCABULARY", but has an attached audio URL
        QuestionDto vocabWithAudio = QuestionDto.builder()
                .id(3L)
                .skillGroup("VOCABULARY")
                .category("Từ vựng N4")
                .audioUrl("https://storage.jeps.edu.vn/audio/word123.mp3")
                .content("Phát âm từ vựng sau đây")
                .build();
        assertEquals("VOCABULARY", scoringService.resolveCanonicalSkill(vocabWithAudio));

        // Question 4: Category is "Hán tự (Kanji)" with word "chọn từ vựng" in text
        QuestionDto kanjiWithVocabContent = QuestionDto.builder()
                .id(4L)
                .category("Hán tự (Kanji)")
                .content("Hãy chọn từ vựng viết đúng chữ Hán sau:")
                .build();
        assertEquals("KANJI", scoringService.resolveCanonicalSkill(kanjiWithVocabContent));

        // Question 5: Truly ambiguous with NO metadata -> Fallback to audioUrl produces LISTENING
        QuestionDto fallbackAudio = QuestionDto.builder()
                .id(5L)
                .audioUrl("https://storage.jeps.edu.vn/audio/dialogue.mp3")
                .build();
        assertEquals("LISTENING", scoringService.resolveCanonicalSkill(fallbackAudio));
    }

    // ── Milestone PASS/FAIL Boundary & Consistency Tests ──

    @Test
    @DisplayName("Boundary Test: 49.96% versus a 50% threshold MUST FAIL despite rounded 50.0% display")
    void testMilestoneBoundary_49Point96PercentVs50PercentThreshold_MustFail() {
        // 2500 total possible score: 250 questions of 10 points each
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> answers = new HashMap<>();

        // 1249 earned points / 2500 total points = 0.4996 (49.96%)
        // Q1..Q124 have 10 points each (1240 pts)
        // Q125 has 9 points (earned 9 pts) -> Wait, question points are integer.
        // Let Q1..Q124 be 10 points, Q125 be 9 points, Q126..Q250 be 10 points. Total = 2500 - 1 = 2499.
        // Better: 250 questions with 10 points each = 2500 points.
        // 124 correct (1240) + Q125: 1 correct option with 9 points?
        // Simpler: 1 question of 9 points, 249 questions of 10 points = 2499 pts? No!
        // To get exactly 1249 out of 2500:
        // Q1 has 9 points, Q2 has 1 point (10 pts total).
        // Q3..Q250 have 10 points each (248 * 10 = 2480).
        // Total points = 9 + 1 + 2490 = 2500 points!
        // Correct answers: Q1 (9 pts) + 124 questions of 10 points (1240 pts) = 1249 points!
        // 1249 / 2500 = 0.4996 = 49.96%!
        questions.add(QuestionDto.builder().id(1L).points(9)
                .options(List.of(OptionDto.builder().id(11L).correct(true).build())).build());
        questions.add(QuestionDto.builder().id(2L).points(1)
                .options(List.of(OptionDto.builder().id(21L).correct(true).build())).build());
        answers.put("answer_1", "11"); // correct (9 pts)
        // Q2 omitted (0 pts)

        for (int i = 3; i <= 251; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            questions.add(QuestionDto.builder().id(qId).points(10)
                    .options(List.of(OptionDto.builder().id(correctOpt).correct(true).build())).build());

            if (i <= 126) { // 126 - 3 + 1 = 124 questions * 10 = 1240 pts
                answers.put("answer_" + qId, String.valueOf(correctOpt));
            }
        }

        // Total score = 9 + 1 + 249 * 10 = 2500 points.
        // Earned = 9 + 124 * 10 = 1249 points (49.96%).
        ScoringEvaluationResult result = scoringService.scoreQuestions(
                questions, answers, "MILESTONE", "N4", null
        );

        assertEquals(1249, result.getEarnedScore());
        assertEquals(2500, result.getTotalPossibleScore());
        // Display percentage is rounded to 50.0%
        assertEquals(new BigDecimal("50.0"), result.getPercentage());
        // Crucial: 49.96% MUST FAIL against the 50% threshold! Display rounding must not pass!
        assertFalse(result.isPassed(), "49.96% MUST FAIL against a 50% threshold (required 1250, earned 1249)");
        assertEquals(1250, result.getPassingScore());
    }

    @Test
    @DisplayName("Boundary Test: Exactly 50.00% MUST PASS")
    void testMilestoneBoundary_Exactly50Percent_MustPass() {
        // 2500 total points, 1250 earned points = exactly 50.00%
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> answers = new HashMap<>();

        for (int i = 1; i <= 250; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            questions.add(QuestionDto.builder().id(qId).points(10)
                    .options(List.of(OptionDto.builder().id(correctOpt).correct(true).build())).build());

            if (i <= 125) { // 125 * 10 = 1250 pts
                answers.put("answer_" + qId, String.valueOf(correctOpt));
            }
        }

        ScoringEvaluationResult result = scoringService.scoreQuestions(
                questions, answers, "MILESTONE", "N4", null
        );

        assertEquals(1250, result.getEarnedScore());
        assertEquals(2500, result.getTotalPossibleScore());
        assertEquals(new BigDecimal("50.0"), result.getPercentage());
        assertTrue(result.isPassed(), "Exactly 50% (1250/2500) MUST PASS");
        assertEquals(1250, result.getPassingScore());
    }

    @Test
    @DisplayName("Boundary Test: Full marks MUST PASS for every valid Milestone configuration")
    void testMilestoneBoundary_FullMarks_MustPass() {
        // 5 questions of 7 points each = 35 points total (< 50)
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> answers = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            questions.add(QuestionDto.builder().id(qId).points(7)
                    .options(List.of(OptionDto.builder().id(correctOpt).correct(true).build())).build());
            answers.put("answer_" + qId, String.valueOf(correctOpt));
        }

        // Configured passingScore is 50 (legacy default exceeding 35 max score)
        ScoringEvaluationResult result = scoringService.scoreQuestions(
                questions, answers, "MILESTONE", "N4", 50
        );

        assertEquals(35, result.getEarnedScore());
        assertEquals(35, result.getTotalPossibleScore());
        assertEquals(new BigDecimal("100.0"), result.getPercentage());
        assertTrue(result.isPassed(), "Full marks (100%) MUST PASS on every valid Milestone configuration");
    }

    @Test
    @DisplayName("Boundary Test: Zero maximum score exams CANNOT PASS")
    void testMilestoneBoundary_ZeroMaximumScore_MustFail() {
        // Exam with no questions -> totalPossibleScore = 0
        ScoringEvaluationResult resultEmpty = scoringService.scoreQuestions(
                Collections.emptyList(), Collections.emptyMap(), "MILESTONE", "N4", 50
        );

        assertEquals(0, resultEmpty.getTotalPossibleScore());
        assertFalse(resultEmpty.isPassed(), "Zero maximum score exams CANNOT PASS");

        // Exam with null questions list -> totalPossibleScore = 0
        ScoringEvaluationResult resultNull = scoringService.scoreQuestions(
                null, Collections.emptyMap(), "MILESTONE", "N4", 50
        );

        assertEquals(0, resultNull.getTotalPossibleScore());
        assertFalse(resultNull.isPassed(), "Null questions collection CANNOT PASS");
    }

    @Test
    @DisplayName("Boundary Test: Legacy Milestone configured with raw passingScore = 50 on 100-point exam")
    void testMilestoneBoundary_LegacyMilestoneConfiguredWithPassingScore50() {
        // 10 questions x 10 points = 100 points total
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> passAnswers = new HashMap<>();
        Map<String, String> failAnswers = new HashMap<>();

        for (int i = 1; i <= 10; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            questions.add(QuestionDto.builder().id(qId).points(10)
                    .options(List.of(OptionDto.builder().id(correctOpt).correct(true).build())).build());

            if (i <= 5) {
                passAnswers.put("answer_" + qId, String.valueOf(correctOpt)); // 50 pts
            }
            if (i <= 4) {
                failAnswers.put("answer_" + qId, String.valueOf(correctOpt)); // 40 pts
            }
        }

        // Passing score 50 on 100-point exam: exactly 50 passes, 40 fails
        ScoringEvaluationResult passRes = scoringService.scoreQuestions(
                questions, passAnswers, "MILESTONE", "N4", 50
        );
        assertEquals(50, passRes.getEarnedScore());
        assertEquals(50, passRes.getPassingScore());
        assertTrue(passRes.isPassed());

        ScoringEvaluationResult failRes = scoringService.scoreQuestions(
                questions, failAnswers, "MILESTONE", "N4", 50
        );
        assertEquals(40, failRes.getEarnedScore());
        assertEquals(50, failRes.getPassingScore());
        assertFalse(failRes.isPassed());
    }

    @Test
    @DisplayName("Boundary Test: Milestone with maximum score below 50 (e.g. 40 points) and legacy passingScore = 50")
    void testMilestoneBoundary_MaxScoreBelow50_WithPassingScore50() {
        // 4 questions x 10 points = 40 points total (< 50)
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> halfAnswers = new HashMap<>();
        Map<String, String> lowAnswers = new HashMap<>();

        for (int i = 1; i <= 4; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            questions.add(QuestionDto.builder().id(qId).points(10)
                    .options(List.of(OptionDto.builder().id(correctOpt).correct(true).build())).build());

            if (i <= 2) {
                halfAnswers.put("answer_" + qId, String.valueOf(correctOpt)); // 20 pts (50%)
            }
            if (i <= 1) {
                lowAnswers.put("answer_" + qId, String.valueOf(correctOpt)); // 10 pts (25%)
            }
        }

        // Configured passingScore = 50 on 40-point exam: resolves to 50% = 20 points
        ScoringEvaluationResult halfRes = scoringService.scoreQuestions(
                questions, halfAnswers, "MILESTONE", "N4", 50
        );
        assertEquals(20, halfRes.getEarnedScore());
        assertEquals(20, halfRes.getPassingScore(), "Passing score must be calibrated to 50% of 40 = 20 pts");
        assertTrue(halfRes.isPassed(), "20/40 (50%) must pass when passingScore=50 on a 40-point exam");

        ScoringEvaluationResult lowRes = scoringService.scoreQuestions(
                questions, lowAnswers, "MILESTONE", "N4", 50
        );
        assertEquals(10, lowRes.getEarnedScore());
        assertFalse(lowRes.isPassed(), "10/40 (25%) must fail");
    }

    @Test
    @DisplayName("Boundary Test: Raw score threshold preservation (does not silently reinterpret raw scores <= 100 as percentages)")
    void testMilestoneBoundary_PreserveRawScoreThresholdWhenValid() {
        // Exam with totalPossibleScore = 150 points (15 questions x 10 points)
        // Configured raw threshold = 80 points.
        // If 80 were silently reinterpreted as 80%, required score would be 120 points (wrong!).
        // Because 80 <= 150, 80 raw points MUST be preserved.
        List<QuestionDto> questions = new ArrayList<>();
        Map<String, String> answers80 = new HashMap<>();
        Map<String, String> answers70 = new HashMap<>();

        for (int i = 1; i <= 15; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            questions.add(QuestionDto.builder().id(qId).points(10)
                    .options(List.of(OptionDto.builder().id(correctOpt).correct(true).build())).build());

            if (i <= 8) {
                answers80.put("answer_" + qId, String.valueOf(correctOpt)); // 80 pts
            }
            if (i <= 7) {
                answers70.put("answer_" + qId, String.valueOf(correctOpt)); // 70 pts
            }
        }

        ScoringEvaluationResult res80 = scoringService.scoreQuestions(
                questions, answers80, "MILESTONE", "N4", 80
        );
        assertEquals(80, res80.getEarnedScore());
        assertEquals(80, res80.getPassingScore(), "Raw threshold 80 must be preserved when 80 <= totalPossibleScore (150)");
        assertTrue(res80.isPassed(), "80/150 points MUST PASS when passingScore=80 raw marks");

        ScoringEvaluationResult res70 = scoringService.scoreQuestions(
                questions, answers70, "MILESTONE", "N4", 80
        );
        assertEquals(70, res70.getEarnedScore());
        assertFalse(res70.isPassed(), "70/150 points MUST FAIL when passingScore=80 raw marks");
    }
}

