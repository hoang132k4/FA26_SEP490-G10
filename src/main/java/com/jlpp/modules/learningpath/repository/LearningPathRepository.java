package com.jlpp.modules.learningpath.repository;

import com.jlpp.modules.learningpath.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    List<LearningPath> findByUserIdOrderByScheduledDateDescIdDesc(Long userId);

    Optional<LearningPath> findFirstByUserIdAndPathStatusOrderByScheduledDateDescIdDesc(Long userId, String pathStatus);

    Optional<LearningPath> findFirstByUserIdOrderByScheduledDateDescIdDesc(Long userId);

    boolean existsByUserId(Long userId);
}
