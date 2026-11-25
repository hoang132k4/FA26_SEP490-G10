package com.jeps.common.util;

import com.jeps.common.util.QuestionOptionsCodec.OptionInput;
import com.jeps.common.util.QuestionOptionsCodec.ParsedOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuestionOptionsCodecTest {

    @Test
    @DisplayName("parse - đọc đầy đủ options và xác định đúng đáp án theo answer_key")
    void parse_standardJson_mapsOptionsAndCorrectKey() {
        String optionsJson = """
                [
                  {"key": "A", "text": "読む"},
                  {"key": "B", "text": "書く"},
                  {"key": "C", "text": "聞く"},
                  {"key": "D", "text": "話す"}
                ]
                """;
        String answerKeyJson = "[\"B\"]";

        List<ParsedOption> options = QuestionOptionsCodec.parse(69L, optionsJson, answerKeyJson);

        assertEquals(4, options.size());
        assertEquals(6901L, options.get(0).id());
        assertEquals("A", options.get(0).label());
        assertFalse(options.get(0).isCorrect());

        assertEquals(6902L, options.get(1).id());
        assertEquals("B", options.get(1).label());
        assertEquals("書く", options.get(1).text());
        assertTrue(options.get(1).isCorrect());
    }

    @Test
    @DisplayName("parse - không mặc định A khi answer_key rỗng hoặc null")
    void parse_missingOrEmptyAnswerKey_neverDefaultsToA() {
        String optionsJson = """
                [
                  {"key": "A", "text": "Opt A"},
                  {"key": "B", "text": "Opt B"}
                ]
                """;

        List<ParsedOption> withNull = QuestionOptionsCodec.parse(10L, optionsJson, null);
        List<ParsedOption> withEmpty = QuestionOptionsCodec.parse(10L, optionsJson, "[]");

        assertEquals(2, withNull.size());
        assertTrue(withNull.stream().noneMatch(ParsedOption::isCorrect));
        assertTrue(withEmpty.stream().noneMatch(ParsedOption::isCorrect));
    }

    @Test
    @DisplayName("parse - giải mã chuỗi JSON bọc chuỗi (kiểu cột JSON của H2)")
    void parse_doubleEncodedJson_unwrapsSuccessfully() {
        String optionsJson = "\"[{\\\"key\\\":\\\"A\\\",\\\"text\\\":\\\"Ăn\\\"},{\\\"key\\\":\\\"B\\\",\\\"text\\\":\\\"Uống\\\"}]\"";
        String answerKeyJson = "\"[\\\"b.\\\"]\"";

        List<ParsedOption> options = QuestionOptionsCodec.parse(5L, optionsJson, answerKeyJson);

        assertEquals(2, options.size());
        assertFalse(options.get(0).isCorrect());
        assertTrue(options.get(1).isCorrect());
    }

    @Test
    @DisplayName("parse - bỏ qua lựa chọn có text rỗng và JSON lỗi không văng exception")
    void parse_blankOptionTextAndMalformedJson_handledSafely() {
        String mixedJson = "[{\"key\":\"A\",\"text\":\"\"},{\"key\":\"B\",\"text\":\"Valid\"}]";
        List<ParsedOption> filtered = QuestionOptionsCodec.parse(7L, mixedJson, "[\"A\"]");
        assertEquals(1, filtered.size());
        assertEquals("B", filtered.get(0).label());
        assertFalse(filtered.get(0).isCorrect());

        assertTrue(QuestionOptionsCodec.parse(7L, "{broken", "[\"A\"]").isEmpty());
        assertEquals(Set.of(), QuestionOptionsCodec.parseAnswerKeys("{broken"));
    }

    @Test
    @DisplayName("toOptionsJson / toAnswerKeyJson - serialize và parse lại nguyên vẹn")
    void serializeAndReparse_roundTrip() {
        String optJson = QuestionOptionsCodec.toOptionsJson(List.of(
                new OptionInput("A", "準備（じゅんび）"),
                new OptionInput("B", "約束（やくそく）")
        ));
        String akJson = QuestionOptionsCodec.toAnswerKeyJson(List.of("b"));

        List<ParsedOption> parsed = QuestionOptionsCodec.parse(42L, optJson, akJson);

        assertEquals(2, parsed.size());
        assertFalse(parsed.get(0).isCorrect());
        assertTrue(parsed.get(1).isCorrect());
        assertEquals("準備（じゅんび）", parsed.get(0).text());
    }
}
