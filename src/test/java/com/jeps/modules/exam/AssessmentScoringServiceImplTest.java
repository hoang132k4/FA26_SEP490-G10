package com.jeps.modules.exam;

import com.jeps.modules.exam.dto.ExamResultResponse;
import com.jeps.modules.exam.dto.OptionDto;
import com.jeps.modules.exam.dto.QuestionDto;
import com.jeps.modules.exam.dto.ScoringEvaluationResult;
import com.jeps.modules.exam.dto.SingleQuestionScoreResult;
import com.jeps.modules.exam.entity.Exam;
import com.jeps.modules.exam.service.impl.AssessmentScoringServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentScoringServiceImplTest {

    private final AssessmentScoringServiceImpl scoring = new AssessmentScoringServiceImpl();

    // ── scoreSingleQuestion ──

    @Test
    void nullQuestion_returnsUnansweredWithDefaultWeight() {
        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(null, "123");

        assertTrue(r.isUnanswered());
        assertFalse(r.isCorrect());
        assertEquals(AssessmentScoringServiceImpl.DEFAULT_QUESTION_POINTS, r.getWeight());
        assertEquals(0, r.getEarnedScore());
    }

    @Test
    void correctAnswer_returnsCorrectWithPoints() {
        QuestionDto q = question(1L, 10, "VOCABULARY",
                option(100L, true), option(101L, false));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "100");

        assertTrue(r.isCorrect());
        assertFalse(r.isUnanswered());
        assertEquals(10, r.getEarnedScore());
        assertEquals(10, r.getWeight());
        assertEquals(100L, r.getSelectedOptionId());
    }

    @Test
    void incorrectAnswer_returnsIncorrectZeroScore() {
        QuestionDto q = question(1L, 10, "VOCABULARY",
                option(100L, true), option(101L, false));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "101");

        assertFalse(r.isCorrect());
        assertFalse(r.isUnanswered());
        assertEquals(0, r.getEarnedScore());
        assertEquals(101L, r.getSelectedOptionId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void blankOrNullAnswer_treatedAsUnanswered(String answer) {
        QuestionDto q = question(1L, 5, "READING", option(100L, true));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, answer);

        assertTrue(r.isUnanswered());
        assertFalse(r.isCorrect());
        assertNull(r.getSelectedOptionId());
    }

    @Test
    void nonNumericAnswer_treatedAsUnanswered() {
        QuestionDto q = question(1L, 5, "READING", option(100L, true));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "abc");

        assertTrue(r.isUnanswered());
        assertNull(r.getSelectedOptionId());
    }

    @Test
    void noCorrectOptionMarked_alwaysIncorrect() {
        QuestionDto q = question(1L, 5, "GRAMMAR",
                option(100L, false), option(101L, false));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "100");

        assertFalse(r.isCorrect());
        assertEquals(0, r.getEarnedScore());
    }

    @Test
    void nullOrZeroPoints_usesDefaultWeight() {
        QuestionDto q = question(1L, null, "VOCABULARY", option(100L, true));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "100");

        assertEquals(AssessmentScoringServiceImpl.DEFAULT_QUESTION_POINTS, r.getWeight());
        assertEquals(AssessmentScoringServiceImpl.DEFAULT_QUESTION_POINTS, r.getEarnedScore());
    }

    @Test
    void zeroPoints_usesDefaultWeight() {
        QuestionDto q = question(1L, 0, "VOCABULARY", option(100L, true));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "100");

        assertEquals(AssessmentScoringServiceImpl.DEFAULT_QUESTION_POINTS, r.getWeight());
    }

    @Test
    void nullOptionsList_treatedAsIncorrect() {
        QuestionDto q = QuestionDto.builder().id(1L).points(5).skillGroup("READING").options(null).build();

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "100");

        assertFalse(r.isCorrect());
        assertFalse(r.isUnanswered());
        assertEquals(100L, r.getSelectedOptionId());
    }

    // ── resolveCanonicalSkill ──

    @Test
    void nullQuestion_defaultsToVocabulary() {
        assertEquals("VOCABULARY", scoring.resolveCanonicalSkill(null));
    }

    @ParameterizedTest
    @CsvSource({"KANJI,KANJI", "VOCABULARY,VOCABULARY", "GRAMMAR,GRAMMAR", "READING,READING", "LISTENING,LISTENING"})
    void skillGroup_exactMatch_returnsCanonical(String skillGroup, String expected) {
        QuestionDto q = QuestionDto.builder().skillGroup(skillGroup).build();
        assertEquals(expected, scoring.resolveCanonicalSkill(q));
    }

    @Test
    void skillGroupTakesPrecedenceOverCategory() {
        QuestionDto q = QuestionDto.builder()
                .skillGroup("READING")
                .category("Nghe hiểu (Choukai)")
                .build();
        assertEquals("READING", scoring.resolveCanonicalSkill(q));
    }

    @ParameterizedTest
    @CsvSource({
            "Hán tự (Kanji), KANJI",
            "Ngữ pháp (Bunpou), GRAMMAR",
            "Từ vựng (Goi), VOCABULARY",
            "Đọc hiểu (Dokkai), READING",
            "Nghe hiểu (Choukai), LISTENING"
    })
    void categoryKeywords_resolveCorrectly(String category, String expected) {
        QuestionDto q = QuestionDto.builder().category(category).build();
        assertEquals(expected, scoring.resolveCanonicalSkill(q));
    }

    @Test
    void skillGroupLabel_fallsBackWhenSkillGroupAndCategoryAbsent() {
        QuestionDto q = QuestionDto.builder().skillGroupLabel("Đọc hiểu (Reading)").build();
        assertEquals("READING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void audioUrl_fallsBackToListeningWhenNoMetadata() {
        QuestionDto q = QuestionDto.builder().audioUrl("https://example.com/audio.mp3").build();
        assertEquals("LISTENING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void contentKeywords_fallbackToListening() {
        QuestionDto q = QuestionDto.builder().content("Bài nghe số 5").build();
        assertEquals("LISTENING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void contentKeywords_fallbackToReading() {
        QuestionDto q = QuestionDto.builder().content("Đọc hiểu đoạn văn sau").build();
        assertEquals("READING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void noMetadataAtAll_defaultsToVocabulary() {
        QuestionDto q = QuestionDto.builder().content("今日は何曜日ですか。").build();
        assertEquals("VOCABULARY", scoring.resolveCanonicalSkill(q));
    }

    // ── resolvePassingScore ──

    @Test
    void customScore_overridesDefault() {
        assertEquals(75, scoring.resolvePassingScore("JLPT_MOCK", "N5", 75));
    }

    @ParameterizedTest
    @CsvSource({"N5,80", "N4,90", "N3,95", "N2,90", "N1,100"})
    void jlptLevels_returnOfficialScores(String level, int expected) {
        assertEquals(expected, scoring.resolvePassingScore("JLPT_MOCK", level, null));
    }

    @Test
    void milestoneDefault_returns50() {
        assertEquals(50, scoring.resolvePassingScore("MILESTONE", "N5", null));
    }

    @Test
    void unknownLevel_returns80() {
        assertEquals(80, scoring.resolvePassingScore("JLPT_MOCK", "X9", null));
    }

    @Test
    void nullLevel_returns80() {
        assertEquals(80, scoring.resolvePassingScore("JLPT_MOCK", null, null));
    }

    // ── resolveMilestonePassingThreshold ──

    @Test
    void totalZero_returnsCustomOrDefault() {
        assertEquals(50, scoring.resolveMilestonePassingThreshold(0, null));
        assertEquals(70, scoring.resolveMilestonePassingThreshold(0, 70));
    }

    @Test
    void thresholdWithinTotal_returnsAsIs() {
        assertEquals(30, scoring.resolveMilestonePassingThreshold(100, 30));
    }

    @Test
    void thresholdEqualsTotal_returnsAsIs() {
        assertEquals(100, scoring.resolveMilestonePassingThreshold(100, 100));
    }

    @Test
    void thresholdExceedsTotalButUnder100_treatsAsPercentage() {
        // threshold=50, total=40 → ceil(40*50/100) = 20
        assertEquals(20, scoring.resolveMilestonePassingThreshold(40, 50));
    }

    @Test
    void thresholdOver100AndOverTotal_clampsToTotal() {
        assertEquals(40, scoring.resolveMilestonePassingThreshold(40, 150));
    }

    @Test
    void nullThreshold_fallsBackTo50Percent() {
        // ceil(80*50/100) = 40
        assertEquals(40, scoring.resolveMilestonePassingThreshold(80, null));
    }

    @Test
    void zeroThreshold_fallsBackTo50Percent() {
        assertEquals(40, scoring.resolveMilestonePassingThreshold(80, 0));
    }

    // ── calculateSkillBreakdown ──

    @Test
    void emptyQuestions_excludeUntested_returnsEmpty() {
        List<ExamResultResponse.SkillScoreDto> result = scoring.calculateSkillBreakdown(List.of(), false);
        assertTrue(result.isEmpty());
    }

    @Test
    void emptyQuestions_includeUntested_returnsAllCanonicalSkills() {
        List<ExamResultResponse.SkillScoreDto> result = scoring.calculateSkillBreakdown(List.of(), true);
        assertEquals(5, result.size());
        assertTrue(result.stream().allMatch(s -> "NOT_ASSESSED".equals(s.getStatus())));
    }

    @Test
    void nullQuestions_includeUntested_returnsAllCanonicalSkills() {
        List<ExamResultResponse.SkillScoreDto> result = scoring.calculateSkillBreakdown(null, true);
        assertEquals(5, result.size());
    }

    @Test
    void mixedSkills_groupsCorrectlyAndCalculatesPercentage() {
        QuestionDto vocab = scoredQuestion(1L, 10, "VOCABULARY", true);
        QuestionDto grammar = scoredQuestion(2L, 10, "GRAMMAR", false);
        QuestionDto reading = scoredQuestion(3L, 10, "READING", true);

        List<ExamResultResponse.SkillScoreDto> result = scoring.calculateSkillBreakdown(
                List.of(vocab, grammar, reading), false);

        assertEquals(3, result.size());
        ExamResultResponse.SkillScoreDto vocabSkill = result.stream()
                .filter(s -> "VOCABULARY".equals(s.getSkillGroup())).findFirst().orElseThrow();
        assertEquals(10, vocabSkill.getEarnedPoints());
        assertEquals(10, vocabSkill.getTotalPoints());
        assertEquals(new BigDecimal("100.0"), vocabSkill.getPercentage());
        assertEquals("ASSESSED", vocabSkill.getStatus());
    }

    @Test
    void multipleQuestionsPerSkill_sumsCorrectly() {
        QuestionDto v1 = scoredQuestion(1L, 5, "VOCABULARY", true);
        QuestionDto v2 = scoredQuestion(2L, 5, "VOCABULARY", false);
        QuestionDto v3 = scoredQuestion(3L, 5, "VOCABULARY", true);

        List<ExamResultResponse.SkillScoreDto> result = scoring.calculateSkillBreakdown(List.of(v1, v2, v3), false);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getEarnedPoints());
        assertEquals(15, result.get(0).getTotalPoints());
        assertEquals(2, result.get(0).getCorrectCount());
        assertEquals(3, result.get(0).getTotalQuestions());
    }

    // ── getSkillDisplayName ──

    @ParameterizedTest
    @CsvSource({
            "KANJI, Chữ Hán (Kanji)",
            "VOCABULARY, Từ vựng (Vocabulary)",
            "GRAMMAR, Ngữ pháp (Grammar)",
            "READING, Đọc hiểu (Reading)",
            "LISTENING, Nghe hiểu (Listening)",
            "VOCAB_GRAMMAR, Kiến thức ngôn ngữ (Từ vựng • Ngữ pháp)"
    })
    void displayName_returnsVietnameseLabel(String key, String expected) {
        assertEquals(expected, scoring.getSkillDisplayName(key));
    }

    @Test
    void displayName_nullKey_returnsGeneric() {
        assertEquals("Kiến thức tổng hợp", scoring.getSkillDisplayName(null));
    }

    @Test
    void displayName_unknownKey_returnsKeyItself() {
        assertEquals("WRITING", scoring.getSkillDisplayName("WRITING"));
    }

    // ── scoreQuestions (end-to-end) ──

    @Test
    void emptyQuestions_returnsEmptyResult() {
        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(), Map.of(), "JLPT_MOCK", "N5", null);

        assertEquals(0, r.getEarnedScore());
        assertEquals(0, r.getTotalPossibleScore());
        assertNotNull(r.getAiFeedback());
    }

    @Test
    void nullQuestions_returnsEmptyResult() {
        ScoringEvaluationResult r = scoring.scoreQuestions(null, Map.of(), "JLPT_MOCK", "N5", null);

        assertEquals(0, r.getTotalPossibleScore());
    }

    @Test
    void placementTest_alwaysPassed() {
        QuestionDto q = question(1L, 10, "VOCABULARY", option(100L, true), option(101L, false));
        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(q), Map.of("answer_1", "101"),
                "PLACEMENT_TEST", "N5", null);

        assertTrue(r.isPassed());
        assertNull(r.getPassingScore());
    }

    @Test
    void jlptMock_fullScore_passes() {
        QuestionDto q = question(1L, 100, "VOCABULARY", option(100L, true), option(101L, false));
        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(q), Map.of("answer_1", "100"),
                "JLPT_MOCK", "N5", null);

        assertTrue(r.isPassed());
        assertEquals(100, r.getEarnedScore());
        assertEquals(1, r.getCorrectCount());
        assertEquals(0, r.getIncorrectCount());
        assertEquals(0, r.getUnansweredCount());
    }

    @Test
    void jlptMock_belowPassingScore_fails() {
        QuestionDto q = question(1L, 10, "VOCABULARY", option(100L, true), option(101L, false));
        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(q), Map.of("answer_1", "101"),
                "JLPT_MOCK", "N5", null);

        assertFalse(r.isPassed());
        assertEquals(0, r.getEarnedScore());
        assertEquals(80, r.getPassingScore());
    }

    @Test
    void milestone_usesResolveMilestoneThreshold() {
        QuestionDto q1 = question(1L, 30, "VOCABULARY", option(100L, true), option(101L, false));
        QuestionDto q2 = question(2L, 30, "GRAMMAR", option(200L, true), option(201L, false));
        // threshold: custom=50, total=60 → 50 (within total)
        ScoringEvaluationResult r = scoring.scoreQuestions(
                List.of(q1, q2), Map.of("answer_1", "100", "answer_2", "200"),
                "MILESTONE", "N5", 50);

        assertTrue(r.isPassed());
        assertEquals(60, r.getEarnedScore());
        assertEquals(50, r.getPassingScore());
    }

    @Test
    void scoreQuestionsWithExamContext_delegatesCorrectly() {
        Exam exam = new Exam(1L, "Test", "JLPT_MOCK", "N5", 90, 10, 80, "ACTIVE", null);
        QuestionDto q = question(1L, 10, "VOCABULARY", option(100L, true), option(101L, false));

        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(q), Map.of("answer_1", "100"), exam);

        assertTrue(r.isPassed());
    }

    @Test
    void scoreQuestions_identifiesStrongestAndWeakestSkills() {
        QuestionDto vocab = question(1L, 10, "VOCABULARY", option(100L, true), option(101L, false));
        QuestionDto reading = question(2L, 10, "READING", option(200L, true), option(201L, false));

        ScoringEvaluationResult r = scoring.scoreQuestions(
                List.of(vocab, reading),
                Map.of("answer_1", "100", "answer_2", "201"),
                "JLPT_MOCK", "N5", null);

        assertEquals("Từ vựng (Vocabulary)", r.getStrongestSkill());
        assertEquals("Đọc hiểu (Reading)", r.getWeakestSkill());
    }

    @Test
    void scoreQuestions_allUnanswered_bothSkillsNull() {
        QuestionDto q = question(1L, 10, "VOCABULARY", option(100L, true));

        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(q), Map.of(), "JLPT_MOCK", "N5", null);

        assertEquals(0, r.getEarnedScore());
        assertEquals(1, r.getUnansweredCount());
    }

    @Test
    void scoreQuestions_alternateAnswerKeyFormat_questionIdDirectly() {
        QuestionDto q = question(1L, 10, "VOCABULARY", option(100L, true), option(101L, false));

        ScoringEvaluationResult r = scoring.scoreQuestions(List.of(q), Map.of("1", "100"),
                "JLPT_MOCK", "N5", null);

        assertEquals(10, r.getEarnedScore());
    }

    // ── NEW: scoreSingleQuestion edge cases ──

    @Test
    void emptyOptionsList_incorrectNotUnanswered() {
        QuestionDto q = QuestionDto.builder().id(50L).points(5).skillGroup("VOCABULARY").options(List.of()).build();

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "999");

        assertFalse(r.isCorrect());
        assertFalse(r.isUnanswered());
        assertEquals(0, r.getEarnedScore());
    }

    @Test
    void negativePoints_fallbackToDefaultWeight() {
        QuestionDto q = question(60L, -5, "READING", option(600L, true));

        SingleQuestionScoreResult r = scoring.scoreSingleQuestion(q, "600");

        assertTrue(r.isCorrect());
        assertEquals(AssessmentScoringServiceImpl.DEFAULT_QUESTION_POINTS, r.getWeight());
    }

    // ── NEW: resolveCanonicalSkill keyword resolution ──

    @Test
    void resolveCanonicalSkill_skillGroupLabel_nghe_returnsListening() {
        QuestionDto q = QuestionDto.builder().id(70L).skillGroupLabel("Nghe hiểu (Listening)").build();
        assertEquals("LISTENING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_category_hanTu_returnsKanji() {
        QuestionDto q = QuestionDto.builder().id(71L).category("Hán tự (Kanji)").build();
        assertEquals("KANJI", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_category_nguPhap_returnsGrammar() {
        QuestionDto q = QuestionDto.builder().id(72L).category("Ngữ pháp (Bunpou)").build();
        assertEquals("GRAMMAR", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_category_tuVung_returnsVocabulary() {
        QuestionDto q = QuestionDto.builder().id(73L).category("Từ vựng cơ bản").build();
        assertEquals("VOCABULARY", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_caseInsensitive_skillGroup() {
        QuestionDto q = QuestionDto.builder().id(74L).skillGroup("reading").build();
        assertEquals("READING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_audioUrl_fallbackListening() {
        QuestionDto q = QuestionDto.builder().id(75L).audioUrl("https://cdn.example.com/audio.mp3").build();
        assertEquals("LISTENING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_contentKeyword_dokkai_returnsReading() {
        QuestionDto q = QuestionDto.builder().id(76L).content("Đọc hiểu đoạn văn sau").build();
        assertEquals("READING", scoring.resolveCanonicalSkill(q));
    }

    @Test
    void resolveCanonicalSkill_noMetadata_defaultsVocabulary() {
        QuestionDto q = QuestionDto.builder().id(77L).content("Chọn đáp án đúng").build();
        assertEquals("VOCABULARY", scoring.resolveCanonicalSkill(q));
    }

    // ── NEW: resolveMilestonePassingThreshold edge cases ──

    @Test
    void milestoneThreshold_customZero_fallsBackTo50Percent() {
        int result = scoring.resolveMilestonePassingThreshold(100, 0);
        assertEquals(50, result);
    }

    @Test
    void milestoneThreshold_negativeCustom_fallsBackTo50Percent() {
        int result = scoring.resolveMilestonePassingThreshold(80, -10);
        assertEquals(40, result);
    }

    @Test
    void milestoneThreshold_totalZero_customPositive_returnsCustom() {
        int result = scoring.resolveMilestonePassingThreshold(0, 30);
        assertEquals(30, result);
    }

    @Test
    void milestoneThreshold_exceedsTotal_under100_treatedAsPercentage() {
        int result = scoring.resolveMilestonePassingThreshold(40, 75);
        assertEquals(30, result);
    }

    @Test
    void milestoneThreshold_above100_clampedToTotal() {
        int result = scoring.resolveMilestonePassingThreshold(50, 200);
        assertEquals(50, result);
    }

    // ── NEW: calculateSkillBreakdown ──

    @Test
    void calculateSkillBreakdown_singleSkill_producesOneEntry() {
        List<QuestionDto> qs = List.of(scoredQuestion(1L, 10, "VOCABULARY", true));
        List<ExamResultResponse.SkillScoreDto> bd = scoring.calculateSkillBreakdown(qs, false);
        assertEquals(1, bd.size());
        assertEquals("VOCABULARY", bd.get(0).getSkillGroup());
        assertEquals(10, bd.get(0).getEarnedPoints());
    }

    @Test
    void calculateSkillBreakdown_allFiveSkills_producesFiveEntries() {
        List<QuestionDto> qs = List.of(
                scoredQuestion(1L, 5, "KANJI", true),
                scoredQuestion(2L, 5, "VOCABULARY", false),
                scoredQuestion(3L, 5, "GRAMMAR", true),
                scoredQuestion(4L, 5, "READING", true),
                scoredQuestion(5L, 5, "LISTENING", false)
        );
        List<ExamResultResponse.SkillScoreDto> bd = scoring.calculateSkillBreakdown(qs, false);
        assertEquals(5, bd.size());
    }

    // ── helpers ──

    private static QuestionDto question(long id, Integer points, String skillGroup, OptionDto... opts) {
        return QuestionDto.builder()
                .id(id)
                .points(points)
                .skillGroup(skillGroup)
                .options(List.of(opts))
                .build();
    }

    private static QuestionDto scoredQuestion(long id, int points, String skillGroup, boolean correct) {
        return QuestionDto.builder()
                .id(id)
                .points(points)
                .skillGroup(skillGroup)
                .isUserCorrect(correct)
                .build();
    }

    private static OptionDto option(long id, boolean correct) {
        return OptionDto.builder().id(id).label("1").text("Option").correct(correct).build();
    }
}
