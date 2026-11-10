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
                "SELECT grammar_id, pattern, meaning, formation, usage_note, notes_vi, example_jp, example_vi, jlpt_level "
                        + "FROM grammar_points WHERE 1 = 1");
        List<Object> arguments = new ArrayList<>();
        if (level != null && !level.isBlank()) {
            sql.append(" AND jlpt_level = ?");
            arguments.add(level.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (pattern ILIKE ? OR meaning ILIKE ? OR formation ILIKE ? OR usage_note ILIKE ? OR meaning_en ILIKE ?)");
            String pattern = "%" + query.trim() + "%";
            for (int i = 0; i < 5; i++) {
                arguments.add(pattern);
            }
        }
        sql.append(" ORDER BY grammar_id");
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            String pattern = resultSet.getString("pattern");
            String meaning = resultSet.getString("meaning");
            String formation = resultSet.getString("formation");
            String usageNote = resultSet.getString("usage_note");
            String meta = formation != null && !formation.isBlank() ? "Cấu trúc: " + formation.trim() : (usageNote != null && !usageNote.isBlank() ? usageNote.trim() : null);
            return new LibraryItem(
                    resultSet.getLong("grammar_id"),
                    pattern,
                    meaning,
                    resultSet.getString("jlpt_level"),
                    meta,
                    formation,
                    meaning,
                    resultSet.getString("example_jp"),
                    resultSet.getString("example_vi"));
        }, arguments.toArray());
    }

    public List<LibraryItem> findVocabularyEntries(String level, String query) {
        StringBuilder sql = new StringBuilder(
                "SELECT entry_id, word, reading, part_of_speech, meaning, example_sentence, example_vi, jlpt_level "
                        + "FROM vocabulary_entries WHERE 1 = 1");
        List<Object> arguments = new ArrayList<>();
        if (level != null && !level.isBlank()) {
            sql.append(" AND jlpt_level = ?");
            arguments.add(level.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (word ILIKE ? OR reading ILIKE ? OR meaning ILIKE ? OR meaning_en ILIKE ?)");
            String pattern = "%" + query.trim() + "%";
            for (int i = 0; i < 4; i++) {
                arguments.add(pattern);
            }
        }
        sql.append(" ORDER BY entry_id");
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            String word = resultSet.getString("word");
            String reading = resultSet.getString("reading");
            String meaning = resultSet.getString("meaning");
            String pos = formatPartOfSpeech(resultSet.getString("part_of_speech"));
            String subtitle = reading == null || reading.isBlank() ? meaning : reading + " · " + meaning;
            return new LibraryItem(
                    resultSet.getLong("entry_id"),
                    word,
                    subtitle,
                    resultSet.getString("jlpt_level"),
                    pos,
                    reading,
                    meaning,
                    resultSet.getString("example_sentence"),
                    resultSet.getString("example_vi"));
        }, arguments.toArray());
    }

    public static String formatPartOfSpeech(String rawPos) {
        if (rawPos == null || rawPos.isBlank()) return "(N)";
        String lower = rawPos.toLowerCase().trim();
        if (lower.startsWith("(") && lower.endsWith(")")) return rawPos;
        if (lower.contains("adj")) return "(Adj)";
        if (lower.contains("v1") || lower.contains("v5") || lower.contains("vt") || lower.contains("vi") || lower.contains("vs")) return "(V)";
        if (lower.contains("adv")) return "(Adv)";
        if (lower.contains("pn")) return "(Pron)";
        if (lower.contains("conj")) return "(Conj)";
        if (lower.contains("exp")) return "(Exp)";
        if (lower.contains("n")) return "(N)";
        return "(" + rawPos.toUpperCase() + ")";
    }

    public List<LibraryItem> findKanjiEntries(String level, String query) {
        StringBuilder sql = new StringBuilder(
                "SELECT entry_id, character, on_reading, kun_reading, meaning, stroke_count, jlpt_level, "
                        + "radical, grade, freq, words "
                        + "FROM kanji_entries WHERE 1 = 1");
        List<Object> arguments = new ArrayList<>();
        if (level != null && !level.isBlank()) {
            sql.append(" AND jlpt_level = ?");
            arguments.add(level.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (character ILIKE ? OR meaning ILIKE ? OR on_reading ILIKE ? OR kun_reading ILIKE ? OR meaning_en ILIKE ? OR radical ILIKE ?)");
            String pattern = "%" + query.trim() + "%";
            for (int i = 0; i < 6; i++) {
                arguments.add(pattern);
            }
        }
        sql.append(" ORDER BY freq ASC NULLS LAST, stroke_count ASC, entry_id ASC");
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            String onReading = resultSet.getString("on_reading");
            String kunReading = resultSet.getString("kun_reading");
            String readings = joinReadings(onReading, kunReading);
            String meaning = resultSet.getString("meaning");
            String subtitle = readings.isEmpty() ? meaning : readings + " · " + meaning;
            Integer strokes = resultSet.getObject("stroke_count", Integer.class);
            String radical = resultSet.getString("radical");
            Integer grade = resultSet.getObject("grade", Integer.class);

            StringBuilder meta = new StringBuilder();
            if (strokes != null) {
                meta.append(strokes).append(" nét");
            }
            if (radical != null && !radical.isBlank()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append("Bộ: ").append(radical.trim());
            }
            if (grade != null && grade > 0) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append("Lớp ").append(grade);
            }

            return new LibraryItem(
                    resultSet.getLong("entry_id"),
                    resultSet.getString("character"),
                    subtitle,
                    resultSet.getString("jlpt_level"),
                    meta.length() > 0 ? meta.toString() : null,
                    readings,
                    meaning,
                    resultSet.getString("words"));
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
                                   String formation, String exampleJp, String exampleVi, String level) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO grammar_points (pattern, meaning, usage_note, formation, example_jp, example_vi, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING grammar_id",
                Long.class, pattern, meaning, usageNote, formation, exampleJp, exampleVi, level);
        return id == null ? 0L : id;
    }

    public long insertGrammarPoint(String pattern, String meaning, String usageNote,
                                   String exampleJp, String exampleVi, String level) {
        return insertGrammarPoint(pattern, meaning, usageNote, null, exampleJp, exampleVi, level);
    }

    public long insertVocabularyEntry(String word, String reading, String partOfSpeech, String meaning,
                                      String exampleSentence, String exampleVi, String level) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO vocabulary_entries (word, reading, part_of_speech, meaning, example_sentence, example_vi, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING entry_id",
                Long.class, word, reading, partOfSpeech, meaning, exampleSentence, exampleVi, level);
        return id == null ? 0L : id;
    }

    public long insertVocabularyEntry(String word, String reading, String meaning,
                                      String exampleSentence, String level) {
        return insertVocabularyEntry(word, reading, null, meaning, exampleSentence, null, level);
    }

    public long insertKanjiEntry(String character, String onReading, String kunReading,
                                 String meaning, Integer strokeCount, String radical, String level) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO kanji_entries (character, on_reading, kun_reading, meaning, stroke_count, radical, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING entry_id",
                Long.class, character, onReading, kunReading, meaning, strokeCount, radical, level);
        return id == null ? 0L : id;
    }

    public long insertKanjiEntry(String character, String onReading, String kunReading,
                                 String meaning, Integer strokeCount, String level) {
        return insertKanjiEntry(character, onReading, kunReading, meaning, strokeCount, null, level);
    }

    public String findVocabularyMeaningByWord(String word) {
        if (word == null || word.isBlank()) return "";
        try {
            List<String> list = jdbcTemplate.query(
                    "SELECT meaning FROM vocabulary_entries WHERE word = ? LIMIT 1",
                    (rs, rowNum) -> rs.getString("meaning"), word.trim());
            return list.isEmpty() ? "" : list.get(0);
        } catch (Exception e) {
            return "";
        }
    }

    public Map<String, String> findVocabularyInfo(String word) {
        if (word == null || word.isBlank()) return Map.of();
        try {
            List<Map<String, String>> list = jdbcTemplate.query(
                    "SELECT reading, meaning FROM vocabulary_entries WHERE word = ? LIMIT 1",
                    (rs, rowNum) -> {
                        Map<String, String> map = new java.util.LinkedHashMap<>();
                        map.put("reading", rs.getString("reading") == null ? "" : rs.getString("reading"));
                        map.put("meaning", rs.getString("meaning") == null ? "" : rs.getString("meaning"));
                        return map;
                    }, word.trim());
            return list.isEmpty() ? Map.of() : list.get(0);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public Optional<Map<String, Object>> findKanjiByCharacter(String character) {
        if (character == null || character.isBlank()) return Optional.empty();
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT * FROM kanji_entries WHERE character = ? LIMIT 1", character.trim());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String findHanvietByCharacter(String character) {
        if (character == null || character.isBlank()) return "";
        try {
            List<String> list = jdbcTemplate.query(
                    "SELECT hanviet FROM kanji_entries WHERE character = ? LIMIT 1",
                    (rs, rowNum) -> rs.getString("hanviet"), character.trim());
            return list.isEmpty() || list.get(0) == null ? "" : list.get(0).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public Optional<Map<String, Object>> findGrammarByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) return Optional.empty();
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT * FROM grammar_points WHERE pattern = ? OR pattern ILIKE ? LIMIT 1",
                    pattern.trim(), "%" + pattern.trim() + "%");
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> findVocabularyByWord(String word) {
        if (word == null || word.isBlank()) return Optional.empty();
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT * FROM vocabulary_entries WHERE word = ? LIMIT 1", word.trim());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
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
