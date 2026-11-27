package com.jeps.modules.manager.lessons.repository;

import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonDetail;
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
        try {
            return jdbcTemplate.query(
                            """
                            SELECT lesson_id, skill_type,
                                   COALESCE(kanji_entry_id, grammar_id, vocabulary_entry_id) AS source_entry_id,
                                   audio_url, body, payload::text AS payload
                            FROM lesson_details
                            WHERE lesson_id = ?
                            """,
                            detailRowMapper, lessonId)
                    .stream()
                    .findFirst();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Saves the detail row. {@code sourceEntryId} is stored in the FK column that matches the skill
     * (kanji_entry_id / grammar_id / vocabulary_entry_id); other skills have no dictionary link.
     */
    public void save(long lessonId, String skillValue, Long sourceEntryId,
                     String audioUrl, String body, String payloadJson) {
        Long kanjiEntryId = "KANJI".equals(skillValue) ? sourceEntryId : null;
        Long grammarId = "GRAMMAR".equals(skillValue) ? sourceEntryId : null;
        Long vocabularyEntryId = "VOCABULARY".equals(skillValue) ? sourceEntryId : null;
        jdbcTemplate.update("""
                        INSERT INTO lesson_details
                            (lesson_id, skill_type, kanji_entry_id, grammar_id, vocabulary_entry_id,
                             audio_url, body, payload, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, NOW())
                        ON CONFLICT (lesson_id) DO UPDATE
                        SET skill_type = EXCLUDED.skill_type,
                            kanji_entry_id = EXCLUDED.kanji_entry_id,
                            grammar_id = EXCLUDED.grammar_id,
                            vocabulary_entry_id = EXCLUDED.vocabulary_entry_id,
                            audio_url = EXCLUDED.audio_url,
                            body = EXCLUDED.body,
                            payload = EXCLUDED.payload,
                            updated_at = NOW()
                        """,
                lessonId, skillValue, kanjiEntryId, grammarId, vocabularyEntryId, audioUrl, body, payloadJson);
    }

    public void delete(long lessonId) {
        jdbcTemplate.update("DELETE FROM lesson_details WHERE lesson_id = ?", lessonId);
    }

    public java.util.List<java.util.Map<String, Object>> findExistingSkillContents(long courseId, String skillValue, Long excludeLessonId) {
        String sql = """
                SELECT l.lesson_id, l.title AS lesson_title, l.content, ld.payload::text AS payload
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
