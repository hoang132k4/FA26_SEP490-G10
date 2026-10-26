package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.reader.impl.JdbcLearnerQuestionReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

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
        verify(jdbcTemplate).queryForObject(contains("SELECT COUNT(*) FROM questions WHERE exam_id = ?"), eq(Long.class), eq(8L));
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
}
