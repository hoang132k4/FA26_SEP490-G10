package com.jeps.modules.manager.course;

import com.jeps.support.LocalPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Report 5.2 – sheet Course Management_LongBM (ITC_CM_01 → ITC_CM_11).
 * Controller → Service → Repository → PostgreSQL jeps_test; mọi thay đổi được rollback sau mỗi test.
 */
@WithMockUser(username = "manager01@test.local", roles = "MANAGER")
class CourseManagementIntegrationTest extends LocalPostgresIntegrationTest {

    // ------------------------------------------------------------------ helpers

    private MockHttpServletRequestBuilder courseForm(String url, String title, String code, String level, String price) {
        return post(url).with(csrf())
                .param("name", title)
                .param("code", code)
                .param("level", level)
                .param("price", price)
                .param("shortDescription", "N5 basics")
                .param("objectives", "Pass JLPT")
                .param("imageMode", "url")
                .param("thumbnailUrl", "");
    }

    /** Tạo khóa học qua màn hình quản lý (POST /manager/courses) và trả về course_id. */
    private long createCourse(String title, String code, String level, String price) throws Exception {
        mockMvc.perform(courseForm("/manager/courses", title, code, level, price))
                .andExpect(redirectedUrl("/manager/courses"))
                .andExpect(flash().attribute("courseSuccess", "Đã thêm khóa học."));
        return courseId(code);
    }

    private long courseId(String code) {
        return jdbc.queryForObject("SELECT course_id FROM courses WHERE code = ?", Long.class, code);
    }

    private Map<String, Object> courseRow(long id) {
        return jdbc.queryForMap("SELECT * FROM courses WHERE course_id = ?", id);
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ Course List

    @Test
    @DisplayName("ITC_CM_01: danh sách khóa học sắp theo cấp độ JLPT giảm dần và số bài học khớp database")
    void viewCourseList() throws Exception {
        long n5 = createCourse("IT Course N5", "IT_CM01_N5", "N5", "0");
        long n1 = createCourse("IT Course N1", "IT_CM01_N1", "N1", "0");

        MvcResult result = mockMvc.perform(get("/manager/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/course/manager-courses"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> courses = (List<Map<String, Object>>) result.getModelAndView().getModel().get("courses");
        List<String> levels = courses.stream().map(c -> String.valueOf(c.get("level"))).toList();
        assertThat(levels).isSortedAccordingTo((a, b) -> b.compareTo(a));
        for (Map<String, Object> course : courses) {
            long id = ((Number) course.get("id")).longValue();
            Integer dbLessons = jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE course_id = ?", Integer.class, id);
            assertThat(course.get("lessonCount")).as("lessonCount của khóa %s", id).isEqualTo(dbLessons);
        }
        assertThat(courses).extracting(c -> ((Number) c.get("id")).longValue()).contains(n5, n1);
    }

    @Test
    @DisplayName("ITC_CM_02: trang danh sách có ô tìm kiếm, bộ lọc cấp độ / trạng thái và dữ liệu lọc trên từng khóa học "
            + "(việc lọc chạy bằng JavaScript phía trình duyệt)")
    void searchAndFilterControls() throws Exception {
        createCourse("IT Search N5", "IT_CM02_N5", "N5", "0");

        mockMvc.perform(get("/manager/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"courseSearch\"")))
                .andExpect(content().string(containsString("id=\"courseLevelFilter\"")))
                .andExpect(content().string(containsString("id=\"courseStatusFilter\"")))
                .andExpect(content().string(containsString("data-name=\"IT Search N5\"")))
                .andExpect(content().string(containsString("data-level=\"N5\"")));
    }

    // ------------------------------------------------------------------ Course Detail

    @Test
    @DisplayName("ITC_CM_03: trang chi tiết khóa học hiển thị đúng dữ liệu database "
            + "(report ghi \"modal\" – thực tế là trang /manager/courses/{id}/view)")
    void viewCourseDetail() throws Exception {
        long id = createCourse("IT Detail Course", "IT_CM03", "N4", "300000");

        MvcResult result = mockMvc.perform(get("/manager/courses/" + id + "/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/course/course-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Đánh giá"))))
                .andExpect(model().attribute("courseId", id))
                .andExpect(model().attribute("courseNextStatus", "PUBLISHED"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) result.getModelAndView().getModel().get("courseDetails");
        Map<String, Object> row = courseRow(id);
        assertThat(details.get("title")).isEqualTo(row.get("title")).isEqualTo("IT Detail Course");
        assertThat(details.get("code")).isEqualTo("IT_CM03");
        assertThat(details.get("level")).isEqualTo("N4");
    }

    @Test
    @DisplayName("ITC_CM_04: xem khóa học không tồn tại thì quay về danh sách kèm lỗi "
            + "(report ghi 'Course not found.' – thực tế 'Không tìm thấy khóa học.')")
    void viewMissingCourse() throws Exception {
        long missing = jdbc.queryForObject("SELECT COALESCE(MAX(course_id), 0) + 1000 FROM courses", Long.class);

        mockMvc.perform(get("/manager/courses/" + missing + "/view"))
                .andExpect(redirectedUrl("/manager/courses"))
                .andExpect(flash().attribute("courseError", "Không tìm thấy khóa học."));
    }

    // ------------------------------------------------------------------ Course Creation

    @Test
    @DisplayName("ITC_CM_05: tạo khóa học mới (DRAFT + bài thi giữa kì tự sinh) rồi thêm bài Từ vựng vào khóa học")
    void createCourseAndVocabularyLesson() throws Exception {
        long id = createCourse("Basic N5 Course", "JLPT_N5_IT", "N5", "500000");

        assertThat(courseRow(id).get("status")).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM lessons WHERE course_id = ? AND title = 'Bài thi giữa kì: Đánh giá năng lực giai đoạn 1'",
                Integer.class, id)).isEqualTo(1);

        mockMvc.perform(post("/manager/lessons").with(csrf())
                        .param("courseId", String.valueOf(id))
                        .param("title", "Vocabulary Lesson 1")
                        .param("sectionName", "Chapter 1")
                        .param("skillType", "VOCABULARY")
                        .param("durationMinutes", "30")
                        .param("word", "日本").param("wordReading", "にほん")
                        .param("partOfSpeech", "Danh từ").param("wordMeaning", "Nhật Bản"))
                .andExpect(redirectedUrl("/manager/lessons?courseId=" + id + "&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Vocabulary Lesson 1” vào kỹ năng Từ vựng."));

        assertThat(jdbc.queryForObject("SELECT course_id FROM lessons WHERE title = 'Vocabulary Lesson 1' AND course_id = ?",
                Long.class, id)).isEqualTo(id);
    }

    @Test
    @DisplayName("ITC_CM_06: từ chối tạo khóa học khi mã đã tồn tại, số khóa học có mã đó vẫn là 1")
    void rejectDuplicateCode() throws Exception {
        createCourse("Existing N1 Course", "JLPT_N1_IT", "N1", "400000");

        mockMvc.perform(courseForm("/manager/courses", "Test", "JLPT_N1_IT", "N1", "400000"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courseErrors", org.hamcrest.Matchers.hasEntry("code", "Mã khóa học này đã được sử dụng.")));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE code = 'JLPT_N1_IT'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("ITC_CM_07: tải ảnh PNG (< 5 MB) làm ảnh đại diện, đường dẫn được lưu và hiển thị trên thẻ khóa học")
    void uploadThumbnail() throws Exception {
        MockMultipartFile image = new MockMultipartFile("imageFile", "cover.png", "image/png", png());

        mockMvc.perform(multipart("/manager/courses").file(image).with(csrf())
                        .param("name", "IT Upload Course").param("code", "IT_CM07").param("level", "N5")
                        .param("price", "0").param("shortDescription", "N5 basics").param("objectives", "Pass JLPT")
                        .param("imageMode", "file"))
                .andExpect(redirectedUrl("/manager/courses"))
                .andExpect(flash().attribute("courseSuccess", "Đã thêm khóa học."));

        String thumbnail = (String) courseRow(courseId("IT_CM07")).get("thumbnail_url");
        assertThat(thumbnail).startsWith("/uploads/courses/").endsWith(".png");
        assertThat(Files.exists(UPLOAD_DIR.resolve("courses").resolve(thumbnail.substring("/uploads/courses/".length())))).isTrue();

        mockMvc.perform(get("/manager/courses"))
                .andExpect(content().string(containsString(thumbnail)));
    }

    @Test
    @DisplayName("ITC_CM_08: cập nhật tên và giá khóa học, updated_at được làm mới")
    void updateCourse() throws Exception {
        long id = createCourse("N1 Course", "JLPT_N1_UPD", "N1", "500000");
        jdbc.update("UPDATE courses SET updated_at = TIMESTAMP '2020-01-01 00:00:00' WHERE course_id = ?", id);

        mockMvc.perform(courseForm("/manager/courses/" + id, "Updated N5 Course 2026", "JLPT_N1_UPD", "N1", "650000"))
                .andExpect(redirectedUrl("/manager/courses"))
                .andExpect(flash().attribute("courseSuccess", "Đã cập nhật khóa học."));

        Map<String, Object> row = courseRow(id);
        assertThat(row.get("title")).isEqualTo("Updated N5 Course 2026");
        assertThat(((Number) row.get("price")).doubleValue()).isEqualTo(650000d);
        assertThat((Timestamp) row.get("updated_at")).isAfter(Timestamp.valueOf("2020-01-01 00:00:00"));
    }

    @Test
    @DisplayName("ITC_CM_09: chỉ sửa mô tả ngắn và giá, các trường khác giữ nguyên "
            + "(report ghi khóa 'IT_CM_09' – thống nhất dùng khóa trong điều kiện đầu vào)")
    void updateKeepsOtherFields() throws Exception {
        long id = createCourse("Keep Fields Course", "JLPT_N1_KEEP", "N2", "500000");
        Map<String, Object> before = courseRow(id);

        mockMvc.perform(post("/manager/courses/" + id).with(csrf())
                        .param("name", "Keep Fields Course").param("code", "JLPT_N1_KEEP").param("level", "N2")
                        .param("price", "450000").param("shortDescription", "New short description")
                        .param("objectives", "Pass JLPT").param("imageMode", "url").param("thumbnailUrl", ""))
                .andExpect(redirectedUrl("/manager/courses"));

        Map<String, Object> after = courseRow(id);
        assertThat(after.get("short_description")).isEqualTo("New short description");
        assertThat(((Number) after.get("price")).doubleValue()).isEqualTo(450000d);
        for (String column : List.of("title", "code", "level", "objectives", "status")) {
            assertThat(after.get(column)).as(column).isEqualTo(before.get(column));
        }
    }

    @Test
    @DisplayName("ITC_CM_10: chuyển khóa học DRAFT → PUBLISHED, published_date được ghi nhận "
            + "(giao diện là nút trạng thái kế tiếp, không phải dropdown)")
    void publishCourse() throws Exception {
        long id = createCourse("N4 Course", "JLPT_N4_IT", "N4", "0");
        assertThat(courseRow(id).get("published_date")).isNull();

        mockMvc.perform(post("/manager/courses/" + id + "/status").with(csrf()).param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/manager/courses/" + id + "/view"))
                .andExpect(flash().attribute("courseSuccess", "Đã chuyển trạng thái khóa học."));

        Map<String, Object> row = courseRow(id);
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(row.get("published_date")).isNotNull();
    }

    @Test
    @DisplayName("ITC_CM_11: chuyển khóa học PUBLISHED → ARCHIVED, giữ nguyên published_date cũ")
    void archiveCourse() throws Exception {
        long id = createCourse("N1 Published", "JLPT_N1_ARC", "N1", "0");
        mockMvc.perform(post("/manager/courses/" + id + "/status").with(csrf()).param("status", "PUBLISHED"))
                .andExpect(flash().attribute("courseSuccess", "Đã chuyển trạng thái khóa học."));
        jdbc.update("UPDATE courses SET published_date = TIMESTAMP '2026-01-15 08:00:00' WHERE course_id = ?", id);

        mockMvc.perform(post("/manager/courses/" + id + "/status").with(csrf()).param("status", "ARCHIVED"))
                .andExpect(redirectedUrl("/manager/courses/" + id + "/view"))
                .andExpect(flash().attribute("courseSuccess", "Đã chuyển trạng thái khóa học."));

        Map<String, Object> row = courseRow(id);
        assertThat(row.get("status")).isEqualTo("ARCHIVED");
        assertThat(row.get("published_date")).isEqualTo(Timestamp.valueOf("2026-01-15 08:00:00"));
    }
}
