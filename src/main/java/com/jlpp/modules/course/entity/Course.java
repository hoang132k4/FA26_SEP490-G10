package com.jlpp.modules.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "title")
    private String title;

    @Column(name = "jlpt_level")
    private String jlptLevel;

    @Column(name = "level")
    private String level;

    @Column(name = "price")
    private Double price;

    @Column(name = "description")
    private String description;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "covered_skills")
    private String coveredSkills;

    @Column(name = "status")
    private String status;

    @Column(name = "duration")
    private String duration;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "lesson_count")
    private Integer lessonCount;

    @Column(name = "is_featured")
    private Boolean featured;

    protected Course() {
    }

    public Course(Long courseId, String courseTitle, String jlptLevel, Double price, String description,
                  String status, String duration, String thumbnailUrl, Integer lessonCount, Boolean featured) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.jlptLevel = jlptLevel;
        this.price = price;
        this.description = description;
        this.status = status;
        this.duration = duration;
        this.thumbnailUrl = thumbnailUrl;
        this.lessonCount = lessonCount;
        this.featured = featured;
    }

    public Long getCourseId() { return courseId; }
    public Long getId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getTitle() { return courseTitle != null ? courseTitle : title; }
    public String getJlptLevel() { return jlptLevel != null ? jlptLevel : level; }
    public String getLevel() { return getJlptLevel(); }
    public Double getPrice() { return price; }
    public String getDescription() { return shortDescription != null && !shortDescription.isBlank() ? shortDescription : description; }
    public String getCoveredSkills() { return coveredSkills; }
    public String getStatus() { return status; }
    public String getDuration() { return duration; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getThumbnail() { return thumbnailUrl; }
    public Integer getLessonCount() { return lessonCount; }
    public Boolean getFeatured() { return featured; }
}
