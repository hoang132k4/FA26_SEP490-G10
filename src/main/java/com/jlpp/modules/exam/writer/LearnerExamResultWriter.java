package com.jlpp.modules.exam.writer;

import com.jlpp.modules.exam.dto.QuestionDto;

import java.math.BigDecimal;
import java.util.List;

public interface LearnerExamResultWriter {

    /**
     * Persists exam attempt master record into Supabase exam_results table.
     * Returns the auto-generated result_id from exam_results_result_id_seq.
     */
    Long saveExamResultSummary(Long userId,
            Long examId,
            Integer score,
            BigDecimal percentage,
            boolean passed,
            String resultStatus,
            Integer timeSpentSeconds);

    /**
     * Persists question-level attempt details into Supabase exam_result_details
     * table.
     */
    void saveExamResultDetails(Long resultId, List<QuestionDto> questions);
}
