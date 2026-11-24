package com.jeps.modules.lesson.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity(name = "PublicLesson")
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "skill_type")
    private String skillType;

    @Column(name = "estimated_duration")
    private String estimatedDuration;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "lesson_type")
    private String lessonType;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "status")
    private String status;

    protected Lesson() {
    }

    public Lesson(Long lessonId, Long courseId, String lessonTitle, String title, Integer orderIndex,
                  Integer lessonOrder, String skillType, String estimatedDuration, Integer durationMinutes,
                  String lessonType, String sectionName, String status, String description) {
        this.lessonId = lessonId;
        this.courseId = courseId;
        this.title = title != null ? title : lessonTitle;
        this.orderIndex = orderIndex != null ? orderIndex : lessonOrder;
        this.skillType = skillType;
        this.estimatedDuration = estimatedDuration;
        this.durationMinutes = durationMinutes;
        this.lessonType = lessonType;
        this.sectionName = sectionName;
        this.status = status;
        this.description = description;
    }

    public void syncColumns() {
        // No-op for backwards compatibility after dropping redundant columns
    }

    public void syncLessonColumns() {
        // No-op for backwards compatibility after dropping redundant columns
    }

    public Long getLessonId() { return lessonId; }
    public Long getId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public void setId(Long id) { this.lessonId = id; }
    public Long getCourseId() { return courseId; }
    public String getLessonTitle() { return title; }
    public String getDescription() { return description; }
    public String getTitle() { return title; }
    public Integer getOrderIndex() { return orderIndex; }
    public Integer getLessonOrder() { return orderIndex; }
    public String getSkillType() { return skillType; }
    public String getEstimatedDuration() { return estimatedDuration; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getLessonType() { return lessonType; }
    public String getSectionName() { return sectionName; }
    public String getStatus() { return status; }
}
