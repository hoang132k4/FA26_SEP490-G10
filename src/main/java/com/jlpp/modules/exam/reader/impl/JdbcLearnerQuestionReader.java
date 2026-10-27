package com.jlpp.modules.exam.reader.impl;

import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.reader.LearnerQuestionReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-Only Implementation using Spring JdbcTemplate.
 * Directly reads real questions and options from Supabase tables (questions, question_options)
 * without registering any JPA entity to Hibernate, thereby completely eliminating DDL alter table risks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcLearnerQuestionReader implements LearnerQuestionReader {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long countQuestionsByExamId(Long examId) {
        if (examId == null) return 0;
        try {
            String sql = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, examId);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not query question count for exam ID {}: {}", examId, e.getMessage());
            return 0;
        }
    }

    @Override
    public List<QuestionDto> findQuestionsForTaking(Long examId) {
        return fetchQuestions(examId, false);
    }

    @Override
    public List<QuestionDto> findQuestionsForEvaluation(Long examId) {
        return fetchQuestions(examId, true);
    }

    private List<QuestionDto> fetchQuestions(Long examId, boolean forEvaluation) {
        if (examId == null) return new ArrayList<>();

        String sql = """
            SELECT q.id AS q_id, q.category, q.content, q.audio_url, q.points, q.explanation, q.order_index,
                   o.id AS opt_id, o.label, o.option_text, o.is_correct
            FROM questions q
            LEFT JOIN question_options o ON o.question_id = q.id
            WHERE q.exam_id = ?
            ORDER BY q.order_index ASC, q.id ASC, o.id ASC
            """;

        try {
            return jdbcTemplate.query(sql, rs -> {
                Map<Long, QuestionDto> map = new LinkedHashMap<>();
                while (rs.next()) {
                    long qId = rs.getLong("q_id");
                    QuestionDto q = map.computeIfAbsent(qId, id -> {
                        try {
                            String cat = rs.getString("category");
                            String audio = rs.getString("audio_url");
                            String skillGroup = resolveSkillGroup(cat, audio);
                            return QuestionDto.builder()
                                    .id(id)
                                    .points(rs.getObject("points") != null ? rs.getInt("points") : 2)
                                    .category(cat)
                                    .skillGroup(skillGroup)
                                    .skillGroupLabel(resolveSkillGroupLabel(skillGroup))
                                    .content(rs.getString("content"))
                                    .audioUrl(audio)
                                    .explanation(forEvaluation ? rs.getString("explanation") : null)
                                    .orderIndex(rs.getObject("order_index") != null ? rs.getInt("order_index") : null)
                                    .options(new ArrayList<>())
                                    .build();
                        } catch (Exception ex) {
                            log.error("Error mapping question row: {}", ex.getMessage());
                            return null;
                        }
                    });

                    if (q != null) {
                        long optId = rs.getLong("opt_id");
                        if (!rs.wasNull()) {
                            q.getOptions().add(OptionDto.builder()
                                    .id(optId)
                                    .label(rs.getString("label"))
                                    .text(rs.getString("option_text"))
                                    .correct(forEvaluation ? rs.getBoolean("is_correct") : null)
                                    .build());
                        }
                    }
                }
                return new ArrayList<>(map.values());
            }, examId);
        } catch (Exception e) {
            log.error("Failed to read questions from Supabase for exam {}: {}", examId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private String resolveSkillGroup(String category, String audioUrl) {
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            return "LISTENING";
        }
        if (category == null) {
            return "VOCAB_GRAMMAR";
        }
        String catLower = category.toLowerCase();
        if (catLower.contains("nghe") || catLower.contains("choukai") || catLower.contains("listening")) {
            return "LISTENING";
        }
        if (catLower.contains("đọc") || catLower.contains("doc") || catLower.contains("dokkai") || catLower.contains("reading")) {
            return "READING";
        }
        return "VOCAB_GRAMMAR";
    }

    private String resolveSkillGroupLabel(String skillGroup) {
        return switch (skillGroup) {
            case "VOCAB_GRAMMAR" -> "Kiến thức ngôn ngữ (Từ vựng • Ngữ pháp • Kanji)";
            case "READING" -> "Đọc hiểu (Reading)";
            case "LISTENING" -> "Nghe hiểu (Listening)";
            default -> "Kiến thức tổng hợp";
        };
    }
}
