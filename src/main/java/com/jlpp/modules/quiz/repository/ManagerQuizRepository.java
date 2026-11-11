package com.jlpp.modules.quiz.repository;

import com.jlpp.modules.quiz.dto.ExerciseQuestionRow;
import com.jlpp.modules.quiz.dto.LessonOption;
import com.jlpp.modules.quiz.dto.QuizOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ManagerQuizRepository {

    private final JdbcTemplate jdbcTemplate;

    public ManagerQuizRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<QuizOption> search(String search, String level, String section, String status) {
        return jdbcTemplate.query("""
                SELECT e.exercise_id, e.lesson_id, l.lesson_title, e.exercise_title,
                       e.total_questions, e.passing_score, e.status,
                       COUNT(eq.question_id) AS linked_question_count
                FROM exercises e
                LEFT JOIN lessons l ON l.lesson_id = e.lesson_id
                LEFT JOIN courses c ON c.course_id = l.course_id
                LEFT JOIN exercise_questions eq ON eq.exercise_id = e.exercise_id
                WHERE (? = '' OR LOWER(e.exercise_title) LIKE LOWER(?))
                  AND (? = '' OR c.jlpt_level = ?)
                  AND (? = '' OR l.section_name = ?)
                  AND (? = '' OR UPPER(e.status) = UPPER(?))
                GROUP BY e.exercise_id, e.lesson_id, l.lesson_title, e.exercise_title,
                         e.total_questions, e.passing_score, e.status
                ORDER BY e.exercise_id DESC
                """, (rs, row) -> mapQuiz(rs.getLong("exercise_id"), rs.getLong("lesson_id"),
                        rs.getString("lesson_title"), rs.getString("exercise_title"),
                        rs.getInt("total_questions"), rs.getInt("passing_score"),
                        rs.getString("status"), rs.getLong("linked_question_count")),
                search, "%" + search + "%", level, level, section, section, status, status);
    }

    public Map<String, List<String>> findLevelSections() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT c.jlpt_level, l.section_name, MIN(l.order_index) AS first_order
                FROM lessons l JOIN courses c ON c.course_id = l.course_id
                WHERE c.jlpt_level IS NOT NULL AND c.jlpt_level <> ''
                GROUP BY c.jlpt_level, l.section_name
                ORDER BY c.jlpt_level DESC, first_order
                """, rs -> {
            List<String> sections = result.computeIfAbsent(rs.getString("jlpt_level"), k -> new ArrayList<>());
            String section = rs.getString("section_name");
            if (section != null && !section.isBlank()) sections.add(section);
        });
        return result;
    }

    public Optional<QuizOption> findById(long id) {
        return jdbcTemplate.query("""
                SELECT e.exercise_id, e.lesson_id, l.lesson_title, e.exercise_title,
                       e.total_questions, e.passing_score, e.status,
                       COUNT(eq.question_id) AS linked_question_count
                FROM exercises e
                LEFT JOIN lessons l ON l.lesson_id = e.lesson_id
                LEFT JOIN exercise_questions eq ON eq.exercise_id = e.exercise_id
                WHERE e.exercise_id = ?
                GROUP BY e.exercise_id, e.lesson_id, l.lesson_title, e.exercise_title,
                         e.total_questions, e.passing_score, e.status
                """, (rs, row) -> mapQuiz(rs.getLong("exercise_id"), rs.getLong("lesson_id"),
                        rs.getString("lesson_title"), rs.getString("exercise_title"),
                        rs.getInt("total_questions"), rs.getInt("passing_score"),
                        rs.getString("status"), rs.getLong("linked_question_count")), id).stream().findFirst();
    }

    public List<LessonOption> findLessons() {
        return jdbcTemplate.query("""
                SELECT l.lesson_id, l.lesson_title, l.section_name, c.course_title, c.jlpt_level
                FROM lessons l LEFT JOIN courses c ON c.course_id = l.course_id
                ORDER BY c.jlpt_level DESC, c.course_id, l.order_index, l.lesson_id
                """,
                (rs, row) -> {
                    String section = rs.getString("section_name");
                    String course = rs.getString("course_title");
                    String scope = section != null && !section.isBlank() ? section
                            : course != null ? course : "Khác";
                    String level = rs.getString("jlpt_level");
                    String group = level == null || level.isBlank() ? scope : level + " · " + scope;
                    return new LessonOption(rs.getLong("lesson_id"), rs.getString("lesson_title"), group);
                });
    }

    public boolean lessonExists(long lessonId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM lessons WHERE lesson_id = ?)", Boolean.class, lessonId));
    }

    public long count() { return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exercises", Long.class); }

    public long insert(long lessonId, String title, int passingScore, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO exercises (lesson_id, exercise_title, total_questions, passing_score, status) VALUES (?, ?, 0, ?, ?)",
                    new String[] {"exercise_id"});
            statement.setLong(1, lessonId);
            statement.setString(2, title);
            statement.setInt(3, passingScore);
            statement.setString(4, status);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Database không trả về mã quiz mới.");
        return key.longValue();
    }

    public int update(long id, long lessonId, String title, int passingScore, String status) {
        return jdbcTemplate.update("""
                UPDATE exercises SET lesson_id = ?, exercise_title = ?, passing_score = ?, status = ?
                WHERE exercise_id = ?
                """, lessonId, title, passingScore, status, id);
    }

    public void syncTotalQuestions(long exerciseId) {
        jdbcTemplate.update("""
                UPDATE exercises
                SET total_questions = (SELECT COUNT(*) FROM exercise_questions WHERE exercise_id = ?)
                WHERE exercise_id = ?
                """, exerciseId, exerciseId);
    }

    // ── Exercise-Question linkage ──

    public List<ExerciseQuestionRow> findExerciseQuestions(long exerciseId) {
        return jdbcTemplate.query("""
                SELECT q.id, q.content, q.question_type, q.jlpt_level, q.skill_type
                FROM exercise_questions eq
                JOIN questions q ON q.id = eq.question_id
                WHERE eq.exercise_id = ?
                ORDER BY eq.question_id
                """, (rs, row) -> new ExerciseQuestionRow(
                        rs.getLong("id"), rs.getString("content"), rs.getString("question_type"),
                        rs.getString("jlpt_level"), rs.getString("skill_type")),
                exerciseId);
    }

    public long countExerciseQuestions(long exerciseId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exercise_questions WHERE exercise_id = ?", Long.class, exerciseId);
        return c != null ? c : 0;
    }

    public List<ExerciseQuestionRow> findAvailableQuestions(long exerciseId, String jlptLevel, String skillType) {
        return jdbcTemplate.query("""
                SELECT q.id, q.content, q.question_type, q.jlpt_level, q.skill_type
                FROM questions q
                WHERE q.status = 'ACTIVE'
                  AND q.parent_id IS NULL
                  AND q.id NOT IN (SELECT eq.question_id FROM exercise_questions eq WHERE eq.exercise_id = ?)
                  AND (? = '' OR q.jlpt_level = ?)
                  AND (? = '' OR q.skill_type = ?)
                ORDER BY q.id DESC
                """, (rs, row) -> new ExerciseQuestionRow(
                        rs.getLong("id"), rs.getString("content"), rs.getString("question_type"),
                        rs.getString("jlpt_level"), rs.getString("skill_type")),
                exerciseId, jlptLevel, jlptLevel, skillType, skillType);
    }

    public boolean exerciseQuestionExists(long exerciseId, long questionId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM exercise_questions WHERE exercise_id = ? AND question_id = ?)",
                Boolean.class, exerciseId, questionId));
    }

    public void insertExerciseQuestion(long exerciseId, long questionId) {
        jdbcTemplate.update("INSERT INTO exercise_questions (exercise_id, question_id) VALUES (?, ?)",
                exerciseId, questionId);
    }

    public int deleteExerciseQuestion(long exerciseId, long questionId) {
        return jdbcTemplate.update("DELETE FROM exercise_questions WHERE exercise_id = ? AND question_id = ?",
                exerciseId, questionId);
    }

    public boolean questionIsActive(long questionId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM questions WHERE id = ? AND status = 'ACTIVE')",
                Boolean.class, questionId));
    }

    public boolean questionExists(long questionId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM questions WHERE id = ?)", Boolean.class, questionId));
    }

    private QuizOption mapQuiz(long id, long lessonId, String lessonTitle, String title,
                               int totalQuestions, int passingScore, String status, long linkedQuestionCount) {
        return new QuizOption(id, lessonId, lessonTitle, title, totalQuestions, passingScore, status, linkedQuestionCount);
    }
}
