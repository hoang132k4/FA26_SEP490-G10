package com.jlpp.modules.exam.writer.impl;

import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.writer.LearnerExamResultWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcLearnerExamResultWriter implements LearnerExamResultWriter {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Long saveExamResultSummary(Long userId,
                                      Long examId,
                                      Integer score,
                                      BigDecimal percentage,
                                      boolean passed,
                                      String resultStatus,
                                      Integer timeSpentSeconds) {
        if (userId == null || examId == null) {
            log.warn("Cannot persist exam result: userId or examId is null (userId={}, examId={})", userId, examId);
            return null;
        }

        try {
            Instant now = Instant.now();
            int spent = (timeSpentSeconds != null && timeSpentSeconds > 0) ? timeSpentSeconds : 0;
            Instant start = now.minusSeconds(spent);

            Timestamp startTime = Timestamp.from(start);
            Timestamp endTime = Timestamp.from(now);

            String sql = """
                INSERT INTO exam_results (user_id, exam_id, start_time, end_time, total_score, percentage, passed_status, result_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING result_id
                """;

            Long resultId = jdbcTemplate.queryForObject(
                    sql,
                    Long.class,
                    userId,
                    examId,
                    startTime,
                    endTime,
                    score != null ? score : 0,
                    percentage != null ? percentage : BigDecimal.ZERO,
                    passed,
                    resultStatus != null ? resultStatus : "COMPLETED"
            );

            log.info("Successfully persisted master exam result to Supabase exam_results: result_id={}, user_id={}, exam_id={}, score={}",
                    resultId, userId, examId, score);
            return resultId;
        } catch (Exception e) {
            log.error("Failed to persist exam result summary for userId={}, examId={}: {}", userId, examId, e.getMessage(), e);
            throw (e instanceof RuntimeException re) ? re : new RuntimeException("Database error saving master exam result", e);
        }
    }

    @Override
    public void saveExamResultDetails(Long resultId, List<QuestionDto> questions) {
        if (resultId == null || questions == null || questions.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO exam_result_details (result_id, question_id, selected_option, is_correct, earned_score)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (result_id, question_id) DO UPDATE
            SET selected_option = EXCLUDED.selected_option,
                is_correct = EXCLUDED.is_correct,
                earned_score = EXCLUDED.earned_score
            """;

        List<Object[]> batchParams = new ArrayList<>();
        for (QuestionDto q : questions) {
            if (q == null || q.getId() == null) continue;
            String selectedOption = q.getUserAnswerId() != null ? String.valueOf(q.getUserAnswerId()) : null;
            boolean isCorrect = Boolean.TRUE.equals(q.getIsUserCorrect());
            int earned = isCorrect ? (q.getPoints() != null ? q.getPoints() : 0) : 0;
            batchParams.add(new Object[]{resultId, q.getId(), selectedOption, isCorrect, earned});
        }

        if (!batchParams.isEmpty()) {
            try {
                jdbcTemplate.batchUpdate(sql, batchParams);
                log.info("Successfully persisted {} question details into exam_result_details for resultId {}",
                        batchParams.size(), resultId);
            } catch (Exception e) {
                log.error("Failed to persist exam_result_details for resultId {}: {}", resultId, e.getMessage(), e);
                throw (e instanceof RuntimeException re) ? re : new RuntimeException("Database error saving exam result details", e);
            }
        }
    }
}
