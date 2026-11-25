package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.reader.impl.JdbcLearnerQuestionReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JdbcLearnerQuestionReaderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcLearnerQuestionReader questionReader;

    @Test
    @DisplayName("countQuestionsByExamId - Trả về số câu hỏi từ database qua SELECT COUNT(*)")
    void testCountQuestionsByExamId() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(8L))).thenReturn(5L);

        long count = questionReader.countQuestionsByExamId(8L);

        assertEquals(5L, count);
        verify(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM exam_questions WHERE exam_id = ?"), eq(Long.class), eq(8L));
    }

    @Test
    @DisplayName("countQuestionsByExamId - Trả về 0 khi examId null hoặc có exception")
    void testCountQuestionsByExamId_NullOrError() {
        assertEquals(0L, questionReader.countQuestionsByExamId(null));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(999L))).thenThrow(new RuntimeException("DB Connection timeout"));
        assertEquals(0L, questionReader.countQuestionsByExamId(999L));
    }

    @Test
    @DisplayName("findQuestionsForTaking - Trả về danh sách câu hỏi rỗng an toàn khi không có dữ liệu")
    void testFindQuestionsForTaking_Empty() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(8L)))
                .thenReturn(Collections.emptyList());

        List<QuestionDto> list = questionReader.findQuestionsForTaking(8L);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("findQuestionsForTaking - Trả về danh sách rỗng khi examId null")
    void testFindQuestionsForTaking_NullId() {
        List<QuestionDto> list = questionReader.findQuestionsForTaking(null);

        assertNotNull(list);
        assertTrue(list.isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }

    // ── findQuestionsForTaking with data ──

    @Test
    @DisplayName("findQuestionsForTaking - có dữ liệu → trả về question với options")
    void testFindQuestionsForTaking_withData_returnsQuestionsWithOptions() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Câu hỏi 1").category("Từ vựng").skillGroup("VOCAB_GRAMMAR")
                .options(List.of(OptionDto.builder().id(10L).label("A").text("Đáp án A").correct(null).build()))
                .build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(8L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(8L);

        assertEquals(1, result.size());
        assertEquals("Câu hỏi 1", result.get(0).getContent());
        assertEquals(1, result.get(0).getOptions().size());
        assertNull(result.get(0).getOptions().get(0).getCorrect());
    }

    @Test
    @DisplayName("findQuestionsForEvaluation - có dữ liệu → trả về question với correct flag và explanation")
    void testFindQuestionsForEvaluation_withData_returnsQuestionsWithCorrectFlag() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Câu hỏi 1").explanation("Giải thích")
                .options(List.of(OptionDto.builder().id(10L).label("A").text("Đáp án A").correct(true).build()))
                .build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(8L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForEvaluation(8L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getOptions().get(0).getCorrect());
        assertEquals("Giải thích", result.get(0).getExplanation());
    }

    // ── resolveSkillGroup (tested indirectly via returned DTO skillGroup) ──

    @Test
    @DisplayName("resolveSkillGroup - audioUrl present → LISTENING")
    void testResolveSkillGroup_audioUrl_returnsListening() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").audioUrl("http://audio.mp3").skillGroup("LISTENING").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("LISTENING", result.get(0).getSkillGroup());
    }

    @Test
    @DisplayName("resolveSkillGroup - category 'nghe' → LISTENING")
    void testResolveSkillGroup_categoryNghe_returnsListening() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").category("Bài nghe").skillGroup("LISTENING").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("LISTENING", result.get(0).getSkillGroup());
    }

    @Test
    @DisplayName("resolveSkillGroup - category 'đọc' → READING")
    void testResolveSkillGroup_categoryDoc_returnsReading() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").category("Đọc hiểu trung văn").skillGroup("READING").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("READING", result.get(0).getSkillGroup());
    }

    @Test
    @DisplayName("resolveSkillGroup - category 'từ vựng' → VOCAB_GRAMMAR")
    void testResolveSkillGroup_categoryTuVung_returnsVocabGrammar() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").category("Từ vựng N4").skillGroup("VOCAB_GRAMMAR").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("VOCAB_GRAMMAR", result.get(0).getSkillGroup());
    }

    @Test
    @DisplayName("resolveSkillGroup - section fallback READING")
    void testResolveSkillGroup_sectionFallbackReading() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").section("READING").skillGroup("READING").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("READING", result.get(0).getSkillGroup());
    }

    @Test
    @DisplayName("resolveSkillGroup - section fallback LISTENING")
    void testResolveSkillGroup_sectionFallbackListening() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").section("LISTENING").skillGroup("LISTENING").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("LISTENING", result.get(0).getSkillGroup());
    }

    @Test
    @DisplayName("resolveSkillGroup - no metadata → VOCAB_GRAMMAR")
    void testResolveSkillGroup_noMetadata_returnsVocabGrammar() {
        QuestionDto q = QuestionDto.builder()
                .id(1L).content("Q").skillGroup("VOCAB_GRAMMAR").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);
        assertEquals("VOCAB_GRAMMAR", result.get(0).getSkillGroup());
    }

    // ── fallback SQL ──

    @Test
    @DisplayName("Primary SQL fails → fallback SQL used")
    void testPrimarySqlFails_fallbackSqlUsed() {
        QuestionDto q = QuestionDto.builder().id(1L).content("Fallback Q").build();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenThrow(new DataAccessResourceFailureException("exam_questions table not found"))
                .thenReturn(List.of(q));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);

        assertEquals(1, result.size());
        assertEquals("Fallback Q", result.get(0).getContent());
        verify(jdbcTemplate, times(2)).query(anyString(), any(ResultSetExtractor.class), eq(1L));
    }

    @Test
    @DisplayName("Both SQL fail → empty list")
    void testBothSqlFail_returnsEmptyList() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(1L)))
                .thenThrow(new DataAccessResourceFailureException("first fail"))
                .thenThrow(new DataAccessResourceFailureException("second fail"));

        List<QuestionDto> result = questionReader.findQuestionsForTaking(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── countQuestionsByExamId fallback ──

    @Test
    @DisplayName("countQuestionsByExamId - primary fails → fallback SQL returns count")
    void testCountQuestionsByExamId_primaryFails_fallbackReturns() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L)))
                .thenThrow(new DataAccessResourceFailureException("table missing"))
                .thenReturn(3L);

        long count = questionReader.countQuestionsByExamId(1L);

        assertEquals(3L, count);
    }

    @Test
    @DisplayName("countQuestionsByExamId - both fail → returns 0")
    void testCountQuestionsByExamId_bothFail_returnsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L)))
                .thenThrow(new DataAccessResourceFailureException("fail"));

        long count = questionReader.countQuestionsByExamId(1L);

        assertEquals(0L, count);
    }

    @Test
    @DisplayName("countQuestionsByExamId - returns null → 0")
    void testCountQuestionsByExamId_nullResult_returnsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L)))
                .thenReturn(null);

        assertEquals(0L, questionReader.countQuestionsByExamId(1L));
    }

    @Test
    @DisplayName("findQuestionsForEvaluation - null examId → empty list")
    void testFindQuestionsForEvaluation_NullId() {
        List<QuestionDto> list = questionReader.findQuestionsForEvaluation(null);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("findQuestionsForEvaluation - empty result → empty list")
    void testFindQuestionsForEvaluation_Empty() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(8L)))
                .thenReturn(Collections.emptyList());

        List<QuestionDto> list = questionReader.findQuestionsForEvaluation(8L);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("findQuestionsForTaking - đọc options trực tiếp từ questions.options và ẩn correct")
    void testFindQuestionsForTaking_parsesQuestionsOptionsJsonWithoutLeakingAnswers() throws Exception {
        ResultSet rs = mockSingleQuestionResultSet(
                67L,
                "Từ vựng",
                "問題？",
                "[{\"key\":\"A\",\"text\":\"不便\"},{\"key\":\"B\",\"text\":\"便利\"}]",
                "[\"B\"]"
        );
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(48L))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<List<QuestionDto>> extractor = inv.getArgument(1);
            return extractor.extractData(rs);
        });

        List<QuestionDto> result = questionReader.findQuestionsForTaking(48L);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getOptions().size());
        assertEquals(6701L, result.get(0).getOptions().get(0).getId());
        assertEquals("不便", result.get(0).getOptions().get(0).getText());
        assertNull(result.get(0).getOptions().get(0).getCorrect());
        assertNull(result.get(0).getOptions().get(1).getCorrect());
    }

    @Test
    @DisplayName("findQuestionsForEvaluation - answer_key B đánh dấu đúng B; rỗng không mặc định A")
    void testFindQuestionsForEvaluation_parsesAnswerKeyWithoutDefaultA() throws Exception {
        ResultSet rsValid = mockSingleQuestionResultSet(
                68L,
                "Ngữ pháp",
                "文法？",
                "[{\"key\":\"A\",\"text\":\"を\"},{\"key\":\"B\",\"text\":\"に\"}]",
                "[\"B\"]"
        );
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(48L))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<List<QuestionDto>> extractor = inv.getArgument(1);
            return extractor.extractData(rsValid);
        });

        List<QuestionDto> eval = questionReader.findQuestionsForEvaluation(48L);
        assertFalse(eval.get(0).getOptions().get(0).getCorrect());
        assertTrue(eval.get(0).getOptions().get(1).getCorrect());

        ResultSet rsNoKey = mockSingleQuestionResultSet(
                69L,
                "Ngữ pháp",
                "文法2？",
                "[{\"key\":\"A\",\"text\":\"を\"},{\"key\":\"B\",\"text\":\"に\"}]",
                "[]"
        );
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(49L))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<List<QuestionDto>> extractor = inv.getArgument(1);
            return extractor.extractData(rsNoKey);
        });

        List<QuestionDto> noKeyEval = questionReader.findQuestionsForEvaluation(49L);
        assertFalse(noKeyEval.get(0).getOptions().get(0).getCorrect());
        assertFalse(noKeyEval.get(0).getOptions().get(1).getCorrect());
    }

    private ResultSet mockSingleQuestionResultSet(long qId, String category, String content,
                                                  String optionsJson, String answerKeyJson) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("q_id")).thenReturn(qId);
        when(rs.getString("category")).thenReturn(category);
        when(rs.getString("audio_url")).thenReturn(null);
        when(rs.getString("section")).thenReturn("LANGUAGE");
        when(rs.getString("options_json")).thenReturn(optionsJson);
        lenient().when(rs.getString("answer_key_json")).thenReturn(answerKeyJson);
        when(rs.getObject("points")).thenReturn(2);
        when(rs.getInt("points")).thenReturn(2);
        when(rs.getString("content")).thenReturn(content);
        lenient().when(rs.getString("explanation")).thenReturn("Giải thích");
        when(rs.getObject("order_index")).thenReturn(1);
        when(rs.getInt("order_index")).thenReturn(1);
        return rs;
    }
}
