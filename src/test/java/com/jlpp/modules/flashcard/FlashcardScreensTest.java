package com.jlpp.modules.flashcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.flashcard.dto.FlashcardReviewRequest;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FlashcardScreensTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testLessonId;
    private Flashcard testCard;

    @BeforeEach
    public void setup() {
        // Setup course and lesson in test database
        Course course = courseRepository.findById(1L).orElseGet(() -> {
            Course c = Course.builder()
                    .title("Khóa học N4 thử nghiệm")
                    .level("N4")
                    .status("PUBLISHED")
                    .price(500000.0)
                    .build();
            c.setId(1L);
            return courseRepository.saveAndFlush(c);
        });

        // Insert lesson using jdbcTemplate to be consistent across entity ID variations
        testLessonId = 88001L;
        jdbcTemplate.update("DELETE FROM flashcards WHERE lesson_id = ?", testLessonId);
        jdbcTemplate.update("DELETE FROM lessons WHERE id = ?", testLessonId);
        jdbcTemplate.update(
                "INSERT INTO lessons (id, course_id, title, order_index, lesson_type, status, duration_minutes) " +
                "VALUES (?, ?, 'Bài 1: Từ vựng N4 nền tảng', 1, 'VIDEO', 'PUBLISHED', 25)",
                testLessonId, course.getId()
        );

        // Seed test flashcard
        testCard = Flashcard.builder()
                .lessonId(testLessonId)
                .vocabulary("桜")
                .reading("さくら (sakura)")
                .meaning("Hoa anh đào")
                .exampleSentence("日本の春は桜の花がとても美しいです。")
                .jlptLevel("N4")
                .reviewStatus("NEW")
                .frontContent("Biểu tượng mùa xuân")
                .backContent("Danh từ")
                .build();
        testCard = flashcardRepository.saveAndFlush(testCard);
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("1. View Flashcard List - GET /flashcards returns list with stats and cards")
    public void testViewFlashcardList() throws Exception {
        mockMvc.perform(get("/flashcards"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/list"))
                .andExpect(model().attributeExists("flashcards", "stats", "lessons"))
                .andExpect(model().attribute("flashcards", hasItem(hasProperty("vocabulary", is("桜")))))
                .andExpect(content().string(containsString("Gói Flashcard Từ Vựng")))
                .andExpect(content().string(containsString("桜")))
                .andExpect(content().string(containsString("Hoa anh đào")));
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("2. View Flashcard Detail - GET /flashcards/{id} returns card details and pronunciation")
    public void testViewFlashcardDetail() throws Exception {
        mockMvc.perform(get("/flashcards/" + testCard.getFlashcardId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/detail"))
                .andExpect(model().attributeExists("flashcard"))
                .andExpect(model().attribute("flashcard", hasProperty("vocabulary", is("桜"))))
                .andExpect(content().string(containsString("Chi tiết thẻ Flashcard")))
                .andExpect(content().string(containsString("さくら (sakura)")))
                .andExpect(content().string(containsString("Nghe phát âm tiếng Nhật")));
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("3. Create Flashcard - GET /flashcards/create renders form, POST /flashcards/create saves card")
    public void testCreateFlashcard() throws Exception {
        // GET Form
        mockMvc.perform(get("/flashcards/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attributeExists("flashcardRequest", "lessons"));

        // POST Submit
        mockMvc.perform(post("/flashcards/create")
                        .with(csrf())
                        .param("lessonId", testLessonId.toString())
                        .param("vocabulary", "準備")
                        .param("reading", "じゅんび")
                        .param("meaning", "Sự chuẩn bị")
                        .param("exampleSentence", "旅行の準備をする。")
                        .param("jlptLevel", "N4")
                        .param("reviewStatus", "NEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        // Verify in DB
        boolean exists = flashcardRepository.findAll().stream()
                .anyMatch(c -> "準備".equals(c.getVocabulary()) && "Sự chuẩn bị".equals(c.getMeaning()));
        assertTrue(exists, "Newly created flashcard '準備' must exist in DB");
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("4. Update Flashcard - GET /flashcards/{id}/edit renders form, POST /flashcards/{id}/edit updates card")
    public void testUpdateFlashcard() throws Exception {
        // GET Edit Form
        mockMvc.perform(get("/flashcards/" + testCard.getFlashcardId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attributeExists("flashcardRequest", "flashcardId"));

        // POST Edit Submit
        mockMvc.perform(post("/flashcards/" + testCard.getFlashcardId() + "/edit")
                        .with(csrf())
                        .param("lessonId", testLessonId.toString())
                        .param("vocabulary", "桜 (Sakura)")
                        .param("reading", "さくら")
                        .param("meaning", "Hoa anh đào Nhật Bản (Cập nhật)")
                        .param("exampleSentence", "春の桜。")
                        .param("jlptLevel", "N4")
                        .param("reviewStatus", "REVIEWING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/" + testCard.getFlashcardId()))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify update in DB
        Flashcard updated = flashcardRepository.findById(testCard.getFlashcardId()).orElseThrow();
        assertEquals("桜 (Sakura)", updated.getVocabulary());
        assertEquals("Hoa anh đào Nhật Bản (Cập nhật)", updated.getMeaning());
        assertEquals("REVIEWING", updated.getReviewStatus());
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("5. Practice Flashcard - GET /flashcards/practice loads cards, POST review updates status via AJAX")
    public void testPracticeFlashcard() throws Exception {
        // GET Practice view
        mockMvc.perform(get("/flashcards/practice"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/practice"))
                .andExpect(model().attributeExists("cards", "cardsJson", "totalCards"))
                .andExpect(content().string(containsString("Flashcard phong cách Quizlet")));

        // POST AJAX review rating
        FlashcardReviewRequest reviewReq = new FlashcardReviewRequest(testCard.getFlashcardId(), "MASTERED");
        mockMvc.perform(post("/flashcards/practice/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.reviewStatus", is("MASTERED")));

        // Verify status in DB updated to MASTERED
        Flashcard updated = flashcardRepository.findById(testCard.getFlashcardId()).orElseThrow();
        assertEquals("MASTERED", updated.getReviewStatus());
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("6. Learner Custom Deck & Flashcards - Bulk import without lessonId, view and delete deck")
    public void testLearnerCustomDeckLifecycle() throws Exception {
        String payload = "[{\"vocabulary\":\"食べる\",\"reading\":\"たべる\",\"meaning\":\"Ăn\",\"exampleSentence\":\"ご飯を食べる。\"}," +
                "{\"vocabulary\":\"飲む\",\"reading\":\"のむ\",\"meaning\":\"Uống\",\"exampleSentence\":\"水を飲む。\"}]";

        // POST /flashcards/import-bulk without lessonId
        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("customDeckName", "Bộ từ N5 tự tạo")
                        .param("jlptLevel", "N5")
                        .param("payload", payload))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify in DB
        List<Flashcard> cards = flashcardRepository.findByCustomDeckName("Bộ từ N5 tự tạo");
        assertEquals(2, cards.size(), "2 custom cards should be inserted without lessonId");
        assertTrue(cards.stream().allMatch(c -> Boolean.TRUE.equals(c.getIsCustom())));
        assertNull(cards.get(0).getLessonId());

        // Delete Deck
        mockMvc.perform(post("/flashcards/deck-delete")
                        .with(csrf())
                        .param("deckName", "Bộ từ N5 tự tạo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Flashcard> afterDelete = flashcardRepository.findByCustomDeckName("Bộ từ N5 tự tạo");
        assertTrue(afterDelete.isEmpty(), "Custom deck should be completely deleted");
    }

    @Test
    @WithMockUser(username = "learner@jeps.edu.vn", roles = {"LEARNER"})
    @DisplayName("7. Full Deck Edit - GET deck-edit loads cards, POST deck-edit modifies existing, adds new, deletes removed")
    public void testFullDeckEditLifecycle() throws Exception {
        // Seed 2 cards in a deck
        Flashcard cardA = flashcardRepository.saveAndFlush(Flashcard.builder()
                .vocabulary("本")
                .reading("ほん")
                .meaning("Sách")
                .customDeckName("Gói ôn tập N5")
                .jlptLevel("N5")
                .isCustom(true)
                .reviewStatus("NEW")
                .build());

        Flashcard cardB = flashcardRepository.saveAndFlush(Flashcard.builder()
                .vocabulary("机")
                .reading("つくえ")
                .meaning("Cái bàn")
                .customDeckName("Gói ôn tập N5")
                .jlptLevel("N5")
                .isCustom(true)
                .reviewStatus("NEW")
                .build());

        // 1. GET /deck-edit
        mockMvc.perform(get("/flashcards/deck-edit")
                        .param("deckName", "Gói ôn tập N5")
                        .param("jlptLevel", "N5"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/deck-edit"))
                .andExpect(model().attributeExists("cards", "deckName", "jlptLevel"))
                .andExpect(model().attribute("deckName", "Gói ôn tập N5"))
                .andExpect(model().attribute("totalCards", 2));

        // 2. POST /deck-edit: modify Card A, add Card C, delete Card B
        String cardsPayload = "[" +
                "{\"flashcardId\":" + cardA.getFlashcardId() + ",\"vocabulary\":\"本 (Hon)\",\"reading\":\"ほん\",\"meaning\":\"Quyển sách\",\"exampleSentence\":\"\"}," +
                "{\"flashcardId\":null,\"vocabulary\":\"椅子\",\"reading\":\"いす\",\"meaning\":\"Cái ghế\",\"exampleSentence\":\"椅子に座る。\"}" +
                "]";
        String deletedIds = "[" + cardB.getFlashcardId() + "]";

        mockMvc.perform(post("/flashcards/deck-edit")
                        .with(csrf())
                        .param("oldDeckName", "Gói ôn tập N5")
                        .param("newDeckName", "Gói ôn tập N5 Nâng cao")
                        .param("jlptLevel", "N5")
                        .param("cardsPayload", cardsPayload)
                        .param("deletedCardIds", deletedIds))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        // 3. Verify in DB
        // Card B is deleted
        assertFalse(flashcardRepository.findById(cardB.getFlashcardId()).isPresent(), "Card B should be deleted");

        // Card A is updated
        Flashcard updatedCardA = flashcardRepository.findById(cardA.getFlashcardId()).orElseThrow();
        assertEquals("本 (Hon)", updatedCardA.getVocabulary());
        assertEquals("Quyển sách", updatedCardA.getMeaning());
        assertEquals("Gói ôn tập N5 Nâng cao", updatedCardA.getCustomDeckName());

        // Card C is created
        List<Flashcard> newDeckCards = flashcardRepository.findByCustomDeckName("Gói ôn tập N5 Nâng cao");
        assertEquals(2, newDeckCards.size(), "Updated deck should contain 2 cards (Card A and Card C)");
        assertTrue(newDeckCards.stream().anyMatch(c -> "椅子".equals(c.getVocabulary())), "New card '椅子' should exist in deck");
    }
}
