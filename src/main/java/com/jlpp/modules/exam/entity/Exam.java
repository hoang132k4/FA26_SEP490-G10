package com.jlpp.modules.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "exam_title")
    private String examTitle;

    @Column(name = "exam_type")
    private String examType;

    @Column(name = "jlpt_level")
    private String jlptLevel;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    protected Exam() {
    }

    public Exam(Long examId, String examTitle, String examType, String jlptLevel, Integer durationMinutes,
                Integer totalQuestions, Integer passingScore, String status, String description) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.examType = examType;
        this.jlptLevel = jlptLevel;
        this.durationMinutes = durationMinutes;
        this.totalQuestions = totalQuestions;
        this.passingScore = passingScore;
        this.status = status;
        this.description = description;
    }

    public Long getExamId() { return examId; }
    public Long getId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getTitle() { return examTitle; }
    public String getExamType() { return examType; }
    public String getJlptLevel() { return jlptLevel; }
    public String getLevel() { return jlptLevel; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getTotalQuestions() { return totalQuestions; }
    public Integer getPassingScore() { return passingScore; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }

    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
    public void setExamType(String examType) { this.examType = examType; }
    public void setJlptLevel(String jlptLevel) { this.jlptLevel = jlptLevel; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setPassingScore(Integer passingScore) { this.passingScore = passingScore; }
    public void setStatus(String status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
}
