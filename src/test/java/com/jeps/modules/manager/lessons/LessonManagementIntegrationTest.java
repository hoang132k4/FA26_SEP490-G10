package com.jeps.modules.manager.lessons;

import com.jeps.modules.manager.courses.service.CourseService;
import com.jeps.modules.manager.lessons.dto.LessonDtos.ChapterGroup;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonStudyView;
import com.jeps.modules.manager.lessons.dto.LessonDtos.SkillColumn;
import com.jeps.support.LocalPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Report 5.2 – sheet Lesson Management_LongBM (ITC_LM_01 → ITC_LM_16).
 * Mỗi test tạo khóa học riêng rồi thao tác qua /manager/lessons; dữ liệu được rollback sau mỗi test.
 */
@WithMockUser(username = "manager01@test.local", roles = "MANAGER")
class LessonManagementIntegrationTest extends LocalPostgresIntegrationTest {

    @Autowired
    private CourseService courseService;

    private long courseId;

    @BeforeEach
    void createCourse() {
        courseId = courseService.createCourse("IT Lesson Course", "IT_LM_" + System.nanoTime(), "N5", 0d, null,
                "Desc", null, null, "Obj", null, null);
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpServletRequestBuilder lessonForm(String title, String chapter, String skill) {
        return post("/manager/lessons").with(csrf())
                .param("courseId", String.valueOf(courseId))
                .param("title", title)
                .param("sectionName", chapter)
                .param("skillType", skill)
                .param("durationMinutes", "30");
    }

    private MockHttpServletRequestBuilder vocabularyForm(String title, String chapter) {
        return lessonForm(title, chapter, "VOCABULARY")
                .param("word", "日本", "今日")
                .param("wordReading", "にほん", "きょう")
                .param("partOfSpeech", "Danh từ", "Danh từ")
                .param("wordMeaning", "Nhật Bản", "Hôm nay")
                .param("wordExample", "日本へ行きます。", "")
                .param("wordExampleVi", "Tôi đi Nhật Bản.", "");
    }

    private long createVocabulary(String title, String chapter) throws Exception {
        mockMvc.perform(vocabularyForm(title, chapter))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “" + title + "” vào kỹ năng Từ vựng."));
        return lessonId(title);
    }

    private long lessonId(String title) {
        return jdbc.queryForObject("SELECT lesson_id FROM lessons WHERE course_id = ? AND title = ?", Long.class, courseId, title);
    }

    private Map<String, Object> lesson(long id) {
        return jdbc.queryForMap("SELECT * FROM lessons WHERE lesson_id = ?", id);
    }

    private String payload(long id) {
        return jdbc.queryForObject("SELECT payload::text FROM lesson_details WHERE lesson_id = ?", String.class, id);
    }

    // ------------------------------------------------------------------ Lesson List

    @Test
    @DisplayName("ITC_LM_01: workspace hiển thị bài học theo chương và 4 cột kỹ năng khớp database")
    void viewWorkspace() throws Exception {
        long vocab = createVocabulary("Vocabulary Lesson 1", "Chapter 1");
        long vocab2 = createVocabulary("Vocabulary Lesson 2", "Chapter 2");

        MvcResult result = mockMvc.perform(get("/manager/lessons").param("courseId", String.valueOf(courseId)))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/lesson/manager-lessons"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ChapterGroup> groups = (List<ChapterGroup>) result.getModelAndView().getModel().get("chapterGroups");
        assertThat(groups).extracting(ChapterGroup::name).contains("Chapter 1", "Chapter 2", "Kiểm tra & Đánh giá định kỳ");
        assertThat(groups).allSatisfy(group -> assertThat(group.columns()).extracting(SkillColumn::value)
                .containsExactly("VOCABULARY,KANJI", "GRAMMAR", "READING,LISTENING", "OTHER"));
        ChapterGroup chapter1 = groups.stream().filter(g -> g.name().equals("Chapter 1")).findFirst().orElseThrow();
        assertThat(chapter1.columns().get(0).lessons()).extracting(l -> l.id()).containsExactly(vocab);
        ChapterGroup chapter2 = groups.stream().filter(g -> g.name().equals("Chapter 2")).findFirst().orElseThrow();
        assertThat(chapter2.columns().get(0).lessons()).extracting(l -> l.id()).containsExactly(vocab2);
        Integer dbCount = jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE course_id = ?", Integer.class, courseId);
        assertThat(result.getModelAndView().getModel().get("courseLessonCount")).isEqualTo(dbCount);
    }

    @Test
    @DisplayName("ITC_LM_02: bộ lọc từ khóa / kỹ năng / trạng thái được giữ lại và mỗi thẻ bài học có dữ liệu để lọc "
            + "(việc ẩn/hiện thẻ chạy bằng JavaScript phía trình duyệt)")
    void searchAndFilter() throws Exception {
        createVocabulary("Kanji words", "Chapter 1");

        mockMvc.perform(get("/manager/lessons").param("courseId", String.valueOf(courseId))
                        .param("search", "Kanji").param("filterSkill", "VOCABULARY").param("filterStatus", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("searchQuery", "Kanji"))
                .andExpect(model().attribute("filterSkill", "VOCABULARY"))
                .andExpect(model().attribute("filterStatus", "ACTIVE"))
                .andExpect(content().string(containsString("data-title=\"Kanji words\"")))
                .andExpect(content().string(containsString("data-skill=\"VOCABULARY\"")));
    }

    // ------------------------------------------------------------------ Lesson Detail

    @Test
    @DisplayName("ITC_LM_03: modal chi tiết bài học hiển thị đúng nội dung đã lưu trong database")
    void viewDetailModal() throws Exception {
        long id = createVocabulary("Vocabulary Detail", "Chapter 1");

        MvcResult result = mockMvc.perform(get("/manager/lessons/" + id + "/detail-modal"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/lesson/manager-lessons :: lessonDetailContainer"))
                .andExpect(model().attribute("openDetailId", id))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<LessonStudyView> views = (List<LessonStudyView>) result.getModelAndView().getModel().get("lessonStudyViews");
        assertThat(views).hasSize(1);
        assertThat(views.get(0).lesson().id()).isEqualTo(id);
        assertThat(views.get(0).lesson().title()).isEqualTo("Vocabulary Detail");
        assertThat(String.valueOf(views.get(0).vocabDetail())).contains("日本", "にほん", "Nhật Bản");
    }

    // ------------------------------------------------------------------ Lesson Creation

    @Test
    @DisplayName("ITC_LM_04: tạo bài Từ vựng, lưu skill_type = VOCABULARY và payload từ vựng trong lesson_details")
    void createVocabularyLesson() throws Exception {
        long id = createVocabulary("Vocabulary Lesson 1", "Chapter 1");

        assertThat(lesson(id).get("skill_type")).isEqualTo("VOCABULARY");
        assertThat(lesson(id).get("section_name")).isEqualTo("Chapter 1");
        assertThat(payload(id)).contains("日本", "にほん", "Nhật Bản", "今日", "Danh từ");
    }

    @Test
    @DisplayName("ITC_LM_05: tạo bài Kanji với âm On/Kun, nghĩa và từ ghép "
            + "(âm On/Kun nhập dạng 'ニチ・ジツ' vì validator không nhận dấu phẩy và gạch nối như trong report)")
    void createKanjiLesson() throws Exception {
        mockMvc.perform(lessonForm("Kanji Lesson 1: Numbers", "Chapter 1", "KANJI")
                        .param("character", "日").param("hanviet", "Nhật").param("strokeCount", "4")
                        .param("onReading", "ニチ・ジツ").param("kunReading", "ひ・び・か")
                        .param("kanjiMeaning", "Mặt trời, ngày")
                        .param("compoundWord", "日本", "今日")
                        .param("compoundReading", "にほん", "きょう")
                        .param("compoundMeaning", "Nhật Bản", "Hôm nay"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=KANJI"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Kanji Lesson 1: Numbers” vào kỹ năng Hán tự."));

        long id = lessonId("Kanji Lesson 1: Numbers");
        assertThat(lesson(id).get("skill_type")).isEqualTo("KANJI");
        assertThat(payload(id)).contains("日", "ニチ・ジツ", "ひ・び・か", "Mặt trời, ngày", "日本", "今日");
    }

    @Test
    @DisplayName("ITC_LM_06: tạo bài Ngữ pháp với mẫu câu, ý nghĩa, cấu trúc, ghi chú và ví dụ")
    void createGrammarLesson() throws Exception {
        mockMvc.perform(lessonForm("Grammar Lesson 1: Basic Particles", "Chapter 1", "GRAMMAR")
                        .param("pattern", "〜は〜です").param("meaning", "A là B")
                        .param("formation", "N1 は N2 です").param("description", "Dùng để giới thiệu, định nghĩa.")
                        .param("exampleJp", "わたしは がくせいです。").param("exampleVi", "Tôi là học sinh."))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=GRAMMAR"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Grammar Lesson 1: Basic Particles” vào kỹ năng Ngữ pháp."));

        long id = lessonId("Grammar Lesson 1: Basic Particles");
        assertThat(lesson(id).get("skill_type")).isEqualTo("GRAMMAR");
        assertThat(payload(id)).contains("〜は〜です", "A là B", "N1 は N2 です", "わたしは がくせいです。");
    }

    @Test
    @DisplayName("ITC_LM_07: tạo bài Đọc hiểu với đoạn văn và 2 câu hỏi 4 đáp án")
    void createReadingLesson() throws Exception {
        mockMvc.perform(lessonForm("Reading Lesson 1: Self Introduction", "Chapter 1", "READING")
                        .param("passage", "わたしは たなかです。にほんから きました。")
                        .param("questionText", "たなかさんは どこから きましたか。", "たなかさんの なまえは？")
                        .param("optionA", "にほん", "たなか").param("optionB", "ちゅうごく", "やまだ")
                        .param("optionC", "かんこく", "すずき").param("optionD", "ベトナム", "さとう")
                        .param("correct", "A", "A"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=READING"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Reading Lesson 1: Self Introduction” vào kỹ năng Đọc hiểu."));

        long id = lessonId("Reading Lesson 1: Self Introduction");
        assertThat(lesson(id).get("skill_type")).isEqualTo("READING");
        assertThat(jdbc.queryForObject("SELECT body FROM lesson_details WHERE lesson_id = ?", String.class, id))
                .contains("わたしは たなかです。");
        assertThat(payload(id)).contains("たなかさんは どこから きましたか。", "たなかさんの なまえは？", "ちゅうごく", "さとう");
    }

    @Test
    @DisplayName("ITC_LM_08: tạo bài Nghe hiểu, file nghe được lưu vào thư mục uploads và đường dẫn nằm trong payload "
            + "(giới hạn file nghe thực tế là 50 MB, report ghi 20 MB)")
    void createListeningLesson() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("questionAudioFile", "conversation.mp3", "audio/mpeg",
                new byte[]{(byte) 0xFF, (byte) 0xFB, 0x10, 0x00, 0x00});

        mockMvc.perform(multipart("/manager/lessons").file(audio).with(csrf())
                        .param("courseId", String.valueOf(courseId))
                        .param("title", "Listening Lesson 1: Daily Conversations")
                        .param("sectionName", "Chapter 1").param("skillType", "LISTENING").param("durationMinutes", "30")
                        .param("questionAudioMode", "file").param("questionAudioUrl", "")
                        .param("questionText", "おとこのひとは なにを しますか。")
                        .param("optionA", "かいもの").param("optionB", "べんきょう").param("optionC", "").param("optionD", "")
                        .param("correct", "A"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=LISTENING"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Listening Lesson 1: Daily Conversations” vào kỹ năng Nghe."));

        long id = lessonId("Listening Lesson 1: Daily Conversations");
        String payload = payload(id);
        assertThat(payload).contains("/uploads/lessons/audio/", "おとこのひとは なにを しますか。");
        String audioUrl = jdbc.queryForObject("SELECT audio_url FROM lesson_details WHERE lesson_id = ?", String.class, id);
        assertThat(audioUrl).startsWith("/uploads/lessons/audio/").endsWith(".mp3");
        assertThat(Files.exists(UPLOAD_DIR.resolve("lessons/audio").resolve(audioUrl.substring("/uploads/lessons/audio/".length())))).isTrue();
    }

    @Test
    @DisplayName("ITC_LM_09: từ chối tạo bài trùng tên trong cùng chương, database vẫn chỉ có 1 bài "
            + "(thông báo thực tế có kèm tên bài: “Vocabulary Lesson 1”)")
    void rejectDuplicateTitle() throws Exception {
        createVocabulary("Vocabulary Lesson 1", "Chapter 1");

        mockMvc.perform(vocabularyForm("Vocabulary Lesson 1", "Chapter 1"))
                .andExpect(redirectedUrl("/manager/lessons?modal=new&courseId=" + courseId + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonError", "Tên bài học “Vocabulary Lesson 1” đã tồn tại trong chương này."));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE course_id = ? AND title = 'Vocabulary Lesson 1'",
                Integer.class, courseId)).isEqualTo(1);
    }

    @Test
    @DisplayName("ITC_LM_10: sửa tên, thời lượng, mô tả bài học; updated_at được làm mới")
    void updateLesson() throws Exception {
        long id = createVocabulary("Vocabulary Lesson 1", "Chapter 1");
        jdbc.update("UPDATE lessons SET updated_at = TIMESTAMP '2020-01-01 00:00:00' WHERE lesson_id = ?", id);

        mockMvc.perform(post("/manager/lessons/" + id).with(csrf())
                        .param("courseId", String.valueOf(courseId)).param("skillType", "VOCABULARY")
                        .param("title", "Updated Vocabulary Lesson 1").param("sectionName", "Chapter 1")
                        .param("durationMinutes", "45").param("description", "Updated description 2026")
                        .param("word", "日本").param("wordReading", "にほん")
                        .param("partOfSpeech", "Danh từ").param("wordMeaning", "Nhật Bản"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Updated Vocabulary Lesson 1”."));

        Map<String, Object> row = lesson(id);
        assertThat(row.get("title")).isEqualTo("Updated Vocabulary Lesson 1");
        assertThat(row.get("duration_minutes")).isEqualTo(45);
        assertThat(row.get("description")).isEqualTo("Updated description 2026");
        assertThat((Timestamp) row.get("updated_at")).isAfter(Timestamp.valueOf("2020-01-01 00:00:00"));
    }

    @Test
    @DisplayName("ITC_LM_11: đổi tên chương, section_name của mọi bài con được cập nhật")
    void renameChapter() throws Exception {
        long first = createVocabulary("Vocabulary Lesson 1", "Chapter 1");
        long second = createVocabulary("Vocabulary Lesson 2", "Chapter 1");

        mockMvc.perform(post("/manager/lessons/sections/rename").with(csrf())
                        .param("courseId", String.valueOf(courseId))
                        .param("oldName", "Chapter 1").param("newName", "Chapter 1: Warm Up"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId))
                .andExpect(flash().attribute("lessonSuccess", "Đã cập nhật tên chương."));

        assertThat(lesson(first).get("section_name")).isEqualTo("Chapter 1: Warm Up");
        assertThat(lesson(second).get("section_name")).isEqualTo("Chapter 1: Warm Up");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE course_id = ? AND section_name = 'Chapter 1'",
                Integer.class, courseId)).isZero();
    }

    @Test
    @DisplayName("ITC_LM_12: xóa chương, bài con được giữ lại với section_name = NULL")
    void deleteChapter() throws Exception {
        long first = createVocabulary("Vocabulary Lesson 1", "Chapter 3");
        long second = createVocabulary("Vocabulary Lesson 2", "Chapter 3");

        mockMvc.perform(post("/manager/lessons/sections/delete").with(csrf())
                        .param("courseId", String.valueOf(courseId)).param("sectionName", "Chapter 3"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId))
                .andExpect(flash().attribute("lessonSuccess",
                        "Đã xóa chương. Bài học trong chương được giữ lại và chuyển sang “Chưa phân chương”."));

        assertThat(lesson(first).get("section_name")).isNull();
        assertThat(lesson(second).get("section_name")).isNull();
    }

    @Test
    @DisplayName("ITC_LM_13: kéo thả đổi chỗ 2 bài trong cột Từ vựng, hai bài hoán đổi order_index "
            + "(order_index đánh 1..N trên toàn khóa học nên không phải đúng 1 và 2 như report)")
    void reorderInColumn() throws Exception {
        long first = createVocabulary("Lesson 1", "Chapter 1");
        long second = createVocabulary("Lesson 2", "Chapter 1");
        int firstOrder = (Integer) lesson(first).get("order_index");
        int secondOrder = (Integer) lesson(second).get("order_index");

        mockMvc.perform(post("/manager/lessons/reorder").with(csrf()).header("X-Requested-With", "XMLHttpRequest")
                        .param("courseId", String.valueOf(courseId)).param("chapter", "Chapter 1")
                        .param("column", "VOCABULARY,KANJI").param("ids", second + "," + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã lưu thứ tự bài học thành công."));

        assertThat(lesson(second).get("order_index")).isEqualTo(firstOrder);
        assertThat(lesson(first).get("order_index")).isEqualTo(secondOrder);
        List<Integer> orders = jdbc.queryForList("SELECT order_index FROM lessons WHERE course_id = ? ORDER BY order_index",
                Integer.class, courseId);
        for (int i = 0; i < orders.size(); i++) assertThat(orders.get(i)).isEqualTo(i + 1);
    }

    @Test
    @DisplayName("ITC_LM_14: bật hiển thị bài đang ẩn, cột status trong database đổi thành ACTIVE")
    void toggleVisibility() throws Exception {
        long id = createVocabulary("Hidden Lesson", "Chapter 1");
        assertThat(lesson(id).get("status")).isEqualTo("INACTIVE");

        mockMvc.perform(post("/manager/lessons/" + id + "/status").with(csrf())
                        .param("status", "ACTIVE").param("courseId", String.valueOf(courseId)).param("skill", "VOCABULARY"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", "Đã hiển thị bài học “Hidden Lesson”."));

        assertThat(lesson(id).get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("ITC_LM_15: xóa bài học chưa có học viên học, bài học và chi tiết bị xóa hoàn toàn")
    void deleteLesson() throws Exception {
        long id = createVocabulary("Disposable Lesson", "Chapter 1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_details WHERE lesson_id = ?", Integer.class, id)).isEqualTo(1);

        mockMvc.perform(post("/manager/lessons/" + id + "/delete").with(csrf())
                        .param("courseId", String.valueOf(courseId)).param("skill", "VOCABULARY"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", "Đã xoá bài học “Disposable Lesson”."));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE lesson_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_details WHERE lesson_id = ?", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("ITC_LM_16: không cho xóa bài học đã có học viên học, bài học vẫn còn trong database")
    void preventDeletingStudiedLesson() throws Exception {
        long id = createVocabulary("Studied Lesson", "Chapter 1");
        long learnerId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'learner01@test.local'", Long.class);
        jdbc.update("INSERT INTO learning_paths (user_id, course_id, daily_hours_setup, target_level, current_level, path_status, scheduled_date, lesson_progress) "
                        + "VALUES (?, ?, 1.5, 'N5', 'N5', 'ACTIVE', CURRENT_DATE, CAST(? AS jsonb))",
                learnerId, courseId, "{\"lessons\":{\"" + id + "\":{\"status\":\"COMPLETED\"}}}");

        mockMvc.perform(post("/manager/lessons/" + id + "/delete").with(csrf())
                        .param("courseId", String.valueOf(courseId)).param("skill", "VOCABULARY"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + courseId + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonError", "Bài học này đã có học viên học, không thể xóa!"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE lesson_id = ?", Integer.class, id)).isEqualTo(1);
    }
}
