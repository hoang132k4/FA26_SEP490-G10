package com.jeps.modules.manager.course;

import com.jeps.modules.admin.entity.AdminSystemLog;
import com.jeps.modules.admin.repository.AdminSystemLogRepository;
import com.jeps.modules.manager.courses.dto.CourseOption;
import com.jeps.modules.manager.courses.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerCourseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminSystemLogRepository systemLogRepository;

    @MockBean
    private CourseService courseService;

    @Test
    @DisplayName("IT_MC_01: Learner role should be forbidden from manager courses")
    @WithMockUser(username = "learner@jeps.edu.vn", roles = "LEARNER")
    void learnerIsForbiddenFromCourses() throws Exception {
        mockMvc.perform(get("/manager/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT_MC_02: Unauthenticated user should be redirected to login")
    @WithAnonymousUser
    void unauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/manager/courses"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("IT_MC_03: Manager lists courses successfully")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerCanListCourses() throws Exception {
        CourseOption course1 = new CourseOption(1L, "Khóa N5", "N5-01", "N5", "PUBLISHED", 10,
                "Desc", "Full", "/thumb.jpg", 100000.0, 80000.0, 120, "Grammar", true);
        when(courseService.findCourses()).thenReturn(List.of(course1));

        mockMvc.perform(get("/manager/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/course/manager-courses"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attribute("courseCount", 1));
    }

    @Test
    @DisplayName("Manager displays base price when a zero discount means no discount")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerDisplaysBasePriceForZeroDiscount() throws Exception {
        CourseOption course = new CourseOption(2L, "Zero Discount Course", "ZD-01", "N5", "PUBLISHED", 10,
                "Desc", "Full", "/thumb.jpg", 100000.0, 0.0, 120, "Grammar", false);
        when(courseService.findCourses()).thenReturn(List.of(course));

        mockMvc.perform(get("/manager/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("100,000 đ")))
                .andExpect(content().string(not(containsString("Miễn phí"))));
    }

    @Test
    @DisplayName("IT_MC_04: Manager opens new course modal successfully")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerOpensNewCourseForm() throws Exception {
        when(courseService.findCourses()).thenReturn(List.of());

        mockMvc.perform(get("/manager/courses/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/course/manager-courses"))
                .andExpect(model().attributeExists("courseForm"));
    }

    @Test
    @DisplayName("IT_MC_05: View non-existent course redirects to course list with error flash")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void viewNonExistentCourseRedirectsWithError() throws Exception {
        when(courseService.findCourseDetails(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/manager/courses/999/view"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/courses"))
                .andExpect(flash().attribute("courseError", containsString("Không tìm thấy khóa học")));
    }

    @Test
    @DisplayName("IT_MC_06: Edit non-existent course redirects to course list with error flash")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void editNonExistentCourseRedirectsWithError() throws Exception {
        when(courseService.findCourseDetails(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/manager/courses/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/courses"))
                .andExpect(flash().attribute("courseError", containsString("Không tìm thấy khóa học")));
    }

    @Test
    @DisplayName("IT_MC_07: Create course with missing required fields fails validation and returns form view")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void createCourseValidationFailure() throws Exception {
        when(courseService.findCourses()).thenReturn(List.of());

        mockMvc.perform(post("/manager/courses")
                        .with(csrf())
                        .param("name", "") // Empty name
                        .param("code", "") // Empty code
                        .param("level", "INVALID_LEVEL")
                        .param("price", "-1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/course/manager-courses"))
                .andExpect(model().attributeExists("courseErrors"));
    }

    @Test
    @DisplayName("Successful manager course creation is persisted and visible in admin system logs")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void successfulManagerMutationIsLoggedAndVisibleToAdmin() throws Exception {
        when(courseService.isTitleInUse("Audit Course", null)).thenReturn(false);
        when(courseService.isCodeInUse("AUD-01", null)).thenReturn(false);
        when(courseService.isTitleInUse("Edited Audit Course", 42L)).thenReturn(false);
        when(courseService.isCodeInUse("AUD-02", 42L)).thenReturn(false);

        mockMvc.perform(post("/manager/courses")
                        .with(csrf())
                        .param("name", "Audit Course")
                        .param("code", "AUD-01")
                        .param("level", "N5")
                        .param("price", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/courses"));

        mockMvc.perform(post("/manager/courses/42")
                        .with(csrf())
                        .param("name", "Edited Audit Course")
                        .param("code", "AUD-02")
                        .param("level", "N5")
                        .param("price", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/courses"));

        List<AdminSystemLog> managerLogs = systemLogRepository.findAll().stream()
                .filter(log -> "MANAGER".equals(log.getLogType()))
                .filter(log -> log.getDetails() != null
                        && (log.getDetails().contains("Audit Course")
                        || log.getDetails().contains("Edited Audit Course")))
                .toList();
        assertThat(managerLogs)
                .extracting(AdminSystemLog::getActionPerformed)
                .contains("MANAGER_CREATE", "MANAGER_UPDATE");
        assertThat(managerLogs.stream()
                .filter(log -> "MANAGER_CREATE".equals(log.getActionPerformed()))
                .findFirst()
                .orElseThrow()
                .getDetails())
                .contains("tạo mới khóa học", "Audit Course", "AUD-01")
                .doesNotContain("POST", "HTTP");
        assertThat(managerLogs.stream()
                .filter(log -> "MANAGER_UPDATE".equals(log.getActionPerformed()))
                .findFirst()
                .orElseThrow()
                .getDetails())
                .contains("cập nhật khóa học #42", "Edited Audit Course", "AUD-02")
                .doesNotContain("POST", "HTTP");

        mockMvc.perform(get("/admin/logs").with(user("admin@jeps.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manager tạo mới")))
                .andExpect(content().string(containsString("Manager cập nhật")))
                .andExpect(content().string(containsString("Audit Course")))
                .andExpect(content().string(containsString("Edited Audit Course")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("POST"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("HTTP"))));
    }

    @Test
    @DisplayName("IT_MC_08: View existing course renders detail template")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void viewExistingCourseRendersDetail() throws Exception {
        Map<String, Object> details = Map.of(
                "course_id", 10L,
                "course_title", "Tiếng Nhật Tổng Hợp N5",
                "status", "PUBLISHED",
                "jlpt_level", "N5"
        );
        when(courseService.findCourseDetails(10L)).thenReturn(Optional.of(details));

        mockMvc.perform(get("/manager/courses/10/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/course/course-detail"))
                .andExpect(model().attributeExists("courseDetails"))
                .andExpect(model().attribute("courseId", 10L));
    }
}

