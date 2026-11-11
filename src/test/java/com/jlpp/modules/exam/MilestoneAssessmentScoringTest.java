package com.jlpp.modules.exam;

import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.StudyMaterialRepository;
import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.dto.ScoringEvaluationResult;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.exam.service.impl.AssessmentScoringServiceImpl;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import com.jlpp.modules.learningpath.dto.LessonSkillFocusDto;
import com.jlpp.modules.learningpath.dto.SkillProfileDto;
import com.jlpp.modules.learningpath.dto.SkillProficiency;
import com.jlpp.modules.learningpath.service.SkillAnalysisService;
import com.jlpp.modules.learningpath.service.SkillFocusRecommendationService;
import com.jlpp.modules.learningpath.service.impl.SkillAnalysisServiceImpl;
import com.jlpp.modules.learningpath.service.impl.SkillFocusRecommendationServiceImpl;
import com.jlpp.modules.quiz.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneAssessmentScoringTest {

    private AssessmentScoringService scoringService;

    @Mock
    private ExamService examService;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private StudyMaterialRepository studyMaterialRepository;

    private SkillAnalysisService skillAnalysisService;
    private SkillFocusRecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        scoringService = new AssessmentScoringServiceImpl();
        skillAnalysisService = new SkillAnalysisServiceImpl(examService, scoringService);
        recommendationService = new SkillFocusRecommendationServiceImpl(exerciseRepository, flashcardRepository, studyMaterialRepository);
    }

    private List<QuestionDto> createStandardAssessmentQuestions() {
        // 5 questions across 3 skills with varying weights:
        // Q1: Kanji (weight 15)
        // Q2: Vocab (weight 15)
        // Q3: Vocab (weight 20)
        // Q4: Reading (weight 25)
        // Q5: Reading (weight 25)
        // Total possible = 100
        return List.of(
                QuestionDto.builder().id(101L).points(15).category("Hán tự (Kanji)")
                        .options(List.of(OptionDto.builder().id(1001L).correct(true).build())).build(),
                QuestionDto.builder().id(102L).points(15).category("Từ vựng (Goi)")
                        .options(List.of(OptionDto.builder().id(1002L).correct(true).build())).build(),
                QuestionDto.builder().id(103L).points(20).category("Từ vựng (Goi)")
                        .options(List.of(
                                OptionDto.builder().id(1003L).correct(true).build(),
                                OptionDto.builder().id(1004L).correct(false).build())).build(),
                QuestionDto.builder().id(104L).points(25).category("Đọc hiểu (Dokkai)")
                        .options(List.of(OptionDto.builder().id(1005L).correct(true).build())).build(),
                QuestionDto.builder().id(105L).points(25).category("Đọc hiểu (Dokkai)")
                        .options(List.of(OptionDto.builder().id(1006L).correct(true).build())).build()
        );
    }

    @Test
    @DisplayName("Mathematical Consistency - Placement and Milestone produce identical scores for identical inputs")
    void testMathematicalConsistency_PlacementVsMilestone() {
        List<QuestionDto> questions = createStandardAssessmentQuestions();

        // Learner answers:
        // Q1: correct (15)
        // Q2: correct (15)
        // Q3: wrong (0)
        // Q4: correct (25)
        // Q5: unanswered (0)
        // Total earned = 15 + 15 + 25 = 55 / 100 (55.0%)
        Map<String, String> answers = Map.of(
                "answer_101", "1001",
                "answer_102", "1002",
                "answer_103", "1004", // wrong
                "answer_104", "1005"  // correct
                // 105 omitted
        );

        ScoringEvaluationResult placementResult = scoringService.scoreQuestions(
                questions, answers, "PLACEMENT_TEST", "N4", null
        );

        ScoringEvaluationResult milestoneResult = scoringService.scoreQuestions(
                questions, answers, "MILESTONE", "N4", null
        );

        // Core score equality verification
        assertEquals(placementResult.getEarnedScore(), milestoneResult.getEarnedScore());
        assertEquals(placementResult.getTotalPossibleScore(), milestoneResult.getTotalPossibleScore());
        assertEquals(placementResult.getPercentage(), milestoneResult.getPercentage());
        assertEquals(placementResult.getCorrectCount(), milestoneResult.getCorrectCount());
        assertEquals(placementResult.getIncorrectCount(), milestoneResult.getIncorrectCount());
        assertEquals(placementResult.getUnansweredCount(), milestoneResult.getUnansweredCount());
        assertEquals(placementResult.getTotalQuestions(), milestoneResult.getTotalQuestions());

        assertEquals(55, placementResult.getEarnedScore());
        assertEquals(100, placementResult.getTotalPossibleScore());
        assertEquals(new BigDecimal("55.0"), placementResult.getPercentage());
        assertEquals(3, placementResult.getCorrectCount());
        assertEquals(1, placementResult.getIncorrectCount());
        assertEquals(1, placementResult.getUnansweredCount());

        // Purpose separation verification
        // Placement: diagnostic, never fails learner, passing score is null
        assertTrue(placementResult.isPassed());
        assertNull(placementResult.getPassingScore());

        // Milestone: stage mastery decision, passing threshold is 50, 55 >= 50 -> passed
        assertTrue(milestoneResult.isPassed());
        assertEquals(50, milestoneResult.getPassingScore());
    }

    @Test
    @DisplayName("Milestone PASS/FAIL Boundary - Default threshold (50% on 100-point exam)")
    void testMilestonePassingBoundary_DefaultThreshold() {
        QuestionDto q1 = QuestionDto.builder().id(1L).points(49)
                .options(List.of(OptionDto.builder().id(11L).correct(true).build())).build();
        QuestionDto q2 = QuestionDto.builder().id(2L).points(1)
                .options(List.of(OptionDto.builder().id(21L).correct(true).build())).build();
        QuestionDto q3 = QuestionDto.builder().id(3L).points(50)
                .options(List.of(OptionDto.builder().id(31L).correct(true).build())).build();

        // Total possible score = 49 + 1 + 50 = 100 points
        // Default passing threshold = 50% = 50 points

        // Scenario A: 49 earned points -> 49 < 50 -> FAIL
        Map<String, String> answers49 = Map.of("answer_1", "11");
        ScoringEvaluationResult failResult = scoringService.scoreQuestions(
                List.of(q1, q2, q3), answers49, "MILESTONE", "N4", null
        );
        assertEquals(49, failResult.getEarnedScore());
        assertEquals(50, failResult.getPassingScore());
        assertFalse(failResult.isPassed());
        assertTrue(failResult.getAiFeedback().contains("chưa đạt điểm sàn"));

        // Scenario B: 50 earned points -> 50 >= 50 -> PASS
        Map<String, String> answers50 = Map.of("answer_1", "11", "answer_2", "21");
        ScoringEvaluationResult passResult = scoringService.scoreQuestions(
                List.of(q1, q2, q3), answers50, "MILESTONE", "N4", null
        );
        assertEquals(50, passResult.getEarnedScore());
        assertEquals(50, passResult.getPassingScore());
        assertTrue(passResult.isPassed());
        assertTrue(passResult.getAiFeedback().contains("vượt qua bài kiểm tra cột mốc"));
    }

    @Test
    @DisplayName("Milestone PASS/FAIL Boundary - Custom configured threshold (e.g., 70% on 100-point exam)")
    void testMilestonePassingBoundary_CustomThreshold() {
        QuestionDto q1 = QuestionDto.builder().id(1L).points(69)
                .options(List.of(OptionDto.builder().id(11L).correct(true).build())).build();
        QuestionDto q2 = QuestionDto.builder().id(2L).points(1)
                .options(List.of(OptionDto.builder().id(21L).correct(true).build())).build();
        QuestionDto q3 = QuestionDto.builder().id(3L).points(30)
                .options(List.of(OptionDto.builder().id(31L).correct(true).build())).build();

        // Total possible score = 69 + 1 + 30 = 100 points
        // Custom threshold = 70% = 70 points

        // 69 earned points against custom threshold 70 -> FAIL
        ScoringEvaluationResult res69 = scoringService.scoreQuestions(
                List.of(q1, q2, q3), Map.of("answer_1", "11"), "MILESTONE", "N4", 70
        );
        assertEquals(69, res69.getEarnedScore());
        assertEquals(70, res69.getPassingScore());
        assertFalse(res69.isPassed());

        // 70 earned points against custom threshold 70 -> PASS
        ScoringEvaluationResult res70 = scoringService.scoreQuestions(
                List.of(q1, q2, q3), Map.of("answer_1", "11", "answer_2", "21"), "MILESTONE", "N4", 70
        );
        assertEquals(70, res70.getEarnedScore());
        assertEquals(70, res70.getPassingScore());
        assertTrue(res70.isPassed());
    }

    @Test
    @DisplayName("Untested Skills in Milestone - Clearly marked as NOT_ASSESSED")
    void testUntestedSkills_MilestoneAssessment() {
        // Milestone 1 only evaluates Vocabulary & Grammar
        List<QuestionDto> questions = List.of(
                QuestionDto.builder().id(1L).points(25).category("Từ vựng (Goi)")
                        .options(List.of(OptionDto.builder().id(10L).correct(true).build())).build(),
                QuestionDto.builder().id(2L).points(25).category("Ngữ pháp (Bunpou)")
                        .options(List.of(OptionDto.builder().id(20L).correct(true).build())).build()
        );

        ScoringEvaluationResult result = scoringService.scoreQuestions(
                questions,
                Map.of("answer_1", "10", "answer_2", "20"),
                "MILESTONE",
                "N5",
                null
        );

        // Assessed skills only contain tested ones (2 skills)
        assertEquals(2, result.getSkillBreakdown().size());
        assertTrue(result.getSkillBreakdown().stream().allMatch(s -> "ASSESSED".equals(s.getStatus())));

        // Canonical breakdown contains all 5 skills
        assertEquals(5, result.getCanonicalSkills().size());

        ExamResultResponse.SkillScoreDto listening = result.getCanonicalSkills().stream()
                .filter(s -> "LISTENING".equals(s.getSkillGroup())).findFirst().orElseThrow();
        assertEquals("NOT_ASSESSED", listening.getStatus());
        assertEquals(0, listening.getTotalQuestions());
        assertEquals(0, listening.getTotalPoints());
        assertEquals(0, listening.getEarnedPoints());
        assertNull(listening.getPercentage(), "Untested skill must have null percentage, not 0.0");

        ExamResultResponse.SkillScoreDto reading = result.getCanonicalSkills().stream()
                .filter(s -> "READING".equals(s.getSkillGroup())).findFirst().orElseThrow();
        assertEquals("NOT_ASSESSED", reading.getStatus());
        assertEquals(0, reading.getTotalQuestions());
    }

    @Test
    @DisplayName("Learning Path Safety - Milestone assessments do not corrupt Placement baseline")
    void testLearningPathSafety_MilestoneDoesNotOverwritePlacementBaseline() {
        Long userId = 100L;

        // User exam history contains both PLACEMENT_TEST (resultId 10) and later MILESTONE (resultId 20)
        ExamHistoryDto placementHistory = ExamHistoryDto.builder()
                .resultId(10L)
                .examId(1L)
                .examTitle("Bài thi đánh giá năng lực đầu vào")
                .examType("PLACEMENT_TEST")
                .totalScore(80)
                .percentage(new BigDecimal("80.0"))
                .passedStatus(true)
                .build();

        ExamHistoryDto milestoneHistory = ExamHistoryDto.builder()
                .resultId(20L)
                .examId(2L)
                .examTitle("Kiểm tra cột mốc Chaper 1")
                .examType("MILESTONE")
                .totalScore(40)
                .percentage(new BigDecimal("40.0"))
                .passedStatus(false)
                .build();

        when(examService.getLearnerExamHistory(userId)).thenReturn(List.of(placementHistory, milestoneHistory));

        // When analyzeLearnerSkills is called without explicit resultId, it MUST resolve to resultId 10 (Placement)
        // NOT resultId 20 (Milestone)
        ExamResultResponse mockPlacementResult = ExamResultResponse.builder()
                .resultId(10L)
                .examTitle("Bài thi đánh giá năng lực đầu vào")
                .examType("PLACEMENT_TEST")
                .jlptLevel("N4")
                .score(80)
                .totalPossibleScore(100)
                .percentage(new BigDecimal("80.0"))
                .questions(Collections.emptyList())
                .skillBreakdown(List.of(
                        ExamResultResponse.SkillScoreDto.builder()
                                .skillGroup("READING")
                                .totalQuestions(5)
                                .correctCount(4)
                                .build()
                ))
                .build();

        when(examService.getHistoricalResult(10L, userId)).thenReturn(mockPlacementResult);

        SkillProfileDto profile = skillAnalysisService.analyzeLearnerSkills(userId, null);

        assertNotNull(profile);
        assertEquals(10L, profile.getResultId(), "Must select Placement Assessment (resultId 10), not Milestone (resultId 20)");
        assertEquals("N4", profile.getJlptLevel());
        verify(examService, never()).getHistoricalResult(eq(20L), any());
    }

    @Test
    @DisplayName("Risk 3 Validation: Untested skills are treated as UNKNOWN in SkillAnalysisService and receive 1.0x duration in Learning Path")
    void testUntestedSkill_TreatedAsUnknownInSkillAnalysisAndLearningPath() {
        Long userId = 200L;

        // User exam result only tested Reading (5 questions, 4 correct -> 80% accuracy)
        // Canonical breakdown includes all 5 skills, where Kanji, Vocab, Grammar, Listening are NOT_ASSESSED
        List<ExamResultResponse.SkillScoreDto> canonicalSkills = List.of(
                ExamResultResponse.SkillScoreDto.builder()
                        .skillGroup("READING").totalQuestions(5).correctCount(4)
                        .percentage(new BigDecimal("80.0")).status("ASSESSED").build(),
                ExamResultResponse.SkillScoreDto.builder()
                        .skillGroup("KANJI").totalQuestions(0).correctCount(0)
                        .percentage(null).status("NOT_ASSESSED").build(),
                ExamResultResponse.SkillScoreDto.builder()
                        .skillGroup("VOCABULARY").totalQuestions(0).correctCount(0)
                        .percentage(null).status("NOT_ASSESSED").build(),
                ExamResultResponse.SkillScoreDto.builder()
                        .skillGroup("GRAMMAR").totalQuestions(0).correctCount(0)
                        .percentage(null).status("NOT_ASSESSED").build(),
                ExamResultResponse.SkillScoreDto.builder()
                        .skillGroup("LISTENING").totalQuestions(0).correctCount(0)
                        .percentage(null).status("NOT_ASSESSED").build()
        );

        ExamResultResponse mockResult = ExamResultResponse.builder()
                .resultId(50L)
                .examTitle("Đánh giá đầu vào")
                .examType("PLACEMENT_TEST")
                .jlptLevel("N4")
                .score(80)
                .totalPossibleScore(100)
                .percentage(new BigDecimal("80.0"))
                .canonicalSkills(canonicalSkills)
                .skillBreakdown(List.of(canonicalSkills.get(0))) // only reading
                .questions(Collections.emptyList())
                .build();

        when(examService.getHistoricalResult(50L, userId)).thenReturn(mockResult);

        SkillProfileDto profile = skillAnalysisService.analyzeLearnerSkills(userId, 50L);

        assertNotNull(profile);
        // Reading was assessed and scored 80% -> STRONG
        assertEquals(SkillProficiency.STRONG, profile.getSkill("READING").getProficiency());
        assertEquals(new BigDecimal("80.0"), profile.getSkill("READING").getAccuracyPercentage());

        // Untested skills must be UNKNOWN, null percentage, and NEVER in weakSkills
        assertEquals(SkillProficiency.UNKNOWN, profile.getSkill("KANJI").getProficiency());
        assertNull(profile.getSkill("KANJI").getAccuracyPercentage());

        assertEquals(SkillProficiency.UNKNOWN, profile.getSkill("VOCABULARY").getProficiency());
        assertNull(profile.getSkill("VOCABULARY").getAccuracyPercentage());

        assertEquals(SkillProficiency.UNKNOWN, profile.getSkill("GRAMMAR").getProficiency());
        assertNull(profile.getSkill("GRAMMAR").getAccuracyPercentage());

        assertEquals(SkillProficiency.UNKNOWN, profile.getSkill("LISTENING").getProficiency());
        assertNull(profile.getSkill("LISTENING").getAccuracyPercentage());

        // Crucial verification: weak skills must be completely empty!
        assertTrue(profile.getWeakSkills().isEmpty(), "Untested skills MUST NOT be classified as WEAK");

        // Now verify Learning Path recommendation duration multipliers
        Lesson kanjiLesson = new Lesson();
        kanjiLesson.setId(101L);
        kanjiLesson.setTitle("Chữ Hán cơ bản");
        kanjiLesson.setSkillType("KANJI");
        kanjiLesson.setDurationMinutes(60);

        LessonSkillFocusDto kanjiFocus = recommendationService.recommendFocus(kanjiLesson, profile);
        assertEquals(SkillProficiency.UNKNOWN, kanjiFocus.getProficiency());
        assertEquals(1.0, kanjiFocus.getDurationMultiplier(), 0.001, "Untested skill must receive 1.0x standard multiplier");
        assertEquals(60, kanjiFocus.getRecommendedDurationMinutes());
        assertTrue(kanjiFocus.getFocusBadgeText().contains("1.0"));
        assertFalse(kanjiFocus.getFocusBadgeText().contains("1.5"));

        Lesson readingLesson = new Lesson();
        readingLesson.setId(102L);
        readingLesson.setTitle("Luyện đọc hiểu");
        readingLesson.setSkillType("READING");
        readingLesson.setDurationMinutes(60);

        LessonSkillFocusDto readingFocus = recommendationService.recommendFocus(readingLesson, profile);
        assertEquals(SkillProficiency.STRONG, readingFocus.getProficiency());
        assertEquals(0.75, readingFocus.getDurationMultiplier(), 0.001);
        assertEquals(45, readingFocus.getRecommendedDurationMinutes());
    }

    @Test
    @DisplayName("Risk 4 Validation: JLPT Mock Exam scoring preserves standard 180-scale raw threshold rules")
    void testJlptMockExamScoring_PreservesStandardRules() {
        // Build 18 questions of 10 points each = 180 points total (standard JLPT raw scale)
        List<QuestionDto> jlptQuestions = new ArrayList<>();
        Map<String, String> answers90 = new HashMap<>();
        Map<String, String> answers89 = new HashMap<>();

        for (int i = 1; i <= 18; i++) {
            long qId = i;
            long correctOpt = i * 10L + 1;
            long wrongOpt = i * 10L + 2;

            jlptQuestions.add(QuestionDto.builder()
                    .id(qId)
                    .points(10)
                    .category(i <= 6 ? "Từ vựng N4" : (i <= 12 ? "Ngữ pháp N4" : "Đọc hiểu N4"))
                    .options(List.of(
                            OptionDto.builder().id(correctOpt).correct(true).build(),
                            OptionDto.builder().id(wrongOpt).correct(false).build()
                    ))
                    .build());

            // 9 correct questions * 10 = 90 points
            if (i <= 9) {
                answers90.put("answer_" + qId, String.valueOf(correctOpt));
            } else {
                answers90.put("answer_" + qId, String.valueOf(wrongOpt));
            }

            // 8 correct questions * 10 = 80 points
            if (i <= 8) {
                answers89.put("answer_" + qId, String.valueOf(correctOpt));
            } else {
                answers89.put("answer_" + qId, String.valueOf(wrongOpt));
            }
        }

        // JLPT N4 Mock Exam: Standard passing threshold is 90/180
        ScoringEvaluationResult passResult = scoringService.scoreQuestions(
                jlptQuestions,
                answers90,
                "JLPT_MOCK",
                "N4",
                null
        );

        assertEquals(90, passResult.getEarnedScore());
        assertEquals(180, passResult.getTotalPossibleScore());
        assertEquals(90, passResult.getPassingScore(), "JLPT N4 threshold must be 90 raw marks");
        assertTrue(passResult.isPassed(), "90/180 raw marks must pass JLPT N4 Mock Exam");

        // Learner with 80/180 against 90 threshold -> FAIL
        ScoringEvaluationResult failResult = scoringService.scoreQuestions(
                jlptQuestions,
                answers89,
                "JLPT_MOCK",
                "N4",
                null
        );
        assertEquals(80, failResult.getEarnedScore());
        assertEquals(90, failResult.getPassingScore());
        assertFalse(failResult.isPassed(), "80/180 raw marks must fail JLPT N4 Mock Exam (threshold 90)");

        // Verify standard threshold mapping for all JLPT levels
        assertEquals(80, scoringService.resolvePassingScore("JLPT_MOCK", "N5", null));
        assertEquals(90, scoringService.resolvePassingScore("JLPT_MOCK", "N4", null));
        assertEquals(95, scoringService.resolvePassingScore("JLPT_MOCK", "N3", null));
        assertEquals(90, scoringService.resolvePassingScore("JLPT_MOCK", "N2", null));
        assertEquals(100, scoringService.resolvePassingScore("JLPT_MOCK", "N1", null));

        // Custom threshold override on JLPT Mock (e.g. 120 points)
        assertEquals(120, scoringService.resolvePassingScore("JLPT_MOCK", "N4", 120));
    }
}

