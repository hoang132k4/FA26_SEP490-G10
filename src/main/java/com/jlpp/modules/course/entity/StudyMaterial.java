package com.jlpp.modules.course.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long materialId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "material_title", nullable = false, length = 255)
    private String materialTitle;

    @Column(name = "material_type", length = 50)
    private String materialType; // PDF, AUDIO, HANDBOOK, VOCAB_LIST, SLIDES

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "body_content", columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "jlpt_level", length = 20)
    private String jlptLevel; // N5, N4, N3, N2, N1

    @Column(name = "skill_type", length = 50)
    private String skillType; // VOCABULARY, GRAMMAR, LISTENING, READING, KANJI

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    // Formatted helper methods
    public String getFormattedTypeBadge() {
        if (materialType == null) return "Tài liệu";
        return switch (materialType.toUpperCase()) {
            case "PDF" -> "Tài liệu PDF";
            case "AUDIO" -> "File Audio";
            case "HANDBOOK" -> "Sổ tay tra cứu";
            case "VOCAB_LIST" -> "Bảng từ vựng";
            case "SLIDES" -> "Slide bài giảng";
            default -> materialType;
        };
    }

    public String getTypeIcon() {
        if (materialType == null) return "bi-file-earmark-text";
        return switch (materialType.toUpperCase()) {
            case "PDF" -> "bi-file-earmark-pdf text-danger";
            case "AUDIO" -> "bi-file-earmark-music text-primary";
            case "HANDBOOK" -> "bi-journal-bookmark text-success";
            case "VOCAB_LIST" -> "bi-card-text text-warning";
            case "SLIDES" -> "bi-file-earmark-slides text-info";
            default -> "bi-file-earmark-text";
        };
    }

    public String getSkillBadgeColor() {
        if (skillType == null) return "secondary";
        return switch (skillType.toUpperCase()) {
            case "GRAMMAR" -> "primary";
            case "VOCABULARY" -> "warning";
            case "LISTENING" -> "info";
            case "READING" -> "success";
            case "KANJI" -> "danger";
            default -> "secondary";
        };
    }

    public String getSkillDisplayName() {
        if (skillType == null) return "Tổng hợp";
        return switch (skillType.toUpperCase()) {
            case "GRAMMAR" -> "Ngữ pháp";
            case "VOCABULARY" -> "Từ vựng";
            case "LISTENING" -> "Nghe hiểu";
            case "READING" -> "Đọc hiểu";
            case "KANJI" -> "Hán tự";
            default -> skillType;
        };
    }
}
