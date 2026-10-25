package com.jlpp.modules.course.entity;

import com.jlpp.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", length = 250)
    private String slug;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "level", nullable = false, length = 20)
    private String level; // N5, N4, N3, N2, N1

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "price", nullable = false)
    @Builder.Default
    private Double price = 0.0;

    @Column(name = "discount_price")
    private Double discountPrice;

    @Column(name = "duration_hours")
    @Builder.Default
    private Integer durationHours = 40;

    @Column(name = "lesson_count")
    @Builder.Default
    private Integer lessonCount = 30;

    @Column(name = "covered_skills", length = 255)
    private String coveredSkills; // Chữ Hán, Từ vựng, Ngữ pháp, Đọc hiểu, Nghe hiểu

    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "target_audience", length = 500)
    private String targetAudience;

    @Column(name = "prerequisites", length = 500)
    private String prerequisites;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PUBLISHED"; // PUBLISHED, DRAFT, ARCHIVED

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "enrollment_count")
    @Builder.Default
    private Integer enrollmentCount = 0;

    @Column(name = "rating")
    @Builder.Default
    private Double rating = 4.9;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();
}
