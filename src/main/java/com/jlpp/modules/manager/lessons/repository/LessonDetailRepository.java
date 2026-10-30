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
                        "SELECT lesson_id, skill_type, source_entry_id, audio_url, body, payload::text AS payload "
                                + "FROM lesson_details WHERE lesson_id = ?",
                        detailRowMapper, lessonId)
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
}
