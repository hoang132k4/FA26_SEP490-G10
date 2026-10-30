package com.jlpp.modules.manager.reports.repository;

import com.jlpp.modules.manager.reports.dto.ManagerReportType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ManagerReportRepository {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final JdbcTemplate jdbcTemplate;

    public ManagerReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<List<String>> findReportRows(ManagerReportType type, LocalDate from, LocalDate to) {
        String dateColumn = type == ManagerReportType.EXAM_RESULTS ? "r.end_time" : "r.registered_at";
        StringBuilder sql = new StringBuilder(type == ManagerReportType.EXAM_RESULTS
                ? """
                    SELECT r.user_id, COALESCE(u.full_name, u.username, u.email, 'Học viên') AS learner_name,
                           e.exam_title AS report_name, e.jlpt_level AS level,
                           r.total_score, r.percentage, r.passed_status, r.end_time AS report_date
                    FROM exam_results r
                    JOIN exams e ON e.exam_id = r.exam_id
                    LEFT JOIN users u ON u.id = r.user_id
                    """
                : """
                    SELECT r.user_id, COALESCE(u.full_name, u.username, u.email, 'Học viên') AS learner_name,
                           c.course_title AS report_name, c.jlpt_level AS level,
                           r.progress_percent, r.status, r.registered_at AS report_date
                    FROM course_registrations r
                    JOIN courses c ON c.course_id = r.course_id
                    LEFT JOIN users u ON u.id = r.user_id
                    """);

        List<Object> parameters = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (from != null) {
            conditions.add(dateColumn + " >= ?");
            parameters.add(Timestamp.from(from.atStartOfDay(VIETNAM_ZONE).toInstant()));
        }
        if (to != null) {
            conditions.add(dateColumn + " <= ?");
            parameters.add(Timestamp.from(to.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant()));
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY ").append(dateColumn).append(" DESC NULLS LAST");

        RowMapper<List<String>> rowMapper = type == ManagerReportType.EXAM_RESULTS
                ? (rs, rowNum) -> List.of(
                        value(rs.getString("user_id")),
                        value(rs.getString("learner_name")),
                        value(rs.getString("report_name")),
                        value(rs.getString("level")),
                        value(rs.getString("total_score")),
                        value(rs.getString("percentage")),
                        rs.getBoolean("passed_status") ? "Đạt" : "Chưa đạt",
                        formatDate(rs.getTimestamp("report_date")))
                : (rs, rowNum) -> List.of(
                        value(rs.getString("user_id")),
                        value(rs.getString("learner_name")),
                        value(rs.getString("report_name")),
                        value(rs.getString("level")),
                        value(rs.getString("progress_percent")),
                        value(rs.getString("status")),
                        formatDate(rs.getTimestamp("report_date")));

        return jdbcTemplate.query(sql.toString(), rowMapper, parameters.toArray());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Timestamp value) {
        return value == null ? "" : value.toInstant().atZone(VIETNAM_ZONE).format(DATE_TIME_FORMATTER);
    }
}
