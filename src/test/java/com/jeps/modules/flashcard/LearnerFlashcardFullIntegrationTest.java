package com.jeps.modules.flashcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.flashcard.dto.FlashcardReviewRequest;
import com.jeps.modules.flashcard.entity.Flashcard;
import com.jeps.modules.flashcard.repository.FlashcardRepository;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
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

import java.util.List;

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
public class LearnerFlashcardFullIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private User learner;
    private Long testLessonId = 97001L;
    private Flashcard systemCard;
    private Flashcard userCard;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("learner_fc_test@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_fc_test@jeps.edu.vn")
                        .username("learner_fc_test")
                        .fullName("Nguyễn Học Viên Flashcard")
                        .password("Password123@")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .currentJlptLevel("N5")
                        .targetJlptLevel("N4")
                        .build()));

        Course course = courseRepository.saveAndFlush(Course.builder()
                .id(8301L)
                .title("Khóa học Flashcard N4")
                .level("N4")
                .status("PUBLISHED")
                .price(200000.0)
                .build());

        jdbcTemplate.update("""
            INSERT INTO lessons (lesson_id, course_id, title, order_index, section_name, duration_minutes, lesson_type, content)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, testLessonId, course.getId(), "Bài 1: Từ vựng Flashcard", 1, "Chương 1", 20, "VOCABULARY", "Nội dung từ vựng");

        // 1. System flashcard (isCustom = false or null)
        systemCard = flashcardRepository.saveAndFlush(Flashcard.builder()
                .lessonId(testLessonId)
                .vocabulary("勉強")
                .reading("べんきょう")
                .meaning("Học tập")
                .exampleSentence("毎日日本語を勉強します。")
                .jlptLevel("N4")
                .reviewStatus("NEW")
                .isCustom(false)
                .build());

        // 2. Learner custom flashcard
        userCard = flashcardRepository.saveAndFlush(Flashcard.builder()
                .vocabulary("友達")
                .reading("ともだち")
                .meaning("Bạn bè")
                .exampleSentence("友達と映画を見ました。")
                .jlptLevel("N4")
                .customDeckName("Từ vựng giao tiếp cá nhân")
                .creatorId(learner.getId())
                .reviewStatus("LEARNING")
                .isCustom(true)
                .build());
    }

    // ==========================================
    // 1. VIEW FLASHCARD LIST & FILTER (/flashcards)
    // ==========================================

    @Test
    @DisplayName("Flashcard List: Tab Hệ thống hiển thị các bộ thẻ có sẵn và thống kê")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewFlashcardList_SystemTab() throws Exception {
        mockMvc.perform(get("/flashcards").param("tab", "system"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/list"))
                .andExpect(model().attributeExists("systemDecks", "userDecks", "stats"))
                .andExpect(model().attribute("activeTab", is("system")))
                .andExpect(model().attribute("systemDecks", hasItem(hasProperty("cards", hasItem(hasProperty("vocabulary", is("勉強")))))));
    }

    @Test
    @DisplayName("Flashcard List: Tab Cá nhân hiển thị bộ thẻ do học viên tự tạo")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewFlashcardList_UserTab() throws Exception {
        mockMvc.perform(get("/flashcards").param("tab", "user"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/list"))
                .andExpect(model().attribute("activeTab", is("user")))
                .andExpect(model().attributeExists("userDecks"));
    }

    @Test
    @DisplayName("Flashcard List: Tìm kiếm từ vựng theo từ khóa và cấp độ JLPT")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewFlashcardList_FilterKeywordAndLevel() throws Exception {
        mockMvc.perform(get("/flashcards")
                        .param("keyword", "勉強")
                        .param("level", "N4"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/list"))
                .andExpect(model().attribute("systemDecks", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(model().attribute("systemDecks", hasItem(hasProperty("cards", hasItem(hasProperty("vocabulary", is("勉強")))))));
    }

    // ==========================================
    // 2. FLASHCARD DETAIL (/flashcards/{id})
    // ==========================================

    @Test
    @DisplayName("Flashcard Detail: Hiển thị chi tiết từ vựng, cách đọc Furigana và ví dụ minh họa")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testViewFlashcardDetail_Success() throws Exception {
        mockMvc.perform(get("/flashcards/{id}", systemCard.getFlashcardId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/detail"))
                .andExpect(model().attributeExists("flashcard", "lesson"))
                .andExpect(model().attribute("flashcard", hasProperty("vocabulary", is("勉強"))))
                .andExpect(model().attribute("flashcard", hasProperty("reading", is("べんきょう"))));
    }

    // ==========================================
    // 3. PRACTICE & MATCH GAME MODE (/flashcards/practice & /flashcards/match)
    // ==========================================

    @Test
    @DisplayName("Practice Mode: Chế độ lật thẻ trực quan phong cách Quizlet (Flashcard Mode)")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testPracticeMode_LoadsCardsAndSerializesJson() throws Exception {
        mockMvc.perform(get("/flashcards/practice")
                        .param("level", "N4")
                        .param("mode", "flashcard"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/practice"))
                .andExpect(model().attributeExists("cards", "cardsJson", "totalCards"))
                .andExpect(model().attribute("currentMode", is("flashcard")))
                .andExpect(model().attribute("totalCards", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Match Game Mode: URL chuyên biệt /flashcards/match chuyển sang chế độ ghép thẻ")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testMatchGameMode_DedicatedUrl() throws Exception {
        mockMvc.perform(get("/flashcards/match").param("level", "N4"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/practice"))
                .andExpect(model().attribute("currentMode", is("match")));
    }

    // ==========================================
    // 4. AJAX REVIEW STATUS (/flashcards/practice/review)
    // ==========================================

    @Test
    @DisplayName("Review Status AJAX: Cập nhật đánh giá Spaced Repetition thành MASTERED")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testUpdateReviewStatus_Success() throws Exception {
        FlashcardReviewRequest req = new FlashcardReviewRequest(systemCard.getFlashcardId(), "MASTERED");

        mockMvc.perform(post("/flashcards/practice/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.flashcardId", is(systemCard.getFlashcardId().intValue())))
                .andExpect(jsonPath("$.reviewStatus", is("MASTERED")));

        Flashcard updated = flashcardRepository.findById(systemCard.getFlashcardId()).orElseThrow();
        assertEquals("MASTERED", updated.getReviewStatus());
    }

    @Test
    @DisplayName("Review Status AJAX: Yêu cầu thiếu ID sẽ trả về 400 Bad Request")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testUpdateReviewStatus_MissingId_BadRequest() throws Exception {
        FlashcardReviewRequest req = new FlashcardReviewRequest(null, "MASTERED");

        mockMvc.perform(post("/flashcards/practice/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    // ==========================================
    // 5. CRUD FLASHCARD (/flashcards/create, edit, delete)
    // ==========================================

    @Test
    @DisplayName("Create Flashcard: Hiển thị form tạo thẻ và submit thành công thẻ cá nhân mới")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCreateFlashcard_FormAndSubmitSuccess() throws Exception {
        // GET Form
        mockMvc.perform(get("/flashcards/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attribute("isEdit", is(false)));

        // POST Submit
        mockMvc.perform(post("/flashcards/create")
                        .with(csrf())
                        .param("vocabulary", "先生")
                        .param("reading", "せんせい")
                        .param("meaning", "Giáo viên")
                        .param("exampleSentence", "先生に質問します。")
                        .param("jlptLevel", "N5")
                        .param("customDeckName", "Bộ từ N5 tự tạo")
                        .param("reviewStatus", "NEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        assertTrue(flashcardRepository.findAll().stream()
                .anyMatch(c -> "先生".equals(c.getVocabulary()) && "Giáo viên".equals(c.getMeaning())));
    }

    @Test
    @DisplayName("Create Flashcard: Báo lỗi validation khi để trống từ vựng hoặc ý nghĩa")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testCreateFlashcard_ValidationError() throws Exception {
        mockMvc.perform(post("/flashcards/create")
                        .with(csrf())
                        .param("vocabulary", "") // trống từ vựng
                        .param("meaning", "")) // trống ý nghĩa
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Edit Flashcard: Hiển thị form sửa thẻ và submit cập nhật thành công")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testEditFlashcard_FormAndSubmitSuccess() throws Exception {
        // GET Edit Form
        mockMvc.perform(get("/flashcards/{id}/edit", userCard.getFlashcardId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attribute("isEdit", is(true)))
                .andExpect(model().attribute("flashcardId", is(userCard.getFlashcardId())));

        // POST Edit Submit
        mockMvc.perform(post("/flashcards/{id}/edit", userCard.getFlashcardId())
                        .with(csrf())
                        .param("vocabulary", "友達 (Tomodachi)")
                        .param("reading", "ともだち")
                        .param("meaning", "Bạn bè thân thiết")
                        .param("jlptLevel", "N4")
                        .param("reviewStatus", "MASTERED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/" + userCard.getFlashcardId()))
                .andExpect(flash().attributeExists("successMessage"));

        Flashcard updated = flashcardRepository.findById(userCard.getFlashcardId()).orElseThrow();
        assertEquals("友達 (Tomodachi)", updated.getVocabulary());
        assertEquals("Bạn bè thân thiết", updated.getMeaning());
        assertEquals("MASTERED", updated.getReviewStatus());
    }

    @Test
    @DisplayName("Delete Flashcard: Xóa thẻ thành công khỏi hệ thống")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testDeleteFlashcard_Success() throws Exception {
        mockMvc.perform(post("/flashcards/{id}/delete", userCard.getFlashcardId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards"))
                .andExpect(flash().attributeExists("successMessage"));

        assertFalse(flashcardRepository.findById(userCard.getFlashcardId()).isPresent());
    }

    // ==========================================
    // 6. BULK IMPORT FLASHCARDS (/flashcards/import-bulk)
    // ==========================================

    @Test
    @DisplayName("Bulk Import: Nhập hàng loạt từ vựng từ JSON payload thành công")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testBulkImport_Success() throws Exception {
        String payload = "[" +
                "{\"vocabulary\":\"車\",\"reading\":\"くるま\",\"meaning\":\"Xe hơi\",\"exampleSentence\":\"車を運転する。\"}," +
                "{\"vocabulary\":\"電車\",\"reading\":\"でんしゃ\",\"meaning\":\"Tàu điện\",\"exampleSentence\":\"電車で行く。\"}" +
                "]";

        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("customDeckName", "Gói phương tiện giao thông")
                        .param("jlptLevel", "N4")
                        .param("payload", payload))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Flashcard> cards = flashcardRepository.findByCustomDeckName("Gói phương tiện giao thông");
        assertEquals(2, cards.size());
    }

    @Test
    @DisplayName("Bulk Import: Báo lỗi khi payload rỗng")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testBulkImport_EmptyPayload_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("payload", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/create"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Bulk Import: Báo lỗi định dạng khi payload JSON bị lỗi cú pháp")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testBulkImport_InvalidJson_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("payload", "{ sai định dạng json }"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/create"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ==========================================
    // 7. DECK EDITOR & DELETE DECK (/flashcards/deck-edit & deck-delete)
    // ==========================================

    @Test
    @DisplayName("Deck Editor: Tải danh sách thẻ và lưu cập nhật toàn bộ gói thẻ")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testDeckEditor_GetAndPostSubmit() throws Exception {
        // GET Deck Edit
        mockMvc.perform(get("/flashcards/deck-edit")
                        .param("deckName", "Từ vựng giao tiếp cá nhân"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/deck-edit"))
                .andExpect(model().attribute("deckName", is("Từ vựng giao tiếp cá nhân")))
                .andExpect(model().attributeExists("cards", "cardsJson"));

        // POST Deck Edit: Cập nhật tên gói
        mockMvc.perform(post("/flashcards/deck-edit")
                        .with(csrf())
                        .param("oldDeckName", "Từ vựng giao tiếp cá nhân")
                        .param("newDeckName", "Từ vựng giao tiếp Nâng cao")
                        .param("jlptLevel", "N4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Flashcard> renamed = flashcardRepository.findByCustomDeckName("Từ vựng giao tiếp Nâng cao");
        assertFalse(renamed.isEmpty());
    }

    @Test
    @DisplayName("Deck Delete: Xóa sạch toàn bộ các thẻ trong gói được chọn")
    @WithMockUser(username = "learner_fc_test@jeps.edu.vn", roles = {"LEARNER"})
    void testDeleteDeck_RemovesAllCards() throws Exception {
        mockMvc.perform(post("/flashcards/deck-delete")
                        .with(csrf())
                        .param("deckName", "Từ vựng giao tiếp cá nhân"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Flashcard> cards = flashcardRepository.findByCustomDeckName("Từ vựng giao tiếp cá nhân");
        assertTrue(cards.isEmpty());
    }
}
