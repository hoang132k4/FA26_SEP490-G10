package com.jlpp.modules.course.repository;

import com.jlpp.modules.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatusOrderByCreatedAtDesc(String status);

    List<Course> findByLevelAndStatus(String level, String status);

    List<Course> findTop3ByLevelAndStatusAndIdNot(String level, String status, Long id);

    @Query("SELECT c FROM Course c WHERE c.featured = true AND (c.status = 'PUBLISHED' OR c.status = 'ACTIVE' OR c.status = 'PUBLIC') ORDER BY c.id ASC")
    List<Course> findByFeaturedTrueOrderByCourseIdAsc();
}
