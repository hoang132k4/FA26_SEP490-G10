package com.jlpp.modules.manager.lessons.repository;

import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Skill specific payload of a lesson, one row per lesson. */
@Repository
public class LessonDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LessonDetail> detailRowMapper = (resultSet, rowNumber) -> new LessonDetail(
            resultSet.getLong("lesson_id"),
            resultSet.getString("skill_type"),
            resultSet.getObject("source_entry_id", Long.class),
            resultSet.getString("audio_url"),
            resultSet.getString("body"),
            resultSet.getString("payload"));

    public LessonDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<LessonDetail> findByLessonId(long lessonId) {
        return jdbcTemplate.query(
                        """
                        SELECT lesson_id, skill_type, source_entry_id, audio_url, body, payload::text AS payload
                        FROM lesson_details
                        WHERE lesson_id = ?
                           OR lesson_id = (SELECT lesson_id FROM lessons WHERE id = ? LIMIT 1)
                           OR lesson_id = (SELECT id FROM lessons WHERE lesson_id = ? LIMIT 1)
                        """,
                        detailRowMapper, lessonId, lessonId, lessonId)
                .stream()
                .findFirst();
    }

    public void save(long lessonId, String skillValue, Long sourceEntryId,
                     String audioUrl, String body, String payloadJson) {
        jdbcTemplate.update("""
                        INSERT INTO lesson_details
                            (lesson_id, skill_type, source_entry_id, audio_url, body, payload, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?::jsonb, NOW())
                        ON CONFLICT (lesson_id) DO UPDATE
                        SET skill_type = EXCLUDED.skill_type,
                            source_entry_id = EXCLUDED.source_entry_id,
                            audio_url = EXCLUDED.audio_url,
                            body = EXCLUDED.body,
                            payload = EXCLUDED.payload,
                            updated_at = NOW()
                        """,
                lessonId, skillValue, sourceEntryId, audioUrl, body, payloadJson);
    }

    public void delete(long lessonId) {
        jdbcTemplate.update("DELETE FROM lesson_details WHERE lesson_id = ?", lessonId);
    }

    public java.util.List<java.util.Map<String, Object>> findExistingSkillContents(long courseId, String skillValue, Long excludeLessonId) {
        String sql = """
                SELECT l.lesson_id, l.lesson_title, l.content, ld.payload::text AS payload
                FROM lessons l
                LEFT JOIN lesson_details ld ON l.lesson_id = ld.lesson_id
                WHERE l.course_id = ? AND COALESCE(l.skill_type, l.lesson_type) = ?
                """;
        if (excludeLessonId != null) {
            sql += " AND l.lesson_id <> ?";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("lessonId", rs.getLong("lesson_id"));
                map.put("title", rs.getString("lesson_title"));
                map.put("content", rs.getString("content"));
                map.put("payload", rs.getString("payload"));
                return map;
            }, courseId, skillValue, excludeLessonId);
        } else {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("lessonId", rs.getLong("lesson_id"));
                map.put("title", rs.getString("lesson_title"));
                map.put("content", rs.getString("content"));
                map.put("payload", rs.getString("payload"));
                return map;
            }, courseId, skillValue);
        }
    }
}
