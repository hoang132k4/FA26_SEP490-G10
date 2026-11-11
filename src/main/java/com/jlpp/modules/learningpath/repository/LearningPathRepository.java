package com.jlpp.modules.learningpath.repository;

import com.jlpp.modules.learningpath.entity.LearningPath;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    List<LearningPath> findByUserIdOrderByScheduledDateDescIdDesc(Long userId);

    List<LearningPath> findByUserIdAndCourseId(Long userId, Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lp FROM LearningPath lp WHERE lp.userId = :userId AND lp.courseId = :courseId ORDER BY lp.scheduledDate DESC, lp.id DESC")
    List<LearningPath> findForUpdateByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    Optional<LearningPath> findFirstByUserIdAndPathStatusOrderByScheduledDateDescIdDesc(Long userId, String pathStatus);

    Optional<LearningPath> findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(Long userId, Long courseId);

    Optional<LearningPath> findFirstByUserIdOrderByScheduledDateDescIdDesc(Long userId);

    boolean existsByUserId(Long userId);
}
