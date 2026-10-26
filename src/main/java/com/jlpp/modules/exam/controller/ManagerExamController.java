package com.jlpp.modules.exam.controller;

import com.jlpp.modules.exam.dto.ManagerExamForm;
import com.jlpp.modules.exam.service.ManagerExamService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/exams")
public class ManagerExamController {

    private final ManagerExamService managerExamService;

    public ManagerExamController(ManagerExamService managerExamService) {
        this.managerExamService = managerExamService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String examType,
                       @RequestParam(defaultValue = "") String jlptLevel,
                       @RequestParam(defaultValue = "") String status,
                       Model model) {
        model.addAttribute("exams", managerExamService.search(search, examType, jlptLevel, status));
        model.addAttribute("search", search);
        model.addAttribute("examType", examType);
        model.addAttribute("jlptLevel", jlptLevel);
        model.addAttribute("status", status);
        model.addAttribute("options", managerExamService.options());
        model.addAttribute("placementCount", managerExamService.countByType("PLACEMENT_TEST"));
        model.addAttribute("mockCount", managerExamService.countByType("JLPT_MOCK"));
        model.addAttribute("milestoneCount", managerExamService.countByType("MILESTONE"));
        return "pages/manager/exam/list";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("examForm", defaultForm());
        model.addAttribute("options", managerExamService.options());
        model.addAttribute("isEdit", false);
        return "pages/manager/exam/form";
    }

    @GetMapping("/{examId}/edit")
    public String editPage(@PathVariable long examId, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("examForm", managerExamService.toForm(managerExamService.findById(examId)));
            model.addAttribute("options", managerExamService.options());
            model.addAttribute("isEdit", true);
            return "pages/manager/exam/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
            return "redirect:/manager/exams";
        }
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("examForm") ManagerExamForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return formPage(form, model, false);
        try {
            managerExamService.create(form);
            redirectAttributes.addFlashAttribute("examSuccess", "Đã tạo đề thi.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            model.addAttribute("examError", exception.getMessage());
            return formPage(form, model, false);
        }
        return "redirect:/manager/exams";
    }

    @PostMapping("/{examId}")
    public String update(@PathVariable long examId, @Valid @ModelAttribute("examForm") ManagerExamForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return formPage(form, model, true);
        try {
            managerExamService.update(examId, form);
            redirectAttributes.addFlashAttribute("examSuccess", "Đã cập nhật đề thi.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            model.addAttribute("examError", exception.getMessage());
            return formPage(form, model, true);
        }
        return "redirect:/manager/exams";
    }

    private String formPage(ManagerExamForm form, Model model, boolean isEdit) {
        model.addAttribute("examForm", form);
        model.addAttribute("options", managerExamService.options());
        model.addAttribute("isEdit", isEdit);
        return "pages/manager/exam/form";
    }

    private ManagerExamForm defaultForm() {
        return new ManagerExamForm();
    }
}
