package com.jlpp.modules.learningpath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.course.repository.StudyMaterialRepository;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import com.jlpp.modules.learningpath.dto.*;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LearningPathService;
import com.jlpp.modules.learningpath.service.LessonProgressService;
import com.jlpp.modules.learningpath.service.SkillAnalysisService;
import com.jlpp.modules.learningpath.service.SkillFocusRecommendationService;
import com.jlpp.modules.learningpath.service.impl.LessonProgressServiceImpl;
import com.jlpp.modules.learningpath.service.impl.SkillAnalysisServiceImpl;
import com.jlpp.modules.learningpath.service.impl.SkillFocusRecommendationServiceImpl;
import com.jlpp.modules.quiz.repository.ExerciseRepository;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LearningPathPersonalizationTest {

    @Mock
    private ExamService examService;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private StudyMaterialRepository studyMaterialRepository;

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private SkillAnalysisService skillAnalysisService;
    private SkillFocusRecommendationService recommendationService;
    private LessonProgressService lessonProgressService;

    private Lesson createLesson(Long id, String title, Integer durationMinutes, String skillType, Integer orderIndex) {
        Course dummyCourse = Course.builder().title("Dummy Course").build();
        dummyCourse.setId(10L);
        Lesson l = Lesson.builder()
                .course(dummyCourse)
                .title(title)
                .durationMinutes(durationMinutes)
                .skillType(skillType)
                .orderIndex(orderIndex != null ? orderIndex : 1)
                .build();
        l.setId(id);
        return l;
    }

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        skillAnalysisService = new SkillAnalysisServiceImpl(examService);
        recommendationService = new SkillFocusRecommendationServiceImpl(
                exerciseRepository, flashcardRepository, studyMaterialRepository
        );
        lessonProgressService = new LessonProgressServiceImpl(
                learningPathRepository, courseRegistrationRepository, lessonRepository, objectMapper
        );
    }

    @Test
    @DisplayName("Scenario 1: Learner with no Placement Assessment returns UNKNOWN profile")
    void testScenario1_NoPlacementAssessment() {
        when(examService.getLearnerExamHistory(101L)).thenReturn(Collections.emptyList());

        SkillProfileDto profile = skillAnalysisService.analyzeLearnerSkills(101L, null);

        assertThat(profile).isNotNull();
        assertThat(profile.isAssessmentTaken()).isFalse();
        assertThat(profile.getProficiencyForSkill("GRAMMAR")).isEqualTo(SkillProficiency.UNKNOWN);
        assertThat(profile.getProficiencyForSkill("VOCABULARY")).isEqualTo(SkillProficiency.UNKNOWN);
    }

    @Test
    @DisplayName("Scenario 2: Learner with weak Grammar and Reading receives WEAK classification")
    void testScenario2_WeakGrammarAndReading() {
        ExamResultResponse examRes = ExamResultResponse.builder()
                .examTitle("Placement N3")
                .jlptLevel("N3")
                .questions(List.of(
                        QuestionDto.builder().id(1L).category("Ngữ pháp N3").isUserCorrect(false).build(),
                        QuestionDto.builder().id(2L).category("Ngữ pháp N3").isUserCorrect(false).build(),
                        QuestionDto.builder().id(3L).category("Đọc hiểu N3").isUserCorrect(false).build(),
                        QuestionDto.builder().id(4L).category("Từ vựng N3").isUserCorrect(true).build(),
                        QuestionDto.builder().id(5L).category("Từ vựng N3").isUserCorrect(true).build()
                ))
                .build();

        when(examService.getHistoricalResult(eq(555L), eq(102L))).thenReturn(examRes);

        SkillProfileDto profile = skillAnalysisService.analyzeLearnerSkills(102L, 555L);

        assertThat(profile.getProficiencyForSkill("GRAMMAR")).isEqualTo(SkillProficiency.WEAK);
        assertThat(profile.getProficiencyForSkill("READING")).isEqualTo(SkillProficiency.WEAK);
        assertThat(profile.getProficiencyForSkill("VOCABULARY")).isEqualTo(SkillProficiency.STRONG);
        assertThat(profile.getWeakSkills()).contains("Ngữ pháp (Grammar)", "Đọc hiểu (Reading)");
    }

    @Test
    @DisplayName("Scenario 3: Learner with weak Listening and Vocabulary")
    void testScenario3_WeakListeningAndVocab() {
        ExamResultResponse examRes = ExamResultResponse.builder()
                .examTitle("Placement N4")
                .jlptLevel("N4")
                .questions(List.of(
                        QuestionDto.builder().id(1L).category("Nghe hiểu N4").audioUrl("audio.mp3").isUserCorrect(false).build(),
                        QuestionDto.builder().id(2L).category("Từ vựng N4").isUserCorrect(false).build(),
                        QuestionDto.builder().id(3L).category("Ngữ pháp N4").isUserCorrect(true).build(),
                        QuestionDto.builder().id(4L).category("Ngữ pháp N4").isUserCorrect(true).build()
                ))
                .build();

        when(examService.getHistoricalResult(eq(556L), eq(103L))).thenReturn(examRes);

        SkillProfileDto profile = skillAnalysisService.analyzeLearnerSkills(103L, 556L);

        assertThat(profile.getProficiencyForSkill("LISTENING")).isEqualTo(SkillProficiency.WEAK);
        assertThat(profile.getProficiencyForSkill("VOCABULARY")).isEqualTo(SkillProficiency.WEAK);
        assertThat(profile.getProficiencyForSkill("GRAMMAR")).isEqualTo(SkillProficiency.STRONG);
    }

    @Test
    @DisplayName("Scenario 4: Two learners with different skill scores receive DIFFERENT recommended durations")
    void testScenario4_TwoLearnersDifferentProfiles() {
        SkillProfileDto profileA = SkillProfileDto.builder()
                .skills(Map.of(
                        "GRAMMAR", SkillMetricDto.builder().skillKey("GRAMMAR").proficiency(SkillProficiency.WEAK).build(),
                        "VOCABULARY", SkillMetricDto.builder().skillKey("VOCABULARY").proficiency(SkillProficiency.STRONG).build()
                )).build();

        SkillProfileDto profileB = SkillProfileDto.builder()
                .skills(Map.of(
                        "GRAMMAR", SkillMetricDto.builder().skillKey("GRAMMAR").proficiency(SkillProficiency.STRONG).build(),
                        "VOCABULARY", SkillMetricDto.builder().skillKey("VOCABULARY").proficiency(SkillProficiency.WEAK).build()
                )).build();

        Lesson grammarLesson = createLesson(10L, "Ngữ pháp bài 1", 60, "GRAMMAR", 1);
        Lesson vocabLesson = createLesson(20L, "Từ vựng bài 1", 60, "VOCABULARY", 2);

        LessonSkillFocusDto focusA_Grammar = recommendationService.recommendFocus(grammarLesson, profileA);
        LessonSkillFocusDto focusB_Grammar = recommendationService.recommendFocus(grammarLesson, profileB);

        // Learner A (weak in Grammar) gets 1.5x = 90 min; Learner B (strong in Grammar) gets 0.75x = 45 min
        assertThat(focusA_Grammar.getRecommendedDurationMinutes()).isEqualTo(90);
        assertThat(focusB_Grammar.getRecommendedDurationMinutes()).isEqualTo(45);
        assertThat(focusA_Grammar.getRecommendedDurationMinutes()).isGreaterThan(focusB_Grammar.getRecommendedDurationMinutes());

        LessonSkillFocusDto focusA_Vocab = recommendationService.recommendFocus(vocabLesson, profileA);
        LessonSkillFocusDto focusB_Vocab = recommendationService.recommendFocus(vocabLesson, profileB);
        assertThat(focusA_Vocab.getRecommendedDurationMinutes()).isEqualTo(45);
        assertThat(focusB_Vocab.getRecommendedDurationMinutes()).isEqualTo(90);
    }

    @Test
    @DisplayName("Scenario 5: Learner with missing skill questions gets UNKNOWN without error")
    void testScenario5_MissingSkillQuestions() {
        ExamResultResponse examRes = ExamResultResponse.builder()
                .examTitle("Placement Mini")
                .questions(List.of(
                        QuestionDto.builder().id(1L).category("Từ vựng").isUserCorrect(true).build()
                ))
                .build();

        when(examService.getHistoricalResult(eq(777L), eq(105L))).thenReturn(examRes);

        SkillProfileDto profile = skillAnalysisService.analyzeLearnerSkills(105L, 777L);
        assertThat(profile.getProficiencyForSkill("LISTENING")).isEqualTo(SkillProficiency.UNKNOWN);
        assertThat(profile.getSkill("LISTENING").isEvidenceAvailable()).isFalse();
    }

    @Test
    @DisplayName("Scenario 6: Lesson with single skill_type applies recommendation accurately")
    void testScenario6_SingleSkillLesson() {
        Lesson kanjiLesson = createLesson(30L, "Chữ Hán 1", 40, "KANJI", 1);
        SkillProfileDto profile = SkillProfileDto.builder()
                .skills(Map.of("KANJI", SkillMetricDto.builder().skillKey("KANJI").proficiency(SkillProficiency.WEAK).build()))
                .build();

        LessonSkillFocusDto focus = recommendationService.recommendFocus(kanjiLesson, profile);
        assertThat(focus.getProficiency()).isEqualTo(SkillProficiency.WEAK);
        assertThat(focus.getRecommendedDurationMinutes()).isEqualTo(60); // 40 * 1.5 = 60
    }

    @Test
    @DisplayName("Scenario 8 & 9: Review resources presence is correctly flagged")
    void testScenario8_9_ReviewResources() {
        Lesson lessonWithNoReview = createLesson(40L, "Bài học mới", 30, "READING", 1);
        when(exerciseRepository.findAllByLessonId(40L)).thenReturn(Collections.emptyList());
        when(flashcardRepository.findByLessonId(40L)).thenReturn(Collections.emptyList());
        when(studyMaterialRepository.findByLessonId(40L)).thenReturn(Collections.emptyList());

        LessonSkillFocusDto focus = recommendationService.recommendFocus(lessonWithNoReview, null);
        assertThat(focus.isHasReviewResources()).isFalse();
    }

    @Test
    @DisplayName("Scenario 10: Lesson with missing duration_minutes defaults to 60")
    void testScenario10_MissingDuration() {
        Lesson lesson = createLesson(50L, "Bài đọc", null, "READING", 1);
        LessonSkillFocusDto focus = recommendationService.recommendFocus(lesson, null);
        assertThat(focus.getBaseDurationMinutes()).isEqualTo(60);
        assertThat(focus.getRecommendedDurationMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("Scenario 13 & 14: Lesson completion is recorded and re-completion preserves timestamp")
    void testScenario13_14_LessonCompletion() {
        LearningPath lp = LearningPath.builder()
                .id(1L)
                .userId(200L)
                .courseId(10L)
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .scheduledDate(LocalDate.now())
                .build();

        Lesson l501 = createLesson(501L, "Lesson 1", 30, "KANJI", 1);
        when(courseRegistrationRepository.existsByUserIdAndCourseId(200L, 10L)).thenReturn(true);
        when(lessonRepository.findById(501L)).thenReturn(Optional.of(l501));

        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(200L, 10L))
                .thenReturn(Optional.of(lp));
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(10L))
                .thenReturn(List.of(l501));

        lessonProgressService.markLessonCompleted(200L, 10L, 501L);

        assertThat(lp.getLessonProgress()).isNotNull();
        assertThat(lp.getLessonProgress()).contains("\"501\"");
        assertThat(lp.getLessonProgress()).contains("COMPLETED");

        LearningPathProgressDto progress = lessonProgressService.getLessonProgress(200L, 10L);
        assertThat(progress.isLessonCompleted(501L)).isTrue();
        String initialCompletedAt = progress.getLessonProgress(501L).getCompletedAt();
        assertThat(initialCompletedAt).isNotNull();

        // Re-completing preserves timestamp
        lessonProgressService.markLessonCompleted(200L, 10L, 501L);
        LearningPathProgressDto secondProgress = lessonProgressService.getLessonProgress(200L, 10L);
        assertThat(secondProgress.getLessonProgress(501L).getCompletedAt()).isEqualTo(initialCompletedAt);
    }

    @Test
    @DisplayName("Scenario 15: Out-of-order lesson completion tracks only completed lesson")
    void testScenario15_OutOfOrderCompletion() {
        LearningPath lp = LearningPath.builder()
                .id(2L)
                .userId(201L)
                .courseId(10L)
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .scheduledDate(LocalDate.now())
                .build();

        Lesson l101 = createLesson(101L, "Lesson 1", 30, "VOCABULARY", 1);
        Lesson l102 = createLesson(102L, "Lesson 2", 30, "GRAMMAR", 2);
        Lesson l103 = createLesson(103L, "Lesson 3", 30, "READING", 3);

        when(courseRegistrationRepository.existsByUserIdAndCourseId(201L, 10L)).thenReturn(true);
        when(lessonRepository.findById(103L)).thenReturn(Optional.of(l103));

        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(201L, 10L))
                .thenReturn(Optional.of(lp));
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(10L))
                .thenReturn(List.of(l101, l102, l103));

        // Learner completes Lesson 3 before Lesson 1 or 2
        lessonProgressService.markLessonCompleted(201L, 10L, 103L);

        LearningPathProgressDto progress = lessonProgressService.getLessonProgress(201L, 10L);
        assertThat(progress.isLessonCompleted(103L)).isTrue();
        assertThat(progress.isLessonCompleted(101L)).isFalse();
        assertThat(progress.isLessonCompleted(102L)).isFalse();
    }

    @Test
    @DisplayName("Scenario 20: Legacy progress fallback correctly infers completed count")
    void testScenario20_LegacyProgressFallback() {
        // No JSON entries in learning_path, but registration has progressPercent = 35% on 12 lessons
        boolean isLesson1Done = lessonProgressService.isLessonCompleted(
                300L, 10L, 1L, 1, 12, 35
        );
        boolean isLesson4Done = lessonProgressService.isLessonCompleted(
                300L, 10L, 4L, 4, 12, 35
        );
        boolean isLesson5Done = lessonProgressService.isLessonCompleted(
                300L, 10L, 5L, 5, 12, 35
        );

        assertThat(isLesson1Done).isTrue();
        assertThat(isLesson4Done).isTrue();
        assertThat(isLesson5Done).isFalse(); // Lesson 5 is in-progress / not completed
    }

    @Test
    @DisplayName("Verification 1: Security check - Cross-course lesson completion is rejected")
    void testVerification1_CrossCourseRejected() {
        Course otherCourse = Course.builder().title("Other Course").build();
        otherCourse.setId(99L);
        Lesson lessonOfOtherCourse = Lesson.builder()
                .course(otherCourse)
                .title("Foreign Lesson")
                .build();
        lessonOfOtherCourse.setId(999L);

        when(courseRegistrationRepository.existsByUserIdAndCourseId(400L, 10L)).thenReturn(true);
        when(lessonRepository.findById(999L)).thenReturn(Optional.of(lessonOfOtherCourse));

        lessonProgressService.markLessonCompleted(400L, 10L, 999L);

        LearningPathProgressDto progress = lessonProgressService.getLessonProgress(400L, 10L);
        assertThat(progress.isLessonCompleted(999L)).isFalse();
    }

    @Test
    @DisplayName("Verification 2: Security check - Un-enrolled user completion is rejected")
    void testVerification2_UnenrolledRejected() {
        when(courseRegistrationRepository.existsByUserIdAndCourseId(401L, 10L)).thenReturn(false);

        lessonProgressService.markLessonCompleted(401L, 10L, 101L);

        verify(learningPathRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verification 3: Two Learners Full Course Schedule Comparison (Divergence verified)")
    void testVerification3_TwoLearnersFullCourseComparison() {
        SkillProfileDto profileA = SkillProfileDto.builder()
                .skills(Map.of(
                        "GRAMMAR", SkillMetricDto.builder().skillKey("GRAMMAR").proficiency(SkillProficiency.WEAK).build(),
                        "VOCABULARY", SkillMetricDto.builder().skillKey("VOCABULARY").proficiency(SkillProficiency.STRONG).build()
                )).build();

        SkillProfileDto profileB = SkillProfileDto.builder()
                .skills(Map.of(
                        "GRAMMAR", SkillMetricDto.builder().skillKey("GRAMMAR").proficiency(SkillProficiency.STRONG).build(),
                        "VOCABULARY", SkillMetricDto.builder().skillKey("VOCABULARY").proficiency(SkillProficiency.WEAK).build()
                )).build();

        List<Lesson> fullCourse = List.of(
                createLesson(1L, "Bài 1: Từ vựng mở đầu", 40, "VOCABULARY", 1),
                createLesson(2L, "Bài 2: Từ vựng chuyên đề", 40, "VOCABULARY", 2),
                createLesson(3L, "Bài 3: Ngữ pháp mẫu câu", 50, "GRAMMAR", 3),
                createLesson(4L, "Bài 4: Ngữ pháp ứng dụng", 50, "GRAMMAR", 4)
        );

        List<LessonSkillFocusDto> focusListA = recommendationService.recommendFocusBatch(fullCourse, profileA);
        List<LessonSkillFocusDto> focusListB = recommendationService.recommendFocusBatch(fullCourse, profileB);

        // Sequence order must be identical
        for (int i = 0; i < fullCourse.size(); i++) {
            assertThat(focusListA.get(i).getLessonId()).isEqualTo(fullCourse.get(i).getId());
            assertThat(focusListB.get(i).getLessonId()).isEqualTo(fullCourse.get(i).getId());
        }

        // Learner A (Weak Grammar): grammar lessons are 75m (50 * 1.5)
        assertThat(focusListA.get(2).getRecommendedDurationMinutes()).isEqualTo(75);
        // Learner B (Strong Grammar): grammar lessons are 38m (50 * 0.75)
        assertThat(focusListB.get(2).getRecommendedDurationMinutes()).isEqualTo(38);

        // Learner A (Strong Vocab): vocab lessons are 30m (40 * 0.75)
        assertThat(focusListA.get(0).getRecommendedDurationMinutes()).isEqualTo(30);
        // Learner B (Weak Vocab): vocab lessons are 60m (40 * 1.5)
        assertThat(focusListB.get(0).getRecommendedDurationMinutes()).isEqualTo(60);

        // Total recommended minutes diverge appropriately
        int totalA = focusListA.stream().mapToInt(LessonSkillFocusDto::getRecommendedDurationMinutes).sum();
        int totalB = focusListB.stream().mapToInt(LessonSkillFocusDto::getRecommendedDurationMinutes).sum();
        assertThat(totalA).isEqualTo(30 + 30 + 75 + 75); // 210 mins
        assertThat(totalB).isEqualTo(60 + 60 + 38 + 38); // 196 mins
    }
}
