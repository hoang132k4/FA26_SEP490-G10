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

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "skill_type", length = 50)
    private String skillType;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "is_free_preview")
    @Builder.Default
    private Boolean isFreePreview = false;

    public String getTypeBadgeClass() {
        if (lessonType == null) return "bg-secondary";
        return switch (lessonType.toUpperCase()) {
            case "VIDEO", "THEORY" -> "bg-primary text-white";
            case "READING" -> "bg-info text-white";
            case "PRACTICE" -> "bg-warning text-dark";
            case "QUIZ" -> "bg-success text-white";
            default -> "bg-secondary text-white";
        };
    }

    public String getTypeIcon() {
        if (lessonType == null) return "bi-journal-text";
        return switch (lessonType.toUpperCase()) {
            case "VIDEO", "THEORY" -> "bi-journal-bookmark-fill";
            case "READING" -> "bi-book-half";
            case "PRACTICE" -> "bi-pencil-square";
            case "QUIZ" -> "bi-question-circle-fill";
            default -> "bi-journal-text";
        };
    }

    public Long getLessonId() {
        return getId();
    }

    public String getTypeDisplayName() {
        if (lessonType == null) return "Bài học";
        return switch (lessonType.toUpperCase()) {
            case "VIDEO", "THEORY" -> "Bài học lý thuyết";
            case "READING" -> "Bài đọc & Ngữ liệu";
            case "PRACTICE" -> "Thực hành ứng dụng";
            case "QUIZ" -> "Trắc nghiệm kiến thức";
            default -> lessonType;
        };
    }
}
