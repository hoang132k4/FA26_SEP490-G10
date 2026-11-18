package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.ExamCardDto;
import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.ExamTakeResponse;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.reader.LearnerExamResultReader;
import com.jlpp.modules.exam.reader.LearnerQuestionReader;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.exam.service.AssessmentScoringService;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.exam.writer.LearnerExamResultWriter;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Test: Exam Persistence, Scoring & History Verification (FN-04, FN-07, FN-08, FN-10)")
public class ExamPersistenceAndScoringIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private LearnerExamResultWriter resultWriter;

    @MockBean
    private LearnerExamResultReader resultReader;

    @MockBean
    private LearnerQuestionReader questionReader;

    private Long testUserId1;
    private Long testUserId2;
    private Long testExamId;
    private Long questionId1;
    private Long questionId2;
    private Long questionId3;

    @BeforeEach
    void setUp() {
        // 1. Seed users in real H2 database
        User u1 = userRepository.save(User.builder()
                .username("exam_learner_it1")
                .email("exam_learner_it1@jeps.edu.vn")
                .password("Pass@123")
                .fullName("Exam Learner IT One")
                .build());
        testUserId1 = u1.getId();

        User u2 = userRepository.save(User.builder()
                .username("exam_learner_it2")
                .email("exam_learner_it2@jeps.edu.vn")
                .password("Pass@123")
                .fullName("Exam Learner IT Two")
                .build());
        testUserId2 = u2.getId();

        // 2. Seed real Exam entity into H2 database
        Exam exam = new Exam(null, "JLPT N5 Standard Mock Exam 2026", "JLPT_MOCK", "N5",
                90, 3, 90, "PUBLISHED", "Mock Exam for N5 Integration Testing");
        exam = examRepository.save(exam);
        testExamId = exam.getExamId();

        // 3. Configure mock question reader with realistic question data across sections
        questionId1 = 101L; // VOCAB_GRAMMAR
        questionId2 = 102L; // READING
        questionId3 = 103L; // LISTENING

        QuestionDto q1 = QuestionDto.builder()
                .id(questionId1)
                .category("VOCAB_GRAMMAR")
                .skillGroup("VOCAB_GRAMMAR")
                .content("Watashi wa gakusei ___ desu.")
                .points(2)
                .explanation("Trợ từ wa biểu thị chủ ngữ trong câu.")
                .options(List.of(
                        OptionDto.builder().id(201L).label("A").text("wa").correct(true).build(),
                        OptionDto.builder().id(202L).label("B").text("ga").correct(false).build()
                ))
                .build();

        QuestionDto q2 = QuestionDto.builder()
                .id(questionId2)
                .category("READING")
                .skillGroup("READING")
                .content("Kono hon wa omoshiroi desu ka?")
                .points(3)
                .explanation("Đọc hiểu câu hỏi xác nhận tính từ.")
                .options(List.of(
                        OptionDto.builder().id(203L).label("A").text("Hai, omoshiroi").correct(true).build(),
                        OptionDto.builder().id(204L).label("B").text("Iie, omoshiroi").correct(false).build()
                ))
                .build();

        QuestionDto q3 = QuestionDto.builder()
                .id(questionId3)
                .category("LISTENING")
                .skillGroup("LISTENING")
                .content("Mondai 1: Kiite kudasai.")
                .points(3)
                .explanation("Hội thoại nghe hiểu.")
                .options(List.of(
                        OptionDto.builder().id(205L).label("A").text("Hai").correct(true).build(),
                        OptionDto.builder().id(206L).label("B").text("Iie").correct(false).build()
                ))
                .build();

        when(questionReader.countQuestionsByExamId(testExamId)).thenReturn(3L);
        when(questionReader.findQuestionsForTaking(testExamId)).thenReturn(List.of(
                QuestionDto.builder().id(questionId1).category("VOCAB_GRAMMAR").skillGroup("VOCAB_GRAMMAR").options(List.of(
                        OptionDto.builder().id(201L).label("A").text("wa").correct(null).build(),
                        OptionDto.builder().id(202L).label("B").text("ga").correct(null).build()
                )).build(),
                QuestionDto.builder().id(questionId2).category("READING").skillGroup("READING").options(List.of(
                        OptionDto.builder().id(203L).label("A").text("Hai, omoshiroi").correct(null).build(),
                        OptionDto.builder().id(204L).label("B").text("Iie, omoshiroi").correct(null).build()
                )).build(),
                QuestionDto.builder().id(questionId3).category("LISTENING").skillGroup("LISTENING").options(List.of(
                        OptionDto.builder().id(205L).label("A").text("Hai").correct(null).build(),
                        OptionDto.builder().id(206L).label("B").text("Iie").correct(null).build()
                )).build()
        ));
        when(questionReader.findQuestionsForEvaluation(testExamId)).thenReturn(List.of(q1, q2, q3));
    }

    @Test
    @DisplayName("IT_EXAM_01: Retrieve exam entity from H2 database and load taking questions with answers hidden")
    void testExamRetrievalAndTakingQuestionsLoading_AnswersHidden() {
        // Verify exam persisted in real H2 DB
        Optional<Exam> fromDb = examRepository.findById(testExamId);
        assertTrue(fromDb.isPresent());
        assertEquals("JLPT N5 Standard Mock Exam 2026", fromDb.get().getExamTitle());

        // Call service to get taking payload
        ExamTakeResponse examTake = examService.getExamForTaking(testExamId);
        assertNotNull(examTake);
        assertEquals(testExamId, examTake.getExamId());
        assertEquals("N5", examTake.getJlptLevel());
        assertEquals(3, examTake.getQuestions().size());

        // Anti-cheating verification: all option 'correct' flags must be null
        for (QuestionDto q : examTake.getQuestions()) {
            for (OptionDto opt : q.getOptions()) {
                assertNull(opt.getCorrect(), "Option correct flag must NOT be leaked to client during taking session");
            }
        }
    }

    @Test
    @DisplayName("IT_EXAM_02: Submit exam answers, compute sectional scores, and evaluate pass/fail based on JLPT thresholds")
    void testExamSubmission_ScoringCalculationAndPassFailEvaluation() {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("q_" + questionId1, "201"); // Correct (VOCAB_GRAMMAR)
        formParams.put("q_" + questionId2, "203"); // Correct (READING)
        formParams.put("q_" + questionId3, "205"); // Correct (LISTENING)

        ExamResultResponse result = examService.evaluateSubmission(testExamId, formParams, 1800);

        assertNotNull(result);
        assertEquals(testExamId, result.getExamId());
        assertEquals(3, result.getCorrectCount());
        assertEquals(0, result.getIncorrectCount());
        assertEquals(180, result.getScore(), "Full marks on all 3 sections yields 180 on JLPT scale");
        assertTrue(result.getPassed(), "Must pass when score >= passingScore and all sections above minimum");
        assertNotNull(result.getSkillBreakdown());
        assertEquals(2, result.getSkillBreakdown().size(), "N5 combines Language Knowledge + Reading (120) and Listening (60)");
    }

    @Test
    @DisplayName("IT_EXAM_03: Sectional failure marks exam as FAILED even if total score meets threshold")
    void testExamSubmission_SectionalFailure_FailsExam() {
        // High score on Language + Reading, but 0 on Listening (below minimum section score 19/60)
        Map<String, String> formParams = new HashMap<>();
        formParams.put("q_" + questionId1, "201"); // Correct (VOCAB)
        formParams.put("q_" + questionId2, "203"); // Correct (READING)
        formParams.put("q_" + questionId3, "206"); // Wrong (LISTENING: 0 points)

        ExamResultResponse result = examService.evaluateSubmission(testExamId, formParams, 1800);

        assertNotNull(result);
        assertEquals(2, result.getCorrectCount());
        assertEquals(1, result.getIncorrectCount());
        assertEquals(120, result.getScore(), "Language+Reading yields 120 (>= passing score 90)");
        assertFalse(result.getPassed(), "Must fail because Listening section earned 0 (< minimum required 19)");
        assertTrue(result.getAiFeedback().contains("Nghe hiểu"), "Feedback must identify the failing section");
    }

    @Test
    @DisplayName("IT_EXAM_04: Persist exam result summary and result details, verifying resultId generation")
    void testExamResultPersistence_MasterAndDetails() {
        when(resultWriter.saveExamResultSummary(eq(testUserId1), eq(testExamId), anyInt(), any(), anyBoolean(), anyString(), anyInt()))
                .thenReturn(5001L);

        Map<String, String> formParams = Map.of(
                "q_" + questionId1, "201",
                "q_" + questionId2, "203",
                "q_" + questionId3, "205"
        );

        ExamResultResponse response = examService.evaluateAndSaveSubmission(testExamId, testUserId1, formParams, 1200);

        assertNotNull(response);
        assertEquals(5001L, response.getResultId(), "Must return generated result ID");
        assertTrue(response.isPersistentSaved());

        // Verify resultWriter interactions
        verify(resultWriter, times(1)).saveExamResultSummary(
                eq(testUserId1), eq(testExamId), eq(180), any(BigDecimal.class), eq(true), eq("PASSED"), eq(1200)
        );
        verify(resultWriter, times(1)).saveExamResultDetails(eq(5001L), anyList());
    }

    @Test
    @DisplayName("IT_EXAM_05: Retrieve historical exam result and reconstruct solutions with user answers and explanations")
    void testRetrievePersistedResult_ReconstructSolutions() {
        Long resultId = 5001L;

        ExamResultResponse dbResult = ExamResultResponse.builder()
                .resultId(resultId)
                .examId(testExamId)
                .examTitle("JLPT N5 Standard Mock Exam 2026")
                .examType("JLPT_MOCK")
                .jlptLevel("N5")
                .score(180)
                .passed(true)
                .timeSpentSeconds(1200)
                .build();

        when(resultReader.findExamResultByIdAndUserId(resultId, testUserId1)).thenReturn(Optional.of(dbResult));
        when(resultReader.findSelectedOptionsByResultId(resultId)).thenReturn(Map.of(
                questionId1, "201",
                questionId2, "203",
                questionId3, "205"
        ));

        ExamResultResponse historical = examService.getHistoricalResult(resultId, testUserId1);

        assertNotNull(historical);
        assertEquals(resultId, historical.getResultId());
        assertEquals(180, historical.getScore());
        assertNotNull(historical.getQuestions());
        assertEquals(3, historical.getQuestions().size());

        for (QuestionDto q : historical.getQuestions()) {
            assertNotNull(q.getExplanation(), "Explanation must be displayed in solution review");
            assertNotNull(q.getUserAnswerId(), "User selected answer must be preserved");
        }
    }

    @Test
    @DisplayName("IT_EXAM_06: Historical result query enforces ownership and blocks cross-user inspection (BOLA/IDOR)")
    void testHistoricalResult_EnforcesOwnership_BlocksCrossUserAccess() {
        Long user2ResultId = 5002L;

        // Result 5002 belongs to User 2, so query for User 1 returns empty
        when(resultReader.findExamResultByIdAndUserId(user2ResultId, testUserId1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            examService.getHistoricalResult(user2ResultId, testUserId1);
        }, "Accessing foreign exam result must throw 404 / 403 ResponseStatusException");
    }

    @Test
    @DisplayName("IT_EXAM_07: Deduplication window returns cached result without invoking redundant result writer calls")
    void testDuplicateSubmissionProtection_DeduplicationWindow() {
        when(resultWriter.saveExamResultSummary(eq(testUserId1), eq(testExamId), anyInt(), any(), anyBoolean(), anyString(), anyInt()))
                .thenReturn(6001L);

        Map<String, String> formParams = Map.of("q_" + questionId1, "201");

        ExamResultResponse res1 = examService.evaluateAndSaveSubmission(testExamId, testUserId1, formParams, 500);
        assertNotNull(res1.getResultId());

        // Immediate duplicate submission
        ExamResultResponse res2 = examService.evaluateAndSaveSubmission(testExamId, testUserId1, formParams, 500);

        assertEquals(res1.getResultId(), res2.getResultId(), "Must return identical cached result ID");
        // Result writer should only have been called ONCE
        verify(resultWriter, times(1)).saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("IT_EXAM_08: Database persistence failure triggers rollback and returns graceful degradation notice")
    void testDatabaseFailure_HandlesGracefullyWithRollback() {
        doThrow(new RuntimeException("Simulated Supabase PostgreSQL Connection Dropped"))
                .when(resultWriter).saveExamResultSummary(any(), any(), any(), any(), anyBoolean(), any(), any());

        Map<String, String> formParams = Map.of("q_" + questionId1, "201");

        ExamResultResponse result = examService.evaluateAndSaveSubmission(testExamId, testUserId1, formParams, 600);

        assertNotNull(result, "Service must not crash on DB persistence failure");
        assertNull(result.getResultId(), "resultId must be null when persistence fails");
        assertFalse(result.isPersistentSaved());
        assertTrue(result.getStatusNotice().contains("lỗi kết nối"), "Must return user-friendly degradation notice");
    }
}
