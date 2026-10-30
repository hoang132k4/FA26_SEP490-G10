package com.jlpp.modules.flashcard.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.flashcard.dto.FlashcardDeckDto;
import com.jlpp.modules.flashcard.dto.FlashcardRequest;
import com.jlpp.modules.flashcard.dto.FlashcardReviewRequest;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;

    /**
     * 1. View Flashcard List (System Decks vs Learner Decks)
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "system") String tab,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        Long currentUserId = userDetails != null ? userDetails.getId() : null;
        List<FlashcardDeckDto> systemDecks = flashcardService.getSystemFlashcardDecks(keyword, level);
        List<FlashcardDeckDto> userDecks = flashcardService.getUserFlashcardDecks(keyword, level, currentUserId);
        List<Flashcard> flashcards = flashcardService.getFlashcards(keyword, level, lessonId, status);
        Map<String, Long> stats = flashcardService.getStatistics();
        List<Lesson> lessons = lessonRepository.findAll();

        model.addAttribute("systemDecks", systemDecks);
        model.addAttribute("userDecks", userDecks);
        model.addAttribute("totalSystemDecks", systemDecks.size());
        model.addAttribute("totalUserDecks", userDecks.size());
        model.addAttribute("activeTab", tab != null ? tab : "system");
        model.addAttribute("flashcards", flashcards);
        model.addAttribute("stats", stats);
        model.addAttribute("lessons", lessons);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedLevel", level != null ? level : "ALL");
        model.addAttribute("selectedLessonId", lessonId);
        model.addAttribute("selectedStatus", status != null ? status : "ALL");

        return "pages/flashcard/list";
    }

    /**
     * 2. View Flashcard Detail
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Flashcard flashcard = flashcardService.getFlashcardById(id);
        Lesson lesson = null;
        if (flashcard.getLessonId() != null) {
            lesson = lessonRepository.findById(flashcard.getLessonId()).orElse(null);
        }

        model.addAttribute("flashcard", flashcard);
        model.addAttribute("lesson", lesson);
        return "pages/flashcard/detail";
    }

    /**
     * 3. Create Flashcard - View Form
     */
    @GetMapping("/create")
    public String createForm(
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String deckName,
            Model model
    ) {
        FlashcardRequest request = FlashcardRequest.builder()
                .lessonId(lessonId)
                .jlptLevel(level != null && !level.isBlank() ? level : "N4")
                .customDeckName(deckName)
                .reviewStatus("NEW")
                .build();

        List<Lesson> lessons = lessonRepository.findAll();
        model.addAttribute("flashcardRequest", request);
        model.addAttribute("initialDeckName", deckName != null ? deckName : "");
        model.addAttribute("lessons", lessons);
        model.addAttribute("isEdit", false);
        return "pages/flashcard/form";
    }

    /**
     * 3. Create Flashcard - Handle Submit
     */
    @PostMapping("/create")
    public String createSubmit(
            @Valid @ModelAttribute("flashcardRequest") FlashcardRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("lessons", lessonRepository.findAll());
            model.addAttribute("isEdit", false);
            return "pages/flashcard/form";
        }

        Long creatorId = userDetails != null ? userDetails.getId() : null;
        Flashcard created = flashcardService.createFlashcard(request, creatorId);
        redirectAttributes.addFlashAttribute("successMessage",
                "Tạo thẻ Flashcard mới '" + created.getVocabulary() + "' thành công!");
        return "redirect:/flashcards?tab=user";
    }

    /**
     * 4. Update Flashcard - View Form
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Flashcard flashcard = flashcardService.getFlashcardById(id);
        FlashcardRequest request = FlashcardRequest.builder()
                .flashcardId(flashcard.getFlashcardId())
                .lessonId(flashcard.getLessonId())
                .vocabulary(flashcard.getVocabulary())
                .reading(flashcard.getReading())
                .meaning(flashcard.getMeaning())
                .frontContent(flashcard.getFrontContent())
                .backContent(flashcard.getBackContent())
                .exampleSentence(flashcard.getExampleSentence())
                .jlptLevel(flashcard.getJlptLevel())
                .reviewStatus(flashcard.getReviewStatus())
                .build();

        List<Lesson> lessons = lessonRepository.findAll();
        model.addAttribute("flashcardRequest", request);
        model.addAttribute("flashcardId", id);
        model.addAttribute("lessons", lessons);
        model.addAttribute("isEdit", true);
        return "pages/flashcard/form";
    }

    /**
     * 4. Update Flashcard - Handle Submit
     */
    @PostMapping("/{id}/edit")
    public String editSubmit(
            @PathVariable Long id,
            @Valid @ModelAttribute("flashcardRequest") FlashcardRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("flashcardId", id);
            model.addAttribute("lessons", lessonRepository.findAll());
            model.addAttribute("isEdit", true);
            return "pages/flashcard/form";
        }

        Flashcard updated = flashcardService.updateFlashcard(id, request);
        redirectAttributes.addFlashAttribute("successMessage",
                "Cập nhật thẻ Flashcard '" + updated.getVocabulary() + "' thành công!");
        return "redirect:/flashcards/" + id;
    }

    /**
     * Delete Flashcard
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Flashcard flashcard = flashcardService.getFlashcardById(id);
        String vocab = flashcard.getVocabulary();
        flashcardService.deleteFlashcard(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã xóa thẻ Flashcard '" + vocab + "' thành công!");
        return "redirect:/flashcards";
    }

    /**
     * 5. Practice Flashcard - Interactive Study (Quizlet style with Flashcards, Match Game, and Test mode)
     */
    @GetMapping("/practice")
    public String practice(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String deckName,
            @RequestParam(defaultValue = "flashcard") String mode,
            @RequestParam(defaultValue = "24") Integer limit,
            Model model
    ) {
        List<Flashcard> cards = flashcardService.getPracticeFlashcards(level, lessonId, deckName, limit);
        List<Lesson> lessons = lessonRepository.findAll();
        Map<String, Long> stats = flashcardService.getStatistics();

        String cardsJson = "[]";
        try {
            cardsJson = objectMapper.writeValueAsString(cards);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize flashcards to JSON: {}", e.getMessage());
        }

        model.addAttribute("cards", cards);
        model.addAttribute("cardsJson", cardsJson);
        model.addAttribute("lessons", lessons);
        model.addAttribute("selectedLevel", level != null ? level : "ALL");
        model.addAttribute("selectedLessonId", lessonId);
        model.addAttribute("selectedDeckName", deckName);
        model.addAttribute("stats", stats);
        model.addAttribute("totalCards", cards.size());
        model.addAttribute("currentMode", mode);

        return "pages/flashcard/practice";
    }

    /**
     * 6. Match Game Mode (Dedicated URL for Quizlet-style Match Game)
     */
    @GetMapping("/match")
    public String match(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String deckName,
            @RequestParam(defaultValue = "16") Integer limit,
            Model model
    ) {
        return practice(level, lessonId, deckName, "match", limit, model);
    }

    /**
     * 5. Practice Flashcard - AJAX Update Spaced Repetition Rating
     */
    @PostMapping("/practice/review")
    @ResponseBody
    public ResponseEntity<?> updateReviewStatus(@RequestBody FlashcardReviewRequest request) {
        if (request == null || request.getFlashcardId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu flashcardId"));
        }
        Flashcard updated = flashcardService.updateReviewStatus(request.getFlashcardId(), request.getStatus());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "flashcardId", updated.getFlashcardId(),
                "reviewStatus", updated.getReviewStatus()
        ));
    }

    /**
     * 7. Bulk Import Flashcards (from the Bulk Import form panel)
     * Accepts a JSON payload string of vocabulary entries and creates cards in batch.
     */
    @PostMapping("/import-bulk")
    public String importBulk(
            @RequestParam(name = "payload", required = false) String payload,
            @RequestParam(name = "customDeckName", required = false) String customDeckName,
            @RequestParam(name = "jlptLevel", defaultValue = "N4") String jlptLevel,
            @RequestParam(name = "lessonId", required = false) Long lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        if (payload == null || payload.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có dữ liệu để nhập. Hãy thử lại.");
            return "redirect:/flashcards/create";
        }

        Long creatorId = userDetails != null ? userDetails.getId() : null;
        int successCount = 0;
        int failCount = 0;

        try {
            // Parse JSON array: [{vocabulary, reading, meaning, exampleSentence}, ...]
            List<Map<String, String>> entries = objectMapper.readValue(payload,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            for (Map<String, String> entry : entries) {
                String vocab = entry.getOrDefault("vocabulary", "").trim();
                String meaning = entry.getOrDefault("meaning", "").trim();
                if (vocab.isEmpty() || meaning.isEmpty()) { failCount++; continue; }

                FlashcardRequest req = FlashcardRequest.builder()
                        .vocabulary(vocab)
                        .reading(entry.getOrDefault("reading", ""))
                        .meaning(meaning)
                        .exampleSentence(entry.getOrDefault("exampleSentence", ""))
                        .jlptLevel(jlptLevel)
                        .lessonId(lessonId)
                        .customDeckName(customDeckName != null && !customDeckName.isBlank() ? customDeckName : null)
                        .reviewStatus("NEW")
                        .build();

                try {
                    flashcardService.createFlashcard(req, creatorId);
                    successCount++;
                } catch (Exception ex) {
                    log.warn("Bulk import: failed to create card for '{}': {}", vocab, ex.getMessage());
                    failCount++;
                }
            }
        } catch (Exception ex) {
            log.error("Bulk import: failed to parse payload: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ. Hãy kiểm tra lại định dạng JSON.");
            return "redirect:/flashcards/create";
        }

        if (successCount > 0) {
            String msg = "Đã nhập thành công " + successCount + " thẻ Flashcard!";
            if (failCount > 0) msg += " (" + failCount + " dòng bị lỗi bỏ qua)";
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có thẻ nào được nhập thành công. Hãy kiểm tra lại dữ liệu.");
        }
        return "redirect:/flashcards?tab=user";
    }

    /**
     * 8. Edit Deck Info - Show Form
     */
    @GetMapping("/deck-edit")
    public String deckEditForm(
            @RequestParam(name = "deckName") String deckName,
            @RequestParam(name = "jlptLevel", required = false) String jlptLevel,
            Model model
    ) {
        model.addAttribute("deckName", deckName);
        model.addAttribute("jlptLevel", jlptLevel != null ? jlptLevel : "N4");
        return "pages/flashcard/deck-edit";
    }

    /**
     * 8. Edit Deck Info - Handle Submit
     */
    @PostMapping("/deck-edit")
    public String deckEditSubmit(
            @RequestParam(name = "oldDeckName") String oldDeckName,
            @RequestParam(name = "newDeckName") String newDeckName,
            @RequestParam(name = "jlptLevel") String jlptLevel,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        Long creatorId = userDetails != null ? userDetails.getId() : null;
        int updated = flashcardService.updateDeckInfo(oldDeckName, newDeckName, jlptLevel, creatorId);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã cập nhật gói \"" + newDeckName + "\" (" + updated + " thẻ) thành công!");
        return "redirect:/flashcards?tab=user";
    }
}

