package com.jlpp.modules.manager.lessons.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonDetail;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LibraryItem;
import com.jlpp.modules.manager.lessons.repository.LessonDetailRepository;
import com.jlpp.modules.manager.lessons.repository.LessonLibraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores skill-specific lesson data and provides the content-library form model. */
@Service
public class LessonDetailService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final LessonDetailRepository detailRepository;
    private final LessonLibraryRepository libraryRepository;
    private final ObjectMapper objectMapper;

    public LessonDetailService(LessonDetailRepository detailRepository,
                               LessonLibraryRepository libraryRepository,
                               ObjectMapper objectMapper) {
        this.detailRepository = detailRepository;
        this.libraryRepository = libraryRepository;
        this.objectMapper = objectMapper;
    }

    public boolean supportsLibrary(SkillTypeEnum skill) {
        return skill == SkillTypeEnum.GRAMMAR || skill == SkillTypeEnum.VOCABULARY
                || skill == SkillTypeEnum.KANJI;
    }

    public List<LibraryItem> findLibrary(SkillTypeEnum skill, String level, String query) {
        return switch (skill) {
            case GRAMMAR -> libraryRepository.findGrammarPoints(level, query);
            case VOCABULARY -> libraryRepository.findVocabularyEntries(level, query);
            case KANJI -> libraryRepository.findKanjiEntries(level, query);
            default -> List.of();
        };
    }

    public Optional<String> findLibraryTitle(SkillTypeEnum skill, Long entryId) {
        if (skill == null || entryId == null) return Optional.empty();
        Optional<Map<String, Object>> entry = findEntry(skill, entryId);
        return entry.map(row -> string(row, switch (skill) {
            case GRAMMAR -> "pattern";
            case VOCABULARY -> "word";
            case KANJI -> "character";
            default -> "";
        }));
    }

    public Optional<LessonDetail> findDetail(long lessonId) {
        return detailRepository.findByLessonId(lessonId);
    }

    public List<Map<String, Object>> findListeningQuestions(long lessonId) {
        LessonDetail detail = findDetail(lessonId).orElse(null);
        if (detail == null) return List.of();
        List<Map<String, Object>> questions = findQuestionRows(detail);
        if (!questions.isEmpty()) return questions;
        if (detail.audioUrl() == null || detail.audioUrl().isBlank()) return List.of();
        Map<String, Object> legacyQuestion = new LinkedHashMap<>();
        legacyQuestion.put("questionAudioUrl", detail.audioUrl());
        return List.of(legacyQuestion);
    }

    public List<Map<String, Object>> findReadingQuestions(long lessonId) {
        LessonDetail detail = findDetail(lessonId).orElse(null);
        return detail == null ? List.of() : findQuestionRows(detail);
    }

    public String findReadingPassage(long lessonId) {
        return findDetail(lessonId).map(LessonDetail::body).orElse("");
    }

    private List<Map<String, Object>> findQuestionRows(LessonDetail detail) {
        if (detail.payload() != null && !detail.payload().isBlank()) {
            try {
                Map<String, Object> payload = objectMapper.readValue(detail.payload(), MAP_TYPE);
                Object rawQuestions = payload.get("questions");
                if (rawQuestions instanceof List<?> questions) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object question : questions) {
                        if (question instanceof Map<?, ?> values) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            values.forEach((key, value) -> {
                                if (key instanceof String textKey) row.put(textKey, value);
                            });
                            result.add(row);
                        }
                    }
                    if (!result.isEmpty()) return List.copyOf(result);
                }
            } catch (Exception ignored) { /* Fall back to legacy audio_url/body fields below. */ }
        }
        return List.of();
    }

    public Map<String, Object> buildForm(SkillTypeEnum skill, Long entryId,
                                         LessonRow lesson, LessonDetail detail) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("title", lesson == null ? "" : lesson.title());
        form.put("sectionName", lesson == null ? "" : value(lesson.sectionName()));
        form.put("description", lesson == null ? "" : value(lesson.description()));
        form.put("passage", lesson == null ? "" : value(lesson.content()));
        form.put("durationMinutes", lesson == null ? "" : lesson.durationMinutes());
        form.put("freePreview", lesson != null && lesson.freePreview());
        form.put("status", lesson == null ? "ACTIVE" : lesson.status().name());
        form.put("examples", List.of(Map.of("exampleJp", "", "exampleVi", "")));
        form.put("words", List.of(Map.of("word", "", "wordReading", "", "wordMeaning", "", "wordExample", "")));
        form.put("compounds", List.of(Map.of("compoundWord", "", "compoundMeaning", "")));
        form.put("questions", List.of(Map.of("questionText", "", "optionA", "", "optionB", "", "optionC", "", "optionD", "", "correct", "0",
                "questionAudioUrl", "")));

        if (entryId != null) findEntry(skill, entryId).ifPresent(row -> applyEntry(form, skill, row));
        boolean hasListeningQuestions = false;
        if (detail != null) {
            form.put("audioUrl", value(detail.audioUrl()));
            form.put("body", value(detail.body()));
            if (skill == SkillTypeEnum.READING && detail.body() != null && !detail.body().isBlank()) {
                form.put("passage", detail.body());
            }
            if (detail.payload() != null && !detail.payload().isBlank()) {
                try {
                    Map<String, Object> savedPayload = objectMapper.readValue(detail.payload(), MAP_TYPE);
                    form.putAll(savedPayload);
                    hasListeningQuestions = skill == SkillTypeEnum.LISTENING
                            && savedPayload.get("questions") instanceof List<?> questions && !questions.isEmpty();
                }
                catch (Exception ignored) { /* Keep the safe defaults when old payload JSON is malformed. */ }
            }
            if (skill == SkillTypeEnum.LISTENING && !hasListeningQuestions) {
                Map<String, String> legacyQuestion = new LinkedHashMap<>();
                legacyQuestion.put("questionAudioUrl", value(detail.audioUrl()));
                legacyQuestion.put("questionText", "");
                legacyQuestion.put("optionA", "");
                legacyQuestion.put("optionB", "");
                legacyQuestion.put("optionC", "");
                legacyQuestion.put("optionD", "");
                legacyQuestion.put("correct", "0");
                form.put("questions", List.of(legacyQuestion));
            }
        }
        return form;
    }

    public Long save(long lessonId, SkillTypeEnum skill, Long sourceEntryId,
                     MultiValueMap<String, String> params, boolean saveToLibrary, String level) {
        Long entryId = sourceEntryId;
        if (saveToLibrary && supportsLibrary(skill)) entryId = createLibraryEntry(skill, params, level);
        Map<String, Object> payload = payload(params, skill);
        String json;
        try { json = objectMapper.writeValueAsString(payload); }
        catch (Exception exception) { throw new IllegalArgumentException("Dữ liệu bài học không hợp lệ.", exception); }
        String audio = skill == SkillTypeEnum.LISTENING
                ? first(params, "questionAudioUrl")
                : first(params, "audioUrl");
        String body = skill == SkillTypeEnum.LISTENING ? "" : first(params, "passage");
        detailRepository.save(lessonId, skill.getValue(), entryId, audio, body, json);
        return entryId;
    }

    public void copyDetail(long sourceLessonId, long targetLessonId) {
        detailRepository.findByLessonId(sourceLessonId).ifPresent(detail -> detailRepository.save(
                targetLessonId, detail.skillValue(), detail.sourceEntryId(), detail.audioUrl(),
                detail.body(), detail.payload() == null ? "{}" : detail.payload()));
    }

    private Map<String, Object> payload(MultiValueMap<String, String> params, SkillTypeEnum skill) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] fields = switch (skill) {
            case GRAMMAR -> new String[]{"pattern", "meaning", "description", "exampleJp", "exampleVi"};
            case VOCABULARY -> new String[]{"description", "word", "wordReading", "wordMeaning", "wordExample"};
            case KANJI -> new String[]{"character", "onReading", "kunReading", "kanjiMeaning", "strokeCount", "compoundWord", "compoundMeaning"};
            case LISTENING -> new String[]{"description"};
            case READING -> new String[]{};
        };
        for (String field : fields) {
            List<String> values = params.get(field);
            if (values != null && values.size() > 1) result.put(field, values);
            else if (values != null) result.put(field, values.get(0));
        }
        if (skill == SkillTypeEnum.READING) {
            result.put("questions", rows(params, "questionText", "optionA", "optionB", "optionC", "optionD", "correct"));
        } else if (skill == SkillTypeEnum.LISTENING) {
            result.put("questions", rows(params, "questionAudioUrl", "questionText", "optionA", "optionB", "optionC", "optionD", "correct"));
        } else if (skill == SkillTypeEnum.GRAMMAR) {
            result.put("examples", rows(params, "exampleJp", "exampleVi"));
        } else if (skill == SkillTypeEnum.VOCABULARY) {
            result.put("words", rows(params, "word", "wordReading", "wordMeaning", "wordExample"));
        } else if (skill == SkillTypeEnum.KANJI) {
            result.put("compounds", rows(params, "compoundWord", "compoundMeaning"));
        }
        return result;
    }

    private List<Map<String, String>> rows(MultiValueMap<String, String> params, String... keys) {
        int count = keys.length == 0 ? 0 : params.getOrDefault(keys[0], List.of()).size();
        List<Map<String, String>> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Map<String, String> row = new LinkedHashMap<>();
            boolean hasValue = false;
            for (String key : keys) {
                List<String> values = params.get(key);
                String value = values == null || index >= values.size() ? "" : value(values.get(index));
                row.put(key, value);
                hasValue |= !value.isBlank();
            }
            if (hasValue) result.add(row);
        }
        return result;
    }

    private void applyEntry(Map<String, Object> form, SkillTypeEnum skill, Map<String, Object> row) {
        switch (skill) {
            case GRAMMAR -> {
                form.put("pattern", string(row, "pattern")); form.put("meaning", string(row, "meaning"));
                form.put("description", string(row, "usage_note"));
                form.put("examples", List.of(Map.of("exampleJp", string(row, "example_jp"), "exampleVi", string(row, "example_vi"))));
            }
            case VOCABULARY -> {
                form.put("words", List.of(Map.of("word", string(row, "word"), "wordReading", string(row, "reading"),
                        "wordMeaning", string(row, "meaning"), "wordExample", string(row, "example_sentence"))));
            }
            case KANJI -> {
                form.put("character", string(row, "character")); form.put("onReading", string(row, "on_reading"));
                form.put("kunReading", string(row, "kun_reading")); form.put("kanjiMeaning", string(row, "meaning"));
                form.put("strokeCount", row.get("stroke_count"));
            }
            default -> { }
        }
    }

    private Long createLibraryEntry(SkillTypeEnum skill, MultiValueMap<String, String> params, String level) {
        return switch (skill) {
            case GRAMMAR -> libraryRepository.insertGrammarPoint(first(params, "pattern"), first(params, "meaning"),
                    first(params, "description"), first(params, "exampleJp"), first(params, "exampleVi"), level);
            case VOCABULARY -> libraryRepository.insertVocabularyEntry(first(params, "word"), first(params, "wordReading"),
                    first(params, "wordMeaning"), first(params, "wordExample"), level);
            case KANJI -> libraryRepository.insertKanjiEntry(first(params, "character"), first(params, "onReading"),
                    first(params, "kunReading"), first(params, "kanjiMeaning"), integer(first(params, "strokeCount")), level);
            default -> null;
        };
    }

    private Optional<Map<String, Object>> findEntry(SkillTypeEnum skill, long id) {
        return switch (skill) {
            case GRAMMAR -> libraryRepository.findGrammarPoint(id);
            case VOCABULARY -> libraryRepository.findVocabularyEntry(id);
            case KANJI -> libraryRepository.findKanjiEntry(id);
            default -> Optional.empty();
        };
    }

    private String first(MultiValueMap<String, String> params, String key) { return value(params.getFirst(key)); }
    private String value(String value) { return value == null ? "" : value.trim(); }
    private Integer integer(String value) { try { return value.isBlank() ? null : Integer.valueOf(value); } catch (NumberFormatException e) { return null; } }
    private String string(Map<String, Object> row, String key) { return value(String.valueOf(row.getOrDefault(key, ""))); }
}
