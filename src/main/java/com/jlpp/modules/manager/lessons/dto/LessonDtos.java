package com.jlpp.modules.manager.lessons.dto;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.modules.course.entity.StudyMaterial;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.quiz.dto.QuizQuestionDto;
import com.jlpp.modules.quiz.entity.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shared DTOs used by the manager lessons module. */
public final class LessonDtos {

    private LessonDtos() {
    }

    /** Skill specific data of a lesson, stored outside the {@code lessons} table. */
    public record LessonDetail(
            long lessonId,
            String skillValue,
            Long sourceEntryId,
            String audioUrl,
            String body,
            String payload) {
    }

    /** Raw values coming from the create/edit form. */
    public record LessonInput(
            String title,
            String sectionName,
            String description,
            String content,
            String skill,
            String status,
            Integer orderIndex,
            Integer durationMinutes,
            boolean freePreview) {
    }

    /** One lesson exactly as it is listed in the manager workspace. */
    public record LessonRow(
            long id,
            long courseId,
            int order,
            String title,
            String description,
            String content,
            String sectionName,
            Integer durationMinutes,
            boolean freePreview,
            LessonStatusEnum status,
            String skillValue) {

        /** Metadata line rendered under the title: chapter · duration · short description. */
        public String meta() {
            List<String> parts = new ArrayList<>(3);
            if (sectionName != null && !sectionName.isBlank()) {
                parts.add(sectionName.trim());
            }
            if (durationMinutes != null && durationMinutes > 0) {
                parts.add(durationMinutes + " phút");
            }
            if (description != null && !description.isBlank()) {
                parts.add(description.trim());
            }
            return String.join(" · ", parts);
        }

        public boolean hasMeta() {
            return !meta().isEmpty();
        }

        public boolean isVisible() {
            return status != null && status.isVisible();
        }
    }

    /** One selectable item of a skill library, rendered identically for all three libraries. */
    public record LibraryItem(
            long id,
            String title,
            String subtitle,
            String level,
            String meta,
            String reading,
            String meaning,
            String example) {
    }

    /** One cell of the skill strip: the five skills are always shown, including the empty ones. */
    public record SkillTab(
            String value,
            String label,
            String glyph,
            int lessonCount,
            boolean selected) {

        public boolean isEmpty() {
            return lessonCount == 0;
        }
    }

    public record SkillColumn(String value, String label, String glyph, List<LessonRow> lessons) { }

    public record ChapterGroup(String name, int lessonCount, List<SkillColumn> columns) { }

    public record LessonStudyView(LessonRow lesson, String skillLabel,
                                  List<StudyMaterial> materials, List<Flashcard> flashcards,
                                  List<Map<String, Object>> listeningQuestions,
                                  String readingPassage, List<Map<String, Object>> readingQuestions,
                                  List<PracticeQuizView> practiceQuizzes) { }

    public record PracticeQuizView(Exercise exercise, List<QuizQuestionDto> questions) { }
}
