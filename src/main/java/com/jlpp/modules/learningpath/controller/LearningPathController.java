package com.jlpp.modules.learningpath.controller;

import com.jlpp.modules.learningpath.dto.LearningPathDtos.*;

import com.jlpp.modules.learningpath.service.LearningPathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping({"/learning-path", "/learning-path/"})
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    /**
     * Hiển thị lộ trình học tập cá nhân hóa của học viên hiện tại.
     * Hỗ trợ Course Switcher (chuyển đổi đa khóa học cho các khóa học học viên đã đăng ký).
     * Tự động hiển thị Empty State nếu chưa có dữ liệu lộ trình.
     */
    @GetMapping({"", "/"})
    public String getLearningPath(
            Principal principal,
            @RequestParam(value = "courseId", required = false) Long courseId,
            Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' accessing personal learning path (courseId={})", username, courseId);

        LearningPathResponse response = learningPathService.getLearningPath(username, courseId);
        model.addAttribute("path", response);
        model.addAttribute("hasPath", response.isHasPath());
        if (!response.isHasPath()) {
            model.addAttribute("emptyMessage", response.getEmptyStateMessage());
        }

        return "pages/learning-path/view";
    }

    /**
     * Hiển thị giao diện điều chỉnh lịch trình lộ trình học tập.
     * Mặc định hiển thị chế độ xem trước (Simulation Preview) với lịch học hiện tại.
     * Hỗ trợ Course Switcher cho khóa học đã đăng ký.
     */
    @GetMapping({"/recalibrate", "/recalibrate/"})
    public String viewRecalibrateSchedule(
            Principal principal,
            @RequestParam(value = "courseId", required = false) Long courseId,
            Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' accessing Recalibrate Schedule screen (courseId={})", username, courseId);

        LearningPathResponse pathResponse = learningPathService.getLearningPath(username, courseId);
        model.addAttribute("path", pathResponse);
        model.addAttribute("hasPath", pathResponse.isHasPath());

        if (pathResponse.isHasPath()) {
            if (pathResponse.isAllLessonsCompleted()) {
                model.addAttribute("courseCompleted", true);
                model.addAttribute("infoMessage", "Bạn đã hoàn thành 100% tất cả các bài học trong khóa học này. Lộ trình học tập đã hoàn tất xuất sắc!");
            } else {
                java.time.LocalDate defaultStartDate = java.time.LocalDate.now();
                if (pathResponse.getStartDateFormatted() != null && !pathResponse.getStartDateFormatted().equals("N/A")) {
                    try {
                        java.time.LocalDate parsed = java.time.LocalDate.parse(pathResponse.getStartDateFormatted(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        if (!parsed.isBefore(java.time.LocalDate.now())) {
                            defaultStartDate = parsed;
                        }
                    } catch (Exception ignored) {}
                }

                RecalibrateRequest initialRequest = RecalibrateRequest.builder()
                        .courseId(pathResponse.getCourseId())
                        .weeklyHours(pathResponse.getWeeklyHours())
                        .dailyHours(pathResponse.getDailyHours())
                        .studyDays(pathResponse.getStudyDays())
                        .targetExamDate(pathResponse.getTargetExamDate())
                        .scheduleMode(pathResponse.getScheduleMode())
                        .startDate(defaultStartDate)
                        .build();
                RecalibratePreviewResponse preview = learningPathService.getRecalibratePreview(username, initialRequest);
                model.addAttribute("preview", preview);
                model.addAttribute("request", initialRequest);
                model.addAttribute("currentStep", 1);
            }
        } else {
            model.addAttribute("emptyMessage", pathResponse.getEmptyStateMessage() != null ? pathResponse.getEmptyStateMessage() : "Chưa đủ dữ liệu để điều chỉnh lịch học.");
        }

        return "pages/learning-path/recalibrate";
    }

    /**
     * Tính toán mô phỏng điều chỉnh lịch học dựa trên số giờ học mới.
     * Chế độ Simulation Preview, không phát sinh lệnh INSERT/UPDATE nào vào cơ sở dữ liệu.
     */
    @PostMapping({"/recalibrate", "/recalibrate/"})
    public String simulateRecalibration(
            Principal principal,
            @Valid @ModelAttribute("request") RecalibrateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' submitting recalibration simulation with courseId={}, weeklyHours={}",
                username, request != null ? request.getCourseId() : null, request != null ? request.getWeeklyHours() : null);

        LearningPathResponse pathResponse = learningPathService.getLearningPath(username, request != null ? request.getCourseId() : null);
        model.addAttribute("path", pathResponse);
        model.addAttribute("hasPath", pathResponse.isHasPath());

        if (!pathResponse.isHasPath()) {
            model.addAttribute("emptyMessage", pathResponse.getEmptyStateMessage() != null ? pathResponse.getEmptyStateMessage() : "Chưa đủ dữ liệu để điều chỉnh lịch học.");
            return "pages/learning-path/recalibrate";
        }

        // Validate: Không cho phép mô phỏng lại lịch học khi đã hoàn thành 100% khóa học
        if (pathResponse.isAllLessonsCompleted()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã hoàn thành tất cả các bài học trong khóa học này! Không thể điều chỉnh lại lịch học.");
            return "redirect:/learning-path" + (request != null && request.getCourseId() != null ? "?courseId=" + request.getCourseId() : "");
        }

        if (request != null && request.getStartDate() != null && request.getStartDate().isBefore(java.time.LocalDate.now())) {
            model.addAttribute("errorMessage", "Ngày bắt đầu học dự kiến không được trước ngày hôm nay (" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
            request.setStartDate(java.time.LocalDate.now());
        }

        if (bindingResult.hasErrors()) {
            RecalibratePreviewResponse fallbackPreview = learningPathService.getRecalibratePreview(
                    username, RecalibrateRequest.builder()
                            .courseId(pathResponse.getCourseId())
                            .weeklyHours(pathResponse.getWeeklyHours())
                            .startDate(request != null ? request.getStartDate() : java.time.LocalDate.now())
                            .build());
            model.addAttribute("preview", fallbackPreview);
            return "pages/learning-path/recalibrate";
        }

        RecalibratePreviewResponse preview = learningPathService.getRecalibratePreview(username, request);
        model.addAttribute("preview", preview);
        model.addAttribute("simulationSuccess", true);
        model.addAttribute("currentStep", 2);

        return "pages/learning-path/recalibrate";
    }

    /**
     * Xác nhận áp dụng và lưu lộ trình học tập mới vào cơ sở dữ liệu.
     * Cập nhật thời lượng học hàng ngày cho lộ trình hiện tại của học viên.
     */
    @PostMapping({"/recalibrate/apply", "/recalibrate/apply/"})
    public String applyRecalibratedSchedule(
            Principal principal,
            @Valid @ModelAttribute("request") RecalibrateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' confirming application of recalibrated schedule with courseId={}, weeklyHours={}",
                username, request != null ? request.getCourseId() : null, request != null ? request.getWeeklyHours() : null);

        String redirectUrl = "/learning-path/recalibrate" + (request != null && request.getCourseId() != null ? "?courseId=" + request.getCourseId() : "");

        // Validate: Không cho phép áp dụng lịch học mới khi đã hoàn thành 100% tất cả các bài học
        LearningPathResponse pathResponse = learningPathService.getLearningPath(username, request != null ? request.getCourseId() : null);
        if (pathResponse != null && pathResponse.isHasPath() && pathResponse.isAllLessonsCompleted()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã hoàn thành tất cả các bài học trong khóa học này! Không thể lưu thay đổi lịch học.");
            return "redirect:/learning-path" + (request != null && request.getCourseId() != null ? "?courseId=" + request.getCourseId() : "");
        }

        if (request != null && request.getStartDate() != null && request.getStartDate().isBefore(java.time.LocalDate.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ngày bắt đầu học dự kiến không được trước ngày hôm nay (" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
            return "redirect:" + redirectUrl;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tham số điều chỉnh lịch học không hợp lệ.");
            return "redirect:" + redirectUrl;
        }

        try {
            learningPathService.applyRecalibratedSchedule(username, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Lịch học mới đã được áp dụng thành công! Lộ trình đã được cập nhật với thời lượng " + request.getWeeklyHours() + " giờ/tuần.");
            return "redirect:/learning-path" + (request != null && request.getCourseId() != null ? "?courseId=" + request.getCourseId() : "");
        } catch (Exception ex) {
            log.error("Failed to apply recalibrated schedule for learner={}", username, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật lịch học: " + ex.getMessage());
            return "redirect:" + redirectUrl;
        }
    }

    /**
     * Tiếp nhận yêu cầu báo nghỉ học (Leave of Absence) của học viên.
     * Tự động dời lịch các bài học tiếp theo và bảo lưu chuỗi streak.
     */
    @PostMapping({"/leave", "/leave/"})
    public String submitLeave(
            Principal principal,
            @Valid @ModelAttribute("leaveRequest") SubmitLeaveRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }
        String username = principal.getName();
        String redirectUrl = "/learning-path" + (request != null && request.getCourseId() != null ? "?courseId=" + request.getCourseId() : "");

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin báo nghỉ không hợp lệ. Vui lòng kiểm tra lại ngày bắt đầu và kết thúc.");
            return "redirect:" + redirectUrl;
        }

        try {
            learningPathService.submitLeaveOfAbsence(username, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Đã ghi nhận báo nghỉ từ %s đến %s. Hệ thống đã tự động dời các bài học tiếp theo và bảo lưu chuỗi ngày học liên tục 🔥 của bạn!",
                            request.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            request.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        } catch (Exception ex) {
            log.error("Error submitting leave of absence for user={}", username, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể ghi nhận báo nghỉ: " + ex.getMessage());
        }

        return "redirect:" + redirectUrl;
    }

    /**
     * Hủy một đợt báo nghỉ học.
     */
    @PostMapping({"/leave/cancel", "/leave/cancel/"})
    public String cancelLeave(
            Principal principal,
            @RequestParam("courseId") Long courseId,
            @RequestParam("leaveId") String leaveId,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }
        String username = principal.getName();
        String redirectUrl = "/learning-path" + (courseId != null ? "?courseId=" + courseId : "");

        try {
            learningPathService.cancelLeaveOfAbsence(username, courseId, leaveId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đợt báo nghỉ. Lịch học của bạn đã được cập nhật lại bình thường!");
        } catch (Exception ex) {
            log.error("Error cancelling leave of absence for user={}, leaveId={}", username, leaveId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đợt báo nghỉ: " + ex.getMessage());
        }

        return "redirect:" + redirectUrl;
    }

    /**
     * Bật hoặc tắt Chế độ Bù bài (Catch-up Mode).
     */
    @PostMapping({"/catch-up", "/catch-up/"})
    public String toggleCatchUp(
            Principal principal,
            @ModelAttribute("catchUpRequest") ToggleCatchUpRequest request,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }
        String username = principal.getName();
        String redirectUrl = "/learning-path" + (request != null && request.getCourseId() != null ? "?courseId=" + request.getCourseId() : "");

        try {
            learningPathService.toggleCatchUpMode(username, request);
            if (Boolean.TRUE.equals(request.getEnable())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        String.format("Đã kích hoạt Chế độ Bù bài với +%d phút/ngày! Lộ trình đã được tối ưu để bắt kịp kế hoạch.",
                                request.getExtraMinutes() != null ? request.getExtraMinutes() : 30));
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã tắt Chế độ Bù bài. Lộ trình trở về thời lượng học chuẩn.");
            }
        } catch (Exception ex) {
            log.error("Error toggling catch-up mode for user={}", username, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thay đổi chế độ bù bài: " + ex.getMessage());
        }

        return "redirect:" + redirectUrl;
    }
}
