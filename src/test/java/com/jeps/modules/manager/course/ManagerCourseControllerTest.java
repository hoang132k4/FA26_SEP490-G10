package com.jeps.modules.manager.course;

import com.jeps.modules.course.service.CoursePriceCalculator;
import com.jeps.modules.manager.courses.controller.ManagerCourseController;
import com.jeps.modules.manager.courses.dto.CourseOption;
import com.jeps.modules.manager.courses.service.CourseImageStorageService;
import com.jeps.modules.manager.courses.service.CourseService;
import com.jeps.modules.notification.service.NotificationService;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Report 5.1 – sheet ManagerCourseController (UTC_MCC_01 → UTC_MCC_52).
 * Controller layer: @WebMvcTest + MockMvc, mọi dependency của controller là @MockBean.
 */
@WebMvcTest(ManagerCourseController.class)
@ActiveProfiles("test")
@WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
class ManagerCourseControllerTest {

    private static final String LIST_VIEW = "pages/manager/course/manager-courses";
    private static final String DETAIL_VIEW = "pages/manager/course/course-detail";
    private static final String CHECK_FIELDS = "Vui lòng kiểm tra các trường được đánh dấu.";

    @Autowired private MockMvc mockMvc;
    @MockBean private CourseService courseService;
    @MockBean private CourseImageStorageService courseImageStorageService;
    @MockBean private CoursePriceCalculator priceCalculator;
    @MockBean private UserRepository userRepository;
    @MockBean private NotificationService notificationService;

    // ------------------------------------------------------------------ helpers

    /** Base form của report: name "N5 Course", code "N5_ONLINE", level "N5", price "500000", ... */
    private static Map<String, String> baseForm() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", "N5 Course");
        form.put("code", "N5_ONLINE");
        form.put("level", "N5");
        form.put("price", "500000");
        form.put("shortDescription", "Desc");
        form.put("objectives", "Obj");
        form.put("imageMode", "url");
        form.put("thumbnailUrl", "");
        return form;
    }

    /** Base form + các trường khác biệt; giá trị null nghĩa là bỏ tham số đó. */
    private static MockHttpServletRequestBuilder postForm(String url, String... overrides) {
        Map<String, String> form = baseForm();
        for (int i = 0; i < overrides.length; i += 2) form.put(overrides[i], overrides[i + 1]);
        MockHttpServletRequestBuilder builder = post(url).with(csrf());
        form.forEach((key, value) -> { if (value != null) builder.param(key, value); });
        return builder;
    }

    private static MockMultipartHttpServletRequestBuilder multipartForm(MockMultipartFile file) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/manager/courses").file(file);
        Map<String, String> form = baseForm();
        form.put("imageMode", "file");
        form.forEach(builder::param);
        builder.with(csrf());
        return builder;
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    private static CourseOption course(long id) {
        return new CourseOption(id, "Course " + id, "CODE_" + id, "N5", "DRAFT", 3,
                "Desc", "Full", null, 500000.0, null, 90, "VOCABULARY", false);
    }

    private static Map<String, Object> courseDetails(String status) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("course_id", 1L);
        details.put("course_title", "N5 Course");
        details.put("code", "N5_ONLINE");
        details.put("jlpt_level", "N5");
        details.put("status", status);
        details.put("price", 500000.0);
        details.put("short_description", "Desc");
        details.put("objectives", "Obj");
        return details;
    }

    private void verifyCreateNeverCalled() {
        verify(courseService, never()).createCourse(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private void verifyUpdateNeverCalled() {
        verify(courseService, never()).updateCourse(anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ------------------------------------------------------------------ courses()

    @Nested
    @DisplayName("GET /manager/courses")
    class ListCourses {

        @Test
        @DisplayName("UTC_MCC_01: hiển thị danh sách 5 khóa học")
        void listsCourses() throws Exception {
            when(courseService.findCourses()).thenReturn(IntStream.rangeClosed(1, 5).mapToObj(i -> course(i)).toList());

            mockMvc.perform(get("/manager/courses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseCount", 5));
        }

        @Test
        @DisplayName("UTC_MCC_02: danh sách rỗng")
        void emptyList() throws Exception {
            when(courseService.findCourses()).thenReturn(List.of());

            mockMvc.perform(get("/manager/courses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courses", empty()))
                    .andExpect(model().attribute("courseCount", 0));
        }

        @Test
        @DisplayName("UTC_MCC_03: lỗi database vẫn hiển thị trang với danh sách rỗng và courseError")
        void databaseFailure() throws Exception {
            when(courseService.findCourses()).thenThrow(new DataAccessResourceFailureException("down"));

            mockMvc.perform(get("/manager/courses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courses", empty()))
                    .andExpect(model().attribute("courseError",
                            "Không kết nối được Supabase. Kiểm tra file .env và thông tin Session Pooler rồi tải lại."));
        }
    }

    // ------------------------------------------------------------------ newCourse / editCourse / viewCourse

    @Nested
    @DisplayName("Form tạo / sửa và trang chi tiết")
    class FormsAndDetail {

        @Test
        @DisplayName("UTC_MCC_04: form tạo khóa học mặc định level N5, price 0, imageMode file")
        void newCourseForm() throws Exception {
            when(courseService.findCourses()).thenReturn(List.of());

            mockMvc.perform(get("/manager/courses/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseForm", hasEntry("level", (Object) "N5")))
                    .andExpect(model().attribute("courseForm", hasEntry("price", (Object) "0")))
                    .andExpect(model().attribute("courseForm", hasEntry("imageMode", (Object) "file")));
        }

        @Test
        @DisplayName("UTC_MCC_05: form sửa khóa học id=1 với imageMode url")
        void editCourseForm() throws Exception {
            when(courseService.findCourseDetails(1L)).thenReturn(Optional.of(courseDetails("DRAFT")));
            when(courseService.findCourses()).thenReturn(List.of(course(1L)));

            mockMvc.perform(get("/manager/courses/1/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseForm", hasEntry("id", (Object) 1L)))
                    .andExpect(model().attribute("courseForm", hasEntry("imageMode", (Object) "url")));
        }

        @Test
        @DisplayName("UTC_MCC_06: sửa khóa học không tồn tại thì quay về danh sách")
        void editUnknownCourse() throws Exception {
            when(courseService.findCourseDetails(999999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/manager/courses/999999/edit"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseError", "Không tìm thấy khóa học cần sửa."));
        }

        @Test
        @DisplayName("UTC_MCC_07: trang chi tiết khóa học DRAFT có trạng thái kế tiếp PUBLISHED")
        void viewDraftCourse() throws Exception {
            when(courseService.findCourseDetails(1L)).thenReturn(Optional.of(courseDetails("DRAFT")));

            mockMvc.perform(get("/manager/courses/1/view"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(DETAIL_VIEW))
                    .andExpect(model().attribute("courseId", 1L))
                    .andExpect(model().attribute("courseNextStatus", "PUBLISHED"));
        }

        @Test
        @DisplayName("UTC_MCC_08: xem khóa học không tồn tại thì quay về danh sách")
        void viewUnknownCourse() throws Exception {
            when(courseService.findCourseDetails(999999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/manager/courses/999999/view"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseError", "Không tìm thấy khóa học."));
        }
    }

    // ------------------------------------------------------------------ createCourse

    @Nested
    @DisplayName("POST /manager/courses (createCourse)")
    class CreateCourse {

        private void expectCreated(MockHttpServletRequestBuilder request) throws Exception {
            mockMvc.perform(request)
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseSuccess", "Đã thêm khóa học."));
        }

        private void expectFieldError(MockHttpServletRequestBuilder request, String field, String message) throws Exception {
            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseErrors", hasEntry(field, message)))
                    .andExpect(model().attribute("courseError", CHECK_FIELDS));
            verifyCreateNeverCalled();
        }

        @Test
        @DisplayName("UTC_MCC_09: tạo khóa học hợp lệ")
        void createsCourse() throws Exception {
            expectCreated(postForm("/manager/courses"));
            verify(courseService, times(1)).createCourse(eq("N5 Course"), eq("N5_ONLINE"), eq("N5"), eq(500000.0),
                    isNull(), eq("Desc"), isNull(), isNull(), eq("Obj"), isNull(), isNull());
        }

        @Test
        @DisplayName("UTC_MCC_10: tên 255 ký tự hợp lệ")
        void name255() throws Exception {
            expectCreated(postForm("/manager/courses", "name", repeat('A', 255)));
        }

        @Test
        @DisplayName("UTC_MCC_11: tên 256 ký tự bị từ chối")
        void name256() throws Exception {
            expectFieldError(postForm("/manager/courses", "name", repeat('A', 256)), "name", "Tên khóa học tối đa 255 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_12: tên rỗng bị từ chối")
        void blankName() throws Exception {
            expectFieldError(postForm("/manager/courses", "name", "   "), "name", "Vui lòng nhập tên khóa học.");
        }

        @Test
        @DisplayName("UTC_MCC_13: mã 50 ký tự hợp lệ")
        void code50() throws Exception {
            expectCreated(postForm("/manager/courses", "code", repeat('C', 50)));
        }

        @Test
        @DisplayName("UTC_MCC_14: mã 51 ký tự bị từ chối")
        void code51() throws Exception {
            expectFieldError(postForm("/manager/courses", "code", repeat('C', 51)), "code", "Mã khóa học tối đa 50 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_15: mã rỗng bị từ chối")
        void blankCode() throws Exception {
            expectFieldError(postForm("/manager/courses", "code", "   "), "code", "Vui lòng nhập mã khóa học.");
        }

        @Test
        @DisplayName("UTC_MCC_16: giá không phải số nguyên bị từ chối")
        void decimalPrice() throws Exception {
            expectFieldError(postForm("/manager/courses", "price", "500000.5"), "price", "Vui lòng nhập một số nguyên hợp lệ.");
        }

        @Test
        @DisplayName("UTC_MCC_17: cấp độ JLPT không hợp lệ bị từ chối")
        void invalidLevel() throws Exception {
            expectFieldError(postForm("/manager/courses", "level", "INVALID_LEVEL"), "level", "Vui lòng chọn cấp độ JLPT hợp lệ.");
        }

        @Test
        @DisplayName("UTC_MCC_18: thiếu cấp độ JLPT bị từ chối")
        void missingLevel() throws Exception {
            expectFieldError(postForm("/manager/courses", "level", null), "level", "Vui lòng chọn cấp độ JLPT hợp lệ.");
        }

        @Test
        @DisplayName("UTC_MCC_19: giá 0 (khóa miễn phí) hợp lệ")
        void freeCourse() throws Exception {
            expectCreated(postForm("/manager/courses", "price", "0"));
        }

        @Test
        @DisplayName("UTC_MCC_20: giá âm bị từ chối")
        void negativePrice() throws Exception {
            expectFieldError(postForm("/manager/courses", "price", "-1"), "price", "Giá phải là số nguyên từ 0 trở lên.");
        }

        @Test
        @DisplayName("UTC_MCC_21: giá ưu đãi bằng giá gốc bị từ chối")
        void discountEqualsPrice() throws Exception {
            expectFieldError(postForm("/manager/courses", "discountPrice", "500000"), "discountPrice",
                    "Giá ưu đãi không được cao hơn hoặc bằng giá gốc.");
        }

        @Test
        @DisplayName("UTC_MCC_22: giá ưu đãi bằng 0 bị từ chối")
        void zeroDiscount() throws Exception {
            expectFieldError(postForm("/manager/courses", "discountPrice", "0"), "discountPrice", "Giá ưu đãi phải lớn hơn 0 nếu có.");
        }

        @Test
        @DisplayName("UTC_MCC_23: giá ưu đãi bằng 1 hợp lệ")
        void discountOne() throws Exception {
            expectCreated(postForm("/manager/courses", "discountPrice", "1"));
        }

        @Test
        @DisplayName("UTC_MCC_24: mô tả ngắn rỗng bị từ chối")
        void blankShortDescription() throws Exception {
            expectFieldError(postForm("/manager/courses", "shortDescription", "   "), "shortDescription", "Vui lòng nhập mô tả ngắn.");
        }

        @Test
        @DisplayName("UTC_MCC_25: mô tả ngắn 500 ký tự hợp lệ")
        void shortDescription500() throws Exception {
            expectCreated(postForm("/manager/courses", "shortDescription", repeat('D', 500)));
        }

        @Test
        @DisplayName("UTC_MCC_26: mô tả ngắn 501 ký tự bị từ chối")
        void shortDescription501() throws Exception {
            expectFieldError(postForm("/manager/courses", "shortDescription", repeat('D', 501)), "shortDescription",
                    "Mô tả ngắn tối đa 500 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_27: mục tiêu rỗng bị từ chối")
        void blankObjectives() throws Exception {
            expectFieldError(postForm("/manager/courses", "objectives", "   "), "objectives", "Vui lòng nhập mục tiêu khóa học.");
        }

        @Test
        @DisplayName("UTC_MCC_28: mục tiêu 5000 ký tự hợp lệ")
        void objectives5000() throws Exception {
            expectCreated(postForm("/manager/courses", "objectives", repeat('O', 5000)));
        }

        @Test
        @DisplayName("UTC_MCC_29: mục tiêu 5001 ký tự bị từ chối")
        void objectives5001() throws Exception {
            expectFieldError(postForm("/manager/courses", "objectives", repeat('O', 5001)), "objectives",
                    "Mục tiêu khóa học tối đa 5000 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_30: ảnh bằng URL hợp lệ")
        void validImageUrl() throws Exception {
            expectCreated(postForm("/manager/courses", "thumbnailUrl", "https://example.com/img.png"));
            verify(courseService).createCourse(any(), any(), any(), any(), any(), any(), any(),
                    eq("https://example.com/img.png"), any(), any(), any());
        }

        @Test
        @DisplayName("UTC_MCC_31: URL ảnh không hợp lệ bị từ chối")
        void invalidImageUrl() throws Exception {
            expectFieldError(postForm("/manager/courses", "thumbnailUrl", "not-a-valid-url"), "thumbnailUrl",
                    "Đường dẫn ảnh phải là URL http/https hợp lệ hoặc ảnh đã tải lên, tối đa 500 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_32: tải ảnh PNG 2MB hợp lệ")
        void uploadsImage() throws Exception {
            MockMultipartFile png = new MockMultipartFile("imageFile", "cover.png", "image/png", new byte[2 * 1024 * 1024]);
            when(courseImageStorageService.hasFile(any())).thenReturn(true);
            when(courseImageStorageService.store(any())).thenReturn("/uploads/courses/abc.png");

            mockMvc.perform(multipartForm(png))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseSuccess", "Đã thêm khóa học."));
            verify(courseService).createCourse(any(), any(), any(), any(), any(), any(), any(),
                    eq("/uploads/courses/abc.png"), any(), any(), any());
        }

        @Test
        @DisplayName("UTC_MCC_33: ảnh vượt 5 MB bị từ chối")
        void imageTooLarge() throws Exception {
            MockMultipartFile big = new MockMultipartFile("imageFile", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
            when(courseImageStorageService.hasFile(any())).thenReturn(true);
            doThrow(new IllegalArgumentException("Ảnh khóa học không được vượt quá 5 MB."))
                    .when(courseImageStorageService).validate(any());

            mockMvc.perform(multipartForm(big))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseErrors", hasEntry("imageFile", "Ảnh khóa học không được vượt quá 5 MB.")))
                    .andExpect(model().attribute("courseError", CHECK_FIELDS));
            verifyCreateNeverCalled();
        }

        @Test
        @DisplayName("UTC_MCC_34: file không phải ảnh bị từ chối")
        void unsupportedFileType() throws Exception {
            MockMultipartFile text = new MockMultipartFile("imageFile", "notes.txt", "text/plain", "hello".getBytes());
            when(courseImageStorageService.hasFile(any())).thenReturn(true);
            doThrow(new IllegalArgumentException("Chỉ chấp nhận ảnh PNG, JPG hoặc JPEG."))
                    .when(courseImageStorageService).validate(any());

            mockMvc.perform(multipartForm(text))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("courseErrors", hasEntry("imageFile", "Chỉ chấp nhận ảnh PNG, JPG hoặc JPEG.")))
                    .andExpect(model().attribute("courseError", CHECK_FIELDS));
            verifyCreateNeverCalled();
        }

        @Test
        @DisplayName("UTC_MCC_35: tên khóa học đã tồn tại bị từ chối")
        void duplicateTitle() throws Exception {
            when(courseService.isTitleInUse("N5 Course", null)).thenReturn(true);

            expectFieldError(postForm("/manager/courses"), "name", "Tên khóa học này đã được sử dụng.");
        }

        @Test
        @DisplayName("UTC_MCC_36: mã khóa học đã tồn tại bị từ chối")
        void duplicateCode() throws Exception {
            when(courseService.isCodeInUse("N5_ONLINE", null)).thenReturn(true);

            expectFieldError(postForm("/manager/courses", "name", "Different Course"), "code", "Mã khóa học này đã được sử dụng.");
        }

        @Test
        @DisplayName("UTC_MCC_37: lỗi database khi tạo khóa học hiện thông báo lỗi")
        void databaseFailure() throws Exception {
            when(courseService.createCourse(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new DataAccessResourceFailureException("down"));

            mockMvc.perform(postForm("/manager/courses"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseError",
                            "Không thể hoàn tất thao tác thêm khóa học. Kiểm tra log ứng dụng để biết chi tiết."));
        }
    }

    // ------------------------------------------------------------------ updateCourse

    @Nested
    @DisplayName("POST /manager/courses/{id} (updateCourse)")
    class UpdateCourse {

        private void expectFieldError(MockHttpServletRequestBuilder request, String field, String message) throws Exception {
            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseErrors", hasEntry(field, message)))
                    .andExpect(model().attribute("courseForm", hasEntry("id", (Object) "1")));
            verifyUpdateNeverCalled();
        }

        @Test
        @DisplayName("UTC_MCC_38: cập nhật khóa học hợp lệ")
        void updatesCourse() throws Exception {
            mockMvc.perform(postForm("/manager/courses/1", "name", "Updated Course", "price", "600000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseSuccess", "Đã cập nhật khóa học."));
            verify(courseService, times(1)).updateCourse(eq(1L), eq("Updated Course"), eq("N5_ONLINE"), eq("N5"),
                    eq(600000.0), isNull(), eq("Desc"), isNull(), isNull(), eq("Obj"), isNull(), isNull());
        }

        @Test
        @DisplayName("UTC_MCC_39: cập nhật khóa học không tồn tại báo lỗi dưới trường ảnh (DEF-CM-01)")
        void updateUnknownCourse() throws Exception {
            doThrow(new IllegalArgumentException("Không tìm thấy khóa học cần sửa."))
                    .when(courseService).updateCourse(eq(999999L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

            mockMvc.perform(postForm("/manager/courses/999999"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(LIST_VIEW))
                    .andExpect(model().attribute("courseErrors", hasEntry("imageFile", "Không tìm thấy khóa học cần sửa.")));
        }

        @Test
        @DisplayName("UTC_MCC_40: tên rỗng khi cập nhật bị từ chối")
        void blankName() throws Exception {
            expectFieldError(postForm("/manager/courses/1", "name", "   "), "name", "Vui lòng nhập tên khóa học.");
        }

        @Test
        @DisplayName("UTC_MCC_41: tên 256 ký tự khi cập nhật bị từ chối")
        void name256() throws Exception {
            expectFieldError(postForm("/manager/courses/1", "name", repeat('A', 256)), "name", "Tên khóa học tối đa 255 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_42: mã 51 ký tự khi cập nhật bị từ chối")
        void code51() throws Exception {
            expectFieldError(postForm("/manager/courses/1", "code", repeat('C', 51)), "code", "Mã khóa học tối đa 50 ký tự.");
        }

        @Test
        @DisplayName("UTC_MCC_43: giá âm khi cập nhật bị từ chối")
        void negativePrice() throws Exception {
            expectFieldError(postForm("/manager/courses/1", "price", "-10"), "price", "Giá phải là số nguyên từ 0 trở lên.");
        }

        @Test
        @DisplayName("UTC_MCC_44: tên trùng khóa học khác khi cập nhật bị từ chối")
        void duplicateTitle() throws Exception {
            when(courseService.isTitleInUse("Existing Title", 1L)).thenReturn(true);

            expectFieldError(postForm("/manager/courses/1", "name", "Existing Title"), "name", "Tên khóa học này đã được sử dụng.");
        }

        @Test
        @DisplayName("UTC_MCC_45: mã trùng khóa học khác khi cập nhật bị từ chối")
        void duplicateCode() throws Exception {
            when(courseService.isCodeInUse("EXISTING_CODE", 1L)).thenReturn(true);

            expectFieldError(postForm("/manager/courses/1", "code", "EXISTING_CODE"), "code", "Mã khóa học này đã được sử dụng.");
        }

        @Test
        @DisplayName("UTC_MCC_46: lỗi database khi cập nhật hiện thông báo lỗi")
        void databaseFailure() throws Exception {
            doThrow(new DataAccessResourceFailureException("down"))
                    .when(courseService).updateCourse(anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

            mockMvc.perform(postForm("/manager/courses/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses"))
                    .andExpect(flash().attribute("courseError",
                            "Không thể hoàn tất thao tác cập nhật khóa học. Kiểm tra log ứng dụng để biết chi tiết."));
        }
    }

    // ------------------------------------------------------------------ transitionCourseStatus

    @Nested
    @DisplayName("POST /manager/courses/{id}/status")
    class TransitionStatus {

        private void expectTransitioned(String target) throws Exception {
            mockMvc.perform(post("/manager/courses/1/status").with(csrf()).param("status", target))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses/1/view"))
                    .andExpect(flash().attribute("courseSuccess", "Đã chuyển trạng thái khóa học."));
            verify(courseService).transitionCourseStatus(1L, target);
        }

        @Test
        @DisplayName("UTC_MCC_47: DRAFT → PUBLISHED")
        void toPublished() throws Exception {
            expectTransitioned("PUBLISHED");
        }

        @Test
        @DisplayName("UTC_MCC_48: PUBLISHED → ARCHIVED")
        void toArchived() throws Exception {
            expectTransitioned("ARCHIVED");
        }

        @Test
        @DisplayName("UTC_MCC_49: ARCHIVED → DRAFT")
        void toDraft() throws Exception {
            expectTransitioned("DRAFT");
        }

        @Test
        @DisplayName("UTC_MCC_50: trạng thái không hợp lệ bị từ chối")
        void invalidStatus() throws Exception {
            doThrow(new IllegalArgumentException("Vui lòng chọn trạng thái khóa học hợp lệ."))
                    .when(courseService).transitionCourseStatus(1L, "INVALID_STATUS");

            mockMvc.perform(post("/manager/courses/1/status").with(csrf()).param("status", "INVALID_STATUS"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses/1/view"))
                    .andExpect(flash().attribute("courseError", "Vui lòng chọn trạng thái khóa học hợp lệ."));
        }

        @Test
        @DisplayName("UTC_MCC_51: khóa học không tồn tại")
        void unknownCourse() throws Exception {
            doThrow(new IllegalArgumentException("Không tìm thấy khóa học cần chuyển trạng thái."))
                    .when(courseService).transitionCourseStatus(999999L, "PUBLISHED");

            mockMvc.perform(post("/manager/courses/999999/status").with(csrf()).param("status", "PUBLISHED"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/courses/999999/view"))
                    .andExpect(flash().attribute("courseError", "Không tìm thấy khóa học cần chuyển trạng thái."));
        }

        @Test
        @DisplayName("UTC_MCC_52: thiếu tham số status trả về 400")
        void missingStatus() throws Exception {
            mockMvc.perform(post("/manager/courses/1/status").with(csrf()))
                    .andExpect(status().isBadRequest());
            verify(courseService, never()).transitionCourseStatus(anyLong(), anyString());
        }
    }
}
