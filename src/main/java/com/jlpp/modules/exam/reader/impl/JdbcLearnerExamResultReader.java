package com.jlpp.modules.exam.reader.impl;

import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.reader.LearnerExamResultReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcLearnerExamResultReader implements LearnerExamResultReader {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public List<ExamHistoryDto> findExamHistoryByUserId(Long userId) {
        if (userId == null) return new ArrayList<>();

        String sql = """
            SELECT r.result_id, r.exam_id, e.title AS exam_title, e.exam_type, e.jlpt_level,
                   r.start_time, r.end_time, r.total_score, r.percentage, r.passed_status, r.result_status,
                   ROW_NUMBER() OVER (PARTITION BY r.user_id, r.exam_id ORDER BY r.end_time ASC NULLS LAST, r.result_id ASC) AS attempt_number
            FROM exam_results r
            JOIN exams e ON e.exam_id = r.exam_id
            WHERE r.user_id = ?
            ORDER BY r.end_time DESC NULLS LAST, r.result_id DESC
            """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String type = rs.getString("exam_type");
                Timestamp endTime = rs.getTimestamp("end_time");
                String formattedDate = endTime != null 
                        ? endTime.toInstant().atZone(VIETNAM_ZONE).format(DATE_TIME_FORMATTER) 
                        : "N/A";

                return ExamHistoryDto.builder()
                        .resultId(rs.getLong("result_id"))
                        .examId(rs.getLong("exam_id"))
                        .examTitle(rs.getString("exam_title"))
                        .examType(type)
                        .examTypeLabel(resolveTypeLabel(type))
                        .jlptLevel(rs.getString("jlpt_level"))
                        .totalScore(rs.getInt("total_score"))
                        .percentage(rs.getBigDecimal("percentage"))
                        .passedStatus(rs.getBoolean("passed_status"))
                        .resultStatus(rs.getString("result_status"))
                        .formattedDate(formattedDate)
                        .attemptNumber(rs.getInt("attempt_number"))
                        .build();
            }, userId);
        } catch (Exception e) {
            log.error("Failed to query exam history from Supabase for userId {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public Optional<ExamResultResponse> findExamResultByIdAndUserId(Long resultId, Long userId) {
        if (resultId == null || userId == null) return Optional.empty();

        String sql = """
            SELECT r.result_id, r.exam_id, e.title AS exam_title, e.exam_type, e.jlpt_level, e.passing_score,
                   r.start_time, r.end_time, r.total_score, r.percentage, r.passed_status, r.result_status
            FROM exam_results r
            JOIN exams e ON e.exam_id = r.exam_id
            WHERE r.result_id = ? AND r.user_id = ?
            """;

        try {
            List<ExamResultResponse> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
                String type = rs.getString("exam_type");
                Timestamp startTime = rs.getTimestamp("start_time");
                Timestamp endTime = rs.getTimestamp("end_time");

                int durationSeconds = 0;
                if (startTime != null && endTime != null) {
                    durationSeconds = (int) Math.max(0, (endTime.getTime() - startTime.getTime()) / 1000);
                }

                int mins = durationSeconds / 60;
                int secs = durationSeconds % 60;
                String formattedTime = String.format("%02d:%02d", mins, secs);

                String formattedDate = endTime != null 
                        ? endTime.toInstant().atZone(VIETNAM_ZONE).format(DATE_TIME_FORMATTER) 
                        : "N/A";

                return ExamResultResponse.builder()
                        .resultId(rs.getLong("result_id"))
                        .examId(rs.getLong("exam_id"))
                        .examTitle(rs.getString("exam_title"))
                        .examType(type)
                        .jlptLevel(rs.getString("jlpt_level"))
                        .score(rs.getInt("total_score"))
                        .totalPossibleScore(null)
                        .percentage(rs.getBigDecimal("percentage"))
                        .passed(rs.getBoolean("passed_status"))
                        .passingScore(rs.getObject("passing_score") != null ? rs.getInt("passing_score") : null)
                        .timeSpentSeconds(durationSeconds)
                        .formattedTimeSpent(formattedTime)
                        .completionDate(formattedDate)
                        .persistentSaved(true)
                        .detailsAvailable(false)
                        .statusNotice("Lịch sử thi được tải từ cơ sở dữ liệu Supabase. Chi tiết từng câu hỏi trong lần thi này hiện chưa khả dụng do ràng buộc dữ liệu từ hệ thống quản lý đề thi.")
                        .build();
            }, resultId, userId);

            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            log.error("Failed to query exam result from Supabase for resultId {} and userId {}: {}", resultId, userId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long resultId) {
        if (resultId == null) return false;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM exam_results WHERE result_id = ?",
                    Integer.class,
                    resultId
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Error checking existence of resultId {}: {}", resultId, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<Long, String> findSelectedOptionsByResultId(Long resultId) {
        if (resultId == null) return Collections.emptyMap();
        String sql = "SELECT question_id, selected_option FROM exam_result_details WHERE result_id = ?";
        try {
            Map<Long, String> map = new HashMap<>();
            jdbcTemplate.query(sql, (rs) -> {
                long qId = rs.getLong("question_id");
                String opt = rs.getString("selected_option");
                map.put(qId, opt);
            }, resultId);
            return map;
        } catch (Exception e) {
            log.warn("Could not query exam_result_details for resultId {}: {}", resultId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String resolveTypeLabel(String type) {
        if (type == null) return "Bài thi";
        return switch (type.toUpperCase()) {
            case "PLACEMENT_TEST" -> "Đánh giá đầu vào";
            case "MILESTONE" -> "Kiểm tra cột mốc";
            case "JLPT_MOCK" -> "Thi thử JLPT";
            default -> type;
        };
    }
}
