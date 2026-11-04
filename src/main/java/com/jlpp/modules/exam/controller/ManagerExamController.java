package com.jlpp.modules.exam.controller;

import com.jlpp.common.constants.ExamSectionEnum;
import com.jlpp.common.constants.JlptLevelEnum;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.exam.dto.ExamQuestionForm;
import com.jlpp.modules.exam.dto.ManagerExamForm;
import com.jlpp.modules.exam.service.ManagerExamService;
import com.jlpp.modules.question.controller.ManagerQuestionController;
import com.jlpp.modules.question.dto.QuestionImportException;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Controller
@RequestMapping("/manager/exams")
public class ManagerExamController {

    private final ManagerExamService managerExamService;

    public ManagerExamController(ManagerExamService managerExamService) {
        this.managerExamService = managerExamService;
    }

    // ── Exam CRUD ──

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
        model.addAttribute("examForm", new ManagerExamForm());
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
            var examQuestions = managerExamService.getExamQuestions(examId);
            model.addAttribute("examQuestions", examQuestions);
            model.addAttribute("linkedCount", managerExamService.countExamQuestions(examId));
            model.addAttribute("totalPoints", examQuestions.stream().mapToInt(eq -> eq.points() != null ? eq.points() : 0).sum());
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

    // ── Exam-Question Management ──

    @GetMapping("/{examId}/questions")
    public String questionsPage(@PathVariable long examId,
                                @RequestParam(defaultValue = "") String jlptLevel,
                                @RequestParam(defaultValue = "") String skillType,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            var exam = managerExamService.findById(examId);
            model.addAttribute("exam", exam);
            model.addAttribute("examQuestions", managerExamService.getExamQuestions(examId));
            model.addAttribute("availableQuestions", managerExamService.findAvailableQuestions(examId, jlptLevel, skillType));
            model.addAttribute("addForm", new ExamQuestionForm());
            model.addAttribute("sections", Arrays.stream(ExamSectionEnum.values()).map(Enum::name).toList());
            model.addAttribute("jlptLevels", Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList());
            model.addAttribute("skillTypes", Arrays.stream(SkillTypeEnum.values()).map(Enum::name).toList());
            model.addAttribute("filterJlptLevel", jlptLevel);
            model.addAttribute("filterSkillType", skillType);
            return "pages/manager/exam/questions";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
            return "redirect:/manager/exams";
        }
    }

    @PostMapping("/{examId}/questions/add")
    public String addQuestions(@PathVariable long examId,
                               @ModelAttribute("addForm") ExamQuestionForm form,
                               RedirectAttributes redirectAttributes) {
        try {
            managerExamService.addQuestionsToExam(examId, form);
            redirectAttributes.addFlashAttribute("examSuccess", "Đã thêm câu hỏi vào đề thi.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
        }
        return "redirect:/manager/exams/" + examId + "/questions";
    }

    @PostMapping("/{examId}/questions/{questionId}/move-up")
    public String moveUp(@PathVariable long examId, @PathVariable long questionId,
                          RedirectAttributes redirectAttributes) {
        try {
            managerExamService.moveQuestion(examId, questionId, "up");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
        }
        return "redirect:/manager/exams/" + examId + "/questions";
    }

    @PostMapping("/{examId}/questions/{questionId}/move-down")
    public String moveDown(@PathVariable long examId, @PathVariable long questionId,
                            RedirectAttributes redirectAttributes) {
        try {
            managerExamService.moveQuestion(examId, questionId, "down");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
        }
        return "redirect:/manager/exams/" + examId + "/questions";
    }

    @PostMapping("/{examId}/questions/{questionId}/remove")
    public String removeQuestion(@PathVariable long examId, @PathVariable long questionId,
                                  RedirectAttributes redirectAttributes) {
        try {
            managerExamService.removeQuestionFromExam(examId, questionId);
            redirectAttributes.addFlashAttribute("examSuccess", "Đã gỡ câu hỏi khỏi đề thi.");
        } catch (IllegalArgumentException | DataAccessException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
        }
        return "redirect:/manager/exams/" + examId + "/questions";
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        return ManagerQuestionController.excelResponse(managerExamService.importTemplate(), "mau-import-de-thi.xlsx");
    }

    @PostMapping("/{examId}/import")
    public String importQuestions(@PathVariable long examId, @RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {
        try {
            int count = managerExamService.importQuestions(examId, file);
            redirectAttributes.addFlashAttribute("examSuccess", "Đã import và gắn " + count + " câu hỏi vào đề thi.");
        } catch (QuestionImportException exception) {
            redirectAttributes.addFlashAttribute("examError", "Import thất bại, chưa có dữ liệu nào được lưu. Sửa các lỗi sau rồi thử lại:");
            redirectAttributes.addFlashAttribute("importErrors", exception.getErrors());
        } catch (IllegalArgumentException | DataAccessException exception) {
            redirectAttributes.addFlashAttribute("examError", exception.getMessage());
        }
        return "redirect:/manager/exams/" + examId + "/questions";
    }

    // ── Helpers ──

    private String formPage(ManagerExamForm form, Model model, boolean isEdit) {
        model.addAttribute("examForm", form);
        model.addAttribute("options", managerExamService.options());
        model.addAttribute("isEdit", isEdit);
        return "pages/manager/exam/form";
    }
}
