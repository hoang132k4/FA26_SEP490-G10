package com.jlpp.modules.flashcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.flashcard.dto.FlashcardReviewRequest;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
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
                .andExpect(content().string(containsString("Bộ thẻ Flashcard Nhật Ngữ")))
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
                .andExpect(content().string(containsString("Luyện trí nhớ Flashcard (Spaced Repetition)")))
                .andExpect(content().string(containsString("Chưa nhớ [1]")))
                .andExpect(content().string(containsString("Đã thuộc [3]")));

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
}
