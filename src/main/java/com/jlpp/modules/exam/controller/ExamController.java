package com.jlpp.modules.exam.controller;

import com.jlpp.common.util.CurrentUserSession;
import com.jlpp.modules.exam.dto.ExamCardDto;
import com.jlpp.modules.exam.dto.ExamHistoryDto;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.exam.dto.ExamTakeResponse;
import com.jlpp.modules.exam.service.ExamService;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final CurrentUserSession currentUserSession;
    private final UserRepository userRepository;

    private boolean isAuthenticated(HttpSession session, Principal principal) {
        return principal != null || (session != null && currentUserSession.isAuthenticated(session));
    }

    private Long getAuthenticatedUserId(HttpSession session, Principal principal) {
        if (principal != null) {
            String name = principal.getName();
            if (name != null && !name.isBlank()) {
                var userOpt = userRepository.findByEmailIgnoreCase(name.trim())
                        .or(() -> userRepository.findByUsername(name.trim()));
                if (userOpt.isPresent()) {
                    return userOpt.get().getId();
                }
            }
        }
        if (session != null) {
            Object learnerId = session.getAttribute("CURRENT_LEARNER_ID");
            if (learnerId instanceof Long l) return l;
            Object userId = session.getAttribute("userId");
            if (userId instanceof Long l) return l;
        }
        return null;
    }

    /**
     * SCREEN: Trung tâm bài thi - Đánh giá năng lực (Learner Exam Center)
     * Hiển thị đúng 3 nhóm bài thi: Đánh giá đầu vào, Đánh giá cột mốc, Thi thử JLPT
     * Và nút "Lịch sử làm bài" ở góc trên bên phải tiêu đề
     */
    @GetMapping({"/exams", "/exams/center"})
    public String examCenterPage(HttpSession session,
                                 Model model,
                                 Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams";
        }

        model.addAttribute("pageTitle", "Đánh giá năng lực - Kotoba JEPS");
        model.addAttribute("activeMenu", "exams");
        return "pages/exam/center";
    }

    /**
     * Màn hình danh sách đề thi thử JLPT (JLPT Mock Exam List)
     * Hỗ trợ lọc theo cấp độ N5 - N1
     */
    @GetMapping({"/exams/mock", "/exams/jlpt"})
    public String jlptMockListPage(@RequestParam(value = "level", required = false) String level,
                                   HttpSession session,
                                   Model model,
                                   Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/mock";
        }

        String activeLevel = (level != null && !level.trim().isEmpty()) ? level.trim().toUpperCase() : "ALL";
        List<ExamCardDto> exams = examService.getLearnerExams(activeLevel);

        model.addAttribute("exams", exams);
        model.addAttribute("activeLevel", activeLevel);
        model.addAttribute("levels", Arrays.asList("ALL", "N5", "N4", "N3", "N2", "N1"));
        model.addAttribute("activeMenu", "exams");
        model.addAttribute("pageTitle", "Thi thử JLPT - Đánh giá năng lực - Kotoba JEPS");
        return "pages/exam/list";
    }

    /**
     * SCREEN 1A: Màn hình giới thiệu đánh giá đầu vào & khảo sát mục tiêu (Placement Assessment Intro)
     * UC-13 / Screen 11: Cho phép Learner xem thông tin bài thi, mở popup khảo sát mục tiêu & kinh nghiệm học.
     */
    @GetMapping({"/exams/placement", "/assessment/placement-test"})
    public String startPlacementExam(HttpSession session,
                                     Model model,
                                     Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/placement";
        }

        ExamTakeResponse examData = examService.getPlacementExamForTaking();
        model.addAttribute("exam", examData);
        model.addAttribute("activeMenu", "placement");
        model.addAttribute("pageTitle", "Đánh giá năng lực tiếng Nhật đầu vào (Placement Test) - JEPS");
        return "pages/exam/placement-intro";
    }

    /**
     * SCREEN 1B: Vào phòng thi làm bài đánh giá đầu vào thực tế (Take Placement Exam Room)
     * Kích hoạt sau khi học viên chọn level mục tiêu & khảo sát tiền sử học
     */
    @GetMapping("/exams/placement/take")
    public String takePlacementExam(@RequestParam(value = "targetLevel", required = false) String targetLevel,
                                    @RequestParam(value = "hasStudied", required = false) String hasStudied,
                                    @RequestParam(value = "priorStudy", required = false) String priorStudy,
                                    @RequestParam(value = "assignedLevel", required = false) String assignedLevel,
                                    HttpSession session,
                                    Model model,
                                    Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/placement/take";
        }

        if (targetLevel != null && !targetLevel.isBlank()) {
            session.setAttribute("placement_target_level", targetLevel);
        }
        if (hasStudied != null && !hasStudied.isBlank()) {
            session.setAttribute("placement_has_studied", hasStudied);
        }
        if (assignedLevel != null && !assignedLevel.isBlank()) {
            session.setAttribute("placement_assigned_level", assignedLevel);
        }

        ExamTakeResponse examData = examService.getPlacementExamForTaking(assignedLevel);
        model.addAttribute("exam", examData);
        model.addAttribute("activeMenu", "placement");
        model.addAttribute("pageTitle", "Làm bài kiểm tra đầu vào (Placement Test " + (assignedLevel != null ? assignedLevel : "") + ") - JEPS");
        return "pages/exam/take-exam";
    }

    /**
     * SCREEN 3: Làm bài kiểm tra cột mốc theo khóa học (Take Milestone Exam)
     */
    @GetMapping("/exams/milestone/{id}")
    public String startMilestoneExam(@PathVariable Long id,
                                     HttpSession session,
                                     Model model,
                                     Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/milestone/" + id;
        }

        ExamTakeResponse examData = examService.getMilestoneExamForTaking(id);
        model.addAttribute("exam", examData);
        model.addAttribute("activeMenu", "exams");
        model.addAttribute("pageTitle", "Kiểm tra cột mốc khóa học - JEPS");
        return "pages/exam/take-exam";
    }

    /**
     * Danh sách bài kiểm tra cột mốc (hỗ trợ lọc theo khóa học)
     */
    @GetMapping("/exams/milestones")
    public String milestoneListPage(@RequestParam(value = "courseId", required = false) Long courseId,
                                    HttpSession session,
                                    Model model,
                                    Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/milestones";
        }

        List<ExamCardDto> exams = examService.getMilestoneExams(courseId);
        model.addAttribute("exams", exams);
        model.addAttribute("isMilestone", true);
        model.addAttribute("activeMenu", "exams");
        model.addAttribute("pageTitle", "Đánh giá cột mốc - Đánh giá năng lực - Kotoba JEPS");
        return "pages/exam/milestone-list";
    }

    /**
     * Bắt đầu làm bài thi chung (dùng cho JLPT Mock hoặc truy cập trực tiếp bằng examId)
     */
    @GetMapping("/exams/{id}/start")
    public String startExam(@PathVariable Long id,
                            HttpSession session,
                            Model model,
                            Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/register?continue=/exams/" + id + "/start";
        }

        ExamTakeResponse examData = examService.getExamForTaking(id);
        model.addAttribute("exam", examData);
        model.addAttribute("activeMenu", "exams");
        return "pages/exam/take-exam";
    }

    /**
     * Nộp bài thi: Chấm điểm thực tế từ Supabase và lưu vĩnh viễn master vào bảng exam_results
     */
    @PostMapping("/exams/{id}/submit")
    public String submitExam(@PathVariable Long id,
                             @RequestParam Map<String, String> allParams,
                             @RequestParam(value = "timeSpentSeconds", defaultValue = "0") Integer timeSpentSeconds,
                             HttpSession session,
                             Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login";
        }

        Long userId = getAuthenticatedUserId(session, principal);
        ExamResultResponse result = examService.evaluateAndSaveSubmission(id, userId, allParams, timeSpentSeconds);
        session.setAttribute("EXAM_RESULT_" + id, result);

        if (result.getResultId() != null) {
            return "redirect:/exams/result/" + id + "?resultId=" + result.getResultId();
        }
        return "redirect:/exams/result/" + id;
    }

    /**
     * SCREEN 2: Xem kết quả thi & lời giải chi tiết (View Exam Result & Solutions)
     */
    @GetMapping("/exams/result/{id}")
    public String examResultPage(@PathVariable Long id,
                                 @RequestParam(value = "resultId", required = false) Long resultId,
                                 Model model,
                                 HttpSession session,
                                 Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/register?continue=/exams/result/" + id;
        }

        ExamResultResponse result = (ExamResultResponse) session.getAttribute("EXAM_RESULT_" + id);
        if (result == null) {
            Long userId = getAuthenticatedUserId(session, principal);
            if (resultId != null && userId != null) {
                try {
                    result = examService.getHistoricalResult(resultId, userId);
                } catch (Exception ignored) {
                    result = examService.evaluateSubmission(id, Map.of(), 0);
                }
            } else {
                result = examService.evaluateSubmission(id, Map.of(), 0);
            }
        }

        model.addAttribute("result", result);
        model.addAttribute("activeMenu", (result != null && "PLACEMENT_TEST".equalsIgnoreCase(result.getExamType())) ? "placement" : "exams");
        return "pages/exam/result";
    }

    /**
     * SCREEN 4: Xem danh sách lịch sử thi (View Exam History List)
     */
    @GetMapping("/exams/history")
    public String examHistoryPage(HttpSession session, Model model, Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/history";
        }

        Long userId = getAuthenticatedUserId(session, principal);
        List<ExamHistoryDto> historyList = examService.getLearnerExamHistory(userId);

        model.addAttribute("historyList", historyList);
        model.addAttribute("activeMenu", "history");
        return "pages/exam/history-list";
    }

    /**
     * SCREEN 5: Xem chi tiết một lần thi trong lịch sử (View Exam History Detail)
     * Kiểm tra chặt chẽ quyền sở hữu (IDOR protection) - trả 403 Forbidden nếu xem bài của người khác
     */
    @GetMapping("/exams/history/{resultId}")
    public String examHistoryDetailPage(@PathVariable Long resultId,
                                        HttpSession session,
                                        Model model,
                                        Principal principal) {
        if (!isAuthenticated(session, principal)) {
            return "redirect:/auth/login?continue=/exams/history/" + resultId;
        }

        Long userId = getAuthenticatedUserId(session, principal);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để xem chi tiết bài thi.");
        }

        ExamResultResponse result = examService.getHistoricalResult(resultId, userId);

        model.addAttribute("result", result);
        model.addAttribute("isHistoricalView", true);
        model.addAttribute("activeMenu", "history");
        return "pages/exam/result";
    }


    @GetMapping("/admin/exams")
    public String adminExamList() {
        return "pages/admin/exam/list";
    }
}

