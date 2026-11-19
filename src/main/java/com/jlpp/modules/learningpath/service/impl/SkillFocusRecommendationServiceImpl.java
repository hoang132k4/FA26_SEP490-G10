package com.jlpp.modules.learningpath.service.impl;

import com.jlpp.modules.learningpath.dto.LearningPathDtos.*;

import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.entity.StudyMaterial;
import com.jlpp.modules.course.repository.StudyMaterialRepository;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import com.jlpp.modules.learningpath.service.SkillFocusRecommendationService;
import com.jlpp.modules.quiz.entity.Exercise;
import com.jlpp.modules.quiz.repository.ExerciseRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class SkillFocusRecommendationServiceImpl implements SkillFocusRecommendationService {

    private final ExerciseRepository exerciseRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final EntityManager entityManager;

    public SkillFocusRecommendationServiceImpl(ExerciseRepository exerciseRepository,
                                                FlashcardRepository flashcardRepository,
                                                StudyMaterialRepository studyMaterialRepository) {
        this(exerciseRepository, flashcardRepository, studyMaterialRepository, null);
    }

    @Autowired
    public SkillFocusRecommendationServiceImpl(ExerciseRepository exerciseRepository,
                                                FlashcardRepository flashcardRepository,
                                                StudyMaterialRepository studyMaterialRepository,
                                                @Autowired(required = false) EntityManager entityManager) {
        this.exerciseRepository = exerciseRepository;
        this.flashcardRepository = flashcardRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.entityManager = entityManager;
    }

    // Configurable heuristic multipliers
    public static final double MULTIPLIER_WEAK = 1.5;
    public static final double MULTIPLIER_MEDIUM = 1.0;
    public static final double MULTIPLIER_STRONG = 0.75;
    public static final double MULTIPLIER_UNKNOWN = 1.0;

    @Override
    public LessonSkillFocusDto recommendFocus(Lesson lesson, SkillProfileDto profile) {
        if (lesson == null) {
            return null;
        }

        int exCount = 0;
        int fcCount = 0;
        int smCount = 0;
        if (!lesson.isMilestoneExam() && lesson.getId() != null) {
            try {
                Long lessonId = lesson.getId();
                List<Exercise> exercises = exerciseRepository.findAllByLessonId(lessonId);
                exCount = exercises != null ? exercises.size() : 0;
                List<Flashcard> flashcards = flashcardRepository.findByLessonId(lessonId);
                fcCount = flashcards != null ? flashcards.size() : 0;
                List<StudyMaterial> materials = studyMaterialRepository.findByLessonId(lessonId);
                smCount = materials != null ? materials.size() : 0;
            } catch (Exception e) {
                log.warn("Could not query review resources for lessonId={}: {}", lesson.getId(), e.getMessage());
            }
        }

        return buildFocusDto(lesson, profile, exCount, fcCount, smCount);
    }

    @Override
    public List<LessonSkillFocusDto> recommendFocusBatch(List<Lesson> lessons, SkillProfileDto profile) {
        if (lessons == null || lessons.isEmpty()) {
            return List.of();
        }

        List<Long> lessonIds = lessons.stream()
                .filter(l -> l != null && !l.isMilestoneExam() && l.getId() != null)
                .map(Lesson::getId)
                .toList();

        Map<Long, Integer> exCounts = queryCountsByLessonIds("Exercise", lessonIds);
        Map<Long, Integer> fcCounts = queryCountsByLessonIds("Flashcard", lessonIds);
        Map<Long, Integer> smCounts = queryCountsByLessonIds("StudyMaterial", lessonIds);

        List<LessonSkillFocusDto> result = new ArrayList<>(lessons.size());
        for (Lesson lesson : lessons) {
            int ex = (lesson != null && lesson.getId() != null) ? exCounts.getOrDefault(lesson.getId(), 0) : 0;
            int fc = (lesson != null && lesson.getId() != null) ? fcCounts.getOrDefault(lesson.getId(), 0) : 0;
            int sm = (lesson != null && lesson.getId() != null) ? smCounts.getOrDefault(lesson.getId(), 0) : 0;
            result.add(buildFocusDto(lesson, profile, ex, fc, sm));
        }
        return result;
    }

    private Map<Long, Integer> queryCountsByLessonIds(String entityName, List<Long> lessonIds) {
        Map<Long, Integer> counts = new HashMap<>();
        if (lessonIds == null || lessonIds.isEmpty()) {
            return counts;
        }
        if (entityManager == null) {
            for (Long lid : lessonIds) {
                try {
                    if ("Exercise".equalsIgnoreCase(entityName) && exerciseRepository != null) {
                        List<Exercise> list = exerciseRepository.findAllByLessonId(lid);
                        if (list != null && !list.isEmpty()) counts.put(lid, list.size());
                    } else if ("Flashcard".equalsIgnoreCase(entityName) && flashcardRepository != null) {
                        List<Flashcard> list = flashcardRepository.findByLessonId(lid);
                        if (list != null && !list.isEmpty()) counts.put(lid, list.size());
                    } else if ("StudyMaterial".equalsIgnoreCase(entityName) && studyMaterialRepository != null) {
                        List<StudyMaterial> list = studyMaterialRepository.findByLessonId(lid);
                        if (list != null && !list.isEmpty()) counts.put(lid, list.size());
                    }
                } catch (Exception ignored) {
                }
            }
            return counts;
        }
        try {
            List<Object[]> rows = entityManager.createQuery(
                    "SELECT e.lessonId, count(e) FROM " + entityName + " e WHERE e.lessonId IN :lessonIds GROUP BY e.lessonId",
                    Object[].class)
                    .setParameter("lessonIds", lessonIds)
                    .getResultList();
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    counts.put((Long) row[0], ((Number) row[1]).intValue());
                }
            }
        } catch (Exception e) {
            log.warn("Batch count query failed for {}: {}", entityName, e.getMessage());
        }
        return counts;
    }

    private LessonSkillFocusDto buildFocusDto(Lesson lesson, SkillProfileDto profile, int exCount, int fcCount, int smCount) {
        if (lesson == null) return null;

        int baseDuration = (lesson.getDurationMinutes() != null && lesson.getDurationMinutes() > 0)
                ? lesson.getDurationMinutes()
                : 60;

        if (lesson.isMilestoneExam()) {
            return LessonSkillFocusDto.builder()
                    .lessonId(lesson.getId())
                    .lessonTitle(lesson.getTitle())
                    .rawSkillType("EXAM")
                    .skillDisplayName("Kiểm tra cột mốc")
                    .proficiency(SkillProficiency.UNKNOWN)
                    .durationMultiplier(1.0)
                    .baseDurationMinutes(baseDuration)
                    .recommendedDurationMinutes(baseDuration)
                    .focusBadgeText("Kiểm tra cột mốc [Bắt buộc]")
                    .focusBadgeClass("bg-warning text-dark")
                    .recommendationReason("Bài kiểm tra định kỳ bắt buộc để đánh giá năng lực và mở khóa giai đoạn học tập tiếp theo.")
                    .availableExercisesCount(0)
                    .availableFlashcardsCount(0)
                    .availableStudyMaterialsCount(0)
                    .reviewResourcesSummary("Bài kiểm tra đánh giá định kỳ")
                    .hasReviewResources(false)
                    .build();
        }

        String rawSkillType = lesson.getSkillType();
        String normalizedSkillKey = normalizeSkillKey(rawSkillType, lesson.getTitle());
        String skillDisplayName = resolveSkillDisplayName(normalizedSkillKey);

        SkillProficiency proficiency = SkillProficiency.UNKNOWN;
        SkillMetricDto metric = null;
        if (profile != null) {
            proficiency = profile.getProficiencyForSkill(normalizedSkillKey);
            metric = profile.getSkill(normalizedSkillKey);
        }

        double multiplier;
        String badgeText;
        String badgeClass;
        String reason;

        String resourceSummary = formatReviewResources(exCount, fcCount, smCount);
        boolean hasResources = (exCount + fcCount + smCount) > 0;

        switch (proficiency) {
            case WEAK -> {
                multiplier = MULTIPLIER_WEAK;
                badgeText = "Trọng tâm cần củng cố [x1.5]";
                badgeClass = "bg-danger text-white";
                String accStr = (metric != null && metric.getAccuracyPercentage() != null)
                        ? " (" + metric.getAccuracyPercentage() + "% trong bài kiểm tra)"
                        : "";
                reason = "Kỹ năng " + skillDisplayName + " của bạn đang cần cải thiện" + accStr
                        + ". Hệ thống khuyến nghị dành thêm thời gian (" + (int) Math.round(baseDuration * multiplier)
                        + " phút) để nắm vững lý thuyết và ôn luyện kỹ bài học này.";
                if (hasResources) {
                    reason += " Hãy kết hợp làm thêm " + resourceSummary + " có sẵn của bài học.";
                }
            }
            case STRONG -> {
                multiplier = MULTIPLIER_STRONG;
                badgeText = "Nền tảng vững [x0.75]";
                badgeClass = "bg-success text-white";
                String accStr = (metric != null && metric.getAccuracyPercentage() != null)
                        ? " (" + metric.getAccuracyPercentage() + "% trong bài kiểm tra)"
                        : "";
                reason = "Bạn đã có nền tảng vững vàng cho kỹ năng " + skillDisplayName + accStr
                        + ". Bạn có thể đẩy nhanh tiến độ hoàn thành trong khoảng " + Math.max(15, (int) Math.round(baseDuration * multiplier))
                        + " phút và chuyển sang luyện tập củng cố.";
            }
            case MEDIUM -> {
                multiplier = MULTIPLIER_MEDIUM;
                badgeText = "Tiếp thu chuẩn [x1.0]";
                badgeClass = "bg-primary text-white";
                String accStr = (metric != null && metric.getAccuracyPercentage() != null)
                        ? " (" + metric.getAccuracyPercentage() + "%)"
                        : "";
                reason = "Kỹ năng " + skillDisplayName + " đạt mức yêu cầu" + accStr
                        + ". Duy trì nhịp độ chuẩn (" + baseDuration + " phút) để tiếp thu trọn vẹn kiến thức.";
            }
            default -> {
                multiplier = MULTIPLIER_UNKNOWN;
                badgeText = "Thời lượng chuẩn [x1.0]";
                badgeClass = "bg-secondary text-white";
                reason = "Chưa có dữ liệu bài đánh giá kỹ năng. Áp dụng thời lượng học tập tiêu chuẩn (" + baseDuration + " phút).";
            }
        }

        int recommendedDuration = Math.max(15, (int) Math.round(baseDuration * multiplier));

        return LessonSkillFocusDto.builder()
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .rawSkillType(rawSkillType)
                .skillDisplayName(skillDisplayName)
                .proficiency(proficiency)
                .durationMultiplier(multiplier)
                .baseDurationMinutes(baseDuration)
                .recommendedDurationMinutes(recommendedDuration)
                .focusBadgeText(badgeText)
                .focusBadgeClass(badgeClass)
                .recommendationReason(reason)
                .availableExercisesCount(exCount)
                .availableFlashcardsCount(fcCount)
                .availableStudyMaterialsCount(smCount)
                .reviewResourcesSummary(resourceSummary)
                .hasReviewResources(hasResources)
                .build();
    }

    private String normalizeSkillKey(String rawSkillType, String lessonTitle) {
        if (rawSkillType != null && !rawSkillType.isBlank()) {
            String upper = rawSkillType.trim().toUpperCase();
            if (upper.contains("KANJI") || upper.contains("HÁN")) return "KANJI";
            if (upper.contains("VOCAB") || upper.contains("TỪ VỰNG")) return "VOCABULARY";
            if (upper.contains("GRAMMAR") || upper.contains("NGỮ PHÁP")) return "GRAMMAR";
            if (upper.contains("READING") || upper.contains("ĐỌC")) return "READING";
            if (upper.contains("LISTEN") || upper.contains("NGHE")) return "LISTENING";
            return upper;
        }

        // Infer from title if skillType is null
        if (lessonTitle != null) {
            String titleLower = lessonTitle.toLowerCase();
            if (titleLower.contains("hán tự") || titleLower.contains("kanji") || titleLower.contains("chữ hán")) {
                return "KANJI";
            }
            if (titleLower.contains("từ vựng") || titleLower.contains("vocab") || titleLower.contains("goi")) {
                return "VOCABULARY";
            }
            if (titleLower.contains("ngữ pháp") || titleLower.contains("grammar") || titleLower.contains("bunpou") || titleLower.contains("mẫu câu")) {
                return "GRAMMAR";
            }
            if (titleLower.contains("đọc hiểu") || titleLower.contains("reading") || titleLower.contains("dokkai")) {
                return "READING";
            }
            if (titleLower.contains("nghe hiểu") || titleLower.contains("listening") || titleLower.contains("choukai") || titleLower.contains("ngữ âm")) {
                return "LISTENING";
            }
        }

        return "VOCABULARY";
    }

    private String resolveSkillDisplayName(String skillKey) {
        if (skillKey == null) return "Kiến thức chung";
        return switch (skillKey.toUpperCase()) {
            case "KANJI" -> "Hán tự";
            case "VOCABULARY" -> "Từ vựng";
            case "GRAMMAR" -> "Ngữ pháp";
            case "READING" -> "Đọc hiểu";
            case "LISTENING" -> "Nghe hiểu";
            default -> skillKey;
        };
    }

    private String formatReviewResources(int exCount, int fcCount, int smCount) {
        List<String> parts = new ArrayList<>();
        if (exCount > 0) parts.add(exCount + " bài tập củng cố");
        if (fcCount > 0) parts.add(fcCount + " thẻ flashcard");
        if (smCount > 0) parts.add(smCount + " tài liệu đính kèm");

        if (parts.isEmpty()) {
            return "Không có tài liệu bổ trợ ngoài";
        }
        return String.join(", ", parts);
    }
}

