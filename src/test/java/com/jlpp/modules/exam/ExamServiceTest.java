package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.*;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.reader.LearnerExamResultReader;
import com.jlpp.modules.exam.reader.LearnerQuestionReader;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import com.jlpp.modules.exam.service.impl.AssessmentScoringServiceImpl;
import com.jlpp.modules.exam.service.impl.ExamServiceImpl;
import com.jlpp.modules.exam.writer.LearnerExamResultWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private LearnerQuestionReader questionReader;

    @Mock
    private LearnerExamResultWriter resultWriter;

    @Mock
    private LearnerExamResultReader resultReader;

    @Spy
    private AssessmentScoringService scoringService = new AssessmentScoringServiceImpl();

    @InjectMocks
    private ExamServiceImpl examService;

    private Exam mockExamN4;
    private Exam mockExamN3;
    private Exam mockPlacementExam;
    private Exam mockMilestoneExam;
    private List<QuestionDto> takingQuestions;
    private List<QuestionDto> evaluationQuestions;

    @BeforeEach
    void setUp() {
        mockExamN4 = new Exam(8L, "JLPT N4 Mock Exam", "JLPT_MOCK", "N4", 50, 0, 35, "ACTIVE", "N4 Description");
        mockExamN3 = new Exam(9L, "JLPT N3 Mock Exam", "JLPT_MOCK", "N3", 70, 0, 35, "ACTIVE", "N3 Description");
        mockPlacementExam = new Exam(7L, "Placement Assessment", "PLACEMENT_TEST", null, 30, 4, 12, "ACTIVE", "Đánh giá năng lực đầu vào");
        mockMilestoneExam = new Exam(10L, "Milestone N4 Course 2", "MILESTONE", "N4", 60, 2, 50, "ACTIVE", "Kiểm tra cột mốc", 2L);

        // Question 1: Vocab
        QuestionDto q1Taking = QuestionDto.builder()
                .id(101L)
                .category("Từ vựng (Goi)")
                .skillGroup("VOCAB_GRAMMAR")
                .skillGroupLabel("Kiến thức ngôn ngữ (Từ vựng • Ngữ pháp • Kanji)")
                .content("映画を____。")
                .points(15)
                .orderIndex(1)
                .explanation(null)
                .options(new ArrayList<>(List.of(
                        OptionDto.builder().id(1001L).label("1.").text("見ました").correct(null).build(),
                        OptionDto.builder().id(1002L).label("2.").text("聞きました").correct(null).build()
                )))
                .build();

        // Question 2: Reading
        QuestionDto q2Taking = QuestionDto.builder()
                .id(102L)
                .category("Đọc hiểu (Dokkai)")
                .skillGroup("READING")
                .skillGroupLabel("Đọc hiểu (Reading)")
                .content("Đoạn văn đọc hiểu ngắn...")
                .points(15)
                .orderIndex(2)
                .explanation(null)
                .options(new ArrayList<>(List.of(
                        OptionDto.builder().id(1003L).label("1.").text("Đáp án A").correct(null).build(),
                        OptionDto.builder().id(1004L).label("2.").text("Đáp án B").correct(null).build()
                )))
                .build();

        // Question 3: Listening with Audio
        QuestionDto q3Taking = QuestionDto.builder()
                .id(103L)
                .category("Nghe hiểu (Choukai)")
                .skillGroup("LISTENING")
                .skillGroupLabel("Nghe hiểu (Listening)")
                .content("Nghe hội thoại và chọn giờ")
                .audioUrl("https://example.com/audio.mp3")
                .points(15)
                .orderIndex(3)
                .explanation(null)
                .options(new ArrayList<>(List.of(
                        OptionDto.builder().id(1005L).label("1.").text("3 giờ").correct(null).build(),
                        OptionDto.builder().id(1006L).label("2.").text("4 giờ").correct(null).build()
                )))
                .build();

        takingQuestions = List.of(q1Taking, q2Taking, q3Taking);

        // Evaluation Questions (contain correct flag & explanations)
        QuestionDto q1Eval = QuestionDto.builder()
                .id(101L)
                .category("Từ vựng (Goi)")
                .skillGroup("VOCAB_GRAMMAR")
                .skillGroupLabel("Kiến thức ngôn ngữ (Từ vựng • Ngữ pháp • Kanji)")
                .content("映画を____。")
                .points(15)
                .orderIndex(1)
                .explanation("Giải thích từ vựng")
                .options(new ArrayList<>(List.of(
                        OptionDto.builder().id(1001L).label("1.").text("見ました").correct(true).build(),
                        OptionDto.builder().id(1002L).label("2.").text("聞きました").correct(false).build()
                )))
                .build();

        QuestionDto q2Eval = QuestionDto.builder()
                .id(102L)
                .category("Đọc hiểu (Dokkai)")
                .skillGroup("READING")
                .skillGroupLabel("Đọc hiểu (Reading)")
                .content("Đoạn văn đọc hiểu ngắn...")
                .points(15)
                .orderIndex(2)
                .explanation("Giải thích đọc hiểu")
                .options(new ArrayList<>(List.of(
                        OptionDto.builder().id(1003L).label("1.").text("Đáp án A").correct(false).build(),
                        OptionDto.builder().id(1004L).label("2.").text("Đáp án B").correct(true).build()
                )))
                .build();

        QuestionDto q3Eval = QuestionDto.builder()
                .id(103L)
                .category("Nghe hiểu (Choukai)")
                .skillGroup("LISTENING")
                .skillGroupLabel("Nghe hiểu (Listening)")
                .content("Nghe hội thoại và chọn giờ")
                .audioUrl("https://example.com/audio.mp3")
                .points(15)
                .orderIndex(3)
                .explanation("Giải thích nghe hiểu")
                .options(new ArrayList<>(List.of(
                        OptionDto.builder().id(1005L).label("1.").text("3 giờ").correct(true).build(),
                        OptionDto.builder().id(1006L).label("2.").text("4 giờ").correct(false).build()
                )))
                .build();

        evaluationQuestions = List.of(q1Eval, q2Eval, q3Eval);
    }

    @Test
    @DisplayName("getLearnerExams(ALL) - Lấy danh sách đề thi kèm số câu hỏi thực tế từ questionReader")
    void testGetLearnerExams_All() {
        when(examRepository.findByExamTypeIgnoreCase("JLPT_MOCK")).thenReturn(List.of(mockExamN4, mockExamN3));
        when(questionReader.countQuestionsByExamId(8L)).thenReturn(3L);
        when(questionReader.countQuestionsByExamId(9L)).thenReturn(0L);

        List<ExamCardDto> cards = examService.getLearnerExams("ALL");

        assertEquals(2, cards.size());
        assertEquals("N4", cards.get(0).getJlptLevel());
        assertEquals(3, cards.get(0).getTotalQuestions());
        assertTrue(cards.get(0).isReady());

        assertEquals("N3", cards.get(1).getJlptLevel());
        assertEquals(0, cards.get(1).getTotalQuestions());
        assertFalse(cards.get(1).isReady());
    }

    @Test
    @DisplayName("getLearnerExams(N2) - Cấp độ không có dữ liệu trả về danh sách rỗng")
    void testGetLearnerExams_LevelWithNoData() {
        when(examRepository.findByExamTypeIgnoreCaseAndJlptLevelIgnoreCase("JLPT_MOCK", "N2"))
                .thenReturn(Collections.emptyList());

        List<ExamCardDto> cards = examService.getLearnerExams("N2");

        assertTrue(cards.isEmpty());
    }

    @Test
    @DisplayName("getExamForTaking - Đề thi hợp lệ phân chia chuẩn 3 nhóm kỹ năng và bảo mật đáp án")
    void testGetExamForTaking_Success() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForTaking(8L)).thenReturn(takingQuestions);

        ExamTakeResponse response = examService.getExamForTaking(8L);

        assertEquals(8L, response.getExamId());
        assertEquals(3, response.getTotalQuestions());
        assertEquals(180, response.getTotalPoints());

        // Verify 3 skill groups are separated
        assertEquals(1, response.getVocabGrammarQuestions().size());
        assertEquals(1, response.getReadingQuestions().size());
        assertEquals(1, response.getListeningQuestions().size());

        // Verify Audio is preserved for Listening
        assertEquals("https://example.com/audio.mp3", response.getListeningQuestions().get(0).getAudioUrl());

        // Security check: options MUST NOT leak correctness
        assertNull(response.getQuestions().get(0).getOptions().get(0).getCorrect());
        // Security check: explanations MUST NOT be sent before submission
        assertNull(response.getQuestions().get(0).getExplanation());
    }

    @Test
    @DisplayName("getExamForTaking - Đề thi không tồn tại ném NOT_FOUND")
    void testGetExamForTaking_NotFound() {
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> examService.getExamForTaking(999L));
    }

    @Test
    @DisplayName("getExamForTaking - Đề thi chưa có câu hỏi trả về totalQuestions = 0")
    void testGetExamForTaking_EmptyQuestions() {
        when(examRepository.findById(9L)).thenReturn(Optional.of(mockExamN3));
        when(questionReader.findQuestionsForTaking(9L)).thenReturn(Collections.emptyList());

        ExamTakeResponse response = examService.getExamForTaking(9L);

        assertEquals(0, response.getTotalQuestions());
        assertTrue(response.getQuestions().isEmpty());
    }

    @Test
    @DisplayName("evaluateSubmission - Chấm điểm chính xác, tính điểm 3 kỹ năng và tỷ lệ %")
    void testEvaluateSubmission_FullScore() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);

        Map<String, String> answers = Map.of(
                "answer_101", "1001", // correct (15 pts)
                "answer_102", "1004", // correct (15 pts)
                "answer_103", "1005"  // correct (15 pts)
        );

        ExamResultResponse result = examService.evaluateSubmission(8L, answers, 120);

        assertEquals(180, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertEquals(new BigDecimal("100.0"), result.getPercentage());
        assertTrue(result.getPassed());
        assertEquals(3, result.getCorrectCount());
        assertEquals(0, result.getIncorrectCount());
        assertEquals(0, result.getUnansweredCount());
        assertEquals("02:00", result.getFormattedTimeSpent());
        assertEquals(2, result.getSkillBreakdown().size()); // N4 language + reading combined

        // Review mode check: correct options and explanations MUST be exposed now
        assertTrue(result.getQuestions().get(0).getOptions().get(0).getCorrect());
        assertEquals("Giải thích từ vựng", result.getQuestions().get(0).getExplanation());
    }

    @Test
    @DisplayName("evaluateSubmission - Trả lời sai một phần và bỏ trống câu hỏi")
    void testEvaluateSubmission_PartialScore() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);

        Map<String, String> answers = Map.of(
                "answer_101", "1001", // correct (15 pts)
                "answer_102", "1003"  // wrong (0 pts, chose option 1003 instead of 1004)
                // Q3 left unanswered
        );

        ExamResultResponse result = examService.evaluateSubmission(8L, answers, 75);

        assertEquals(60, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertEquals(new BigDecimal("33.3"), result.getPercentage());
        assertFalse(result.getPassed()); // passing score is 35, 15 < 35 -> false
        assertEquals(1, result.getCorrectCount());
        assertEquals(1, result.getIncorrectCount());
        assertEquals(1, result.getUnansweredCount());
        assertEquals("01:15", result.getFormattedTimeSpent());
    }

    // --- SCREEN 1: PLACEMENT TEST TESTS ---

    @Test
    @DisplayName("getPlacementExamForTaking - Thành công lấy đề đánh giá đầu vào đang ACTIVE")
    void testGetPlacementExamForTaking_Success() {
        when(examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST")).thenReturn(List.of(mockPlacementExam));
        when(examRepository.findById(7L)).thenReturn(Optional.of(mockPlacementExam));
        when(questionReader.countQuestionsByExamId(7L)).thenReturn(3L);
        when(questionReader.findQuestionsForTaking(7L)).thenReturn(takingQuestions);

        ExamTakeResponse response = examService.getPlacementExamForTaking();

        assertNotNull(response);
        assertEquals(7L, response.getExamId());
        assertEquals("PLACEMENT_TEST", response.getExamType());
        assertEquals(3, response.getTotalQuestions());
    }

    @Test
    @DisplayName("getPlacementExamForTaking - Chưa có đề trong DB ném NOT_FOUND 404")
    void testGetPlacementExamForTaking_NotFound() {
        when(examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST")).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getPlacementExamForTaking());
        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Hiện chưa có bài đánh giá đầu vào"));
    }

    @Test
    @DisplayName("getPlacementExamForTaking - Có đề nhưng chưa có câu hỏi ném BAD_REQUEST 400")
    void testGetPlacementExamForTaking_ZeroQuestions() {
        when(examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST")).thenReturn(List.of(mockPlacementExam));
        when(questionReader.countQuestionsByExamId(7L)).thenReturn(0L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getPlacementExamForTaking());
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("chưa có câu hỏi"));
    }

    // --- SCREEN 3: MILESTONE EXAM TESTS ---

    @Test
    @DisplayName("getMilestoneExams - Lấy danh sách đề kiểm tra cột mốc theo courseId")
    void testGetMilestoneExams_ByCourse() {
        when(examRepository.findByCourseIdAndExamTypeIgnoreCase(2L, "MILESTONE"))
                .thenReturn(List.of(mockMilestoneExam));
        when(questionReader.countQuestionsByExamId(10L)).thenReturn(2L);

        List<ExamCardDto> list = examService.getMilestoneExams(2L);

        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getExamId());
        assertEquals(2, list.get(0).getTotalQuestions());
        assertTrue(list.get(0).isReady());
    }

    @Test
    @DisplayName("getMilestoneExamForTaking - Đề thi hợp lệ vào phòng thi bình thường")
    void testGetMilestoneExamForTaking_Success() {
        when(examRepository.findById(10L)).thenReturn(Optional.of(mockMilestoneExam));
        when(questionReader.countQuestionsByExamId(10L)).thenReturn(3L);
        when(questionReader.findQuestionsForTaking(10L)).thenReturn(takingQuestions);

        ExamTakeResponse response = examService.getMilestoneExamForTaking(10L);

        assertNotNull(response);
        assertEquals(10L, response.getExamId());
        assertEquals("MILESTONE", response.getExamType());
    }

    @Test
    @DisplayName("getMilestoneExamForTaking - Sai loại đề thi ném 400")
    void testGetMilestoneExamForTaking_WrongType() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4)); // JLPT_MOCK

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getMilestoneExamForTaking(8L));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("không phải là bài kiểm tra cột mốc"));
    }

    // --- SCREEN 2 & PERSISTENCE TESTS ---

    @Test
    @DisplayName("evaluateAndSaveSubmission - Học viên nộp bài thành công, ghi nhận master và detail")
    void testEvaluateAndSaveSubmission_WithUser_PersistsMasterAndDetails() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);
        when(resultWriter.saveExamResultSummary(eq(1L), eq(8L), anyInt(), any(), anyBoolean(), any(), anyInt()))
                .thenReturn(55L);

        Map<String, String> answers = Map.of("answer_101", "1001", "answer_102", "1004", "answer_103", "1005");
        ExamResultResponse response = examService.evaluateAndSaveSubmission(8L, 1L, answers, 100);

        assertTrue(response.isPersistentSaved());
        assertEquals(55L, response.getResultId());
        assertNotNull(response.getCompletionDate());
        verify(resultWriter, times(1)).saveExamResultSummary(eq(1L), eq(8L), eq(180), eq(new BigDecimal("100.0")), eq(true), eq("PASSED"), eq(100));
        verify(resultWriter, times(1)).saveExamResultDetails(eq(55L), anyList());
    }

    @Test
    @DisplayName("evaluateAndSaveSubmission - Lỗi lưu master (saveExamResultSummary) hủy bỏ và không lưu detail")
    void testEvaluateAndSaveSubmission_MasterError_HandlesFailure() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);
        when(resultWriter.saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenThrow(new RuntimeException("DB Connection timeout"));

        Map<String, String> answers = Map.of("answer_101", "1001");
        ExamResultResponse response = examService.evaluateAndSaveSubmission(8L, 2L, answers, 100);

        assertFalse(response.isPersistentSaved());
        assertNull(response.getResultId());
        assertFalse(response.isDetailsAvailable());
        assertTrue(response.getStatusNotice().contains("Chưa thể ghi nhận kết quả dài hạn"));
        verify(resultWriter, never()).saveExamResultDetails(any(), anyList());
    }

    @Test
    @DisplayName("evaluateAndSaveSubmission - Lỗi lưu detail (saveExamResultDetails) rollback master và đặt persistentSaved = false")
    void testEvaluateAndSaveSubmission_DetailError_RollsBackMaster() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);
        when(resultWriter.saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(77L);
        doThrow(new RuntimeException("Foreign key violation on detail batch"))
                .when(resultWriter).saveExamResultDetails(eq(77L), anyList());

        Map<String, String> answers = Map.of("answer_101", "1001");
        ExamResultResponse response = examService.evaluateAndSaveSubmission(8L, 3L, answers, 100);

        assertFalse(response.isPersistentSaved());
        assertNull(response.getResultId());
        assertFalse(response.isDetailsAvailable());
        assertTrue(response.getStatusNotice().contains("Chưa thể ghi nhận kết quả dài hạn"));
        verify(resultWriter, times(1)).saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any());
        verify(resultWriter, times(1)).saveExamResultDetails(eq(77L), anyList());
    }

    @Test
    @DisplayName("evaluateAndSaveSubmission - Double-submit: bấm nộp bài trùng trong 5s không tạo 2 kết quả")
    void testEvaluateAndSaveSubmission_DoubleSubmit_Deduplicated() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);
        when(resultWriter.saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(88L);

        Map<String, String> answers = Map.of("answer_101", "1001", "answer_102", "1004");

        // Submit 1st time
        ExamResultResponse resp1 = examService.evaluateAndSaveSubmission(8L, 10L, answers, 60);
        assertTrue(resp1.isPersistentSaved());
        assertEquals(88L, resp1.getResultId());

        // Submit 2nd time immediately (within 5 seconds with same answers)
        ExamResultResponse resp2 = examService.evaluateAndSaveSubmission(8L, 10L, answers, 60);

        // Verification: Exactly the same response returned, writer called only once
        assertSame(resp1, resp2);
        verify(resultWriter, times(1)).saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any());
        verify(resultWriter, times(1)).saveExamResultDetails(eq(88L), anyList());
    }

    @Test
    @DisplayName("evaluateAndSaveSubmission - Khách làm bài (userId = null) không ghi vào database")
    void testEvaluateAndSaveSubmission_Guest_DoesNotPersist() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);

        Map<String, String> answers = Map.of("answer_101", "1001");
        ExamResultResponse response = examService.evaluateAndSaveSubmission(8L, null, answers, 50);

        assertFalse(response.isPersistentSaved());
        assertNull(response.getResultId());
        verify(resultWriter, never()).saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    // --- SCREEN 4: HISTORY LIST TESTS ---

    @Test
    @DisplayName("getLearnerExamHistory - Lấy danh sách lịch sử thi của learner từ Supabase")
    void testGetLearnerExamHistory_Success() {
        ExamHistoryDto historyItem = ExamHistoryDto.builder()
                .resultId(12L)
                .examId(8L)
                .examTitle("JLPT N4 Mock Exam")
                .examType("JLPT_MOCK")
                .jlptLevel("N4")
                .totalScore(45)
                .passedStatus(true)
                .attemptNumber(1)
                .formattedDate("26/10/2026 14:00")
                .build();

        when(resultReader.findExamHistoryByUserId(1L)).thenReturn(List.of(historyItem));

        List<ExamHistoryDto> history = examService.getLearnerExamHistory(1L);

        assertEquals(1, history.size());
        assertEquals(12L, history.get(0).getResultId());
        assertEquals("JLPT_MOCK", history.get(0).getExamType());
        verify(resultReader, times(1)).findExamHistoryByUserId(1L);
    }

    // --- SCREEN 5: HISTORY DETAIL TESTS ---

    @Test
    @DisplayName("getHistoricalResult - Học viên xem kết quả của chính mình thành công")
    void testGetHistoricalResult_Success() {
        ExamResultResponse mockDetail = ExamResultResponse.builder()
                .resultId(12L)
                .examId(8L)
                .examTitle("JLPT N4 Mock Exam")
                .score(45)
                .passed(true)
                .persistentSaved(true)
                .build();

        when(resultReader.findExamResultByIdAndUserId(12L, 1L)).thenReturn(Optional.of(mockDetail));

        ExamResultResponse response = examService.getHistoricalResult(12L, 1L);

        assertNotNull(response);
        assertEquals(12L, response.getResultId());
        assertEquals(45, response.getScore());
    }

    @Test
    @DisplayName("getHistoricalResult - Truy cập kết quả của người khác bị chặn 403 Forbidden")
    void testGetHistoricalResult_Forbidden_OtherUser() {
        // Result 12 exists in DB, but does NOT belong to user 2
        when(resultReader.findExamResultByIdAndUserId(12L, 2L)).thenReturn(Optional.empty());
        when(resultReader.existsById(12L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getHistoricalResult(12L, 2L));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("không có quyền truy cập"));
    }

    @Test
    @DisplayName("getHistoricalResult - Kết quả không tồn tại trả về 404 Not Found")
    void testGetHistoricalResult_NotFound() {
        when(resultReader.findExamResultByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());
        when(resultReader.existsById(999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getHistoricalResult(999L, 1L));

        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Không tìm thấy"));
    }

    // --- JLPT 180-POINT SCORING SPECIFICATION TESTS ---

    private List<QuestionDto> buildMockQuestions(int langCorrect, int langTotal,
                                                 int readCorrect, int readTotal,
                                                 int listenCorrect, int listenTotal) {
        List<QuestionDto> list = new ArrayList<>();
        long id = 1;
        id = appendQuestions(list, id, "VOCAB_GRAMMAR", "LANGUAGE", langCorrect, langTotal);
        id = appendQuestions(list, id, "READING", "READING", readCorrect, readTotal);
        appendQuestions(list, id, "LISTENING", "LISTENING", listenCorrect, listenTotal);
        return list;
    }

    private long appendQuestions(List<QuestionDto> list, long startId, String skillGroup, String section, int correct, int total) {
        for (int i = 0; i < total; i++) {
            long qId = startId++;
            boolean isCorrectQuestion = i < correct;
            list.add(QuestionDto.builder()
                    .id(qId)
                    .category(skillGroup)
                    .section(section)
                    .skillGroup(skillGroup)
                    .points(1)
                    .orderIndex((int) qId)
                    .explanation("Giải thích")
                    .options(List.of(
                            OptionDto.builder().id(qId * 10 + 1).label("1").text("Opt 1").correct(isCorrectQuestion).build(),
                            OptionDto.builder().id(qId * 10 + 2).label("2").text("Opt 2").correct(!isCorrectQuestion).build()
                    ))
                    .build());
        }
        return startId;
    }

    private Map<String, String> buildAllAnswers(List<QuestionDto> questions) {
        Map<String, String> map = new HashMap<>();
        for (QuestionDto q : questions) {
            // Option 1 is chosen by the learner for every question
            map.put("answer_" + q.getId(), String.valueOf(q.getId() * 10 + 1));
        }
        return map;
    }

    @Test
    @DisplayName("Test 1: N5 đỗ - Language 25/43, Reading 5/5, Listening 15/24 -> 113/180 ĐỖ")
    void testJlpt180_Test1_N5_Pass() {
        Exam examN5 = new Exam(501L, "JLPT N5 Mock Exam", "JLPT_MOCK", "N5", 100, 72, 80, "ACTIVE", "N5 Mock");
        List<QuestionDto> questions = buildMockQuestions(25, 43, 5, 5, 15, 24);
        Map<String, String> answers = buildAllAnswers(questions);

        when(examRepository.findById(501L)).thenReturn(Optional.of(examN5));
        when(questionReader.findQuestionsForEvaluation(501L)).thenReturn(questions);

        ExamResultResponse result = examService.evaluateSubmission(501L, answers, 3600);

        // Language + Reading combined (120): 30/48 -> round(30*120/48) = 75 (sàn 38)
        // Listening (60): 15/24 -> round(15*60/24) = 38 (sàn 19)
        // Total = 113 >= 80 -> ĐỖ
        assertEquals(113, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertEquals(new BigDecimal("62.8"), result.getPercentage());
        assertTrue(result.getPassed());

        assertEquals(2, result.getSkillBreakdown().size());
        assertEquals(75, result.getSkillBreakdown().get(0).getEarnedPoints());
        assertEquals(120, result.getSkillBreakdown().get(0).getTotalPoints());
        assertEquals(38, result.getSkillBreakdown().get(1).getEarnedPoints());
        assertEquals(60, result.getSkillBreakdown().get(1).getTotalPoints());
    }

    @Test
    @DisplayName("Test 2: N5 đỗ (ví dụ bàn giao) - Language+Reading 30/43, Listening 15/24 -> 122/180 ĐỖ")
    void testJlpt180_Test2_N5_Pass_HandoverExample() {
        Exam examN5 = new Exam(502L, "JLPT N5 Mock Exam", "JLPT_MOCK", "N5", 100, 67, 80, "ACTIVE", "N5 Mock");
        List<QuestionDto> questions = buildMockQuestions(30, 43, 0, 0, 15, 24);
        Map<String, String> answers = buildAllAnswers(questions);

        when(examRepository.findById(502L)).thenReturn(Optional.of(examN5));
        when(questionReader.findQuestionsForEvaluation(502L)).thenReturn(questions);

        ExamResultResponse result = examService.evaluateSubmission(502L, answers, 3600);

        // Language+Reading: 30/43 -> round(30*120/43) = 84 (sàn 38)
        // Listening: 15/24 -> round(15*60/24) = 38 (sàn 19)
        // Total = 122 >= 80 -> ĐỖ
        assertEquals(122, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertTrue(result.getPassed());
        assertEquals(84, result.getSkillBreakdown().get(0).getEarnedPoints());
        assertEquals(38, result.getSkillBreakdown().get(1).getEarnedPoints());
    }

    @Test
    @DisplayName("Test 3: N3 trượt vì điểm sàn - Language 50/58 (52), Reading 15/16 (56), Listening 8/28 (17 < 19) -> TRƯỢT")
    void testJlpt180_Test3_N3_Fail_SectionMinScore() {
        Exam examN3 = new Exam(503L, "JLPT N3 Mock Exam", "JLPT_MOCK", "N3", 140, 102, 95, "ACTIVE", "N3 Mock");
        List<QuestionDto> questions = buildMockQuestions(50, 58, 15, 16, 8, 28);
        Map<String, String> answers = buildAllAnswers(questions);

        when(examRepository.findById(503L)).thenReturn(Optional.of(examN3));
        when(questionReader.findQuestionsForEvaluation(503L)).thenReturn(questions);

        ExamResultResponse result = examService.evaluateSubmission(503L, answers, 3600);

        // Language: 50/58 -> round(50*60/58) = 52
        // Reading: 15/16 -> round(15*60/16) = 56
        // Listening: 8/28 -> round(8*60/28) = 17 (< 19)
        // Total = 125 >= 95 nhưng Nghe hiểu 17 < 19 -> TRƯỢT
        assertEquals(125, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertFalse(result.getPassed());

        // Kiểm tra thông điệp phản hồi nêu rõ lý do điểm sàn
        assertNotNull(result.getAiFeedback());
        assertTrue(result.getAiFeedback().contains("Bạn chưa đạt điểm sàn bài thi N3 lần này"));
        assertTrue(result.getAiFeedback().contains("Tổng điểm của bạn đạt 125/180 (đủ điểm đỗ 95)"));
        assertTrue(result.getAiFeedback().contains("Nghe hiểu chỉ đạt 17 điểm, chưa đạt mức tối thiểu 19 điểm"));
    }

    @Test
    @DisplayName("Test 4: N3 thiếu phần Nghe (milestone) - Language 10/10 (90), Reading 5/10 (45) -> 135/180 ĐỖ")
    void testJlpt180_Test4_N3_Milestone_MissingListening() {
        Exam milestoneN3 = new Exam(504L, "Milestone N3 Course", "MILESTONE", "N3", 90, 20, 95, "ACTIVE", "Milestone N3");
        List<QuestionDto> questions = buildMockQuestions(10, 10, 5, 10, 0, 0);
        Map<String, String> answers = buildAllAnswers(questions);

        when(examRepository.findById(504L)).thenReturn(Optional.of(milestoneN3));
        when(questionReader.findQuestionsForEvaluation(504L)).thenReturn(questions);

        ExamResultResponse result = examService.evaluateSubmission(504L, answers, 2400);

        // Present max = 60 + 60 = 120. New max each = 60*180/120 = 90.
        // Language: 10/10 -> 90. Reading: 5/10 -> 45. Sàn mỗi nhóm = ceil(90*19/60) = 29.
        // Total = 135 >= 95 -> ĐỖ
        assertEquals(135, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertTrue(result.getPassed());
        assertEquals(2, result.getSkillBreakdown().size());
        assertEquals(90, result.getSkillBreakdown().get(0).getTotalPoints());
        assertEquals(90, result.getSkillBreakdown().get(0).getEarnedPoints());
        assertEquals(90, result.getSkillBreakdown().get(1).getTotalPoints());
        assertEquals(45, result.getSkillBreakdown().get(1).getEarnedPoints());
    }

    @Test
    @DisplayName("Test 5: PLACEMENT_TEST luôn passed = true dù điểm thấp")
    void testJlpt180_Test5_PlacementTest_AlwaysPassed() {
        Exam placement = new Exam(505L, "Placement Assessment", "PLACEMENT_TEST", "N5", 60, 40, null, "ACTIVE", "Placement");
        List<QuestionDto> questions = buildMockQuestions(2, 20, 1, 10, 1, 10);
        Map<String, String> answers = buildAllAnswers(questions);

        when(examRepository.findById(505L)).thenReturn(Optional.of(placement));
        when(questionReader.findQuestionsForEvaluation(505L)).thenReturn(questions);

        ExamResultResponse result = examService.evaluateSubmission(505L, answers, 1800);

        assertTrue(result.getPassed());
        assertNull(result.getPassingScore());
    }

    @Test
    @DisplayName("Test 6: Bỏ trống hết -> 0 điểm, TRƯỢT")
    void testJlpt180_Test6_AllUnanswered_ZeroScore_Fail() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);

        ExamResultResponse result = examService.evaluateSubmission(8L, Collections.emptyMap(), 0);

        assertEquals(0, result.getScore());
        assertEquals(180, result.getTotalPossibleScore());
        assertEquals(new BigDecimal("0.0"), result.getPercentage());
        assertFalse(result.getPassed());
        assertEquals(0, result.getCorrectCount());
        assertEquals(3, result.getUnansweredCount());

        // Kiểm tra phần cần cải thiện khi tất cả các kỹ năng đều 0%
        assertNull(result.getStrongestSkill());
        assertEquals("Toàn diện các kỹ năng (Ngôn ngữ + Đọc hiểu, Nghe hiểu)", result.getWeakestSkill());
        assertTrue(result.getAiFeedback().contains("Toàn diện các kỹ năng (Ngôn ngữ + Đọc hiểu, Nghe hiểu)"));
    }

    @Test
    @DisplayName("Test 7: WeakestSkill chỉ ra chính xác kỹ năng bị điểm thấp nhất")
    void testWeakestSkill_IdentifiesTrueWeakestSkill() {
        when(examRepository.findById(8L)).thenReturn(Optional.of(mockExamN4));
        when(questionReader.findQuestionsForEvaluation(8L)).thenReturn(evaluationQuestions);

        // Học viên trả lời đúng câu 1 (Vocab: 15đ) và câu 2 (Reading: 15đ), nhưng trả lời sai câu 3 (Listening)
        Map<String, String> answers = Map.of(
                "answer_101", "1001", // correct (Vocab)
                "answer_102", "1004", // correct (Reading)
                "answer_103", "1006"  // incorrect (Listening)
        );

        ExamResultResponse result = examService.evaluateSubmission(8L, answers, 180);

        assertEquals("Ngôn ngữ + Đọc hiểu", result.getStrongestSkill());
        assertEquals("Nghe hiểu", result.getWeakestSkill());
        assertTrue(result.getRecommendation().contains("Nghe hiểu"));
    }

    @Test
    @DisplayName("Test 8: N3 phân tích 3 kỹ năng độc lập, khi sai hết báo cáo toàn diện các kỹ năng")
    void testWeakestSkill_N3_AllZero_ReportsAllSections() {
        when(examRepository.findById(9L)).thenReturn(Optional.of(mockExamN3));
        when(questionReader.findQuestionsForEvaluation(9L)).thenReturn(evaluationQuestions);

        ExamResultResponse result = examService.evaluateSubmission(9L, Collections.emptyMap(), 0);

        assertEquals(3, result.getSkillBreakdown().size());
        assertNull(result.getStrongestSkill());
        assertEquals("Toàn diện các kỹ năng (Kiến thức ngôn ngữ, Đọc hiểu, Nghe hiểu)", result.getWeakestSkill());
    }
}

