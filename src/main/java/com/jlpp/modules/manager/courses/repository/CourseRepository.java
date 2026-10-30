package com.jlpp.modules.manager.courses.repository;

import com.jlpp.modules.manager.courses.dto.CourseOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

@Repository("managerCourseRepository")
public class CourseRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<CourseOption> courseRowMapper = (rs, row) -> new CourseOption(
            rs.getLong("course_id"), rs.getString("course_title"), rs.getString("jlpt_level"),
            rs.getString("status"), rs.getInt("lesson_count"));

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CourseOption> findAll() {
        return jdbcTemplate.query("""
                SELECT c.course_id, c.course_title, c.jlpt_level, c.status,
                       COUNT(DISTINCT l.lesson_id) AS lesson_count
                FROM courses c
                LEFT JOIN lessons l ON l.course_id = c.course_id
                GROUP BY c.course_id, c.course_title, c.jlpt_level, c.status
                ORDER BY c.jlpt_level DESC, c.course_id
                """, courseRowMapper);
    }

    public Optional<CourseOption> findById(long courseId) {
        return jdbcTemplate.query("""
                SELECT c.course_id, c.course_title, c.jlpt_level, c.status,
                       COUNT(DISTINCT l.lesson_id) AS lesson_count
                FROM courses c
                LEFT JOIN lessons l ON l.course_id = c.course_id
                WHERE c.course_id = ?
                GROUP BY c.course_id, c.course_title, c.jlpt_level, c.status
                """, courseRowMapper, courseId).stream().findFirst();
    }

    public Optional<String> findStatusById(long courseId) {
        return jdbcTemplate.query("SELECT status FROM courses WHERE course_id = ?",
                (rs, row) -> rs.getString("status"), courseId).stream().findFirst();
    }

    public int transitionStatus(long courseId, String status) {
        List<String> columns = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection ->
                matchingColumns(connection.getMetaData().getColumns(null, null, "courses", null)));
        if (columns.contains("published_date")) {
            return jdbcTemplate.update("""
                    UPDATE courses
                    SET status = ?,
                        published_date = CASE
                            WHEN UPPER(?) = 'PUBLISHED'
                                 AND UPPER(COALESCE(status, '')) <> 'PUBLISHED'
                            THEN CURRENT_TIMESTAMP
                            ELSE published_date
                        END
                    WHERE course_id = ?
                    """, status, status, courseId);
        }
        return jdbcTemplate.update("UPDATE courses SET status = ? WHERE course_id = ?", status, courseId);
    }

    /** Returns every persisted course column for the manager detail view. */
    public Optional<Map<String, Object>> findDetailsById(long courseId) {
        return jdbcTemplate.queryForList("SELECT * FROM courses WHERE course_id = ?", courseId)
                .stream().findFirst();
    }

    public long insert(String title, String level, String status, Double price, String description,
                       Integer durationHours, String objectives, String prerequisites, String targetAudience) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            Map<String, Object> values = courseValues(title, level, status, price, description, durationHours,
                    objectives, prerequisites, targetAudience);
            List<String> columns = matchingColumns(connection.getMetaData().getColumns(null, null, "courses", null));
            if ("PUBLISHED".equalsIgnoreCase(status) && columns.contains("published_date")) {
                values.put("published_date", null);
            }
            List<String> selected = values.keySet().stream().filter(columns::contains).toList();
            String expressions = selected.stream().map(column -> "published_date".equals(column) ? "CURRENT_TIMESTAMP" : "?")
                    .collect(java.util.stream.Collectors.joining(", "));
            String sql = "INSERT INTO courses (" + String.join(", ", selected) + ") VALUES (" + expressions + ")";
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"course_id"});
            int parameterIndex = 1;
            for (String column : selected) {
                if (!"published_date".equals(column)) statement.setObject(parameterIndex++, values.get(column));
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Database không trả về mã khóa học mới.");
        return key.longValue();
    }

    public int update(long courseId, String title, String level, Double price, String description,
                      Integer durationHours, String objectives, String prerequisites, String targetAudience) {
        Map<String, Object> values = courseValues(title, level, null, price, description, durationHours,
                objectives, prerequisites, targetAudience);
        values.remove("status");
        List<String> columns = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection ->
                matchingColumns(connection.getMetaData().getColumns(null, null, "courses", null)));
        List<String> selected = values.keySet().stream().filter(columns::contains).toList();
        List<Object> parameters = new ArrayList<>();
        String assignments = selected.stream().map(column -> { parameters.add(values.get(column)); return column + " = ?"; })
                .collect(java.util.stream.Collectors.joining(", "));
        parameters.add(courseId);
        return jdbcTemplate.update("UPDATE courses SET " + assignments + " WHERE course_id = ?", parameters.toArray());
    }

    private Map<String, Object> courseValues(String title, String level, String status, Double price,
                                             String description, Integer durationHours, String objectives,
                                             String prerequisites, String targetAudience) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("course_title", title);
        values.put("title", title);
        values.put("jlpt_level", level);
        values.put("level", level);
        values.put("status", status);
        values.put("price", price);
        values.put("short_description", description);
        values.put("full_description", description);
        values.put("description", description);
        values.put("duration_hours", durationHours);
        values.put("objectives", objectives);
        values.put("prerequisites", prerequisites);
        values.put("target_audience", targetAudience);
        return values;
    }

    private List<String> matchingColumns(java.sql.ResultSet metadata) throws java.sql.SQLException {
        try (metadata) {
            Set<String> allowed = new java.util.HashSet<>(courseValues("", "", "", 0d, "", 0, "", "", "").keySet());
            allowed.add("published_date");
            allowed.add("status");
            List<String> columns = new ArrayList<>();
            while (metadata.next()) {
                String name = metadata.getString("COLUMN_NAME");
                if (allowed.contains(name.toLowerCase(Locale.ROOT))) columns.add(name.toLowerCase(Locale.ROOT));
            }
            return columns;
        }
    }
}
