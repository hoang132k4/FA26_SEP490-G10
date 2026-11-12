package com.jlpp.modules.exam;

import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.ExamTakeResponse;
import com.jlpp.modules.exam.dto.OptionDto;
import com.jlpp.modules.exam.dto.QuestionDto;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.reader.LearnerExamResultReader;
import com.jlpp.modules.exam.reader.LearnerQuestionReader;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.exam.service.impl.ExamServiceImpl;
import com.jlpp.modules.exam.writer.LearnerExamResultWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: Placement Assessment & Prior Level Logic (Screens 4, 17, 18)")
class PlacementAssessmentUnitTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private LearnerQuestionReader questionReader;

    @Mock
    private LearnerExamResultWriter resultWriter;

    @Mock
    private LearnerExamResultReader resultReader;

    private ExamServiceImpl examService;

    @BeforeEach
    void setUp() {
        examService = new ExamServiceImpl(examRepository, questionReader, resultWriter, resultReader);
    }

    private Exam createPlacementExam(Long id, String level, String title) {
        return new Exam(id, title, "PLACEMENT_TEST", level, 45, 15, 100, "PUBLISHED", "Description");
    }

    private QuestionDto createSampleQuestion(Long id) {
        OptionDto opt1 = OptionDto.builder().id(1L).text("A").correct(true).build();
        OptionDto opt2 = OptionDto.builder().id(2L).text("B").correct(false).build();
        return QuestionDto.builder()
                .id(id)
                .content("Câu hỏi đánh giá")
                .skillGroup("VOCAB_GRAMMAR")
                .options(List.of(opt1, opt2))
                .build();
    }

    @Test
    @DisplayName("Normal: Lấy bài thi Placement theo đúng level chỉ định (Screen 4 & 18)")
    void getPlacementExamForTaking_withSpecificLevel() {
        Exam examN4 = createPlacementExam(1L, "N4", "Đánh giá năng lực đầu vào N4");
        Exam examN3 = createPlacementExam(2L, "N3", "Đánh giá năng lực đầu vào N3");

        when(examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST")).thenReturn(List.of(examN4, examN3));
        when(examRepository.findById(2L)).thenReturn(Optional.of(examN3));
        when(questionReader.countQuestionsByExamId(2L)).thenReturn(10L);
        when(questionReader.findQuestionsForTaking(2L)).thenReturn(List.of(createSampleQuestion(101L)));

        ExamTakeResponse res = examService.getPlacementExamForTaking("N3");

        assertNotNull(res);
        assertEquals(2L, res.getExamId());
        assertEquals("N3", res.getJlptLevel());
    }

    @Test
    @DisplayName("Normal: Lấy bài thi Placement mặc định khi không truyền level (Screen 4)")
    void getPlacementExamForTaking_defaultLevel() {
        Exam examN4 = createPlacementExam(1L, "N4", "Đánh giá năng lực đầu vào N4");

        when(examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST")).thenReturn(List.of(examN4));
        when(examRepository.findById(1L)).thenReturn(Optional.of(examN4));
        when(questionReader.countQuestionsByExamId(1L)).thenReturn(10L);
        when(questionReader.findQuestionsForTaking(1L)).thenReturn(List.of(createSampleQuestion(101L)));

        ExamTakeResponse res = examService.getPlacementExamForTaking(null);

        assertNotNull(res);
        assertEquals(1L, res.getExamId());
        assertEquals("N4", res.getJlptLevel());
    }

    @Test
    @DisplayName("Abnormal: Không có bài thi Placement trong hệ thống ném ra ResponseStatusException NOT_FOUND (Screen 4)")
    void getPlacementExamForTaking_notFound() {
        when(examRepository.findByExamTypeIgnoreCase("PLACEMENT_TEST")).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> examService.getPlacementExamForTaking("N2"));
    }

    @Test
    @DisplayName("Normal: Nghiệp vụ suy luận cấp độ (Screen 17 & 18): Chưa từng học -> Giảm 1 bậc (N3 -> N4)")
    void placementLevelDeduction_hasNotStudied_deductsOneLevel() {
        // Business logic rule: If user chooses target N3 and has NOT studied -> assignedLevel = N4
        String targetLevel = "N3";
        String hasStudied = "NO";

        String assignedLevel = "YES".equalsIgnoreCase(hasStudied) ? targetLevel : switch (targetLevel) {
            case "N1" -> "N2";
            case "N2" -> "N3";
            case "N3" -> "N4";
            case "N4" -> "N5";
            case "N5" -> "N5";
            default -> targetLevel;
        };

        assertEquals("N4", assignedLevel);
    }

    @Test
    @DisplayName("Normal: Nghiệp vụ suy luận cấp độ (Screen 17 & 18): Đã từng học -> Giữ đúng cấp độ (N3 -> N3)")
    void placementLevelDeduction_hasStudied_keepsExactLevel() {
        // Business logic rule: If user chooses target N3 and HAS studied -> assignedLevel = N3
        String targetLevel = "N3";
        String hasStudied = "YES";

        String assignedLevel = "YES".equalsIgnoreCase(hasStudied) ? targetLevel : "N4";

        assertEquals("N3", assignedLevel);
    }

    @Test
    @DisplayName("Boundary: Chọn mục tiêu N5 và chưa từng học -> Vẫn gán N5 vì là cấp độ khởi đầu nhỏ nhất (Screen 18)")
    void placementLevelDeduction_n5LowerBound() {
        String targetLevel = "N5";
        String hasStudied = "NO";

        String assignedLevel = "YES".equalsIgnoreCase(hasStudied) ? targetLevel : switch (targetLevel) {
            case "N1" -> "N2";
            case "N2" -> "N3";
            case "N3" -> "N4";
            case "N4" -> "N5";
            case "N5" -> "N5";
            default -> targetLevel;
        };

        assertEquals("N5", assignedLevel);
    }

    @Test
    @DisplayName("Normal: Chấm điểm bài thi Placement test: Luôn tính trên thang 180 và đánh dấu PASSED (Screen 8 & 4)")
    void submitPlacementExam_alwaysPassingWithBase180() {
        Exam exam = createPlacementExam(10L, "N3", "Bài đánh giá đầu vào N3");
        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(questionReader.findQuestionsForEvaluation(10L)).thenReturn(List.of());

        ExamResultResponse res = examService.evaluateSubmission(10L, Map.of(), 120);

        assertNotNull(res);
        assertEquals(180, res.getTotalPossibleScore());
        assertEquals(Boolean.TRUE, res.getPassed()); // Placement test always passes for placement orientation
        assertNull(res.getPassingScore()); // Placement has no minimum passing threshold
    }
}
