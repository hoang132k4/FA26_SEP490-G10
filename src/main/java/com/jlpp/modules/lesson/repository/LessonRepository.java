package com.jlpp.modules.lesson.repository;

import com.jlpp.modules.lesson.dto.LessonPreview;
import com.jlpp.modules.lesson.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("""
            select new com.jlpp.modules.lesson.dto.LessonPreview(
                lesson.lessonId,
                coalesce(lesson.lessonTitle, lesson.title),
                coalesce(lesson.orderIndex, lesson.lessonOrder),
                lesson.skillType,
                lesson.estimatedDuration,
                lesson.durationMinutes,
                lesson.lessonType,
                lesson.sectionName
            )
            from Lesson lesson
            where lesson.courseId = :courseId
            order by coalesce(lesson.orderIndex, lesson.lessonOrder), lesson.lessonId
            """)
    List<LessonPreview> findPreviewsByCourseId(@Param("courseId") Long courseId);
}
