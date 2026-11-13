package com.jlpp.modules.manager.lessons;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.courses.service.CourseService;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.service.LessonService;
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
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerLessonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private LessonService lessonService;

    private CourseOption sampleCourse(long id, String title) {
        return new CourseOption(id, title, "CODE-" + id, "N5", "PUBLISHED", 5,
                "Desc", "Full", "/thumb.jpg", 100000.0, 80000.0, 120, "Grammar", true);
    }

    private LessonRow sampleLesson(long id, long courseId, String title, String section, String skill) {
        return new LessonRow(id, courseId, 1, title, "Desc", "Content", section, 20, false,
                LessonStatusEnum.ACTIVE, skill);
    }

    @Test
    @DisplayName("IT_ML_01: Learner role should be forbidden from manager lessons workspace")
    @WithMockUser(username = "learner@jeps.edu.vn", roles = "LEARNER")
    void learnerIsForbiddenFromLessons() throws Exception {
        mockMvc.perform(get("/manager/lessons"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT_ML_02: Unauthenticated user should be redirected to login")
    @WithAnonymousUser
    void unauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/manager/lessons"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("IT_ML_03: Manager opens lessons workspace without courses")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerOpensWorkspaceWithoutCourses() throws Exception {
        when(courseService.findCourses()).thenReturn(List.of());

        mockMvc.perform(get("/manager/lessons"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/lesson/manager-lessons"))
                .andExpect(model().attributeExists("courses"));
    }

    @Test
    @DisplayName("IT_ML_04: Manager opens workspace with selected course and grouped chapters")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerOpensWorkspaceWithCourse() throws Exception {
        CourseOption course = sampleCourse(1L, "Khóa N5 Cơ bản");
        LessonRow lesson = sampleLesson(10L, 1L, "Từ vựng Bài 1", "Chương 1", "VOCABULARY");

        when(courseService.findCourses()).thenReturn(List.of(course));
        when(lessonService.findLessons(1L)).thenReturn(List.of(lesson));
        when(lessonService.findLessonIdsWithFlashcards(any())).thenReturn(Set.of());
        when(lessonService.findLessonIdsWithExercises(any())).thenReturn(Set.of());

        mockMvc.perform(get("/manager/lessons").param("courseId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/manager/lesson/manager-lessons"))
                .andExpect(model().attribute("selectedCourseId", 1L))
                .andExpect(model().attributeExists("lessons"))
                .andExpect(model().attributeExists("skillTabs"));
    }

    @Test
    @DisplayName("IT_ML_05: Manager deletes lesson and redirects with success flash")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerDeletesLessonSuccessfully() throws Exception {
        LessonRow lesson = sampleLesson(10L, 1L, "Bài xóa", "Chương 1", "VOCABULARY");
        when(lessonService.findLesson(10L)).thenReturn(Optional.of(lesson));

        mockMvc.perform(post("/manager/lessons/10/delete")
                        .with(csrf())
                        .param("courseId", "1")
                        .param("skill", "VOCABULARY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/lessons?courseId=1&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", containsString("Đã xoá bài học")));

        verify(lessonService).delete(10L);
    }

    @Test
    @DisplayName("IT_ML_06: Manager duplicates lesson and redirects with success flash")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerDuplicatesLessonSuccessfully() throws Exception {
        LessonRow lesson = sampleLesson(10L, 1L, "Bài gốc", "Chương 1", "VOCABULARY");
        when(lessonService.findLesson(10L)).thenReturn(Optional.of(lesson));
        when(lessonService.duplicate(10L)).thenReturn(20L);

        mockMvc.perform(post("/manager/lessons/10/duplicate")
                        .with(csrf())
                        .param("courseId", "1")
                        .param("skill", "VOCABULARY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/lessons?courseId=1&skill=VOCABULARY"))
                .andExpect(flash().attribute("lessonSuccess", containsString("Đã nhân bản")));

        verify(lessonService).duplicate(10L);
    }

    @Test
    @DisplayName("IT_ML_07: Manager toggles lesson status and redirects")
    @WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
    void managerChangesLessonStatus() throws Exception {
        LessonRow lesson = sampleLesson(10L, 1L, "Bài toggle", "Chương 1", "VOCABULARY");
        when(lessonService.findLesson(10L)).thenReturn(Optional.of(lesson));
        when(lessonService.changeStatus(eq(10L), any())).thenReturn(LessonStatusEnum.INACTIVE);

        mockMvc.perform(post("/manager/lessons/10/status")
                        .with(csrf())
                        .param("courseId", "1")
                        .param("status", "INACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                .andExpect(flash().attribute("lessonSuccess", containsString("Đã ẩn bài học")));

        verify(lessonService).changeStatus(eq(10L), eq(LessonStatusEnum.INACTIVE));
    }
}
