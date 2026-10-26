package com.jlpp.modules.course.entity;

import com.jlpp.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 1;

    @Column(name = "section_name", length = 150)
    @Builder.Default
    private String sectionName = "Chương 1: Kiến thức nền tảng";

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 20;

    @Column(name = "lesson_type", length = 30)
    @Builder.Default
    private String lessonType = "VIDEO"; // VIDEO, READING, PRACTICE, QUIZ

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_free_preview")
    @Builder.Default
    private Boolean isFreePreview = false;
}
