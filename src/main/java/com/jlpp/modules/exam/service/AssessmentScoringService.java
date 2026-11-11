package com.jlpp.modules.exam.service;

import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.dto.ScoringEvaluationResult;
import com.jlpp.modules.exam.dto.SingleQuestionScoreResult;
import com.jlpp.modules.exam.entity.Exam;

import java.util.List;
import java.util.Map;

/**
 * Shared scoring engine for all JEPS assessment types (Placement, Milestone, JLPT Mock).
 * Guarantees consistent mathematical evaluation, question weighting, rounding,
 * handling of unanswered/malformed answers, and skill-level breakdown across all assessments.
 */
public interface AssessmentScoringService {

    /**
     * Score a collection of questions against learner form inputs with an Exam entity context.
     *
     * @param questions   Questions with options and evaluation metadata (correct flags)
     * @param formParams  Form parameters containing learner answers (e.g., answer_{questionId} -> optionId)
     * @param examContext The exam entity providing metadata (type, level, passingScore)
     * @return Complete evaluation result
     */
    ScoringEvaluationResult scoreQuestions(List<QuestionDto> questions,
                                          Map<String, String> formParams,
                                          Exam examContext);

    /**
     * Score a collection of questions against learner form inputs with explicit parameters.
     *
     * @param questions           Questions with options and evaluation metadata
     * @param formParams          Form parameters containing learner answers
     * @param examType            Exam type (e.g., "PLACEMENT_TEST", "MILESTONE", "JLPT_MOCK")
     * @param jlptLevel           Target JLPT level (e.g., "N5", "N4", "N3", "N2", "N1")
     * @param customPassingScore  Custom passing score configured on exam, or null for default
     * @return Complete evaluation result
     */
    ScoringEvaluationResult scoreQuestions(List<QuestionDto> questions,
                                          Map<String, String> formParams,
                                          String examType,
                                          String jlptLevel,
                                          Integer customPassingScore);

    /**
     * Evaluate a single question against a raw user input string.
     *
     * @param question      Question with its options
     * @param rawUserAnswer Raw string option ID submitted by user
     * @return Question evaluation outcome
     */
    SingleQuestionScoreResult scoreSingleQuestion(QuestionDto question, String rawUserAnswer);

    /**
     * Resolves a question into one of the 5 canonical JLPT skills:
     * KANJI, VOCABULARY, GRAMMAR, READING, LISTENING
     *
     * @param question Question DTO
     * @return Canonical skill key
     */
    String resolveCanonicalSkill(QuestionDto question);

    /**
     * Human-readable Vietnamese display name for a canonical skill key.
     *
     * @param skillKey Canonical skill key
     * @return Localized Vietnamese label
     */
    String getSkillDisplayName(String skillKey);

    /**
     * Computes the 5 canonical skills breakdown from evaluated questions.
     *
     * @param scoredQuestions Scored questions containing user correctness and points
     * @param includeUntested Whether to include skills that have 0 questions (marked as NOT_ASSESSED)
     * @return List of skill scores
     */
    List<ExamResultResponse.SkillScoreDto> calculateSkillBreakdown(List<QuestionDto> scoredQuestions, boolean includeUntested);

    /**
     * Resolves default passing score based on exam type and JLPT level.
     *
     * @param examType           Exam type
     * @param jlptLevel          JLPT level
     * @param customPassingScore Optional explicit passing score override
     * @return Resolved integer passing score threshold
     */
    int resolvePassingScore(String examType, String jlptLevel, Integer customPassingScore);

    /**
     * Resolves the unambiguous required passing score for a Milestone Assessment.
     *
     * @param totalPossibleScore Maximum possible points in the exam
     * @param customPassingScore Configured passing score on exam, or null
     * @return Required integer passing score
     */
    int resolveMilestonePassingThreshold(int totalPossibleScore, Integer customPassingScore);
}

