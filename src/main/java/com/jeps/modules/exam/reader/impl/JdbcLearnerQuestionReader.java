package com.jeps.modules.exam.reader.impl;

import com.jeps.common.util.QuestionOptionsCodec;
import com.jeps.common.util.QuestionOptionsCodec.ParsedOption;
import com.jeps.modules.exam.dto.OptionDto;
import com.jeps.modules.exam.dto.QuestionDto;
import com.jeps.modules.exam.reader.LearnerQuestionReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcLearnerQuestionReader implements LearnerQuestionReader {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long countQuestionsByExamId(Long examId) {
        if (examId == null) return 0;
        try {
            String sql = "SELECT COUNT(*) FROM exam_questions WHERE exam_id = ?";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, examId);
            return count != null ? count : 0;
        } catch (Exception e) {
            try {
                String fallbackSql = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";
                Long count = jdbcTemplate.queryForObject(fallbackSql, Long.class, examId);
                return count != null ? count : 0;
            } catch (Exception ex) {
                log.warn("Could not query question count for exam ID {}: {}", examId, ex.getMessage());
                return 0;
            }
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

        String sqlWithJunction = """
            SELECT q.id AS q_id, q.category, q.content, q.audio_url, eq.points, q.explanation, eq.order_index, eq.section,
                   q.options AS options_json, q.answer_key AS answer_key_json
            FROM exam_questions eq
            JOIN questions q ON q.id = eq.question_id
            WHERE eq.exam_id = ?
            ORDER BY eq.order_index ASC, q.id ASC
            """;

        String fallbackSql = """
            SELECT q.id AS q_id, q.category, q.content, q.audio_url, q.points, q.explanation, q.order_index, NULL AS section,
                   q.options AS options_json, q.answer_key AS answer_key_json
            FROM questions q
            WHERE q.exam_id = ?
            ORDER BY q.order_index ASC, q.id ASC
            """;

        try {
            return doQueryQuestions(sqlWithJunction, examId, forEvaluation);
        } catch (Exception e) {
            log.warn("Query via exam_questions failed ({}), trying direct questions fallback...", e.getMessage());
            try {
                return doQueryQuestions(fallbackSql, examId, forEvaluation);
            } catch (Exception ex) {
                log.error("Failed to query questions for exam ID {}: {}", examId, ex.getMessage());
                return new ArrayList<>();
            }
        }
    }

    private List<QuestionDto> doQueryQuestions(String sql, Long examId, boolean forEvaluation) {
        return jdbcTemplate.query(sql, rs -> {
            Map<Long, QuestionDto> map = new LinkedHashMap<>();
            while (rs.next()) {
                long qId = rs.getLong("q_id");
                map.computeIfAbsent(qId, id -> {
                    try {
                        String cat = rs.getString("category");
                        String audio = rs.getString("audio_url");
                        String sec = rs.getString("section");
                        String skillGroup = resolveSkillGroup(cat, audio, sec);

                        String optionsJson = rs.getString("options_json");
                        String answerKeyJson = forEvaluation ? rs.getString("answer_key_json") : null;
                        List<ParsedOption> parsedOptions = QuestionOptionsCodec.parse(id, optionsJson, answerKeyJson);

                        if (parsedOptions.isEmpty()) {
                            log.warn("Question {} in exam {} has no valid options in questions.options", id, examId);
                        } else if (forEvaluation && parsedOptions.stream().noneMatch(ParsedOption::isCorrect)) {
                            log.warn("Question {} in exam {} has no valid answer_key matching options", id, examId);
                        }

                        List<OptionDto> optionDtos = new ArrayList<>(parsedOptions.size());
                        for (ParsedOption po : parsedOptions) {
                            optionDtos.add(OptionDto.builder()
                                    .id(po.id())
                                    .label(po.label())
                                    .text(po.text())
                                    .correct(forEvaluation ? po.isCorrect() : null)
                                    .build());
                        }

                        return QuestionDto.builder()
                                .id(id)
                                .points(rs.getObject("points") != null ? rs.getInt("points") : 2)
                                .category(cat)
                                .section(sec)
                                .skillGroup(skillGroup)
                                .skillGroupLabel(resolveSkillGroupLabel(skillGroup))
                                .content(rs.getString("content"))
                                .audioUrl(audio)
                                .explanation(forEvaluation ? rs.getString("explanation") : null)
                                .orderIndex(rs.getObject("order_index") != null ? rs.getInt("order_index") : null)
                                .options(optionDtos)
                                .build();
                    } catch (Exception ex) {
                        log.error("Error mapping question row: {}", ex.getMessage());
                        return null;
                    }
                });
            }
            return new ArrayList<>(map.values());
        }, examId);
    }

    private String resolveSkillGroup(String category, String audioUrl, String section) {
        // 1. Ưu tiên cao nhất: Câu hỏi có file audio chắc chắn là Nghe hiểu (LISTENING)
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            return "LISTENING";
        }

        // 2. Kiểm tra danh mục câu hỏi (category)
        if (category != null && !category.trim().isEmpty()) {
            String catLower = category.toLowerCase().trim();
            if (catLower.contains("nghe") || catLower.contains("choukai") || catLower.contains("listening")) {
                return "LISTENING";
            }
            if (catLower.contains("đọc") || catLower.contains("doc") || catLower.contains("dokkai") ||
                catLower.contains("reading") || catLower.contains("trung văn") || catLower.contains("đoản văn") ||
                catLower.contains("trường văn") || catLower.contains("thông báo")) {
                return "READING";
            }
            if (catLower.contains("từ vựng") || catLower.contains("tu vung") || catLower.contains("goi") ||
                catLower.contains("ngữ pháp") || catLower.contains("ngu phap") || catLower.contains("bunpou") ||
                catLower.contains("kanji") || catLower.contains("hán tự") || catLower.contains("han tu") ||
                catLower.contains("chữ hán") || catLower.contains("kính ngữ") || catLower.contains("phó từ")) {
                return "VOCAB_GRAMMAR";
            }
        }

        // 3. Fallback theo section nếu category chưa rõ ràng
        if (section != null && !section.trim().isEmpty()) {
            return switch (section.trim().toUpperCase()) {
                case "READING" -> "READING";
                case "LISTENING" -> "LISTENING";
                default -> "VOCAB_GRAMMAR";
            };
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
