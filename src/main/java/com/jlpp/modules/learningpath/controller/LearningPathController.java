package com.jlpp.modules.learningpath.controller;

import com.jlpp.modules.learningpath.dto.LearningPathResponse;
import com.jlpp.modules.learningpath.dto.RecalibratePreviewResponse;
import com.jlpp.modules.learningpath.dto.RecalibrateRequest;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/learning-path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    /**
     * Hiển thị lộ trình học tập cá nhân hóa của học viên hiện tại.
     * Tự động hiển thị Empty State nếu chưa có dữ liệu lộ trình.
     */
    @GetMapping
    public String getLearningPath(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' accessing personal learning path", username);

        LearningPathResponse response = learningPathService.getLearningPath(username);
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
     */
    @GetMapping("/recalibrate")
    public String viewRecalibrateSchedule(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' accessing Recalibrate Schedule screen", username);

        LearningPathResponse pathResponse = learningPathService.getLearningPath(username);
        model.addAttribute("path", pathResponse);
        model.addAttribute("hasPath", pathResponse.isHasPath());

        if (pathResponse.isHasPath()) {
            RecalibrateRequest initialRequest = RecalibrateRequest.builder()
                    .weeklyHours(pathResponse.getWeeklyHours())
                    .build();
            RecalibratePreviewResponse preview = learningPathService.getRecalibratePreview(username, initialRequest);
            model.addAttribute("preview", preview);
            model.addAttribute("request", initialRequest);
        } else {
            model.addAttribute("emptyMessage", "Chưa đủ dữ liệu để điều chỉnh lịch học.");
        }

        return "pages/learning-path/recalibrate";
    }

    /**
     * Tính toán mô phỏng điều chỉnh lịch học dựa trên số giờ học mới.
     * Chế độ Simulation Preview, không phát sinh lệnh INSERT/UPDATE nào vào cơ sở dữ liệu.
     */
    @PostMapping("/recalibrate")
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
        log.info("Learner '{}' submitting recalibration simulation with weeklyHours={}",
                username, request.getWeeklyHours());

        LearningPathResponse pathResponse = learningPathService.getLearningPath(username);
        model.addAttribute("path", pathResponse);
        model.addAttribute("hasPath", pathResponse.isHasPath());

        if (!pathResponse.isHasPath()) {
            model.addAttribute("emptyMessage", "Chưa đủ dữ liệu để điều chỉnh lịch học.");
            return "pages/learning-path/recalibrate";
        }

        if (bindingResult.hasErrors()) {
            RecalibratePreviewResponse fallbackPreview = learningPathService.getRecalibratePreview(
                    username, RecalibrateRequest.builder()
                            .weeklyHours(pathResponse.getWeeklyHours())
                            .build());
            model.addAttribute("preview", fallbackPreview);
            return "pages/learning-path/recalibrate";
        }

        RecalibratePreviewResponse preview = learningPathService.getRecalibratePreview(username, request);
        model.addAttribute("preview", preview);
        model.addAttribute("simulationSuccess", true);

        return "pages/learning-path/recalibrate";
    }

    /**
     * Xác nhận áp dụng và lưu lộ trình học tập mới vào cơ sở dữ liệu.
     * Cập nhật thời lượng học hàng ngày cho lộ trình hiện tại của học viên.
     */
    @PostMapping("/recalibrate/apply")
    public String applyRecalibratedSchedule(
            Principal principal,
            @Valid @ModelAttribute("request") RecalibrateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' confirming application of recalibrated schedule with weeklyHours={}",
                username, request != null ? request.getWeeklyHours() : null);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tham số điều chỉnh lịch học không hợp lệ.");
            return "redirect:/learning-path/recalibrate";
        }

        try {
            learningPathService.applyRecalibratedSchedule(username, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Lịch học mới đã được áp dụng thành công! Lộ trình đã được cập nhật với thời lượng " + request.getWeeklyHours() + " giờ/tuần.");
            return "redirect:/learning-path";
        } catch (Exception ex) {
            log.error("Failed to apply recalibrated schedule for learner={}", username, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật lịch học: " + ex.getMessage());
            return "redirect:/learning-path/recalibrate";
        }
    }
}
