package com.jlpp.modules.manager.flashcards.repository;

import com.jlpp.modules.manager.flashcards.model.ManagerFlashcard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@SuppressWarnings("null")
public class ManagerFlashcardRepository {

    private static final String COLUMNS = """
            flashcard_id, lesson_id, front_content, back_content, vocabulary, reading,
            meaning, example_sentence, jlpt_level, review_status, creator_id, custom_deck_name, is_custom
            """;

    private static final String INSERT_SQL = """
            INSERT INTO flashcards
                (lesson_id, front_content, back_content, vocabulary, reading, meaning,
                 example_sentence, jlpt_level, review_status, creator_id, custom_deck_name, is_custom)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE flashcards
            SET lesson_id = ?, front_content = ?, back_content = ?, vocabulary = ?, reading = ?,
                meaning = ?, example_sentence = ?, jlpt_level = ?, review_status = ?,
                custom_deck_name = ?, is_custom = ?
            WHERE flashcard_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ManagerFlashcard> rowMapper = this::mapFlashcard;

    public ManagerFlashcardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ManagerFlashcard> findAll() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM flashcards ORDER BY flashcard_id DESC", rowMapper);
    }

    public Optional<ManagerFlashcard> findById(long flashcardId) {
        List<ManagerFlashcard> results = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM flashcards WHERE flashcard_id = ?",
                rowMapper,
                flashcardId);
        return results.stream().findFirst();
    }

    public ManagerFlashcard insert(ManagerFlashcard flashcard) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            setInsertParameters(statement, flashcard);
            return statement;
        }, keyHolder);
        Number generatedId = Objects.requireNonNull(keyHolder.getKey(), "Database did not return flashcard_id");
        flashcard.setFlashcardId(generatedId.longValue());
        return flashcard;
    }

    public boolean update(ManagerFlashcard flashcard) {
        int updated = jdbcTemplate.update(UPDATE_SQL,
                flashcard.getLessonId(),
                flashcard.getFrontContent(),
                flashcard.getBackContent(),
                flashcard.getVocabulary(),
                flashcard.getReading(),
                flashcard.getMeaning(),
                flashcard.getExampleSentence(),
                flashcard.getJlptLevel(),
                flashcard.getReviewStatus(),
                flashcard.getCustomDeckName(),
                flashcard.isCustom(),
                flashcard.getFlashcardId());
        return updated > 0;
    }

    private void setInsertParameters(PreparedStatement statement, ManagerFlashcard flashcard) throws SQLException {
        statement.setObject(1, flashcard.getLessonId());
        statement.setObject(2, flashcard.getFrontContent());
        statement.setObject(3, flashcard.getBackContent());
        statement.setObject(4, flashcard.getVocabulary());
        statement.setObject(5, flashcard.getReading());
        statement.setObject(6, flashcard.getMeaning());
        statement.setObject(7, flashcard.getExampleSentence());
        statement.setObject(8, flashcard.getJlptLevel());
        statement.setObject(9, flashcard.getReviewStatus());
        statement.setObject(10, flashcard.getCreatorId());
        statement.setObject(11, flashcard.getCustomDeckName());
        statement.setBoolean(12, flashcard.isCustom());
    }

    private ManagerFlashcard mapFlashcard(ResultSet resultSet, int rowNumber) throws SQLException {
        ManagerFlashcard flashcard = new ManagerFlashcard();
        flashcard.setFlashcardId(resultSet.getLong("flashcard_id"));
        flashcard.setLessonId(resultSet.getLong("lesson_id"));
        flashcard.setFrontContent(resultSet.getString("front_content"));
        flashcard.setBackContent(resultSet.getString("back_content"));
        flashcard.setVocabulary(resultSet.getString("vocabulary"));
        flashcard.setReading(resultSet.getString("reading"));
        flashcard.setMeaning(resultSet.getString("meaning"));
        flashcard.setExampleSentence(resultSet.getString("example_sentence"));
        flashcard.setJlptLevel(resultSet.getString("jlpt_level"));
        flashcard.setReviewStatus(resultSet.getString("review_status"));
        flashcard.setCreatorId(resultSet.getObject("creator_id", Long.class));
        flashcard.setCustomDeckName(resultSet.getString("custom_deck_name"));
        flashcard.setCustom(resultSet.getBoolean("is_custom"));
        return flashcard;
    }
}
