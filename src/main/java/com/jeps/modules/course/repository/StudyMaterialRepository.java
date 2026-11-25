package com.jeps.modules.course.repository;

import com.jeps.modules.course.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByLessonId(Long lessonId);

    List<StudyMaterial> findByLessonIdIn(List<Long> lessonIds);

    @Query("""
        SELECT m FROM StudyMaterial m
        WHERE (:level IS NULL OR :level = '' OR :level = 'ALL' OR m.jlptLevel = :level)
          AND (:skill IS NULL OR :skill = '' OR :skill = 'ALL' OR m.skillType = :skill)
          AND (:type IS NULL OR :type = '' OR :type = 'ALL' OR m.materialType = :type)
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(m.materialTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY m.materialId ASC
    """)
    List<StudyMaterial> searchMaterials(
            @Param("level") String level,
            @Param("skill") String skill,
            @Param("type") String type,
            @Param("keyword") String keyword
    );
}
