package com.jlpp.modules.lesson.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.lesson.dto.LessonDetail;
import com.jlpp.modules.lesson.dto.LessonRow;
import com.jlpp.modules.lesson.dto.LibraryItem;
import com.jlpp.modules.lesson.repository.LessonDetailRepository;
import com.jlpp.modules.lesson.repository.LessonLibraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a skill library entry plus the submitted popup fields into the skill specific record of a
 * lesson, and back into the values the popup needs when it is reopened.
 */
@Service
public class LessonDetailService {

    private static final int DEFAULT_WORD_ROWS = 3;
    private static final int DEFAULT_EXAMPLE_ROWS = 2;
    private static final int DEFAULT_COMPOUND_ROWS = 2;
    private static final int DEFAULT_QUESTION_ROWS = 1;

    private final LessonLibraryRepository libraryRepository;
    private final LessonDetailRepository detailRepository;
    private final ObjectMapper objectMapper;

    public LessonDetailService(LessonLibraryRepository libraryRepository,
                               LessonDetailRepository detailRepository,
                               ObjectMapper objectMapper) {
        this.libraryRepository = libraryRepository;
        this.detailRepository = detailRepository;
        this.objectMapper = objectMapper;
    }

    public List<LibraryItem> findLibrary(SkillTypeEnum skill, String level, String query) {
        if (skill == null) {
            return List.of();
        }
        return switch (skill) {
            case GRAMMAR -> libraryRepository.findGrammarPoints(level, query);
            case VOCABULARY -> libraryRepository.findVocabularyEntries(level, query);
            case KANJI -> libraryRepository.findKanjiEntries(level, query);
            default -> List.of();
        };
    }

    public boolean supportsLibrary(SkillTypeEnum skill) {
        return skill == SkillTypeEnum.GRAMMAR
                || skill == SkillTypeEnum.VOCABULARY
                || skill == SkillTypeEnum.KANJI;
    }

    public Optional<LessonDetail> findDetail(long lessonId) {
        return detailRepository.findByLessonId(lessonId);
    }

    /** Values shown in the popup: lesson basics, previously saved payload and the picked entry. */
    public Map<String, Object> buildForm(SkillTypeEnum skill, Long entryId, LessonRow lesson, LessonDetail detail) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("title", lesson == null ? "" : nullSafe(lesson.title()));
        form.put("sectionName", lesson == null ? "" : nullSafe(lesson.sectionName()));
        form.put("description", lesson == null ? "" : nullSafe(lesson.description()));
        form.put("durationMinutes", lesson == null || lesson.durationMinutes() == null
                ? "" : lesson.durationMinutes());
        form.put("freePreview", lesson != null && lesson.freePreview());
        form.put("body", detail == null ? "" : nullSafe(detail.body()));
        form.put("audioUrl", detail == null ? "" : nullSafe(detail.audioUrl()));
        form.put("sourceEntryId", entryId);

        Map<String, Object> payload = readPayload(detail == null ? null : detail.payload());
        form.put("pattern", asText(payload.get("pattern")));
        form.put("meaning", asText(payload.get("meaning")));
        form.put("usageNote", asText(payload.get("usageNote")));
        form.put("kanjiMeaning", asText(payload.get("meaning")));
        form.put("character", asText(payload.get("character")));
        form.put("onReading", asText(payload.get("onReading")));
        form.put("kunReading", asText(payload.get("kunReading")));
        form.put("strokeCount", asText(payload.get("strokeCount")));
        form.put("passage", asText(payload.get("passage")));

        form.put("words", ensureRows(payload.get("words"), DEFAULT_WORD_ROWS, Map.of(
                "word", "", "reading", "", "meaning", "", "example", "")));
        form.put("examples", ensureRows(payload.get("examples"), DEFAULT_EXAMPLE_ROWS, Map.of(
                "jp", "", "vi", "")));
        form.put("compounds", ensureRows(payload.get("compounds"), DEFAULT_COMPOUND_ROWS, Map.of(
                "word", "", "meaning", "")));
        form.put("questions", ensureRows(payload.get("questions"), DEFAULT_QUESTION_ROWS, Map.of(
                "text", "", "a", "", "b", "", "c", "", "d", "", "correct", "0")));

        if (entryId != null) {
            applyLibraryEntry(form, skill, entryId);
            form.put("sourceEntryId", entryId);
        }
        return form;
    }

    public Optional<String> findLibraryTitle(SkillTypeEnum skill, Long entryId) {
        if (skill == null || entryId == null) {
            return Optional.empty();
        }
        return switch (skill) {
            case GRAMMAR -> libraryRepository.findGrammarPoint(entryId).map(row -> asText(row.get("pattern")));
            case VOCABULARY -> libraryRepository.findVocabularyEntry(entryId).map(row -> asText(row.get("word")));
            case KANJI -> libraryRepository.findKanjiEntry(entryId).map(row -> asText(row.get("character")));
            default -> Optional.empty();
        };
    }

    /** Persists the skill specific part of a lesson. Returns the library entry the lesson links to. */
    @Transactional
    public Long save(long lessonId, SkillTypeEnum skill, Long sourceEntryId,
                     MultiValueMap<String, String> params, boolean saveToLibrary, String level) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String body = null;
        String audioUrl = null;

        switch (skill) {
            case VOCABULARY -> {
                List<Map<String, Object>> words = collectRows(params, new String[]{"word", "wordReading", "wordMeaning", "wordExample"},
                        new String[]{"word", "reading", "meaning", "example"});
                if (words.isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng nhập ít nhất một từ cho bài học.");
                }
                payload.put("words", words);
            }
            case GRAMMAR -> {
                payload.put("pattern", value(params, "pattern"));
                payload.put("meaning", value(params, "meaning"));
                payload.put("usageNote", value(params, "usageNote"));
                payload.put("examples", collectRows(params, new String[]{"exampleJp", "exampleVi"},
                        new String[]{"jp", "vi"}));
                if (asText(payload.get("pattern")).isBlank()) {
                    throw new IllegalArgumentException("Vui lòng nhập mẫu câu ngữ pháp.");
                }
            }
            case KANJI -> {
                payload.put("character", value(params, "character"));
                payload.put("onReading", value(params, "onReading"));
                payload.put("kunReading", value(params, "kunReading"));
                payload.put("meaning", value(params, "kanjiMeaning"));
                payload.put("strokeCount", value(params, "strokeCount"));
                payload.put("compounds", collectRows(params, new String[]{"compoundWord", "compoundMeaning"},
                        new String[]{"word", "meaning"}));
                if (asText(payload.get("character")).isBlank()) {
                    throw new IllegalArgumentException("Vui lòng nhập chữ Hán cho bài học.");
                }
            }
            case LISTENING -> {
                audioUrl = value(params, "audioUrl");
                body = value(params, "transcript");
                payload.put("transcript", body);
            }
            case READING -> {
                body = value(params, "passage");
                payload.put("passage", body);
                payload.put("questions", collectRows(params,
                        new String[]{"questionText", "optionA", "optionB", "optionC", "optionD", "correct"},
                        new String[]{"text", "a", "b", "c", "d", "correct"}));
                if (body == null) {
                    throw new IllegalArgumentException("Vui lòng nhập đoạn văn cho bài đọc hiểu.");
                }
            }
            default -> {
                // nothing skill specific
            }
        }

        Long resolvedEntryId = sourceEntryId;
        if (saveToLibrary && sourceEntryId == null && supportsLibrary(skill)) {
            resolvedEntryId = createLibraryEntry(skill, params, level);
        }

        detailRepository.save(lessonId, skill.getValue(), resolvedEntryId, audioUrl, body, writePayload(payload));
        return resolvedEntryId;
    }

    /** Copies the skill payload of a lesson onto its duplicate. */
    @Transactional
    public void copyDetail(long sourceLessonId, long targetLessonId) {
        detailRepository.findByLessonId(sourceLessonId).ifPresent(detail -> detailRepository.save(
                targetLessonId,
                detail.skillValue(),
                detail.sourceEntryId(),
                detail.audioUrl(),
                detail.body(),
                detail.payload()));
    }

    public void delete(long lessonId) {
        detailRepository.delete(lessonId);
    }

    private long createLibraryEntry(SkillTypeEnum skill, MultiValueMap<String, String> params, String level) {
        String resolvedLevel = level == null || level.isBlank() ? "N5" : level;
        return switch (skill) {
            case GRAMMAR -> libraryRepository.insertGrammarPoint(
                    value(params, "pattern"),
                    fallback(value(params, "meaning"), "Mẫu ngữ pháp mới"),
                    value(params, "usageNote"),
                    firstValue(params, "exampleJp"),
                    firstValue(params, "exampleVi"),
                    resolvedLevel);
            case VOCABULARY -> libraryRepository.insertVocabularyEntry(
                    firstValue(params, "word"),
                    firstValue(params, "wordReading"),
                    fallback(firstValue(params, "wordMeaning"), "Từ vựng mới"),
                    firstValue(params, "wordExample"),
                    resolvedLevel);
            case KANJI -> libraryRepository.insertKanjiEntry(
                    firstValue(params, "character"),
                    firstValue(params, "onReading"),
                    firstValue(params, "kunReading"),
                    fallback(value(params, "kanjiMeaning"), "Hán tự mới"),
                    parseInteger(value(params, "strokeCount")),
                    resolvedLevel);
            default -> 0L;
        };
    }

    private void applyLibraryEntry(Map<String, Object> form, SkillTypeEnum skill, long entryId) {
        switch (skill) {
            case GRAMMAR -> libraryRepository.findGrammarPoint(entryId).ifPresent(row -> {
                form.put("pattern", asText(row.get("pattern")));
                form.put("meaning", asText(row.get("meaning")));
                form.put("usageNote", asText(row.get("usage_note")));
                Map<String, Object> example = new LinkedHashMap<>();
                example.put("jp", asText(row.get("example_jp")));
                example.put("vi", asText(row.get("example_vi")));
                form.put("examples", ensureRows(List.of(example), DEFAULT_EXAMPLE_ROWS, Map.of("jp", "", "vi", "")));
            });
            case VOCABULARY -> libraryRepository.findVocabularyEntry(entryId).ifPresent(row -> {
                Map<String, Object> word = new LinkedHashMap<>();
                word.put("word", asText(row.get("word")));
                word.put("reading", asText(row.get("reading")));
                word.put("meaning", asText(row.get("meaning")));
                word.put("example", asText(row.get("example_sentence")));
                List<Object> words = new ArrayList<>();
                words.add(word);
                Object existing = form.get("words");
                if (existing instanceof List<?> rows) {
                    rows.stream()
                            .filter(Map.class::isInstance)
                            .filter(candidate -> !asText(((Map<?, ?>) candidate).get("word")).isBlank())
                            .forEach(words::add);
                }
                form.put("words", words);
            });
            case KANJI -> libraryRepository.findKanjiEntry(entryId).ifPresent(row -> {
                form.put("character", asText(row.get("character")));
                form.put("onReading", asText(row.get("on_reading")));
                form.put("kunReading", asText(row.get("kun_reading")));
                form.put("meaning", asText(row.get("meaning")));
                form.put("kanjiMeaning", asText(row.get("meaning")));
                Object strokes = row.get("stroke_count");
                form.put("strokeCount", strokes == null ? "" : String.valueOf(strokes));
            });
            default -> {
                // listening and reading have no library
            }
        }
    }

    private List<Map<String, Object>> collectRows(MultiValueMap<String, String> params,
                                                  String[] sourceKeys, String[] targetKeys) {
        List<String> firstColumn = params.get(sourceKeys[0]);
        int rowCount = firstColumn == null ? 0 : firstColumn.size();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < rowCount; index++) {
            Map<String, Object> row = new LinkedHashMap<>();
            boolean blank = true;
            for (int column = 0; column < sourceKeys.length; column++) {
                String cell = cellAt(params.get(sourceKeys[column]), index);
                row.put(targetKeys[column], cell);
                if (!cell.isEmpty()) {
                    blank = false;
                }
            }
            if (!blank) {
                rows.add(row);
            }
        }
        return rows;
    }

    private String cellAt(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value.trim();
    }

    private String value(MultiValueMap<String, String> params, String key) {
        String value = params.getFirst(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstValue(MultiValueMap<String, String> params, String key) {
        List<String> values = params.get(key);
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<Map<String, Object>> ensureRows(Object rawRows, int minimumRows, Map<String, Object> blankRow) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (rawRows instanceof List<?> candidates) {
            for (Object candidate : candidates) {
                if (candidate instanceof Map<?, ?> map) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    map.forEach((key, value) -> row.put(String.valueOf(key), value));
                    rows.add(row);
                }
            }
        }
        while (rows.size() < minimumRows) {
            rows.add(new LinkedHashMap<>(blankRow));
        }
        return rows;
    }

    private Map<String, Object> readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() { });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Không tạo được dữ liệu bài học.", exception);
        }
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
