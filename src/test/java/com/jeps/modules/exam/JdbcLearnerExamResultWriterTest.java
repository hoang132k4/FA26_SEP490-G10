package com.jeps.modules.exam;

import com.jeps.modules.exam.dto.QuestionDto;
import com.jeps.modules.exam.writer.impl.JdbcLearnerExamResultWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class JdbcLearnerExamResultWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcLearnerExamResultWriter writer;

    @Captor
    private ArgumentCaptor<List<Object[]>> batchCaptor;

    // ── saveExamResultSummary ──

    @Test
    @DisplayName("saveExamResultSummary - null userId → returns null")
    void saveExamResultSummary_nullUserId_returnsNull() {
        Long result = writer.saveExamResultSummary(null, 10L, 100, BigDecimal.valueOf(80), true, "PASSED", 300);
        assertNull(result);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("saveExamResultSummary - null examId → returns null")
    void saveExamResultSummary_nullExamId_returnsNull() {
        Long result = writer.saveExamResultSummary(5L, null, 100, BigDecimal.valueOf(80), true, "PASSED", 300);
        assertNull(result);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("saveExamResultSummary - success → returns resultId")
    void saveExamResultSummary_success_returnsResultId() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(42L);

        Long result = writer.saveExamResultSummary(5L, 10L, 120, BigDecimal.valueOf(66.7), true, "PASSED", 3600);

        assertEquals(42L, result);
        verify(jdbcTemplate).queryForObject(contains("INSERT INTO exam_results"), eq(Long.class),
                eq(5L), eq(10L), any(), any(), eq(120), eq(BigDecimal.valueOf(66.7)), eq(true), eq("PASSED"));
    }

    @Test
    @DisplayName("saveExamResultSummary - DB error → throws RuntimeException")
    void saveExamResultSummary_dbError_throws() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> writer.saveExamResultSummary(5L, 10L, 100, BigDecimal.TEN, true, "PASSED", 60));
    }

    @Test
    @DisplayName("saveExamResultSummary - null score → treated as 0")
    void saveExamResultSummary_nullScore_treatedAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        writer.saveExamResultSummary(5L, 10L, null, BigDecimal.TEN, false, "FAILED", 60);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class),
                eq(5L), eq(10L), any(), any(), eq(0), any(), eq(false), eq("FAILED"));
    }

    @Test
    @DisplayName("saveExamResultSummary - null percentage → treated as ZERO")
    void saveExamResultSummary_nullPercentage_treatedAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        writer.saveExamResultSummary(5L, 10L, 0, null, false, "FAILED", 60);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class),
                eq(5L), eq(10L), any(), any(), eq(0), eq(BigDecimal.ZERO), eq(false), eq("FAILED"));
    }

    @Test
    @DisplayName("saveExamResultSummary - null timeSpent → treated as 0 (start==end)")
    void saveExamResultSummary_nullTimeSpent_treatedAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        Long result = writer.saveExamResultSummary(5L, 10L, 100, BigDecimal.TEN, true, "PASSED", null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("saveExamResultSummary - negative timeSpent → treated as 0")
    void saveExamResultSummary_negativeTimeSpent_treatedAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        Long result = writer.saveExamResultSummary(5L, 10L, 100, BigDecimal.TEN, true, "PASSED", -10);

        assertNotNull(result);
    }

    @Test
    @DisplayName("saveExamResultSummary - null resultStatus → defaults to COMPLETED")
    void saveExamResultSummary_nullResultStatus_defaultsToCompleted() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        writer.saveExamResultSummary(5L, 10L, 0, BigDecimal.ZERO, false, null, 0);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class),
                eq(5L), eq(10L), any(), any(), eq(0), eq(BigDecimal.ZERO), eq(false), eq("COMPLETED"));
    }

    // ── saveExamResultDetails ──

    @Test
    @DisplayName("saveExamResultDetails - null resultId → no-op")
    void saveExamResultDetails_nullResultId_noop() {
        writer.saveExamResultDetails(null, List.of(QuestionDto.builder().id(1L).build()));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("saveExamResultDetails - null questions → no-op")
    void saveExamResultDetails_nullQuestions_noop() {
        writer.saveExamResultDetails(1L, null);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("saveExamResultDetails - empty questions → no-op")
    void saveExamResultDetails_emptyQuestions_noop() {
        writer.saveExamResultDetails(1L, Collections.emptyList());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("saveExamResultDetails - valid questions → batch insert")
    void saveExamResultDetails_success_batchInsert() {
        QuestionDto q1 = QuestionDto.builder().id(10L).userAnswerId(50L).isUserCorrect(true).points(2).build();
        QuestionDto q2 = QuestionDto.builder().id(11L).userAnswerId(null).isUserCorrect(false).points(2).build();

        when(jdbcTemplate.batchUpdate(anyString(), any(List.class))).thenReturn(new int[]{1, 1});

        writer.saveExamResultDetails(1L, List.of(q1, q2));

        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO exam_result_details"), batchCaptor.capture());
        List<Object[]> params = batchCaptor.getValue();
        assertEquals(2, params.size());

        assertEquals(1L, params.get(0)[0]);  // resultId
        assertEquals(10L, params.get(0)[1]); // questionId
        assertEquals("50", params.get(0)[2]); // selectedOption
        assertEquals(true, params.get(0)[3]); // isCorrect
        assertEquals(2, params.get(0)[4]);    // earnedScore

        assertEquals(1L, params.get(1)[0]);
        assertEquals(11L, params.get(1)[1]);
        assertNull(params.get(1)[2]); // null userAnswerId → null
        assertEquals(false, params.get(1)[3]);
        assertEquals(0, params.get(1)[4]); // incorrect → 0
    }

    @Test
    @DisplayName("saveExamResultDetails - null question in list → skipped")
    void saveExamResultDetails_nullQuestionInList_skipped() {
        List<QuestionDto> questions = new ArrayList<>();
        questions.add(null);
        questions.add(QuestionDto.builder().id(10L).userAnswerId(50L).isUserCorrect(true).points(2).build());

        when(jdbcTemplate.batchUpdate(anyString(), any(List.class))).thenReturn(new int[]{1});

        writer.saveExamResultDetails(1L, questions);

        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());
        assertEquals(1, batchCaptor.getValue().size());
    }

    @Test
    @DisplayName("saveExamResultDetails - question with null id → skipped")
    void saveExamResultDetails_nullQuestionId_skipped() {
        QuestionDto noId = QuestionDto.builder().id(null).build();
        QuestionDto valid = QuestionDto.builder().id(10L).userAnswerId(50L).isUserCorrect(true).points(2).build();

        when(jdbcTemplate.batchUpdate(anyString(), any(List.class))).thenReturn(new int[]{1});

        writer.saveExamResultDetails(1L, List.of(noId, valid));

        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());
        assertEquals(1, batchCaptor.getValue().size());
    }

    @Test
    @DisplayName("saveExamResultDetails - DB error → throws RuntimeException")
    void saveExamResultDetails_dbError_throws() {
        QuestionDto q = QuestionDto.builder().id(10L).userAnswerId(50L).isUserCorrect(true).points(2).build();

        when(jdbcTemplate.batchUpdate(anyString(), any(List.class)))
                .thenThrow(new DataAccessResourceFailureException("Batch failed"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> writer.saveExamResultDetails(1L, List.of(q)));
    }

    @Test
    @DisplayName("saveExamResultDetails - all null/invalid questions → no batch (no-op)")
    void saveExamResultDetails_allNullQuestions_noop() {
        List<QuestionDto> questions = new ArrayList<>();
        questions.add(null);
        questions.add(QuestionDto.builder().id(null).build());

        writer.saveExamResultDetails(1L, questions);

        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(List.class));
    }

    @Test
    @DisplayName("saveExamResultDetails - null points on correct answer → earned = 0")
    void saveExamResultDetails_nullPoints_earnedZero() {
        QuestionDto q = QuestionDto.builder().id(10L).userAnswerId(50L).isUserCorrect(true).points(null).build();

        when(jdbcTemplate.batchUpdate(anyString(), any(List.class))).thenReturn(new int[]{1});

        writer.saveExamResultDetails(1L, List.of(q));

        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());
        assertEquals(0, batchCaptor.getValue().get(0)[4]);
    }
}
