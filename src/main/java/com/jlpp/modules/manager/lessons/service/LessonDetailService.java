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

    public Map<String, Object> findKanjiDetail(LessonRow lesson) {
        if (lesson == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        LessonDetail detail = detailRepository.findByLessonId(lesson.id()).orElse(null);

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        if (detail != null && detail.payload() != null && !detail.payload().isBlank()) {
            try {
                payloadMap = objectMapper.readValue(detail.payload(), MAP_TYPE);
            } catch (Exception ignored) { }
        }

        String character = string(payloadMap, "character");
        if (character.isBlank()) {
            String title = lesson.title() == null ? "" : lesson.title().trim();
            if (title.startsWith("Hán tự:")) {
                String sub = title.substring(7).trim();
                int dash = sub.indexOf('-');
                int paren = sub.indexOf('(');
                int cut = dash > 0 ? (paren > 0 ? Math.min(dash, paren) : dash) : paren;
                character = (cut > 0 ? sub.substring(0, cut) : sub).trim();
            } else if (!title.isBlank()) {
                character = title.length() <= 2 ? title : title.substring(0, 1);
            }
        }
        result.put("character", character);

        Map<String, Object> kanjiEntry = null;
        if (!character.isBlank()) {
            kanjiEntry = libraryRepository.findKanjiByCharacter(character).orElse(null);
        } else if (detail != null && detail.sourceEntryId() != null) {
            kanjiEntry = libraryRepository.findKanjiEntry(detail.sourceEntryId()).orElse(null);
            if (kanjiEntry != null && character.isBlank()) {
                character = string(kanjiEntry, "character");
                result.put("character", character);
            }
        }

        String hanviet = string(payloadMap, "hanviet");
        if (hanviet.isBlank() && kanjiEntry != null) {
            hanviet = string(kanjiEntry, "hanviet");
            if (hanviet.contains(",")) {
                hanviet = hanviet.split(",")[0].trim();
            }
        }
        result.put("hanviet", hanviet);

        String onReading = string(payloadMap, "onReading");
        if (onReading.isBlank() && kanjiEntry != null) {
            onReading = string(kanjiEntry, "on_reading");
        }
        if (!onReading.isBlank()) {
            onReading = onReading.replace(";", "、").replace("；", "、");
        }
        result.put("onReading", onReading);

        String kunReading = string(payloadMap, "kunReading");
        if (kunReading.isBlank() && kanjiEntry != null) {
            kunReading = string(kanjiEntry, "kun_reading");
        }
        if (!kunReading.isBlank()) {
            kunReading = kunReading.replace(".", "・").replace(";", "、").replace("；", "、");
        }
        result.put("kunReading", kunReading);

        Object strokeCount = payloadMap.get("strokeCount");
        if (strokeCount == null && kanjiEntry != null) {
            strokeCount = kanjiEntry.get("stroke_count");
        }
        result.put("strokeCount", strokeCount);

        String radical = string(payloadMap, "radical");
        if (radical.isBlank() && kanjiEntry != null) {
            radical = string(kanjiEntry, "radical");
        }
        result.put("radical", radical);

        String meaning = string(payloadMap, "kanjiMeaning");
        if (meaning.isBlank() && kanjiEntry != null) {
            meaning = string(kanjiEntry, "meaning");
        }
        result.put("meaning", meaning);

        List<Map<String, String>> compounds = new ArrayList<>();
        Object rawCompounds = payloadMap.get("compounds");
        if (rawCompounds instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String word = map.get("compoundWord") != null ? map.get("compoundWord").toString().trim()
                            : (map.get("word") != null ? map.get("word").toString().trim() : "");
                    String reading = map.get("compoundReading") != null ? map.get("compoundReading").toString().trim()
                            : (map.get("reading") != null ? map.get("reading").toString().trim() : "");
                    String compMeaning = map.get("compoundMeaning") != null ? map.get("compoundMeaning").toString().trim()
                            : (map.get("meaning") != null ? map.get("meaning").toString().trim() : "");

                    if (!word.isBlank()) {
                        if (reading.isBlank() || compMeaning.isBlank()) {
                            Map<String, String> vocabInfo = libraryRepository.findVocabularyInfo(word);
                            if (reading.isBlank() && vocabInfo.containsKey("reading")) {
                                reading = vocabInfo.get("reading");
                            }
                            if (compMeaning.isBlank() && vocabInfo.containsKey("meaning")) {
                                compMeaning = vocabInfo.get("meaning");
                            }
                        }
                        Map<String, String> c = new LinkedHashMap<>();
                        c.put("compoundWord", word);
                        c.put("compoundReading", reading);
                        c.put("compoundMeaning", compMeaning);
                        compounds.add(c);
                    }
                }
            }
        }

        if (compounds.isEmpty() && kanjiEntry != null) {
            String wordsStr = string(kanjiEntry, "words");
            if (!wordsStr.isBlank()) {
                String[] words = wordsStr.split(";");
                for (String w : words) {
                    String cleanW = w.trim();
                    if (!cleanW.isEmpty()) {
                        Map<String, String> vocabInfo = libraryRepository.findVocabularyInfo(cleanW);
                        Map<String, String> c = new LinkedHashMap<>();
                        c.put("compoundWord", cleanW);
                        c.put("compoundReading", vocabInfo.getOrDefault("reading", ""));
                        c.put("compoundMeaning", vocabInfo.getOrDefault("meaning", ""));
                        compounds.add(c);
                    }
                }
            }
        }

        result.put("compounds", compounds);
        return result;
    }

    public Map<String, Object> findGrammarDetail(LessonRow lesson) {
        if (lesson == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        LessonDetail detail = detailRepository.findByLessonId(lesson.id()).orElse(null);

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        if (detail != null && detail.payload() != null && !detail.payload().isBlank()) {
            try {
                payloadMap = objectMapper.readValue(detail.payload(), MAP_TYPE);
            } catch (Exception ignored) { }
        }

        String pattern = string(payloadMap, "pattern");
        if (pattern.isBlank()) {
            String title = lesson.title() == null ? "" : lesson.title().trim();
            if (title.startsWith("Ngữ pháp:")) {
                String sub = title.substring(9).trim();
                int paren = sub.indexOf('(');
                int dash = sub.indexOf('-');
                int cut = dash > 0 ? (paren > 0 ? Math.min(dash, paren) : dash) : paren;
                pattern = (cut > 0 ? sub.substring(0, cut) : sub).trim();
            } else if (!title.isBlank()) {
                pattern = title;
            }
        }

        Map<String, Object> grammarEntry = null;
        if (!pattern.isBlank()) {
            grammarEntry = libraryRepository.findGrammarByPattern(pattern).orElse(null);
        } else if (detail != null && detail.sourceEntryId() != null) {
            grammarEntry = libraryRepository.findGrammarPoint(detail.sourceEntryId()).orElse(null);
            if (grammarEntry != null && pattern.isBlank()) {
                pattern = string(grammarEntry, "pattern");
            }
        }
        result.put("pattern", pattern);

        String meaning = string(payloadMap, "meaning");
        if (meaning.isBlank() && grammarEntry != null) {
            meaning = string(grammarEntry, "meaning");
        }
        result.put("meaning", meaning);

        String formation = string(payloadMap, "formation");
        if (formation.isBlank() && grammarEntry != null) {
            formation = string(grammarEntry, "formation");
        }
        result.put("formation", formation);

        List<Map<String, String>> formationRows = new ArrayList<>();
        if (!formation.isBlank()) {
            String[] lines = formation.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                String[] parts;
                if (trimmed.contains("\t")) {
                    parts = trimmed.split("\t+", 2);
                } else if (trimmed.contains("   ")) {
                    parts = trimmed.split("\\s{3,}", 2);
                } else if (trimmed.contains(" / ")) {
                    parts = trimmed.split(" / ", 2);
                } else {
                    parts = new String[]{trimmed, ""};
                }
                Map<String, String> rowMap = new LinkedHashMap<>();
                rowMap.put("col1", parts[0].trim());
                rowMap.put("col2", parts.length > 1 ? parts[1].trim() : "");
                formationRows.add(rowMap);
            }
        }
        result.put("formationRows", formationRows);

        String description = string(payloadMap, "description");
        if (description.isBlank() && grammarEntry != null) {
            String notesVi = string(grammarEntry, "notes_vi");
            description = !notesVi.isBlank() ? notesVi : string(grammarEntry, "usage_note");
        }
        if (description.isBlank() && lesson.description() != null) {
            description = lesson.description().trim();
        }
        result.put("description", description);

        List<String> usageNotes = new ArrayList<>();
        if (!description.isBlank()) {
            String[] rawBullets = description.split("(?:\\r?\\n|❖|✦|•)+");
            for (String b : rawBullets) {
                String clean = b.trim();
                if (!clean.isEmpty()) {
                    usageNotes.add(clean);
                }
            }
        }
        result.put("usageNotes", usageNotes);

        List<Map<String, String>> examples = new ArrayList<>();
        Object rawExamples = payloadMap.get("examples");
        if (rawExamples instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> m) {
                    String jp = m.get("exampleJp") != null ? m.get("exampleJp").toString().trim()
                            : (m.get("jp") != null ? m.get("jp").toString().trim() : "");
                    String vi = m.get("exampleVi") != null ? m.get("exampleVi").toString().trim()
                            : (m.get("vi") != null ? m.get("vi").toString().trim() : "");
                    if (vi.isEmpty() && grammarEntry != null) {
                        vi = string(grammarEntry, "example_vi");
                    }
                    if (!jp.isEmpty() || !vi.isEmpty()) {
                        examples.add(Map.of("jp", jp, "vi", vi));
                    }
                }
            }
        }
        if (examples.isEmpty() && grammarEntry != null) {
            String ejp = string(grammarEntry, "example_jp");
            String evi = string(grammarEntry, "example_vi");
            if (!ejp.isBlank() || !evi.isBlank()) {
                examples.add(Map.of("jp", ejp, "vi", evi));
            }
        }
        result.put("examples", examples);

        return result;
    }

    public Map<String, Object> findVocabDetail(LessonRow lesson) {
        if (lesson == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        LessonDetail detail = detailRepository.findByLessonId(lesson.id()).orElse(null);

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        if (detail != null && detail.payload() != null && !detail.payload().isBlank()) {
            try {
                payloadMap = objectMapper.readValue(detail.payload(), MAP_TYPE);
            } catch (Exception ignored) { }
        }

        List<Map<String, Object>> words = new ArrayList<>();
        Object rawWords = payloadMap.get("words");
        if (rawWords instanceof List<?> list && !list.isEmpty()) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> m) {
                    String w = m.get("word") != null ? m.get("word").toString().trim() : "";
                    String reading = m.get("wordReading") != null ? m.get("wordReading").toString().trim()
                            : (m.get("reading") != null ? m.get("reading").toString().trim() : "");
                    String meaning = m.get("wordMeaning") != null ? m.get("wordMeaning").toString().trim()
                            : (m.get("meaning") != null ? m.get("meaning").toString().trim() : "");
                    String pos = m.get("partOfSpeech") != null ? m.get("partOfSpeech").toString().trim() : "";
                    String hanviet = m.get("hanviet") != null ? m.get("hanviet").toString().trim() : "";
                    String exJp = m.get("wordExample") != null ? m.get("wordExample").toString().trim()
                            : (m.get("example_sentence") != null ? m.get("example_sentence").toString().trim()
                            : (m.get("example") != null ? m.get("example").toString().trim() : ""));
                    String exVi = m.get("wordExampleVi") != null ? m.get("wordExampleVi").toString().trim()
                            : (m.get("example_vi") != null ? m.get("example_vi").toString().trim()
                            : (m.get("exampleVi") != null ? m.get("exampleVi").toString().trim() : ""));

                    if (!w.isEmpty()) {
                        Map<String, Object> dbEntry = null;
                        if (reading.isEmpty() || meaning.isEmpty() || pos.isEmpty() || exJp.isEmpty() || exVi.isEmpty()) {
                            dbEntry = libraryRepository.findVocabularyByWord(w).orElse(null);
                        }
                        if (reading.isEmpty() && dbEntry != null) reading = string(dbEntry, "reading");
                        if (meaning.isEmpty() && dbEntry != null) meaning = string(dbEntry, "meaning");
                        if (pos.isEmpty()) {
                            pos = dbEntry != null ? formatPartOfSpeech(string(dbEntry, "part_of_speech")) : "(N)";
                        } else {
                            pos = formatPartOfSpeech(pos);
                        }
                        if (hanviet.isEmpty()) hanviet = findHanvietForWord(w);
                        if (exJp.isEmpty() && dbEntry != null) {
                            exJp = string(dbEntry, "example_sentence");
                        }
                        if (exVi.isEmpty() && dbEntry != null) {
                            exVi = string(dbEntry, "example_vi");
                        }

                        Map<String, Object> wordMap = new LinkedHashMap<>();
                        wordMap.put("word", w);
                        wordMap.put("reading", reading);
                        wordMap.put("meaning", meaning);
                        wordMap.put("partOfSpeech", pos);
                        wordMap.put("hanviet", hanviet);

                        List<Map<String, String>> exList = new ArrayList<>();
                        if (!exJp.isEmpty() || !exVi.isEmpty()) {
                            exList.add(Map.of("jp", exJp, "vi", exVi));
                        }
                        wordMap.put("examples", exList);
                        words.add(wordMap);
                    }
                }
            }
        }

        if (words.isEmpty()) {
            Map<String, Object> vocabEntry = null;
            if (detail != null && detail.sourceEntryId() != null) {
                vocabEntry = libraryRepository.findVocabularyEntry(detail.sourceEntryId()).orElse(null);
            }
            if (vocabEntry == null && lesson.title() != null) {
                String title = lesson.title().trim();
                String targetWord = title.startsWith("Từ vựng:") ? title.substring(8).trim() : title;
                int paren = targetWord.indexOf('(');
                int dash = targetWord.indexOf('-');
                int cut = dash > 0 ? (paren > 0 ? Math.min(dash, paren) : dash) : paren;
                targetWord = (cut > 0 ? targetWord.substring(0, cut) : targetWord).trim();
                if (!targetWord.isEmpty()) {
                    vocabEntry = libraryRepository.findVocabularyByWord(targetWord).orElse(null);
                }
            }
            if (vocabEntry != null) {
                String w = string(vocabEntry, "word");
                String reading = string(vocabEntry, "reading");
                String meaning = string(vocabEntry, "meaning");
                String pos = formatPartOfSpeech(string(vocabEntry, "part_of_speech"));
                String hanviet = findHanvietForWord(w);
                String exJp = string(vocabEntry, "example_sentence");
                String exVi = string(vocabEntry, "example_vi");

                Map<String, Object> wordMap = new LinkedHashMap<>();
                wordMap.put("word", w);
                wordMap.put("reading", reading);
                wordMap.put("meaning", meaning);
                wordMap.put("partOfSpeech", pos);
                wordMap.put("hanviet", hanviet);

                List<Map<String, String>> exList = new ArrayList<>();
                if (!exJp.isEmpty() || !exVi.isEmpty()) {
                    exList.add(Map.of("jp", exJp, "vi", exVi));
                }
                wordMap.put("examples", exList);
                words.add(wordMap);
            }
        }

        result.put("words", words);
        return result;
    }

    public String findHanvietForWord(String word) {
        if (word == null || word.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                String hv = libraryRepository.findHanvietByCharacter(String.valueOf(c));
                if (!hv.isBlank()) {
                    if (sb.length() > 0) sb.append(" ");
                    if (hv.contains(",")) hv = hv.split(",")[0].trim();
                    sb.append(hv.toUpperCase());
                }
            }
        }
        return sb.toString();
    }

    public String formatPartOfSpeech(String rawPos) {
        return LessonLibraryRepository.formatPartOfSpeech(rawPos);
    }

    public String findReadingPassage(long lessonId) {
        Optional<LessonDetail> detailOpt = findDetail(lessonId);
        if (detailOpt.isPresent()) {
            LessonDetail detail = detailOpt.get();
            if (detail.body() != null && !detail.body().isBlank()) {
                return detail.body();
            }
            if (detail.payload() != null && !detail.payload().isBlank()) {
                try {
                    Map<String, Object> map = objectMapper.readValue(detail.payload(), MAP_TYPE);
                    Object passage = map.get("passage");
                    if (passage != null && !passage.toString().isBlank()) {
                        return passage.toString();
                    }
                } catch (Exception ignored) {}
            }
        }
        return "";
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
        form.put("examples", List.of(Map.of("exampleJp", "", "exampleVi", "", "jp", "", "vi", "")));
        Map<String, String> defaultWord = new LinkedHashMap<>();
        defaultWord.put("word", "");
        defaultWord.put("wordReading", "");
        defaultWord.put("reading", "");
        defaultWord.put("wordMeaning", "");
        defaultWord.put("meaning", "");
        defaultWord.put("partOfSpeech", "(N)");
        defaultWord.put("hanviet", "");
        defaultWord.put("wordExample", "");
        defaultWord.put("example", "");
        defaultWord.put("wordExampleVi", "");
        defaultWord.put("exampleVi", "");
        form.put("words", List.of(defaultWord));
        form.put("compounds", List.of(Map.of("compoundWord", "", "compoundMeaning", "")));
        Map<String, String> defaultQuestion = new LinkedHashMap<>();
        defaultQuestion.put("questionText", "");
        defaultQuestion.put("optionA", "");
        defaultQuestion.put("optionB", "");
        defaultQuestion.put("optionC", "");
        defaultQuestion.put("optionD", "");
        defaultQuestion.put("correct", "0");
        defaultQuestion.put("questionAudioUrl", "");
        form.put("questions", List.of(defaultQuestion));

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

        if (entryId != null) {
            findEntry(skill, entryId).ifPresent(row -> applyEntry(form, skill, row));
        }

        if (skill == SkillTypeEnum.GRAMMAR) {
            Object exObj = form.get("examples");
            List<Map<String, String>> normalizedExamples = new ArrayList<>();
            if (exObj instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String jp = string(m, "exampleJp");
                        if (jp.isBlank()) jp = string(m, "jp");
                        String vi = string(m, "exampleVi");
                        if (vi.isBlank()) vi = string(m, "vi");
                        if (!jp.isBlank() || !vi.isBlank()) {
                            Map<String, String> row = new LinkedHashMap<>();
                            row.put("exampleJp", jp);
                            row.put("exampleVi", vi);
                            row.put("jp", jp);
                            row.put("vi", vi);
                            normalizedExamples.add(row);
                        }
                    }
                }
            }
            if (normalizedExamples.isEmpty()) {
                Map<String, Object> gEntry = null;
                if (entryId != null) {
                    gEntry = findEntry(skill, entryId).orElse(null);
                } else if (detail != null && detail.sourceEntryId() != null) {
                    gEntry = libraryRepository.findGrammarPoint(detail.sourceEntryId()).orElse(null);
                }
                if (gEntry == null) {
                    String pat = string(form, "pattern");
                    if (pat.isBlank() && lesson != null && lesson.title() != null) {
                        pat = lesson.title().trim();
                    }
                    if (!pat.isBlank()) {
                        gEntry = libraryRepository.findGrammarByPattern(pat).orElse(null);
                    }
                }
                if (gEntry != null) {
                    String ejp = string(gEntry, "example_jp");
                    String evi = string(gEntry, "example_vi");
                    if (!ejp.isBlank() || !evi.isBlank()) {
                        Map<String, String> row = new LinkedHashMap<>();
                        row.put("exampleJp", ejp);
                        row.put("exampleVi", evi);
                        row.put("jp", ejp);
                        row.put("vi", evi);
                        normalizedExamples.add(row);
                    }
                }
            }
            if (!normalizedExamples.isEmpty()) {
                form.put("examples", normalizedExamples);
            }
        } else if (skill == SkillTypeEnum.VOCABULARY) {
            Object wordsObj = form.get("words");
            List<Map<String, String>> enrichedWords = new ArrayList<>();
            if (wordsObj instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String w = string(m, "word");
                        String r = string(m, "wordReading");
                        if (r.isBlank()) r = string(m, "reading");
                        String meaning = string(m, "wordMeaning");
                        if (meaning.isBlank()) meaning = string(m, "meaning");
                        String pos = string(m, "partOfSpeech");
                        String hanviet = string(m, "hanviet");
                        String exJp = string(m, "wordExample");
                        if (exJp.isBlank()) exJp = string(m, "example");
                        if (exJp.isBlank()) exJp = string(m, "example_sentence");
                        String exVi = string(m, "wordExampleVi");
                        if (exVi.isBlank()) exVi = string(m, "exampleVi");
                        if (exVi.isBlank()) exVi = string(m, "example_vi");

                        if (!w.isBlank()) {
                            if (r.isBlank() || meaning.isBlank() || pos.isBlank() || exJp.isBlank() || exVi.isBlank()) {
                                Map<String, Object> dbEntry = libraryRepository.findVocabularyByWord(w).orElse(null);
                                if (dbEntry != null) {
                                    if (r.isBlank()) r = string(dbEntry, "reading");
                                    if (meaning.isBlank()) meaning = string(dbEntry, "meaning");
                                    if (pos.isBlank()) pos = formatPartOfSpeech(string(dbEntry, "part_of_speech"));
                                    if (exJp.isBlank()) exJp = string(dbEntry, "example_sentence");
                                    if (exVi.isBlank()) exVi = string(dbEntry, "example_vi");
                                }
                            }
                            if (hanviet.isBlank()) hanviet = findHanvietForWord(w);
                            if (pos.isBlank()) pos = "(N)";

                            Map<String, String> row = new LinkedHashMap<>();
                            row.put("word", w);
                            row.put("wordReading", r);
                            row.put("reading", r);
                            row.put("wordMeaning", meaning);
                            row.put("meaning", meaning);
                            row.put("partOfSpeech", pos);
                            row.put("hanviet", hanviet);
                            row.put("wordExample", exJp);
                            row.put("example", exJp);
                            row.put("wordExampleVi", exVi);
                            row.put("exampleVi", exVi);
                            enrichedWords.add(row);
                        }
                    }
                }
            }
            if (enrichedWords.isEmpty()) {
                Map<String, Object> dbEntry = null;
                if (entryId != null) {
                    dbEntry = findEntry(skill, entryId).orElse(null);
                } else if (detail != null && detail.sourceEntryId() != null) {
                    dbEntry = libraryRepository.findVocabularyEntry(detail.sourceEntryId()).orElse(null);
                }
                if (dbEntry == null && lesson != null && lesson.title() != null) {
                    String title = lesson.title().trim();
                    String targetWord = title.startsWith("Từ vựng:") ? title.substring(8).trim() : title;
                    int paren = targetWord.indexOf('(');
                    int dash = targetWord.indexOf('-');
                    int cut = dash > 0 ? (paren > 0 ? Math.min(dash, paren) : dash) : paren;
                    targetWord = (cut > 0 ? targetWord.substring(0, cut) : targetWord).trim();
                    if (!targetWord.isEmpty()) {
                        dbEntry = libraryRepository.findVocabularyByWord(targetWord).orElse(null);
                    }
                }
                if (dbEntry != null) {
                    String w = string(dbEntry, "word");
                    String r = string(dbEntry, "reading");
                    String meaning = string(dbEntry, "meaning");
                    String pos = formatPartOfSpeech(string(dbEntry, "part_of_speech"));
                    String hanviet = findHanvietForWord(w);
                    String exJp = string(dbEntry, "example_sentence");
                    String exVi = string(dbEntry, "example_vi");

                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("word", w);
                    row.put("wordReading", r);
                    row.put("reading", r);
                    row.put("wordMeaning", meaning);
                    row.put("meaning", meaning);
                    row.put("partOfSpeech", pos);
                    row.put("hanviet", hanviet);
                    row.put("wordExample", exJp);
                    row.put("example", exJp);
                    row.put("wordExampleVi", exVi);
                    row.put("exampleVi", exVi);
                    enrichedWords.add(row);
                }
            }
            if (!enrichedWords.isEmpty()) {
                form.put("words", enrichedWords);
            }
        }
        if (skill == SkillTypeEnum.READING) {
            Object curPassage = form.get("passage");
            if (curPassage == null || curPassage.toString().isBlank()) {
                if (detail != null && detail.body() != null && !detail.body().isBlank()) {
                    form.put("passage", detail.body());
                } else if (lesson != null && lesson.content() != null && !lesson.content().isBlank()) {
                    form.put("passage", lesson.content());
                }
            }
        }
        return form;
    }

    public Long save(long lessonId, SkillTypeEnum skill, Long sourceEntryId,
                     MultiValueMap<String, String> params, boolean saveToLibrary, String level) {
        validateDetail(skill, params);
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
            case GRAMMAR -> new String[]{"pattern", "meaning", "description", "formation", "notes", "exampleJp", "exampleVi"};
            case VOCABULARY -> new String[]{"description", "word", "wordReading", "wordMeaning", "partOfSpeech", "hanviet", "wordExample", "wordExampleVi"};
            case KANJI -> new String[]{"character", "hanviet", "onReading", "kunReading", "kanjiMeaning", "strokeCount", "radical", "nanori", "compoundWord", "compoundReading", "compoundMeaning"};
            case LISTENING -> new String[]{"description"};
            case READING -> new String[]{"passage"};
        };
        for (String field : fields) {
            List<String> values = params.get(field);
            if (values != null && values.size() > 1) result.put(field, values);
            else if (values != null) result.put(field, values.get(0));
        }
        if (skill == SkillTypeEnum.READING) {
            String passage = first(params, "passage");
            if (!passage.isBlank()) {
                result.put("passage", passage);
            }
            result.put("questions", rows(params, "questionText", "optionA", "optionB", "optionC", "optionD", "correct"));
        } else if (skill == SkillTypeEnum.LISTENING) {
            result.put("questions", rows(params, "questionAudioUrl", "questionText", "optionA", "optionB", "optionC", "optionD", "correct"));
        } else if (skill == SkillTypeEnum.GRAMMAR) {
            result.put("examples", rows(params, "exampleJp", "exampleVi"));
        } else if (skill == SkillTypeEnum.VOCABULARY) {
            result.put("words", rows(params, "word", "wordReading", "wordMeaning", "partOfSpeech", "hanviet", "wordExample", "wordExampleVi"));
        } else if (skill == SkillTypeEnum.KANJI) {
            result.put("compounds", rows(params, "compoundWord", "compoundReading", "compoundMeaning"));
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
                String pattern = string(row, "pattern");
                String meaning = string(row, "meaning");
                String formation = string(row, "formation");
                String notesVi = string(row, "notes_vi");
                String usageNote = string(row, "usage_note");
                String desc = !notesVi.isBlank() ? notesVi : usageNote;
                form.put("pattern", pattern);
                form.put("meaning", meaning);
                form.put("formation", formation);
                form.put("description", desc);
                if (form.get("title") == null || form.get("title").toString().isBlank()) {
                    form.put("title", "Ngữ pháp: " + pattern + (!meaning.isBlank() ? " (" + meaning + ")" : ""));
                }
                Map<String, String> exMap = new LinkedHashMap<>();
                exMap.put("exampleJp", string(row, "example_jp"));
                exMap.put("exampleVi", string(row, "example_vi"));
                exMap.put("jp", string(row, "example_jp"));
                exMap.put("vi", string(row, "example_vi"));
                form.put("examples", List.of(exMap));
            }
            case VOCABULARY -> {
                String word = string(row, "word");
                String reading = string(row, "reading");
                String meaning = string(row, "meaning");
                String pos = formatPartOfSpeech(string(row, "part_of_speech"));
                String hanviet = findHanvietForWord(word);
                String exJp = string(row, "example_sentence");
                String exVi = string(row, "example_vi");
                if (form.get("title") == null || form.get("title").toString().isBlank()) {
                    form.put("title", "Từ vựng: " + word + (!reading.isBlank() ? " (" + reading + ")" : "") + (!meaning.isBlank() ? " - " + meaning : ""));
                }
                Map<String, String> wordRow = new LinkedHashMap<>();
                wordRow.put("word", word);
                wordRow.put("wordReading", reading);
                wordRow.put("reading", reading);
                wordRow.put("wordMeaning", meaning);
                wordRow.put("meaning", meaning);
                wordRow.put("partOfSpeech", pos);
                wordRow.put("hanviet", hanviet);
                wordRow.put("wordExample", exJp);
                wordRow.put("example", exJp);
                wordRow.put("wordExampleVi", exVi);
                wordRow.put("exampleVi", exVi);
                form.put("words", List.of(wordRow));
            }
            case KANJI -> {
                String character = string(row, "character");
                String kanjiMeaning = string(row, "meaning");
                String hanviet = string(row, "hanviet");
                if (hanviet.contains(",")) {
                    hanviet = hanviet.split(",")[0].trim();
                }
                String onRead = string(row, "on_reading").replace(";", "、").replace("；", "、");
                String kunRead = string(row, "kun_reading").replace(".", "・").replace(";", "、").replace("；", "、");
                form.put("character", character);
                form.put("hanviet", hanviet);
                form.put("onReading", onRead);
                form.put("kunReading", kunRead);
                form.put("kanjiMeaning", kanjiMeaning);
                form.put("strokeCount", row.get("stroke_count"));
                form.put("radical", string(row, "radical"));
                form.put("nanori", string(row, "nanori"));
                if (form.get("title") == null || form.get("title").toString().isBlank()) {
                    form.put("title", "Hán tự: " + character + (!hanviet.isBlank() ? " (" + hanviet + ")" : (!kanjiMeaning.isBlank() ? " - " + kanjiMeaning : "")));
                }

                String wordsStr = string(row, "words");
                List<Map<String, String>> compounds = new ArrayList<>();
                if (!wordsStr.isBlank()) {
                    String[] words = wordsStr.split(";");
                    for (String w : words) {
                        String cleanW = w.trim();
                        if (!cleanW.isEmpty()) {
                            Map<String, String> vocabInfo = libraryRepository.findVocabularyInfo(cleanW);
                            Map<String, String> cMap = new LinkedHashMap<>();
                            cMap.put("compoundWord", cleanW);
                            cMap.put("compoundReading", vocabInfo.getOrDefault("reading", ""));
                            cMap.put("compoundMeaning", vocabInfo.getOrDefault("meaning", ""));
                            compounds.add(cMap);
                        }
                    }
                }
                if (!compounds.isEmpty()) {
                    form.put("compounds", compounds);
                }
            }
            default -> { }
        }
    }

    private Long createLibraryEntry(SkillTypeEnum skill, MultiValueMap<String, String> params, String level) {
        return switch (skill) {
            case GRAMMAR -> libraryRepository.insertGrammarPoint(first(params, "pattern"), first(params, "meaning"),
                    first(params, "description"), first(params, "formation"), first(params, "exampleJp"), first(params, "exampleVi"), level);
            case VOCABULARY -> libraryRepository.insertVocabularyEntry(first(params, "word"), first(params, "wordReading"),
                    first(params, "partOfSpeech"), first(params, "wordMeaning"), first(params, "wordExample"), first(params, "wordExampleVi"), level);
            case KANJI -> libraryRepository.insertKanjiEntry(first(params, "character"), first(params, "onReading"),
                    first(params, "kunReading"), first(params, "kanjiMeaning"), integer(first(params, "strokeCount")),
                    first(params, "radical"), level);
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

    public void validateDetail(SkillTypeEnum skill, MultiValueMap<String, String> params) {
        if (skill == null || params == null) return;
        switch (skill) {
            case VOCABULARY -> validateVocabulary(params);
            case KANJI -> validateKanji(params);
            case GRAMMAR -> validateGrammar(params);
            case READING -> validateReading(params);
            case LISTENING -> validateListening(params);
        }
    }

    private void validateVocabulary(MultiValueMap<String, String> params) {
        List<String> words = params.get("word");
        List<String> readings = params.get("wordReading") != null ? params.get("wordReading") : params.get("reading");
        List<String> meanings = params.get("wordMeaning") != null ? params.get("wordMeaning") : params.get("meaning");
        List<String> examples = params.get("wordExample") != null ? params.get("wordExample") : params.get("example");
        List<String> exampleVis = params.get("wordExampleVi") != null ? params.get("wordExampleVi") : params.get("exampleVi");

        boolean hasWord = false;
        int count = words == null ? 0 : words.size();
        for (int i = 0; i < count; i++) {
            String w = words.get(i) == null ? "" : words.get(i).trim();
            if (w.isBlank()) continue;
            hasWord = true;
            if (w.length() > 50) {
                throw new IllegalArgumentException("Từ vựng thứ " + (i + 1) + " tối đa 50 ký tự.");
            }
            String reading = readings != null && i < readings.size() ? value(readings.get(i)).trim() : "";
            if (reading.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập cách đọc cho từ: “" + w + "”.");
            }
            if (reading.length() > 50) {
                throw new IllegalArgumentException("Cách đọc của từ “" + w + "” tối đa 50 ký tự.");
            }
            String meaning = meanings != null && i < meanings.size() ? value(meanings.get(i)).trim() : "";
            if (meaning.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập ý nghĩa cho từ: “" + w + "”.");
            }
            if (meaning.length() > 255) {
                throw new IllegalArgumentException("Ý nghĩa của từ “" + w + "” tối đa 255 ký tự.");
            }
            String exJp = examples != null && i < examples.size() ? value(examples.get(i)).trim() : "";
            String exVi = exampleVis != null && i < exampleVis.size() ? value(exampleVis.get(i)).trim() : "";
            if (!exJp.isBlank() && exVi.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập câu dịch tiếng Việt cho ví dụ của từ: “" + w + "”.");
            }
        }
        if (!hasWord) {
            throw new IllegalArgumentException("Bài học Từ vựng phải có ít nhất 1 từ vựng.");
        }
    }

    private void validateKanji(MultiValueMap<String, String> params) {
        String character = first(params, "character").trim();
        if (character.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập chữ Hán (Kanji).");
        }
        if (character.codePointCount(0, character.length()) != 1) {
            throw new IllegalArgumentException("Chữ Hán chỉ được nhập đúng 1 ký tự.");
        }

        String hanviet = first(params, "hanviet").trim();
        if (hanviet.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập âm Hán Việt cho chữ Hán.");
        }
        if (hanviet.length() > 50) {
            throw new IllegalArgumentException("Âm Hán Việt tối đa 50 ký tự.");
        }

        String strokeStr = first(params, "strokeCount").trim();
        if (strokeStr.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số nét của chữ Hán.");
        }
        try {
            int strokes = Integer.parseInt(strokeStr);
            if (strokes < 1 || strokes > 40) {
                throw new IllegalArgumentException("Số nét chữ Hán phải từ 1 đến 40 nét.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số nét chữ Hán không hợp lệ.");
        }

        String meaning = first(params, "kanjiMeaning").trim();
        if (meaning.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nghĩa của chữ Hán.");
        }
        if (meaning.length() > 200) {
            throw new IllegalArgumentException("Nghĩa chữ Hán tối đa 200 ký tự.");
        }

        String on = first(params, "onReading").trim();
        String kun = first(params, "kunReading").trim();
        if (on.isBlank() && kun.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất một âm On hoặc âm Kun cho chữ Hán.");
        }
    }

    private void validateGrammar(MultiValueMap<String, String> params) {
        String pattern = first(params, "pattern").trim();
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mẫu ngữ pháp (ví dụ: 〜てはいけない).");
        }
        if (pattern.length() > 100) {
            throw new IllegalArgumentException("Mẫu ngữ pháp tối đa 100 ký tự.");
        }

        String meaning = first(params, "meaning").trim();
        if (meaning.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập ý nghĩa của mẫu ngữ pháp.");
        }
        if (meaning.length() > 255) {
            throw new IllegalArgumentException("Ý nghĩa mẫu ngữ pháp tối đa 255 ký tự.");
        }

        String formation = first(params, "formation").trim();
        if (formation.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập cấu trúc kết hợp ngữ pháp.");
        }
        if (formation.length() > 500) {
            throw new IllegalArgumentException("Cấu trúc kết hợp tối đa 500 ký tự.");
        }

        List<String> exJpList = params.get("exampleJp");
        List<String> exViList = params.get("exampleVi");
        if (exJpList != null) {
            for (int i = 0; i < exJpList.size(); i++) {
                String jp = exJpList.get(i) == null ? "" : exJpList.get(i).trim();
                String vi = exViList != null && i < exViList.size() && exViList.get(i) != null ? exViList.get(i).trim() : "";
                if (!jp.isBlank() && vi.isBlank()) {
                    throw new IllegalArgumentException("Vui lòng nhập bản dịch tiếng Việt cho câu ví dụ " + (i + 1) + ": “" + jp + "”.");
                }
            }
        }
    }

    private void validateReading(MultiValueMap<String, String> params) {
        String passage = first(params, "passage");
        String stripped = passage.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
        if (stripped.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung đoạn văn cho bài Đọc hiểu.");
        }
        if (passage.length() > 20000) {
            throw new IllegalArgumentException("Nội dung đoạn văn tối đa 20.000 ký tự.");
        }

        List<String> questions = params.get("questionText");
        List<String> optA = params.get("optionA");
        List<String> optB = params.get("optionB");
        List<String> optC = params.get("optionC");
        List<String> optD = params.get("optionD");

        boolean hasQuestion = false;
        int qCount = questions == null ? 0 : questions.size();
        for (int i = 0; i < qCount; i++) {
            String qText = questions.get(i) == null ? "" : questions.get(i).trim();
            if (qText.isBlank()) continue;
            hasQuestion = true;
            String a = optA != null && i < optA.size() ? value(optA.get(i)).trim() : "";
            String b = optB != null && i < optB.size() ? value(optB.get(i)).trim() : "";
            String c = optC != null && i < optC.size() ? value(optC.get(i)).trim() : "";
            String d = optD != null && i < optD.size() ? value(optD.get(i)).trim() : "";
            if (a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                throw new IllegalArgumentException("Câu hỏi số " + (i + 1) + " phải nhập đủ cả 4 phương án A, B, C, D.");
            }
        }
        if (!hasQuestion) {
            throw new IllegalArgumentException("Bài Đọc hiểu phải có ít nhất 1 câu hỏi.");
        }
    }

    private void validateListening(MultiValueMap<String, String> params) {
        // Lưu ý: Người dùng yêu cầu không giới hạn dung lượng file nghe
        List<String> questions = params.get("questionText");
        List<String> optA = params.get("optionA");
        List<String> optB = params.get("optionB");
        List<String> optC = params.get("optionC");
        List<String> optD = params.get("optionD");

        int qCount = questions == null ? 0 : questions.size();
        for (int i = 0; i < qCount; i++) {
            String qText = questions.get(i) == null ? "" : questions.get(i).trim();
            if (qText.isBlank()) continue;
            String a = optA != null && i < optA.size() ? value(optA.get(i)).trim() : "";
            String b = optB != null && i < optB.size() ? value(optB.get(i)).trim() : "";
            String c = optC != null && i < optC.size() ? value(optC.get(i)).trim() : "";
            String d = optD != null && i < optD.size() ? value(optD.get(i)).trim() : "";
            if (a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                throw new IllegalArgumentException("Câu hỏi nghe số " + (i + 1) + " phải nhập đủ cả 4 phương án A, B, C, D.");
            }
        }
    }

    private String first(MultiValueMap<String, String> params, String key) { return value(params.getFirst(key)); }
    private String value(String value) { return value == null ? "" : value.trim(); }
    private Integer integer(String value) { try { return value.isBlank() ? null : Integer.valueOf(value); } catch (NumberFormatException e) { return null; } }
    private String string(Map<?, ?> row, String key) {
        if (row == null) return "";
        Object val = row.get(key);
        return val == null ? "" : value(String.valueOf(val));
    }
}
