package com.jlpp.modules.manager.lessons.repository;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Reads and writes the {@code lessons} table.
 *
 * <p>The table carries two parallel sets of columns because several migrations and Hibernate's
 * {@code ddl-auto: update} were applied over time:
 * {@code lesson_title}/{@code title}, {@code skill_type}/{@code lesson_type},
 * {@code order_index}/{@code lesson_order} and {@code lesson_id}/{@code id}.
 * Every write here fills both members of each pair to keep the legacy columns synchronized.
 * Reads classify lesson skills only from {@code skill_type}. {@code id} is NOT NULL without a
 * default, so it is fed from the same sequence value as {@code lesson_id}.</p>
 */
@Repository("managerLessonRepository")
public class LessonRepository {

    /** Skill classification is read only from the canonical skill_type column. */
    private static final String SKILL_VALUE = "skill_type";

    private static final String LESSON_SELECT = """
            SELECT lesson_id,
                   course_id,
                   lesson_title,
                   skill_type AS skill_value,
                   order_index,
                   description,
                   content,
                   section_name,
                   duration_minutes,
                   is_free_preview,
                   status
            FROM lessons
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LessonRow> lessonRowMapper = (resultSet, rowNumber) -> new LessonRow(
            resultSet.getLong("lesson_id"),
            resultSet.getLong("course_id"),
            resultSet.getInt("order_index"),
            resultSet.getString("lesson_title"),
            resultSet.getString("description"),
            resultSet.getString("content"),
            resultSet.getString("section_name"),
            resultSet.getObject("duration_minutes", Integer.class),
            resultSet.getBoolean("is_free_preview"),
            LessonStatusEnum.fromParam(resultSet.getString("status")),
            resultSet.getString("skill_value"));

    public LessonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LessonRow> findByCourse(long courseId) {
        return jdbcTemplate.query(LESSON_SELECT + " WHERE course_id = ? ORDER BY order_index, lesson_id",
                lessonRowMapper, courseId);
    }

    public Optional<LessonRow> findById(long lessonId) {
        return jdbcTemplate.query(LESSON_SELECT + " WHERE lesson_id = ?", lessonRowMapper, lessonId)
                .stream()
                .findFirst();
    }

    public List<Long> findIdsInOrder(long courseId, String skill) {
        return jdbcTemplate.queryForList(
                "SELECT lesson_id FROM lessons WHERE course_id = ? AND " + SKILL_VALUE
                        + " IS NOT DISTINCT FROM ? ORDER BY order_index, lesson_id",
                Long.class, courseId, skill);
    }

    public int nextOrderIndex(long courseId, String skill) {
        Integer highest = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(order_index), 0) FROM lessons WHERE course_id = ? AND " + SKILL_VALUE
                        + " IS NOT DISTINCT FROM ?",
                Integer.class, courseId, skill);
        return highest == null ? 1 : highest + 1;
    }

    public long insert(long courseId, LessonInput input, int orderIndex) {
        Long lessonId = jdbcTemplate.queryForObject(
                "SELECT nextval(pg_get_serial_sequence('lessons', 'lesson_id'))", Long.class);
        if (lessonId == null) {
            throw new IllegalStateException("Không lấy được mã bài học mới từ sequence của bảng lessons.");
        }
        jdbcTemplate.update("""
                        INSERT INTO lessons (lesson_id, id, course_id,
                                             lesson_title, title,
                                             skill_type, lesson_type,
                                             order_index, lesson_order,
                                             description, content, section_name,
                                             duration_minutes, is_free_preview, status,
                                             created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                lessonId, lessonId, courseId,
                input.title(), input.title(),
                input.skill(), input.skill(),
                orderIndex, orderIndex,
                input.description(), input.content(), input.sectionName(),
                input.durationMinutes(), input.freePreview(), input.status());
        return lessonId;
    }

    public void update(long lessonId, LessonInput input, int orderIndex) {
        jdbcTemplate.update("""
                        UPDATE lessons
                        SET lesson_title = ?, title = ?,
                            skill_type = ?, lesson_type = ?,
                            order_index = ?, lesson_order = ?,
                            description = ?, content = ?, section_name = ?,
                            duration_minutes = ?, is_free_preview = ?, status = ?,
                            updated_at = NOW()
                        WHERE lesson_id = ?
                        """,
                input.title(), input.title(),
                input.skill(), input.skill(),
                orderIndex, orderIndex,
                input.description(), input.content(), input.sectionName(),
                input.durationMinutes(), input.freePreview(), input.status(),
                lessonId);
    }

    public void delete(long lessonId) {
        jdbcTemplate.update("DELETE FROM lessons WHERE lesson_id = ?", lessonId);
    }

    public void updateStatus(long lessonId, LessonStatusEnum status) {
        jdbcTemplate.update("UPDATE lessons SET status = ?, updated_at = NOW() WHERE lesson_id = ?",
                status.name(), lessonId);
    }

    public void updateOrder(long lessonId, int orderIndex) {
        jdbcTemplate.update(
                "UPDATE lessons SET order_index = ?, lesson_order = ?, updated_at = NOW() WHERE lesson_id = ?",
                orderIndex, orderIndex, lessonId);
    }
}
