package com.jlpp.modules.quiz.controller;

import com.jlpp.modules.quiz.dto.ManagerQuizForm;
import com.jlpp.modules.quiz.service.ManagerQuizService;
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
@RequestMapping("/manager/quizzes")
public class ManagerQuizController {

    private final ManagerQuizService managerQuizService;

    public ManagerQuizController(ManagerQuizService managerQuizService) { this.managerQuizService = managerQuizService; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(required = false) Long lessonId,
                       @RequestParam(defaultValue = "") String status, Model model) {
        model.addAttribute("quizzes", managerQuizService.search(search, lessonId, status));
        model.addAttribute("search", search);
        model.addAttribute("lessonId", lessonId);
        model.addAttribute("status", status);
        addOptions(model);
        model.addAttribute("quizCount", managerQuizService.count());
        return "pages/manager/quiz/list";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("quizForm", new ManagerQuizForm());
        addOptions(model);
        model.addAttribute("isEdit", false);
        return "pages/manager/quiz/form";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("quizForm", managerQuizService.toForm(managerQuizService.findById(id)));
            addOptions(model);
            model.addAttribute("isEdit", true);
            return "pages/manager/quiz/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("quizError", exception.getMessage());
            return "redirect:/manager/quizzes";
        }
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("quizForm") ManagerQuizForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return formPage(form, model, false);
        try {
            managerQuizService.create(form);
            redirectAttributes.addFlashAttribute("quizSuccess", "Đã tạo quiz.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            model.addAttribute("quizError", exception.getMessage());
            return formPage(form, model, false);
        }
        return "redirect:/manager/quizzes";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("quizForm") ManagerQuizForm form,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return formPage(form, model, true);
        try {
            managerQuizService.update(id, form);
            redirectAttributes.addFlashAttribute("quizSuccess", "Đã cập nhật quiz.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            model.addAttribute("quizError", exception.getMessage());
            return formPage(form, model, true);
        }
        return "redirect:/manager/quizzes";
    }

    private String formPage(ManagerQuizForm form, Model model, boolean isEdit) {
        model.addAttribute("quizForm", form);
        addOptions(model);
        model.addAttribute("isEdit", isEdit);
        return "pages/manager/quiz/form";
    }

    private void addOptions(Model model) {
        model.addAttribute("lessons", managerQuizService.lessons());
        model.addAttribute("statuses", managerQuizService.statuses());
    }
}
