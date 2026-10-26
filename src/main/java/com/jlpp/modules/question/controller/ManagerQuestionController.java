package com.jlpp.modules.question.controller;

import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.service.ManagerQuestionService;
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
@RequestMapping("/manager/questions")
public class ManagerQuestionController {

    private final ManagerQuestionService managerQuestionService;

    public ManagerQuestionController(ManagerQuestionService managerQuestionService) {
        this.managerQuestionService = managerQuestionService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(required = false) Long examId,
                       Model model) {
        model.addAttribute("questions", managerQuestionService.search(search, category, examId));
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("examId", examId);
        addOptions(model);
        model.addAttribute("questionCount", managerQuestionService.count());
        return "pages/manager/question/list";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("questionForm", new ManagerQuestionForm());
        addOptions(model);
        model.addAttribute("isEdit", false);
        return "pages/manager/question/form";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("questionForm", managerQuestionService.toForm(managerQuestionService.findById(id)));
            addOptions(model);
            model.addAttribute("isEdit", true);
            return "pages/manager/question/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("questionError", exception.getMessage());
            return "redirect:/manager/questions";
        }
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("questionForm") ManagerQuestionForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return formPage(form, model, false);
        try {
            managerQuestionService.create(form);
            redirectAttributes.addFlashAttribute("questionSuccess", "Đã thêm câu hỏi.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            model.addAttribute("questionError", exception.getMessage());
            return formPage(form, model, false);
        }
        return "redirect:/manager/questions";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("questionForm") ManagerQuestionForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return formPage(form, model, true);
        try {
            managerQuestionService.update(id, form);
            redirectAttributes.addFlashAttribute("questionSuccess", "Đã cập nhật câu hỏi.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            model.addAttribute("questionError", exception.getMessage());
            return formPage(form, model, true);
        }
        return "redirect:/manager/questions";
    }

    private String formPage(ManagerQuestionForm form, Model model, boolean isEdit) {
        model.addAttribute("questionForm", form);
        addOptions(model);
        model.addAttribute("isEdit", isEdit);
        return "pages/manager/question/form";
    }

    private void addOptions(Model model) {
        model.addAttribute("categories", managerQuestionService.categories());
        model.addAttribute("exams", managerQuestionService.exams());
    }
}
