package com.jlpp.modules.flashcard.repository;

import com.jlpp.modules.flashcard.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByLessonId(Long lessonId);

    List<Flashcard> findByJlptLevel(String jlptLevel);

    List<Flashcard> findByReviewStatus(String reviewStatus);

    List<Flashcard> findByCustomDeckName(String customDeckName);

    @Query("SELECT f FROM Flashcard f WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(f.vocabulary) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(f.reading) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(f.meaning) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:level IS NULL OR :level = '' OR f.jlptLevel = :level) AND " +
           "(:lessonId IS NULL OR f.lessonId = :lessonId) AND " +
           "(:reviewStatus IS NULL OR :reviewStatus = '' OR f.reviewStatus = :reviewStatus) " +
           "ORDER BY f.flashcardId DESC")
    List<Flashcard> searchFlashcards(@Param("keyword") String keyword,
                                     @Param("level") String level,
                                     @Param("lessonId") Long lessonId,
                                     @Param("reviewStatus") String reviewStatus);

    long countByReviewStatus(String reviewStatus);

    long countByJlptLevel(String jlptLevel);
}
