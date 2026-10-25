package com.jlpp.modules.manager.course.repository;

import com.jlpp.modules.manager.course.dto.CourseOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
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

    public long insert(String title, String level, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO courses (course_title, jlpt_level, status) VALUES (?, ?, ?)",
                    new String[] {"course_id"});
            statement.setString(1, title);
            statement.setString(2, level);
            statement.setString(3, status);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Database không trả về mã khóa học mới.");
        return key.longValue();
    }

    public int update(long courseId, String title, String level, String status) {
        return jdbcTemplate.update("UPDATE courses SET course_title = ?, jlpt_level = ?, status = ? WHERE course_id = ?",
                title, level, status, courseId);
    }
}
