package com.jeps.modules.course.repository;

import com.jeps.modules.course.entity.CourseRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    Optional<CourseRegistration> findByUserIdAndCourseId(Long userId, Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CourseRegistration r WHERE r.user.id = :userId AND r.course.id = :courseId")
    Optional<CourseRegistration> findForUpdateByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CourseRegistration r WHERE r.id = :registrationId AND r.user.id = :userId AND r.course.id = :courseId")
    Optional<CourseRegistration> findForUpdateByIdAndUserIdAndCourseId(
            @Param("registrationId") Long registrationId,
            @Param("userId") Long userId,
            @Param("courseId") Long courseId);

    @Query("SELECT r FROM CourseRegistration r JOIN FETCH r.course c WHERE r.id = :registrationId AND r.user.id = :userId")
    Optional<CourseRegistration> findByIdAndUserIdWithCourse(
            @Param("registrationId") Long registrationId,
            @Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM CourseRegistration r
            WHERE r.user.id = :userId AND r.course.id = :courseId
              AND UPPER(TRIM(r.status)) IN ('ACTIVE', 'COMPLETED')
            """)
    boolean existsEligibleByUserIdAndCourseId(
            @Param("userId") Long userId, @Param("courseId") Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT r FROM CourseRegistration r JOIN FETCH r.course c WHERE r.user.id = :userId ORDER BY r.registeredAt DESC")
    List<CourseRegistration> findByUserIdWithCourse(@Param("userId") Long userId);

    long countByCourseId(Long courseId);
}
