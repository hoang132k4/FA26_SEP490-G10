package com.jlpp.modules.manager.flashcards.controller;

import com.jlpp.modules.lesson.entity.Lesson;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardForm;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetForm;
import com.jlpp.modules.manager.flashcards.model.ManagerFlashcard;
import com.jlpp.modules.manager.flashcards.service.ManagerFlashcardService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/manager/flashcards")
@SuppressWarnings("null")
public class ManagerFlashcardController {

    private final ManagerFlashcardService flashcardService;
    private final UserRepository userRepository;

    public ManagerFlashcardController(ManagerFlashcardService flashcardService, UserRepository userRepository) {
        this.flashcardService = flashcardService;
        this.userRepository = userRepository;
    }

    @InitBinder("flashcardSetForm")
    void configureFlashcardSetBinding(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(256);
    }

    @GetMapping
    public String list(Model model) {
        loadFlashcards(model);
        loadLessons(model);
        model.addAttribute("flashcardForm", model.containsAttribute("flashcardForm")
                ? model.getAttribute("flashcardForm") : null);
        return "pages/manager/flashcard/manager-flashcards";
    }

    @GetMapping("/new")
    public String newFlashcardSet(Model model) {
        model.addAttribute("flashcardSetForm", new ManagerFlashcardSetForm());
        loadLessons(model);
        return "pages/manager/flashcard/manager-flashcard-create";
    }

    @PostMapping("/sets")
    public String createFlashcardSet(@Valid @ModelAttribute("flashcardSetForm") ManagerFlashcardSetForm form,
                                     BindingResult bindingResult, Principal principal, HttpSession session,
                                     Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcard-create";
        }

        try {
            List<ManagerFlashcard> cards = flashcardService.createFlashcardSet(form, currentUserId(principal, session));
            redirectAttributes.addFlashAttribute("flashcardSuccess",
                    "Đã tạo bộ thẻ “" + form.getDeckName().trim() + "” gồm " + cards.size() + " flashcard.");
            return "redirect:/manager/flashcards";
        } catch (IllegalArgumentException exception) {
            reject(bindingResult, exception, "Thông tin bộ thẻ không hợp lệ.");
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcard-create";
        } catch (IllegalStateException exception) {
            log.error("Could not resolve the manager creating flashcards", exception);
            bindingResult.reject("flashcard.creator", "Không xác định được tài khoản quản lý hiện tại.");
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcard-create";
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not create flashcard set '{}'", form.getDeckName(), exception);
            bindingResult.reject("flashcard.database",
                    "Không thể lưu bộ thẻ vào database. Chi tiết lỗi đã được ghi vào log ứng dụng.");
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcard-create";
        }
    }

    @GetMapping("/{flashcardId}/edit")
    public String editFlashcard(@PathVariable long flashcardId, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Optional<ManagerFlashcard> flashcard = flashcardService.findFlashcard(flashcardId);
            if (flashcard.isEmpty()) {
                redirectAttributes.addFlashAttribute("flashcardError", "Không tìm thấy flashcard cần cập nhật.");
                return "redirect:/manager/flashcards";
            }
            model.addAttribute("flashcardForm", toForm(flashcard.get()));
            loadFlashcards(model);
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcards";
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not load flashcard {} for editing", flashcardId, exception);
            redirectAttributes.addFlashAttribute("flashcardError",
                    "Không thể đọc flashcard cần sửa từ database. Chi tiết lỗi đã được ghi vào log ứng dụng.");
            return "redirect:/manager/flashcards";
        }
    }

    @PostMapping("/{flashcardId}")
    public String updateFlashcard(@PathVariable long flashcardId,
                                  @Valid @ModelAttribute("flashcardForm") ManagerFlashcardForm form,
                                  BindingResult bindingResult, Model model,
                                  RedirectAttributes redirectAttributes) {
        form.setFlashcardId(flashcardId);
        if (bindingResult.hasErrors()) {
            loadFlashcards(model);
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcards";
        }
        try {
            flashcardService.updateFlashcard(flashcardId, form);
            redirectAttributes.addFlashAttribute("flashcardSuccess", "Đã cập nhật flashcard.");
            return "redirect:/manager/flashcards";
        } catch (IllegalArgumentException exception) {
            reject(bindingResult, exception, "Thông tin flashcard không hợp lệ.");
            loadFlashcards(model);
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcards";
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not update flashcard {}", flashcardId, exception);
            bindingResult.reject("flashcard.database",
                    "Không thể cập nhật flashcard trong database. Chi tiết lỗi đã được ghi vào log ứng dụng.");
            loadFlashcards(model);
            loadLessons(model);
            return "pages/manager/flashcard/manager-flashcards";
        }
    }

    private void loadFlashcards(Model model) {
        try {
            List<ManagerFlashcard> flashcards = flashcardService.findFlashcards();
            model.addAttribute("flashcards", flashcards);
            model.addAttribute("flashcardCount", flashcards.size());
            model.addAttribute("flashcardListAvailable", true);
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not read rows from Supabase table 'flashcards'", exception);
            model.addAttribute("flashcards", List.of());
            model.addAttribute("flashcardCount", null);
            model.addAttribute("flashcardListAvailable", false);
            model.addAttribute("flashcardError",
                    "Không đọc được bảng flashcards trong database. Danh sách không thể hiển thị; xem log ứng dụng để biết lỗi SQL.");
        }
    }

    private void loadLessons(Model model) {
        try {
            List<LessonOption> lessons = flashcardService.findLessons().stream()
                    .map(this::toLessonOption)
                    .toList();
            Map<Long, String> lessonTitles = new LinkedHashMap<>();
            for (LessonOption lesson : lessons) {
                lessonTitles.put(lesson.id(), lesson.title());
            }
            model.addAttribute("lessons", lessons);
            model.addAttribute("lessonTitles", lessonTitles);
            model.addAttribute("lessonsAvailable", true);
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not read lessons for manager flashcards", exception);
            model.addAttribute("lessons", List.of());
            model.addAttribute("lessonTitles", Map.of());
            model.addAttribute("lessonsAvailable", false);
            model.addAttribute("lessonsError",
                    "Không tải được danh sách bài học. Vui lòng kiểm tra bảng lessons hoặc kết nối database.");
        }
    }

    private ManagerFlashcardForm toForm(ManagerFlashcard flashcard) {
        ManagerFlashcardForm form = new ManagerFlashcardForm();
        form.setFlashcardId(flashcard.getFlashcardId());
        form.setLessonId(flashcard.getLessonId());
        form.setVocabulary(flashcard.getVocabulary());
        form.setReading(flashcard.getReading());
        form.setMeaning(flashcard.getMeaning());
        form.setFrontContent(flashcard.getFrontContent());
        form.setBackContent(flashcard.getBackContent());
        form.setExampleSentence(flashcard.getExampleSentence());
        form.setJlptLevel(flashcard.getJlptLevel());
        form.setReviewStatus(flashcard.getReviewStatus());
        form.setCustomDeckName(flashcard.getCustomDeckName());
        form.setCustom(flashcard.isCustom());
        return form;
    }

    private LessonOption toLessonOption(Lesson lesson) {
        String title = firstNonBlank(lesson.getTitle(), lesson.getLessonTitle());
        if (title == null) {
            title = "Bài học #" + lesson.getLessonId();
        }
        return new LessonOption(lesson.getLessonId(), title);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private long currentUserId(Principal principal, HttpSession session) {
        Object sessionUserId = session.getAttribute("CURRENT_LEARNER_ID");
        if (sessionUserId instanceof Number number) {
            return number.longValue();
        }
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            String identifier = principal.getName().trim();
            User currentUser = userRepository.findByEmailIgnoreCase(identifier).orElse(null);
            if (currentUser == null) {
                currentUser = userRepository.findByUsername(identifier).orElse(null);
            }
            if (currentUser != null && currentUser.getId() != null) {
                return currentUser.getId();
            }
        }
        throw new IllegalStateException("Không tìm thấy tài khoản hiện tại.");
    }

    private void reject(BindingResult bindingResult, IllegalArgumentException exception, String fallback) {
        String message = exception.getMessage();
        bindingResult.reject("flashcard.invalid", message == null ? fallback : message);
    }

    private record LessonOption(Long id, String title) {
    }
}
