package com.jlpp.modules.manager.lessons.repository;

import com.jlpp.modules.manager.lessons.dto.LessonDtos.LibraryItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Content libraries the add/edit popup can pull from: grammar patterns, vocabulary and kanji.
 * Every library lives in its own table so a skill can expose exactly the fields it needs.
 */
@Repository
public class LessonLibraryRepository {

    private final JdbcTemplate jdbcTemplate;

    public LessonLibraryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LibraryItem> findGrammarPoints(String level, String query) {
        StringBuilder sql = new StringBuilder(
                "SELECT grammar_id, pattern, meaning, jlpt_level FROM grammar_points WHERE 1 = 1");
        List<Object> arguments = buildFilters(sql, level, query, "pattern", "meaning");
        sql.append(" ORDER BY grammar_id");
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> new LibraryItem(
                resultSet.getLong("grammar_id"),
                resultSet.getString("pattern"),
                resultSet.getString("meaning"),
                resultSet.getString("jlpt_level"),
                null, null, resultSet.getString("meaning"), null), arguments.toArray());
    }

    public List<LibraryItem> findVocabularyEntries(String level, String query) {
        StringBuilder sql = new StringBuilder(
                "SELECT entry_id, word, reading, meaning, example_sentence, jlpt_level FROM vocabulary_entries WHERE 1 = 1");
        List<Object> arguments = buildFilters(sql, level, query, "word", "meaning");
        sql.append(" ORDER BY entry_id");
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            String reading = resultSet.getString("reading");
            String meaning = resultSet.getString("meaning");
            String subtitle = reading == null || reading.isBlank() ? meaning : reading + " · " + meaning;
            return new LibraryItem(
                    resultSet.getLong("entry_id"),
                    resultSet.getString("word"),
                    subtitle,
                    resultSet.getString("jlpt_level"),
                    null,
                    reading,
                    meaning,
                    resultSet.getString("example_sentence"));
        }, arguments.toArray());
    }

    public List<LibraryItem> findKanjiEntries(String level, String query) {
        StringBuilder sql = new StringBuilder(
                "SELECT entry_id, character, on_reading, kun_reading, meaning, stroke_count, jlpt_level "
                        + "FROM kanji_entries WHERE 1 = 1");
        List<Object> arguments = buildFilters(sql, level, query, "character", "meaning");
        sql.append(" ORDER BY entry_id");
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            String onReading = resultSet.getString("on_reading");
            String kunReading = resultSet.getString("kun_reading");
            String readings = joinReadings(onReading, kunReading);
            String meaning = resultSet.getString("meaning");
            String subtitle = readings.isEmpty() ? meaning : readings + " · " + meaning;
            Integer strokes = resultSet.getObject("stroke_count", Integer.class);
            return new LibraryItem(
                    resultSet.getLong("entry_id"),
                    resultSet.getString("character"),
                    subtitle,
                    resultSet.getString("jlpt_level"),
                    strokes == null ? null : strokes + " nét",
                    null, null, null);
        }, arguments.toArray());
    }

    public Optional<Map<String, Object>> findGrammarPoint(long grammarId) {
        return jdbcTemplate.queryForList("SELECT * FROM grammar_points WHERE grammar_id = ?", grammarId)
                .stream()
                .findFirst();
    }

    public Optional<Map<String, Object>> findVocabularyEntry(long entryId) {
        return jdbcTemplate.queryForList("SELECT * FROM vocabulary_entries WHERE entry_id = ?", entryId)
                .stream()
                .findFirst();
    }

    public Optional<Map<String, Object>> findKanjiEntry(long entryId) {
        return jdbcTemplate.queryForList("SELECT * FROM kanji_entries WHERE entry_id = ?", entryId)
                .stream()
                .findFirst();
    }

    public long insertGrammarPoint(String pattern, String meaning, String usageNote,
                                   String exampleJp, String exampleVi, String level) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO grammar_points (pattern, meaning, usage_note, example_jp, example_vi, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING grammar_id",
                Long.class, pattern, meaning, usageNote, exampleJp, exampleVi, level);
        return id == null ? 0L : id;
    }

    public long insertVocabularyEntry(String word, String reading, String meaning,
                                      String exampleSentence, String level) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO vocabulary_entries (word, reading, meaning, example_sentence, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING entry_id",
                Long.class, word, reading, meaning, exampleSentence, level);
        return id == null ? 0L : id;
    }

    public long insertKanjiEntry(String character, String onReading, String kunReading,
                                 String meaning, Integer strokeCount, String level) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO kanji_entries (character, on_reading, kun_reading, meaning, stroke_count, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING entry_id",
                Long.class, character, onReading, kunReading, meaning, strokeCount, level);
        return id == null ? 0L : id;
    }

    private List<Object> buildFilters(StringBuilder sql, String level, String query,
                                      String primaryColumn, String meaningColumn) {
        List<Object> arguments = new ArrayList<>();
        if (level != null && !level.isBlank()) {
            sql.append(" AND jlpt_level = ?");
            arguments.add(level.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (").append(primaryColumn).append(" ILIKE ? OR ")
                    .append(meaningColumn).append(" ILIKE ?)");
            String pattern = "%" + query.trim() + "%";
            arguments.add(pattern);
            arguments.add(pattern);
        }
        return arguments;
    }

    private String joinReadings(String onReading, String kunReading) {
        List<String> parts = new ArrayList<>(2);
        if (onReading != null && !onReading.isBlank()) {
            parts.add(onReading.trim());
        }
        if (kunReading != null && !kunReading.isBlank()) {
            parts.add(kunReading.trim());
        }
        return String.join(" · ", parts);
    }
}
