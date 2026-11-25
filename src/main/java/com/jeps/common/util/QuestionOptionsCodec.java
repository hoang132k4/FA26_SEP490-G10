package com.jeps.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared parser and serializer for canonical question options ({@code questions.options})
 * and answer keys ({@code questions.answer_key}).
 * Never injects a default answer key when none is configured.
 */
@Slf4j
public final class QuestionOptionsCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QuestionOptionsCodec() {
    }

    public record ParsedOption(long id, String label, String text, boolean isCorrect) {
    }

    public record OptionInput(String key, String text) {
    }

    /** Deterministic globally-unique option ID matching exam_result_details storage. */
    public static long optionId(long questionId, int oneBasedIndex) {
        return questionId * 100L + oneBasedIndex;
    }

    public static String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        String trimmed = rawKey.trim();
        while (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static Set<String> parseAnswerKeys(String answerKeyJson) {
        Set<String> keys = new LinkedHashSet<>();
        JsonNode root = parseArrayNode(answerKeyJson, "answer_key");
        if (root == null) {
            return keys;
        }
        for (JsonNode item : root) {
            if (item != null && !item.isNull()) {
                String normalized = normalizeKey(item.asText(""));
                if (!normalized.isEmpty()) {
                    keys.add(normalized);
                }
            }
        }
        return keys;
    }

    public static List<ParsedOption> parse(long questionId, String optionsJson, String answerKeyJson) {
        JsonNode root = parseArrayNode(optionsJson, "options");
        if (root == null || root.isEmpty()) {
            return List.of();
        }
        Set<String> correctKeys = parseAnswerKeys(answerKeyJson);
        List<ParsedOption> parsed = new ArrayList<>();
        int index = 0;
        for (JsonNode node : root) {
            index++;
            if (node == null || !node.isObject()) {
                continue;
            }
            String key = node.path("key").asText("").trim();
            String text = node.path("text").asText("").trim();
            if (key.isEmpty() || text.isEmpty()) {
                continue;
            }
            boolean correct = correctKeys.contains(normalizeKey(key));
            parsed.add(new ParsedOption(optionId(questionId, index), key, text, correct));
        }
        return parsed;
    }

    public static String toOptionsJson(List<OptionInput> entries) {
        List<Map<String, String>> payload = new ArrayList<>();
        if (entries != null) {
            for (OptionInput entry : entries) {
                if (entry == null) {
                    continue;
                }
                Map<String, String> item = new LinkedHashMap<>();
                item.put("key", entry.key() != null ? entry.key().trim() : "");
                item.put("text", entry.text() != null ? entry.text().trim() : "");
                payload.add(item);
            }
        }
        return writeJson(payload);
    }

    public static String toAnswerKeyJson(Collection<String> keys) {
        List<String> normalized = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                String k = normalizeKey(key);
                if (!k.isEmpty()) {
                    normalized.add(k);
                }
            }
        }
        return writeJson(normalized);
    }

    private static JsonNode parseArrayNode(String rawJson, String fieldName) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(rawJson);
            // Guard against double-encoded JSON text nodes (e.g. from H2 JSON columns)
            int depth = 0;
            while (node != null && node.isTextual() && depth < 3) {
                String inner = node.asText();
                if (inner == null || inner.isBlank()) {
                    return null;
                }
                node = MAPPER.readTree(inner);
                depth++;
            }
            return (node != null && node.isArray()) ? node : null;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse {} JSON payload: {}", fieldName, e.getMessage());
            return null;
        }
    }

    private static String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize question options JSON", e);
        }
    }
}
