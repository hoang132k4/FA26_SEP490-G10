package com.jlpp.modules.question.controller;

import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.service.ManagerQuestionService;
import com.jlpp.modules.question.service.QuestionImportService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/questions")
public class ManagerQuestionController {

    private final ManagerQuestionService managerQuestionService;
    private final QuestionImportService questionImportService;

    public ManagerQuestionController(ManagerQuestionService managerQuestionService,
                                     QuestionImportService questionImportService) {
        this.managerQuestionService = managerQuestionService;
        this.questionImportService = questionImportService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "") String questionType,
                       @RequestParam(defaultValue = "") String jlptLevel,
                       @RequestParam(defaultValue = "") String skillType,
                       @RequestParam(defaultValue = "") String difficulty,
                       @RequestParam(defaultValue = "") String status,
                       Model model) {
        model.addAttribute("questions", managerQuestionService.search(search, category, questionType, jlptLevel, skillType, difficulty, status));
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("questionType", questionType);
        model.addAttribute("jlptLevel", jlptLevel);
        model.addAttribute("skillType", skillType);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("status", status);
        addOptions(model);
        model.addAttribute("questionCount", managerQuestionService.count());
        return "pages/manager/question/list";
    }

    @GetMapping("/new")
    public String createPage(@RequestParam(required = false) Long parentId, Model model) {
        ManagerQuestionForm form = new ManagerQuestionForm();
        if (parentId != null) {
            form.setParentId(parentId);
            form.setQuestionType("SINGLE_CHOICE");
        }
        model.addAttribute("questionForm", form);
        addOptions(model);
        model.addAttribute("isEdit", false);
        return "pages/manager/question/form";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            var question = managerQuestionService.findById(id);
            model.addAttribute("questionForm", managerQuestionService.toForm(question));
            if ("PASSAGE".equals(question.getQuestionType())) {
                model.addAttribute("children", managerQuestionService.findChildren(id));
            }
            model.addAttribute("usageStats", managerQuestionService.getUsageStats(id));
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

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            managerQuestionService.delete(id);
            redirectAttributes.addFlashAttribute("questionSuccess", "Đã xóa câu hỏi.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("questionError", exception.getMessage());
        }
        return "redirect:/manager/questions";
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        return excelResponse(questionImportService.buildTemplate(false), "mau-import-cau-hoi.xlsx");
    }

    @PostMapping("/import")
    public String importFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            int count = questionImportService.importFile(file, null).size();
            redirectAttributes.addFlashAttribute("questionSuccess", "Đã import " + count + " câu hỏi vào ngân hàng.");
        } catch (QuestionImportException exception) {
            redirectAttributes.addFlashAttribute("questionError", "Import thất bại, chưa có câu hỏi nào được lưu. Sửa các lỗi sau rồi thử lại:");
            redirectAttributes.addFlashAttribute("importErrors", exception.getErrors());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("questionError", "Lỗi lưu dữ liệu: " + exception.getMostSpecificCause().getMessage());
        }
        return "redirect:/manager/questions";
    }

    public static ResponseEntity<byte[]> excelResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    private String formPage(ManagerQuestionForm form, Model model, boolean isEdit) {
        model.addAttribute("questionForm", form);
        addOptions(model);
        model.addAttribute("isEdit", isEdit);
        return "pages/manager/question/form";
    }

    private void addOptions(Model model) {
        model.addAttribute("categories", managerQuestionService.categories());
        model.addAttribute("options", managerQuestionService.options());
    }
}
