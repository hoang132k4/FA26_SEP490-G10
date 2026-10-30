package com.jlpp.modules.course.service;

import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.entity.StudyMaterial;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.course.repository.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final LessonRepository lessonRepository;

    public List<StudyMaterial> searchMaterials(String level, String skill, String type, String keyword) {
        String cleanLevel = (level != null && !level.isBlank() && !"ALL".equalsIgnoreCase(level)) ? level.trim().toUpperCase() : null;
        String cleanSkill = (skill != null && !skill.isBlank() && !"ALL".equalsIgnoreCase(skill)) ? skill.trim().toUpperCase() : null;
        String cleanType = (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) ? type.trim().toUpperCase() : null;
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        return studyMaterialRepository.searchMaterials(cleanLevel, cleanSkill, cleanType, cleanKeyword);
    }

    public List<StudyMaterial> getMaterialsByLessonId(Long lessonId) {
        if (lessonId == null) return Collections.emptyList();
        return studyMaterialRepository.findByLessonId(lessonId);
    }

    public List<StudyMaterial> getMaterialsByCourseId(Long courseId) {
        if (courseId == null) return Collections.emptyList();
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        if (lessons.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        return studyMaterialRepository.findByLessonIdIn(lessonIds);
    }

    public StudyMaterial getMaterialById(Long id) {
        return studyMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu học tập với mã ID: " + id));
    }
}
