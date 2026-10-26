package com.jlpp.modules.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private String category;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "order_index")
    private Integer orderIndex;

    private Integer points;

    @Column(name = "exam_id")
    private Long examId;

    protected Question() { }

    public Question(String content, String category, String audioUrl, String explanation,
                    Integer orderIndex, Integer points, Long examId) {
        this.content = content;
        this.category = category;
        this.audioUrl = audioUrl;
        this.explanation = explanation;
        this.orderIndex = orderIndex;
        this.points = points;
        this.examId = examId;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getAudioUrl() { return audioUrl; }
    public String getExplanation() { return explanation; }
    public Integer getOrderIndex() { return orderIndex; }
    public Integer getPoints() { return points; }
    public Long getExamId() { return examId; }

    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public void setPoints(Integer points) { this.points = points; }
    public void setExamId(Long examId) { this.examId = examId; }
}
