package com.jeps.modules.manager.lessons.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeps.common.constants.LessonStatusEnum;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                   title AS lesson_title,
                   COALESCE(skill_type, lesson_type) AS skill_value,
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
        // Câu "fallback" cũ trùng y hệt câu chính nên vô dụng, và nuốt lỗi trong transaction PostgreSQL
        // làm hỏng các câu lệnh tiếp theo; để lỗi lan ra.
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

    /** Next position at the end of the whole course: order_index is a single 1..N sequence per course. */
    public int nextOrderIndex(long courseId) {
        Integer highest = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(order_index), 0) FROM lessons WHERE course_id = ?",
                Integer.class, courseId);
        return highest == null ? 1 : highest + 1;
    }

    /** Raw skill_type (without the lesson_type fallback), used to recognise milestone exam lessons. */
    public Optional<String> findRawSkillType(long lessonId) {
        return jdbcTemplate.query("SELECT " + SKILL_VALUE + " FROM lessons WHERE lesson_id = ?",
                        (rs, row) -> rs.getString(1), lessonId)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    /**
     * lesson_type for a lesson. Lessons of the "Khác" column have no skill and must NOT be stored
     * as EXAM, otherwise the learner side ({@code Lesson.isMilestoneExam()}) treats them as milestone exams.
     */
    private static String lessonTypeFor(String skillType) {
        return (skillType != null && !skillType.isBlank()) ? skillType : OTHER_LESSON_TYPE;
    }

    private static final String OTHER_LESSON_TYPE = "THEORY";

    public long insert(long courseId, LessonInput input, int orderIndex) {
        Long lessonId = jdbcTemplate.queryForObject(
                "SELECT nextval(pg_get_serial_sequence('lessons', 'lesson_id'))", Long.class);
        if (lessonId == null) {
            throw new IllegalStateException("Không lấy được mã bài học mới từ sequence của bảng lessons.");
        }
        String skillType = input.skill();
        String lessonType = lessonTypeFor(skillType);
        jdbcTemplate.update("""
                        INSERT INTO lessons (lesson_id, course_id,
                                             title,
                                             skill_type, lesson_type,
                                             order_index,
                                             description, content, section_name,
                                             duration_minutes, is_free_preview, status,
                                             created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                lessonId, courseId,
                input.title(),
                skillType, lessonType,
                orderIndex,
                input.description(), input.content(), input.sectionName(),
                input.durationMinutes(), input.freePreview(), input.status());
        return lessonId;
    }

    public void update(long lessonId, LessonInput input, int orderIndex) {
        String skillType = input.skill();
        String lessonType = lessonTypeFor(skillType);
        jdbcTemplate.update("""
                        UPDATE lessons
                        SET title = ?,
                            skill_type = ?, lesson_type = ?,
                            order_index = ?,
                            description = ?, content = ?, section_name = ?,
                            duration_minutes = ?, is_free_preview = ?, status = ?,
                            updated_at = NOW()
                        WHERE lesson_id = ?
                        """,
                input.title(),
                skillType, lessonType,
                orderIndex,
                input.description(), input.content(), input.sectionName(),
                input.durationMinutes(), input.freePreview(), input.status(),
                lessonId);
    }

    public void delete(long lessonId) {
        jdbcTemplate.update("DELETE FROM lesson_details WHERE lesson_id = ?", lessonId);
        jdbcTemplate.update("DELETE FROM flashcards WHERE lesson_id = ?", lessonId);
        jdbcTemplate.update("DELETE FROM study_materials WHERE lesson_id = ?", lessonId);
        // Không bọc try/catch: trên PostgreSQL câu lệnh lỗi sẽ abort cả transaction, nuốt lỗi chỉ làm
        // câu DELETE lessons phía sau hỏng với thông báo khó hiểu. Để lỗi lan ra cho transaction rollback.
        List<Long> exIds = jdbcTemplate.queryForList("SELECT exercise_id FROM exercises WHERE lesson_id = ?", Long.class, lessonId);
        if (!exIds.isEmpty()) {
            for (Long exId : exIds) {
                List<Long> qIds = jdbcTemplate.queryForList("SELECT question_id FROM exercise_questions WHERE exercise_id = ?", Long.class, exId);
                jdbcTemplate.update("DELETE FROM exercise_questions WHERE exercise_id = ?", exId);
                if (!qIds.isEmpty()) {
                    for (Long qId : qIds) {
                        jdbcTemplate.update("DELETE FROM question_options WHERE question_id = ?", qId);
                        jdbcTemplate.update("DELETE FROM questions WHERE id = ?", qId);
                    }
                }
            }
            jdbcTemplate.update("DELETE FROM exercises WHERE lesson_id = ?", lessonId);
        }
        jdbcTemplate.update("DELETE FROM lessons WHERE lesson_id = ?", lessonId);
    }

    public void updateStatus(long lessonId, LessonStatusEnum status) {
        jdbcTemplate.update("UPDATE lessons SET status = ?, updated_at = NOW() WHERE lesson_id = ?",
                status.name(), lessonId);
    }

    public void updateOrder(long lessonId, int orderIndex) {
        jdbcTemplate.update(
                "UPDATE lessons SET order_index = ?, updated_at = NOW() WHERE lesson_id = ?",
                orderIndex, lessonId);
    }

    public void batchUpdateOrders(List<Long> orderedLessonIds) {
        if (orderedLessonIds == null || orderedLessonIds.isEmpty()) return;
        String sql = "UPDATE lessons SET order_index = ?, updated_at = NOW() WHERE lesson_id = ?";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                int order = i + 1;
                ps.setInt(1, order);
                ps.setLong(2, orderedLessonIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return orderedLessonIds.size();
            }
        });
    }

    public void batchUpdateOrders(List<Long> lessonIds, List<Integer> orders) {
        if (lessonIds == null || lessonIds.isEmpty()) return;
        String sql = "UPDATE lessons SET order_index = ?, updated_at = NOW() WHERE lesson_id = ?";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                int order = orders.get(i);
                ps.setInt(1, order);
                ps.setLong(2, lessonIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return lessonIds.size();
            }
        });
    }

    public int renameSection(long courseId, String oldName, String newName) {
        return jdbcTemplate.update("UPDATE lessons SET section_name = ?, updated_at = NOW() " +
                        "WHERE course_id = ? AND section_name = ?", newName, courseId, oldName);
    }

    public int clearSection(long courseId, String sectionName) {
        return jdbcTemplate.update("UPDATE lessons SET section_name = NULL, updated_at = NOW() " +
                        "WHERE course_id = ? AND section_name = ?", courseId, sectionName);
    }

    public Set<Long> findLessonIdsWithFlashcards(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return Set.of();
        String inSql = String.join(",", Collections.nCopies(lessonIds.size(), "?"));
        List<Long> ids = jdbcTemplate.query(
                "SELECT DISTINCT lesson_id FROM flashcards WHERE lesson_id IN (" + inSql + ")",
                (rs, rowNum) -> rs.getLong("lesson_id"),
                lessonIds.toArray());
        return new HashSet<>(ids);
    }

    public Set<Long> findLessonIdsWithExercises(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return Set.of();
        String inSql = String.join(",", Collections.nCopies(lessonIds.size(), "?"));
        List<Long> ids = jdbcTemplate.query(
                "SELECT DISTINCT lesson_id FROM exercises WHERE lesson_id IN (" + inSql + ")",
                (rs, rowNum) -> rs.getLong("lesson_id"),
                lessonIds.toArray());
        return new HashSet<>(ids);
    }

    private static final Logger log = LoggerFactory.getLogger(LessonRepository.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Set<Long> findStudiedLessonIds(long courseId, List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> studiedIds = new HashSet<>();
        String inSql = String.join(",", Collections.nCopies(lessonIds.size(), "?"));
        Object[] params = lessonIds.toArray();

        // 1. Check exercise results (students who submitted quiz exercises in this lesson)
        studiedIds.addAll(bestEffort("exercise_results", List.of(), () -> jdbcTemplate.query(
                "SELECT DISTINCT e.lesson_id FROM exercise_results er JOIN exercises e ON e.exercise_id = er.exercise_id WHERE e.lesson_id IN (" + inSql + ")",
                (rs, rowNum) -> rs.getLong("lesson_id"),
                params)));

        // 2. Check exam / exercise attempts
        studiedIds.addAll(bestEffort("attempts", List.of(), () -> jdbcTemplate.query(
                "SELECT DISTINCT e.lesson_id FROM attempts a JOIN exercises e ON e.exercise_id = a.exercise_id WHERE e.lesson_id IN (" + inSql + ")",
                (rs, rowNum) -> rs.getLong("lesson_id"),
                params)));

        // 3. Check learning_paths (granular per-lesson completion recorded by learners)
        // Note: CAST(... AS TEXT) is required so PostgreSQL jsonb column can be read into String without PSQLException
        try {
            List<String> progressJsons = bestEffort("learning_paths", List.of(), () -> jdbcTemplate.queryForList(
                    "SELECT CAST(lesson_progress AS TEXT) FROM learning_paths WHERE lesson_progress IS NOT NULL",
                    String.class));
            for (String json : progressJsons) {
                if (json == null || json.isBlank()) continue;
                try {
                    JsonNode root = OBJECT_MAPPER.readTree(json);
                    JsonNode lessonsNode = root.get("lessons");
                    if (lessonsNode != null && lessonsNode.isObject()) {
                        for (Long lid : lessonIds) {
                            if (lessonsNode.has(String.valueOf(lid))) {
                                studiedIds.add(lid);
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    for (Long lid : lessonIds) {
                        String needle = "\"" + lid + "\"";
                        if (json.contains(needle)) {
                            studiedIds.add(lid);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Check learning_paths: {}", e.getMessage());
        }

        // 4. Check course_registrations progress_percent > 0 for ACTIVE lessons
        try {
            List<Integer> percents = bestEffort("course_registrations", List.of(), () -> jdbcTemplate.queryForList(
                    "SELECT progress_percent FROM course_registrations WHERE course_id = ? AND progress_percent > 0",
                    Integer.class, courseId));
            if (!percents.isEmpty()) {
                // Only consider ACTIVE lessons, because inactive / newly created draft lessons are never displayed to learners
                List<LessonRow> activeLessons = findByCourse(courseId).stream()
                        .filter(LessonRow::isVisible)
                        .toList();
                int totalActive = activeLessons.size();
                if (totalActive > 0) {
                    for (Integer percent : percents) {
                        if (percent != null && percent > 0) {
                            int completedCount = (int) Math.round((percent / 100.0) * totalActive);
                            for (int i = 0; i < Math.min(completedCount, totalActive); i++) {
                                studiedIds.add(activeLessons.get(i).id());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Check course_registrations: {}", e.getMessage());
        }

        return studiedIds;
    }

    public boolean hasLearnersStudied(long lessonId) {
        // 1. Direct check exercise results
        Integer resultCount = bestEffort("exercise_results", 0, () -> jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exercise_results er JOIN exercises e ON e.exercise_id = er.exercise_id WHERE e.lesson_id = ?",
                Integer.class, lessonId));
        if (resultCount != null && resultCount > 0) return true;

        // 2. Direct check attempts
        Integer attemptCount = bestEffort("attempts", 0, () -> jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attempts a JOIN exercises e ON e.exercise_id = a.exercise_id WHERE e.lesson_id = ?",
                Integer.class, lessonId));
        if (attemptCount != null && attemptCount > 0) return true;

        // 3. Direct check learning_paths
        try {
            List<String> progressJsons = bestEffort("learning_paths", List.of(), () -> jdbcTemplate.queryForList(
                    "SELECT CAST(lesson_progress AS TEXT) FROM learning_paths WHERE lesson_progress IS NOT NULL",
                    String.class));
            for (String json : progressJsons) {
                if (json == null || json.isBlank()) continue;
                try {
                    JsonNode root = OBJECT_MAPPER.readTree(json);
                    JsonNode lessonsNode = root.get("lessons");
                    if (lessonsNode != null && lessonsNode.has(String.valueOf(lessonId))) {
                        return true;
                    }
                } catch (Exception e) {
                    if (json.contains("\"" + lessonId + "\"")) return true;
                }
            }
        } catch (Exception ignored) {}

        // 4. Fallback check via course active lessons
        Optional<LessonRow> lessonOpt = findById(lessonId);
        if (lessonOpt.isPresent()) {
            LessonRow lesson = lessonOpt.get();
            if (lesson.isVisible()) {
                try {
                    List<Integer> percents = bestEffort("course_registrations", List.of(), () -> jdbcTemplate.queryForList(
                            "SELECT progress_percent FROM course_registrations WHERE course_id = ? AND progress_percent > 0",
                            Integer.class, lesson.courseId()));
                    if (!percents.isEmpty()) {
                        List<LessonRow> activeLessons = findByCourse(lesson.courseId()).stream()
                                .filter(LessonRow::isVisible)
                                .toList();
                        int totalActive = activeLessons.size();
                        int rank = -1;
                        for (int i = 0; i < totalActive; i++) {
                            if (activeLessons.get(i).id() == lessonId) {
                                rank = i + 1;
                                break;
                            }
                        }
                        if (rank > 0 && totalActive > 0) {
                            for (Integer percent : percents) {
                                if (percent != null && percent > 0) {
                                    int completedCount = (int) Math.round((percent / 100.0) * totalActive);
                                    if (rank <= completedCount) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return false;
    }

    /**
     * Runs an optional check query without poisoning the surrounding transaction. On PostgreSQL a failed
     * statement aborts the whole transaction, so inside a transaction the query runs behind a savepoint
     * that is rolled back on failure; the caller then gets {@code fallback} and can keep going.
     */
    private <T> T bestEffort(String label, T fallback, java.util.function.Supplier<T> query) {
        T result = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<T>) connection -> {
            java.sql.Savepoint savepoint = connection.getAutoCommit() ? null : connection.setSavepoint();
            try {
                T value = query.get();
                if (savepoint != null) connection.releaseSavepoint(savepoint);
                return value;
            } catch (org.springframework.dao.DataAccessException exception) {
                if (savepoint != null) connection.rollback(savepoint);
                log.debug("Optional check on {} failed: {}", label, exception.getMessage());
                return fallback;
            }
        });
        return result == null ? fallback : result;
    }
}
