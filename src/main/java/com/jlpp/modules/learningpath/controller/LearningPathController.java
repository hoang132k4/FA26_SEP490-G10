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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/learning-path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    /**
     * Màn hình Xem lộ trình học tập cá nhân hóa (UC-27).
     * Chỉ hiển thị dữ liệu thực tế từ Database, không sinh dữ liệu giả lập.
     */
    @GetMapping
    public String viewLearningPath(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        log.info("Learner '{}' accessing View Learning Path screen", username);

        LearningPathResponse response = learningPathService.getLearningPath(username);
        model.addAttribute("path", response);
        model.addAttribute("hasPath", response.isHasPath());
        return "pages/learning-path/view";
    }

    /**
     * Màn hình Điều chỉnh lịch trình lộ trình học tập (UC-28).
     * Hoạt động ở chế độ PREVIEW / SIMULATION MODE, tuyệt đối không lưu vào Database.
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
        log.info("Learner '{}' submitting recalibration simulation with weeklyHours={}", username, request.getWeeklyHours());

        LearningPathResponse pathResponse = learningPathService.getLearningPath(username);
        model.addAttribute("path", pathResponse);
        model.addAttribute("hasPath", pathResponse.isHasPath());

        if (!pathResponse.isHasPath()) {
            model.addAttribute("emptyMessage", "Chưa đủ dữ liệu để điều chỉnh lịch học.");
            return "pages/learning-path/recalibrate";
        }

        if (bindingResult.hasErrors()) {
            RecalibratePreviewResponse fallbackPreview = learningPathService.getRecalibratePreview(
                    username, RecalibrateRequest.builder().weeklyHours(pathResponse.getWeeklyHours()).build());
            model.addAttribute("preview", fallbackPreview);
            return "pages/learning-path/recalibrate";
        }

        RecalibratePreviewResponse preview = learningPathService.getRecalibratePreview(username, request);
        model.addAttribute("preview", preview);
        model.addAttribute("simulationSuccess", true);

        return "pages/learning-path/recalibrate";
    }
}
