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
            rs.getLong("course_id"), rs.getString("course_title"), rs.getString("course_code"), rs.getString("jlpt_level"),
            rs.getString("status"), rs.getInt("lesson_count"), rs.getString("course_short_description"), rs.getString("course_description"),
            rs.getString("course_thumbnail"), nullableDouble(rs.getObject("course_price")),
            nullableDouble(rs.getObject("course_discount_price")), nullableInteger(rs.getObject("course_duration_minutes")),
            rs.getString("course_skills"), nullableBoolean(rs.getObject("course_featured")));

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CourseOption> findAll() {
        Set<String> columns = allCourseColumns();
        return jdbcTemplate.query(courseSelect(columns) + " ORDER BY c.jlpt_level DESC, c.course_id", courseRowMapper);
    }

    public Optional<CourseOption> findById(long courseId) {
        return jdbcTemplate.query(courseSelect(allCourseColumns()) + " WHERE c.course_id = ?", courseRowMapper, courseId)
                .stream().findFirst();
    }

    private String courseSelect(Set<String> columns) {
        return "SELECT c.course_id, " + optionalColumn(columns, "course_code", "code", "course_code") + ", c.course_title, c.jlpt_level, c.status, "
                + optionalColumn(columns, "course_short_description", "short_description", "course_description", "description") + ", "
                + optionalColumn(columns, "course_description", "full_description", "description") + ", "
                + optionalColumn(columns, "course_thumbnail", "thumbnail_url", "thumbnail") + ", "
                + optionalColumn(columns, "course_price", "price") + ", "
                + optionalColumn(columns, "course_discount_price", "discount_price") + ", "
                + optionalColumn(columns, "course_skills", "covered_skills") + ", "
                + optionalColumn(columns, "course_featured", "is_featured", "featured") + ", "
                + "(SELECT CASE WHEN COUNT(l.duration_minutes) = 0 THEN NULL ELSE SUM(l.duration_minutes) END "
                + "FROM lessons l WHERE l.course_id = c.course_id) AS course_duration_minutes, "
                + "(SELECT COUNT(*) FROM lessons l WHERE l.course_id = c.course_id) AS lesson_count "
                + "FROM courses c";
    }

    private String optionalColumn(Set<String> columns, String alias, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate)) return "c." + candidate + " AS " + alias;
        }
        return "NULL AS " + alias;
    }

    private Set<String> allCourseColumns() {
        return jdbcTemplate.execute((ConnectionCallback<Set<String>>) connection -> {
            Set<String> columns = new java.util.HashSet<>();
            try (java.sql.ResultSet metadata = connection.getMetaData().getColumns(null, null, "courses", null)) {
                while (metadata.next()) columns.add(metadata.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
            return columns;
        });
    }

    private static Double nullableDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static Integer nullableInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Boolean nullableBoolean(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        if (value instanceof Number number) return number.intValue() != 0;
        return value == null ? null : Boolean.parseBoolean(value.toString());
    }

    public Optional<String> findStatusById(long courseId) {
        return jdbcTemplate.query("SELECT status FROM courses WHERE course_id = ?",
                (rs, row) -> rs.getString("status"), courseId).stream().findFirst();
    }

    public int transitionStatus(long courseId, String status) {
        List<String> columns = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection ->
                matchingColumns(connection.getMetaData().getColumns(null, null, "courses", null)));
        String updatedAtAssignment = columns.contains("updated_at") ? ", updated_at = CURRENT_TIMESTAMP" : "";
        if (columns.contains("published_date")) {
            String sql = """
                    UPDATE courses
                    SET status = ?,
                        published_date = CASE
                            WHEN UPPER(?) = 'PUBLISHED'
                                 AND UPPER(COALESCE(status, '')) <> 'PUBLISHED'
                            THEN CURRENT_TIMESTAMP
                            ELSE published_date
                        END
                    """ + updatedAtAssignment + " WHERE course_id = ?";
            return jdbcTemplate.update(sql, status, status, courseId);
        }
        return jdbcTemplate.update("UPDATE courses SET status = ?" + updatedAtAssignment + " WHERE course_id = ?", status, courseId);
    }

    /** Returns every persisted course column for the manager detail view. */
    public Optional<Map<String, Object>> findDetailsById(long courseId) {
        return jdbcTemplate.queryForList("SELECT * FROM courses WHERE course_id = ?", courseId)
                .stream().findFirst();
    }

    public boolean existsByCode(String code, Long excludedCourseId) {
        return existsByValue(code, excludedCourseId, "code", "course_code");
    }

    public boolean existsByTitle(String title, Long excludedCourseId) {
        return existsByValue(title, excludedCourseId, "course_title", "title");
    }

    private boolean existsByValue(String value, Long excludedCourseId, String... candidates) {
        Set<String> columns = allCourseColumns();
        String column = java.util.Arrays.stream(candidates).filter(columns::contains).findFirst().orElse(null);
        if (column == null) return false;
        String sql = "SELECT COUNT(*) FROM courses WHERE LOWER(" + column + ") = LOWER(?)"
                + (excludedCourseId == null ? "" : " AND course_id <> ?");
        Long matches = excludedCourseId == null
                ? jdbcTemplate.queryForObject(sql, Long.class, value)
                : jdbcTemplate.queryForObject(sql, Long.class, value, excludedCourseId);
        return matches != null && matches > 0;
    }

    public long insert(String title, String code, String level, String status, Double price, Double discountPrice,
                       String shortDescription, String description, String thumbnailUrl, String objectives,
                       String prerequisites, String targetAudience) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            Map<String, Object> values = courseValues(title, code, level, status, price, discountPrice,
                    shortDescription, description, thumbnailUrl, objectives, prerequisites, targetAudience);
            List<String> columns = matchingColumns(connection.getMetaData().getColumns(null, null, "courses", null));
            if ("PUBLISHED".equalsIgnoreCase(status) && columns.contains("published_date")) {
                values.put("published_date", null);
            }
            if (columns.contains("updated_at")) values.put("updated_at", null);
            List<String> selected = values.keySet().stream().filter(columns::contains).toList();
            String expressions = selected.stream().map(column -> "published_date".equals(column) || "updated_at".equals(column)
                            ? "CURRENT_TIMESTAMP" : "?")
                    .collect(java.util.stream.Collectors.joining(", "));
            String sql = "INSERT INTO courses (" + String.join(", ", selected) + ") VALUES (" + expressions + ")";
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"course_id"});
            int parameterIndex = 1;
            for (String column : selected) {
                if (!"published_date".equals(column) && !"updated_at".equals(column)) {
                    statement.setObject(parameterIndex++, values.get(column));
                }
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Database không trả về mã khóa học mới.");
        return key.longValue();
    }

    public int update(long courseId, String title, String code, String level, Double price, Double discountPrice,
                      String shortDescription, String description, String thumbnailUrl, String objectives,
                      String prerequisites, String targetAudience) {
        Map<String, Object> values = courseValues(title, code, level, null, price, discountPrice,
                shortDescription, description, thumbnailUrl, objectives, prerequisites, targetAudience);
        values.remove("status");
        List<String> columns = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection ->
                matchingColumns(connection.getMetaData().getColumns(null, null, "courses", null)));
        List<String> selected = values.keySet().stream().filter(columns::contains).toList();
        List<Object> parameters = new ArrayList<>();
        List<String> assignments = new ArrayList<>();
        selected.forEach(column -> {
            parameters.add(values.get(column));
            assignments.add(column + " = ?");
        });
        if (columns.contains("updated_at")) assignments.add("updated_at = CURRENT_TIMESTAMP");
        parameters.add(courseId);
        return jdbcTemplate.update("UPDATE courses SET " + String.join(", ", assignments) + " WHERE course_id = ?", parameters.toArray());
    }

    private Map<String, Object> courseValues(String title, String code, String level, String status, Double price,
                                             Double discountPrice, String shortDescription, String description,
                                             String thumbnailUrl, String objectives,
                                             String prerequisites, String targetAudience) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("course_title", title);
        values.put("title", title);
        values.put("code", code);
        values.put("course_code", code);
        values.put("jlpt_level", level);
        values.put("level", level);
        values.put("status", status);
        values.put("price", price);
        values.put("course_price", price);
        values.put("discount_price", discountPrice);
        values.put("course_discount_price", discountPrice);
        values.put("short_description", shortDescription);
        values.put("full_description", description);
        values.put("description", description);
        values.put("course_description", shortDescription);
        values.put("thumbnail_url", thumbnailUrl);
        values.put("course_thumbnail", thumbnailUrl);
        values.put("thumbnail", thumbnailUrl);
        values.put("objectives", objectives);
        values.put("prerequisites", prerequisites);
        values.put("target_audience", targetAudience);
        return values;
    }

    private List<String> matchingColumns(java.sql.ResultSet metadata) throws java.sql.SQLException {
        try (metadata) {
            Set<String> allowed = new java.util.HashSet<>(courseValues("", "", "", "", 0d, 0d, "", "", "", "", "", "").keySet());
            allowed.add("published_date");
            allowed.add("updated_at");
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
