package com.jlpp.modules.course.repository;

import com.jlpp.modules.course.entity.CourseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    Optional<CourseRegistration> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT r FROM CourseRegistration r JOIN FETCH r.course c WHERE r.user.id = :userId ORDER BY r.registeredAt DESC")
    List<CourseRegistration> findByUserIdWithCourse(@Param("userId") Long userId);

    long countByCourseId(Long courseId);
}
