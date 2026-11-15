package com.jlpp.modules.dashboard.service;

import com.jlpp.modules.dashboard.dto.ManagerDashboardData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ManagerDashboardService {

    private static final Logger log = LoggerFactory.getLogger(ManagerDashboardService.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ManagerDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ManagerDashboardData getDashboardData() {
        long totalCourses = queryCount("SELECT COUNT(*) FROM courses");
        long publishedCourses = queryCount("SELECT COUNT(*) FROM courses WHERE UPPER(status) = 'PUBLISHED'");
        long draftCourses = queryCount("SELECT COUNT(*) FROM courses WHERE UPPER(status) = 'DRAFT'");

        long totalLessons = queryCount("SELECT COUNT(*) FROM lessons");
        long activeLessons = queryCount("SELECT COUNT(*) FROM lessons WHERE UPPER(status) = 'ACTIVE'");

        long totalQuestions = queryCount("SELECT COUNT(*) FROM questions");
        long totalExams = queryCount("SELECT COUNT(*) FROM exams");
        long publishedExams = queryCount("SELECT COUNT(*) FROM exams WHERE UPPER(status) IN ('PUBLISHED', 'ACTIVE')");
        long totalQuizzes = queryCount("SELECT COUNT(*) FROM exercises");
        long totalFlashcards = queryCount("SELECT COUNT(*) FROM flashcards");

        long totalEnrollments = queryCount("SELECT COUNT(*) FROM course_registrations");
        long totalAttempts = queryCount("SELECT COUNT(*) FROM exam_results");
        long passedAttempts = queryCount("SELECT COUNT(*) FROM exam_results WHERE passed_status = true");

        double passRate = totalAttempts > 0 ? Math.round(((double) passedAttempts / totalAttempts) * 1000.0) / 10.0 : 0.0;

        Map<String, Long> coursesByLevel = queryLevelCounts("courses", "jlpt_level");
        Map<String, Long> questionsByLevel = queryLevelCounts("questions", "jlpt_level");

        List<ManagerDashboardData.RecentCourse> recentCourses = queryRecentCourses();
        List<ManagerDashboardData.RecentExamResult> recentExamResults = queryRecentExamResults();
        List<ManagerDashboardData.RecentEnrollment> recentEnrollments = queryRecentEnrollments();

        return new ManagerDashboardData(
                totalCourses,
                publishedCourses,
                draftCourses,
                totalLessons,
                activeLessons,
                totalQuestions,
                totalExams,
                publishedExams,
                totalQuizzes,
                totalFlashcards,
                totalEnrollments,
                totalAttempts,
                passedAttempts,
                passRate,
                coursesByLevel,
                questionsByLevel,
                recentCourses,
                recentExamResults,
                recentEnrollments
        );
    }

    private long queryCount(String sql) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Dashboard count query failed: {} - {}", sql, e.getMessage());
            return 0L;
        }
    }

    private Map<String, Long> queryLevelCounts(String table, String levelColumn) {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("N5", 0L);
        result.put("N4", 0L);
        result.put("N3", 0L);
        result.put("N2", 0L);
        result.put("N1", 0L);

        try {
            String sql = "SELECT UPPER(TRIM(" + levelColumn + ")) AS lvl, COUNT(*) AS cnt FROM "
                    + table + " WHERE " + levelColumn + " IS NOT NULL GROUP BY " + levelColumn;
            jdbcTemplate.query(sql, (rs) -> {
                String lvl = rs.getString("lvl");
                long cnt = rs.getLong("cnt");
                if (lvl != null && result.containsKey(lvl)) {
                    result.put(lvl, cnt);
                }
            });
        } catch (Exception e) {
            log.warn("Dashboard level count failed for {}.{}: {}", table, levelColumn, e.getMessage());
        }
        return result;
    }

    private List<ManagerDashboardData.RecentCourse> queryRecentCourses() {
        try {
            String sql = """
                    SELECT c.course_id, c.course_title, c.jlpt_level, c.status,
                           (SELECT COUNT(*) FROM lessons l WHERE l.course_id = c.course_id) AS lesson_count
                    FROM courses c
                    ORDER BY c.course_id DESC
                    LIMIT 5
                    """;
            return jdbcTemplate.query(sql, (rs, rowNum) -> new ManagerDashboardData.RecentCourse(
                    rs.getLong("course_id"),
                    rs.getString("course_title"),
                    "",
                    rs.getString("jlpt_level"),
                    rs.getString("status"),
                    rs.getInt("lesson_count"),
                    ""
            ));
        } catch (Exception e) {
            log.warn("Dashboard recent courses query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ManagerDashboardData.RecentExamResult> queryRecentExamResults() {
        try {
            String sql = """
                    SELECT r.user_id, COALESCE(u.full_name, u.username, u.email, 'Học viên') AS learner_name,
                           e.exam_title, e.jlpt_level, r.total_score, r.percentage, r.passed_status, r.end_time
                    FROM exam_results r
                    JOIN exams e ON e.exam_id = r.exam_id
                    LEFT JOIN users u ON u.id = r.user_id
                    ORDER BY r.end_time DESC NULLS LAST
                    LIMIT 5
                    """;
            return jdbcTemplate.query(sql, (rs, rowNum) -> new ManagerDashboardData.RecentExamResult(
                    rs.getString("user_id"),
                    rs.getString("learner_name"),
                    rs.getString("exam_title"),
                    rs.getString("jlpt_level"),
                    rs.getString("total_score"),
                    rs.getString("percentage"),
                    rs.getBoolean("passed_status"),
                    formatDate(rs.getTimestamp("end_time"))
            ));
        } catch (Exception e) {
            log.warn("Dashboard recent exam results query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ManagerDashboardData.RecentEnrollment> queryRecentEnrollments() {
        try {
            String sql = """
                    SELECT r.user_id, COALESCE(u.full_name, u.username, u.email, 'Học viên') AS learner_name,
                           c.course_title, c.jlpt_level, r.progress_percent, r.status, r.registered_at
                    FROM course_registrations r
                    JOIN courses c ON c.course_id = r.course_id
                    LEFT JOIN users u ON u.id = r.user_id
                    ORDER BY r.registered_at DESC NULLS LAST
                    LIMIT 5
                    """;
            return jdbcTemplate.query(sql, (rs, rowNum) -> new ManagerDashboardData.RecentEnrollment(
                    rs.getString("user_id"),
                    rs.getString("learner_name"),
                    rs.getString("course_title"),
                    rs.getString("jlpt_level"),
                    rs.getString("progress_percent"),
                    rs.getString("status"),
                    formatDate(rs.getTimestamp("registered_at"))
            ));
        } catch (Exception e) {
            log.warn("Dashboard recent enrollments query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String formatDate(Timestamp value) {
        if (value == null) {
            return "—";
        }
        try {
            return value.toInstant().atZone(VIETNAM_ZONE).format(DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return value.toString();
        }
    }
}

