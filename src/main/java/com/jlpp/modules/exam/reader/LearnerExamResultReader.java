package com.jlpp.modules.exam.reader;

import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;

import java.util.List;
import java.util.Optional;

public interface LearnerExamResultReader {

    /**
     * Retrieves all historical exam attempts for the authenticated learner from Supabase exam_results.
     */
    List<ExamHistoryDto> findExamHistoryByUserId(Long userId);

    /**
     * Retrieves specific exam attempt result by resultId and verifies ownership by userId.
     */
    Optional<ExamResultResponse> findExamResultByIdAndUserId(Long resultId, Long userId);

    /**
     * Checks if a result exists in exam_results for any user (used to distinguish 403 Forbidden vs 404 Not Found).
     */
    boolean existsById(Long resultId);

    /**
     * Retrieves a map of question_id -> selected_option from exam_result_details for the specified resultId.
     */
    java.util.Map<Long, String> findSelectedOptionsByResultId(Long resultId);
}
