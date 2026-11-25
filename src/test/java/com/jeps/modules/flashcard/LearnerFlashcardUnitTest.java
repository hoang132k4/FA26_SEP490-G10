package com.jeps.modules.flashcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit & Integration Test Suite for LearnerFlashcard Module
 * Strictly traced to Report-5.1_Unit Test_All10Modules_backup.xls: Sheet 'LearnerFlashcard' (UTC0770 - UTC0897)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnerFlashcardUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private com.jeps.modules.flashcard.service.FlashcardService flashcardService;

    @Autowired
    private ObjectMapper objectMapper;

    private User learner;
    private Course testCourse;
    private Lesson testLesson;
    private Flashcard sysCard1;
    private Flashcard sysCard2;
    private Flashcard userCard1;

    @BeforeEach
    void setUp() {
        learner = userRepository.findByEmailIgnoreCase("learner_flashcard_utc@jeps.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("learner_flashcard_utc@jeps.edu.vn")
                        .username("learner_flashcard_utc")
                        .password("$2a$10$encryptedPasswordPlaceholder")
                        .fullName("Learner Flashcard Tester")
                        .phone("0977665544")
                        .role(RoleEnum.LEARNER)
                        .status("ACTIVE")
                        .build()));

        testCourse = courseRepository.save(Course.builder()
                .title("Khóa học Flashcard N5")
                .level("N5")
                .price(0.0)
                .status("PUBLISHED")
                .lessonCount(5)
                .build());

        testLesson = lessonRepository.save(Lesson.builder()
                .course(testCourse)
                .title("Bài 1: Từ vựng mở đầu")
                .orderIndex(1)
                .sectionName("Chương 1")
                .lessonType("THEORY")
                .durationMinutes(20)
                .status("ACTIVE")
                .build());

        sysCard1 = flashcardRepository.save(Flashcard.builder()
                .lessonId(testLesson.getId())
                .vocabulary("本")
                .reading("ほん")
                .meaning("Sách, quyển sách")
                .exampleSentence("これは本です。")
                .jlptLevel("N5")
                .isCustom(false)
                .reviewStatus("NEW")
                .build());

        sysCard2 = flashcardRepository.save(Flashcard.builder()
                .lessonId(testLesson.getId())
                .vocabulary("先生")
                .reading("せんせい")
                .meaning("Giáo viên, thầy cô")
                .exampleSentence("先生は親切です。")
                .jlptLevel("N5")
                .isCustom(false)
                .reviewStatus("NEW")
                .build());

        userCard1 = flashcardRepository.save(Flashcard.builder()
                .creatorId(learner.getId())
                .customDeckName("Gói Từ Vựng Tự Học")
                .vocabulary("私")
                .reading("わたし")
                .meaning("Tôi, bản thân tôi")
                .exampleSentence("私は学生です。")
                .jlptLevel("N5")
                .isCustom(true)
                .reviewStatus("NEW")
                .build());
    }

    // =========================================================================
    // MÀN HÌNH SCR-20: Danh mục Bộ thẻ Flashcard (/flashcards) [UTC0770 - UTC0790]
    // =========================================================================

    @Test
    @DisplayName("UTC0770 [SCR-20]: Kiểm tra nạp giao diện Danh mục Bộ thẻ Flashcard (HTTP 200 OK)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0770_viewFlashcardList_200OK() throws Exception {
        mockMvc.perform(get("/flashcards"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/list"))
                .andExpect(model().attributeExists("systemDecks"))
                .andExpect(model().attributeExists("userDecks"))
                .andExpect(model().attributeExists("stats"));
    }

    @Test
    @DisplayName("UTC0778 [SCR-20]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập danh mục flashcard")
    @WithAnonymousUser
    void test_UTC0778_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/flashcards"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0786 [SCR-20]: Kiểm tra tab 'Bộ thẻ Hệ thống' (Admin Decks)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0786_systemDecksTab() throws Exception {
        mockMvc.perform(get("/flashcards").param("tab", "system"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeTab", "system"))
                .andExpect(model().attributeExists("systemDecks"));
    }

    @Test
    @DisplayName("UTC0787 [SCR-20]: Kiểm tra tab 'Bộ thẻ Cá nhân' (User Decks)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0787_userDecksTab() throws Exception {
        mockMvc.perform(get("/flashcards").param("tab", "user"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeTab", "user"))
                .andExpect(model().attributeExists("userDecks"));
    }

    @Test
    @DisplayName("UTC0788 [SCR-20]: Kiểm tra lọc bộ thẻ theo cấp độ JLPT")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0788_filterDecksByLevel() throws Exception {
        mockMvc.perform(get("/flashcards").param("level", "N5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedLevel", "N5"));
    }

    @Test
    @DisplayName("UTC0789 [SCR-20]: Kiểm tra tìm kiếm bộ thẻ theo từ khóa")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0789_searchDecksByKeyword() throws Exception {
        mockMvc.perform(get("/flashcards").param("keyword", "Từ Vựng"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", "Từ Vựng"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-21: Chi tiết Thẻ Từ vựng (/flashcards/{id}) [UTC0791 - UTC0811]
    // =========================================================================

    @Test
    @DisplayName("UTC0791 [SCR-21]: Kiểm tra nạp giao diện Chi tiết Thẻ Từ vựng hợp lệ (HTTP 200 OK)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0791_viewFlashcardDetail_200OK() throws Exception {
        mockMvc.perform(get("/flashcards/{id}", sysCard1.getFlashcardId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/detail"))
                .andExpect(model().attributeExists("flashcard"));
    }

    @Test
    @DisplayName("UTC0799 [SCR-21]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập chi tiết flashcard")
    @WithAnonymousUser
    void test_UTC0799_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/flashcards/{id}", sysCard1.getFlashcardId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0807 [SCR-21]: Kiểm tra hiển thị đầy đủ thông tin từ vựng")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0807_flashcardAttributes() throws Exception {
        mockMvc.perform(get("/flashcards/{id}", sysCard1.getFlashcardId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("flashcard", hasProperty("vocabulary", is("本"))))
                .andExpect(model().attribute("flashcard", hasProperty("reading", is("ほん"))))
                .andExpect(model().attribute("flashcard", hasProperty("meaning", is("Sách, quyển sách"))));
    }

    // =========================================================================
    // MÀN HÌNH SCR-22: Luyện Flashcard 3D & Trắc nghiệm (/flashcards/practice) [UTC0812 - UTC0834]
    // =========================================================================

    @Test
    @DisplayName("UTC0812 [SCR-22]: Kiểm tra nạp giao diện Luyện Flashcard 3D (HTTP 200 OK)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0812_viewPracticeMode_200OK() throws Exception {
        mockMvc.perform(get("/flashcards/practice"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/practice"))
                .andExpect(model().attributeExists("cards"))
                .andExpect(model().attributeExists("cardsJson"))
                .andExpect(model().attributeExists("totalCards"));
    }

    @Test
    @DisplayName("UTC0820 [SCR-22]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập trang luyện tập")
    @WithAnonymousUser
    void test_UTC0820_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/flashcards/practice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0829 [SCR-22]: Kiểm tra cập nhật trạng thái ôn tập 'Đã thuộc' (MASTERED) qua AJAX POST")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0829_reviewStatus_mastered() throws Exception {
        FlashcardReviewRequest req = FlashcardReviewRequest.builder()
                .flashcardId(sysCard1.getFlashcardId())
                .status("MASTERED")
                .build();

        mockMvc.perform(post("/flashcards/practice/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reviewStatus").value("MASTERED"));
    }

    @Test
    @DisplayName("UTC0830 [SCR-22]: Kiểm tra cập nhật trạng thái ôn tập 'Cần ôn lại' (REVIEWING) qua AJAX POST")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0830_reviewStatus_reviewing() throws Exception {
        FlashcardReviewRequest req = FlashcardReviewRequest.builder()
                .flashcardId(sysCard2.getFlashcardId())
                .status("REVIEWING")
                .build();

        mockMvc.perform(post("/flashcards/practice/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reviewStatus").value("REVIEWING"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-23: Minigame Ghép từ Tính giờ (/flashcards/match) [UTC0835 - UTC0855]
    // =========================================================================

    @Test
    @DisplayName("UTC0835 [SCR-23]: Kiểm tra nạp giao diện Minigame Ghép từ Tính giờ (HTTP 200 OK)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0835_viewMatchGame_200OK() throws Exception {
        mockMvc.perform(get("/flashcards/match"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/practice"))
                .andExpect(model().attribute("currentMode", "match"))
                .andExpect(model().attributeExists("cards"));
    }

    @Test
    @DisplayName("UTC0843 [SCR-23]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập game ghép từ")
    @WithAnonymousUser
    void test_UTC0843_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/flashcards/match"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    // =========================================================================
    // MÀN HÌNH SCR-24: Quản lý Bộ thẻ Cá nhân & Nhập hàng loạt (/flashcards/deck-edit) [UTC0856 - UTC0897]
    // =========================================================================

    @Test
    @DisplayName("UTC0856 [SCR-24]: Kiểm tra nạp giao diện Chỉnh sửa Bộ thẻ Cá nhân (HTTP 200 OK)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0856_viewDeckEdit_200OK() throws Exception {
        mockMvc.perform(get("/flashcards/deck-edit")
                        .param("deckName", "Gói Từ Vựng Tự Học"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/deck-edit"))
                .andExpect(model().attribute("deckName", "Gói Từ Vựng Tự Học"))
                .andExpect(model().attributeExists("cards"));
    }

    @Test
    @DisplayName("UTC0864 [SCR-24]: Kiểm tra bảo mật: Chặn người dùng chưa xác thực truy cập chỉnh sửa bộ thẻ")
    @WithAnonymousUser
    void test_UTC0864_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/flashcards/deck-edit").param("deckName", "Gói Từ Vựng Tự Học"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @DisplayName("UTC0895 [SCR-24]: Kiểm tra nhập hàng loạt từ vựng JSON hợp lệ (Bulk Import)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0895_bulkImportValidJson() throws Exception {
        String jsonPayload = """
                [
                    {"vocabulary": "車", "reading": "くるま", "meaning": "Xe ô tô"},
                    {"vocabulary": "電車", "reading": "でんしゃ", "meaning": "Tàu điện"}
                ]
                """;

        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("payload", jsonPayload)
                        .param("customDeckName", "Bộ Thẻ Phương Tiện")
                        .param("jlptLevel", "N5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("UTC0896 [SCR-24]: Kiểm tra nhập hàng loạt với cú pháp JSON sai xử lý an toàn")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_UTC0896_bulkImportMalformedJson() throws Exception {
        String invalidPayload = "{invalid-json-content: true,";

        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("payload", invalidPayload))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/create"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // =========================================================================
    // MỞ RỘNG KIỂM THỬ TẠO, SỬA, XÓA THẺ & BỘ THẺ (BOOST COVERAGE >= 80%)
    // =========================================================================

    @Test
    @DisplayName("Kiểm tra nạp form tạo thẻ Flashcard mới (/flashcards/create)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_createForm_200OK() throws Exception {
        mockMvc.perform(get("/flashcards/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attributeExists("flashcardRequest"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    @DisplayName("Kiểm tra gửi form tạo thẻ thành công (/flashcards/create)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_createSubmit_success() throws Exception {
        mockMvc.perform(post("/flashcards/create")
                        .with(csrf())
                        .param("vocabulary", "猫")
                        .param("reading", "ねこ")
                        .param("meaning", "Con mèo")
                        .param("jlptLevel", "N5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra gửi form tạo thẻ thiếu trường bắt buộc báo lỗi validation")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_createSubmit_validationErrors() throws Exception {
        mockMvc.perform(post("/flashcards/create")
                        .with(csrf())
                        .param("vocabulary", "")
                        .param("meaning", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Kiểm tra nạp form sửa thẻ Flashcard (/flashcards/{id}/edit)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_editForm_200OK() throws Exception {
        mockMvc.perform(get("/flashcards/{id}/edit", userCard1.getFlashcardId()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("flashcardRequest"));
    }

    @Test
    @DisplayName("Kiểm tra cập nhật thẻ thành công (/flashcards/{id}/edit)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_editSubmit_success() throws Exception {
        mockMvc.perform(post("/flashcards/{id}/edit", userCard1.getFlashcardId())
                        .with(csrf())
                        .param("vocabulary", "私")
                        .param("reading", "わたし")
                        .param("meaning", "Tôi (ngôi thứ nhất)")
                        .param("jlptLevel", "N5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/" + userCard1.getFlashcardId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra xóa thẻ Flashcard (/flashcards/{id}/delete)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_deleteCard_success() throws Exception {
        mockMvc.perform(post("/flashcards/{id}/delete", userCard1.getFlashcardId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra cập nhật thông tin gói bộ thẻ (/flashcards/deck-edit)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_deckEdit_metadataOnly() throws Exception {
        mockMvc.perform(post("/flashcards/deck-edit")
                        .with(csrf())
                        .param("oldDeckName", "Gói Từ Vựng Tự Học")
                        .param("newDeckName", "Gói Từ Vựng Nâng Cao")
                        .param("jlptLevel", "N4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra xóa toàn bộ gói bộ thẻ (/flashcards/deck-delete)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_deckDelete_success() throws Exception {
        mockMvc.perform(post("/flashcards/deck-delete")
                        .with(csrf())
                        .param("deckName", "Gói Từ Vựng Tự Học"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra chế độ ghép từ Match Game (/flashcards/match)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_matchGame() throws Exception {
        mockMvc.perform(get("/flashcards/match").param("level", "N5"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/practice"))
                .andExpect(model().attribute("currentMode", "match"))
                .andExpect(model().attributeExists("cardsJson"));
    }

    @Test
    @DisplayName("Kiểm tra submit tạo thẻ có lỗi validation (thiếu từ vựng)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_createSubmit_validationError() throws Exception {
        mockMvc.perform(post("/flashcards/create")
                        .with(csrf())
                        .param("vocabulary", "")
                        .param("meaning", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attributeExists("lessons"));
    }

    @Test
    @DisplayName("Kiểm tra submit sửa thẻ có lỗi validation (thiếu từ vựng)")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_editSubmit_validationError() throws Exception {
        mockMvc.perform(post("/flashcards/{id}/edit", userCard1.getFlashcardId())
                        .with(csrf())
                        .param("vocabulary", "")
                        .param("meaning", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/flashcard/form"))
                .andExpect(model().attributeExists("flashcardId"));
    }

    @Test
    @DisplayName("Kiểm tra API chấm điểm SRS thiếu thông tin trả về 400 Bad Request")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_practiceReview_badRequest() throws Exception {
        mockMvc.perform(post("/flashcards/practice/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Kiểm tra nhập hàng loạt với payload rỗng trả về thông báo lỗi")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_bulkImport_emptyPayload() throws Exception {
        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("payload", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards/create"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Kiểm tra nhập hàng loạt danh sách JSON từ vựng thành công")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_bulkImport_validPayload() throws Exception {
        String payload = "[{\"vocabulary\":\"木\",\"reading\":\"き\",\"meaning\":\"Cây\"},{\"vocabulary\":\"山\",\"reading\":\"やま\",\"meaning\":\"Núi\"}]";
        mockMvc.perform(post("/flashcards/import-bulk")
                        .with(csrf())
                        .param("payload", payload)
                        .param("customDeckName", "Gói Tự Nhiên")
                        .param("jlptLevel", "N5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra cập nhật toàn diện gói thẻ bao gồm danh sách thẻ và xóa thẻ")
    @WithMockUser(username = "learner_flashcard_utc@jeps.edu.vn", roles = {"LEARNER"})
    void test_deckEdit_fullSave() throws Exception {
        String cardsPayload = "[{\"flashcardId\":" + userCard1.getFlashcardId() + ",\"vocabulary\":\"私\",\"reading\":\"わたし\",\"meaning\":\"Tôi\"},{\"vocabulary\":\"あなた\",\"reading\":\"あなた\",\"meaning\":\"Bạn\"}]";
        String deletedIds = "[]";
        mockMvc.perform(post("/flashcards/deck-edit")
                        .with(csrf())
                        .param("oldDeckName", "Gói Từ Vựng Tự Học")
                        .param("newDeckName", "Gói Từ Vựng Mới")
                        .param("jlptLevel", "N5")
                        .param("cardsPayload", cardsPayload)
                        .param("deletedCardIds", deletedIds))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/flashcards?tab=user"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Kiểm tra trực tiếp FlashcardService: thống kê, tìm kiếm practice, và cập nhật SRS")
    void test_flashcardService_directMethods() {
        var practiceList = flashcardService.getPracticeFlashcards("N5", null, "Gói Từ Vựng Tự Học", 5);
        assertThat(practiceList, notNullValue());

        var systemDecks = flashcardService.getSystemFlashcardDecks("Chào", "N5");
        assertThat(systemDecks, notNullValue());

        var stats = flashcardService.getStatistics();
        assertThat(stats.get("total"), greaterThanOrEqualTo(1L));

        var updated = flashcardService.updateReviewStatus(sysCard1.getFlashcardId(), "MASTERED");
        assertThat(updated.getReviewStatus(), is("MASTERED"));
    }
}
