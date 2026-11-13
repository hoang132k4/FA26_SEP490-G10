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

    @Column(name = "lesson_title", length = 200)
    private String lessonTitle;

    @PrePersist
    @PreUpdate
    public void syncLessonTitle() {
        if (lessonTitle == null && title != null) {
            lessonTitle = title;
        } else if (title == null && lessonTitle != null) {
            title = lessonTitle;
        }
    }

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
    private String status = "ACTIVE";

    @Column(name = "is_free_preview")
    @Builder.Default
    private Boolean isFreePreview = false;

    @Column(name = "exam_id")
    private Long examId;

    public Boolean getIsFreePreview() {
        return Boolean.TRUE.equals(this.isFreePreview);
    }

    public boolean isFreePreview() {
        return Boolean.TRUE.equals(this.isFreePreview);
    }

    public boolean isExam() {
        return "EXAM".equalsIgnoreCase(lessonType) || examId != null;
    }

    public boolean getIsExam() {
        return isExam();
    }

    public String getTypeBadgeClass() {
        if (isExam()) return "bg-danger text-white";
        if (lessonType == null) return "bg-secondary";
        return switch (lessonType.toUpperCase()) {
            case "VIDEO", "THEORY" -> "bg-primary text-white";
            case "READING" -> "bg-info text-white";
            case "PRACTICE" -> "bg-warning text-dark";
            case "QUIZ" -> "bg-success text-white";
            case "EXAM" -> "bg-danger text-white";
            default -> "bg-secondary text-white";
        };
    }

    public String getTypeIcon() {
        if (isExam()) return "bi-clipboard2-check-fill";
        if (lessonType == null) return "bi-journal-text";
        return switch (lessonType.toUpperCase()) {
            case "VIDEO", "THEORY" -> "bi-journal-bookmark-fill";
            case "READING" -> "bi-book-half";
            case "PRACTICE" -> "bi-pencil-square";
            case "QUIZ" -> "bi-question-circle-fill";
            case "EXAM" -> "bi-clipboard2-check-fill";
            default -> "bi-journal-text";
        };
    }

    public Long getLessonId() {
        return getId();
    }

    public String getTypeDisplayName() {
        if (isExam()) return "Bài thi";
        if (lessonType == null) return "Bài học";
        return switch (lessonType.toUpperCase()) {
            case "VIDEO", "THEORY" -> "Bài học lý thuyết";
            case "READING" -> "Bài đọc & Ngữ liệu";
            case "PRACTICE" -> "Thực hành ứng dụng";
            case "QUIZ" -> "Trắc nghiệm kiến thức";
            case "EXAM" -> "Bài thi đánh giá";
            default -> lessonType;
        };
    }
}
