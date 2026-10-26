package com.jlpp.modules.course.repository;

import com.jlpp.modules.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    Optional<Course> findByCode(String code);

    List<Course> findByStatusOrderByCreatedAtDesc(String status);

    List<Course> findByLevelAndStatus(String level, String status);

    List<Course> findTop4ByFeaturedTrueAndStatus(String status);

    List<Course> findTop3ByLevelAndStatusAndIdNot(String level, String status, Long id);

    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' " +
           "AND (:level IS NULL OR :level = '' OR c.level = :level) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.coveredSkills) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchCourses(
            @Param("keyword") String keyword,
            @Param("level") String level,
            Pageable pageable
    );

    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' " +
           "AND c.level = :level ORDER BY c.enrollmentCount DESC")
    List<Course> findRecommendedByLevel(@Param("level") String level);

    @Query("SELECT c FROM Course c WHERE c.featured = true ORDER BY c.id ASC")
    List<Course> findByFeaturedTrueOrderByCourseIdAsc();
}
