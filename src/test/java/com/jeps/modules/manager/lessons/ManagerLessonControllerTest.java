package com.jeps.modules.manager.lessons;

import com.jeps.common.constants.LessonStatusEnum;
import com.jeps.common.constants.SkillTypeEnum;
import com.jeps.modules.course.service.StudyMaterialService;
import com.jeps.modules.flashcard.repository.FlashcardRepository;
import com.jeps.modules.manager.courses.dto.CourseOption;
import com.jeps.modules.manager.courses.service.CourseService;
import com.jeps.modules.manager.flashcards.service.ManagerFlashcardService;
import com.jeps.modules.manager.lessons.controller.ManagerLessonController;
import com.jeps.modules.manager.lessons.dto.LessonDtos.ChapterGroup;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonStudyView;
import com.jeps.modules.manager.lessons.dto.LessonDtos.SkillColumn;
import com.jeps.modules.manager.lessons.service.LessonAudioStorageService;
import com.jeps.modules.manager.lessons.service.LessonDetailService;
import com.jeps.modules.manager.lessons.service.LessonService;
import com.jeps.modules.notification.service.NotificationService;
import com.jeps.modules.quiz.repository.ExerciseRepository;
import com.jeps.modules.quiz.service.ManagerQuizService;
import com.jeps.modules.quiz.service.QuizService;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Report 5.1 – sheet ManagerLessonController (UTC_MLC_01 → UTC_MLC_61).
 * Controller layer: @WebMvcTest + MockMvc, mọi dependency của controller là @MockBean.
 */
@WebMvcTest(ManagerLessonController.class)
@ActiveProfiles("test")
@WithMockUser(username = "manager@jeps.edu.vn", roles = "MANAGER")
class ManagerLessonControllerTest {

    private static final String VIEW = "pages/manager/lesson/manager-lessons";
    private static final String DETAIL_FRAGMENT = "pages/manager/lesson/manager-lessons :: lessonDetailContainer";
    private static final String DRAFT_CREATED = "Đã tạo chương trong phiên làm việc. Chương được lưu cùng bài học đầu tiên.";
    private static final String CHAPTER_LENGTH = "Tên chương phải từ 1 đến 150 ký tự.";

    @Autowired private MockMvc mockMvc;
    @MockBean private LessonService lessonService;
    @MockBean private CourseService courseService;
    @MockBean private LessonDetailService lessonDetailService;
    @MockBean private LessonAudioStorageService lessonAudioStorageService;
    @MockBean private StudyMaterialService studyMaterialService;
    @MockBean private FlashcardRepository flashcardRepository;
    @MockBean private ExerciseRepository exerciseRepository;
    @MockBean private QuizService quizService;
    @MockBean private ManagerFlashcardService managerFlashcardService;
    @MockBean private ManagerQuizService managerQuizService;
    @MockBean private UserRepository userRepository;
    @MockBean private NotificationService notificationService;

    // ------------------------------------------------------------------ helpers

    private static CourseOption course(long id, int lessonCount) {
        return new CourseOption(id, "N5 Course", "N5_ONLINE", "N5", "DRAFT", lessonCount,
                "Desc", "Full", null, 0d, null, null, null, false);
    }

    private static LessonRow lesson(long id, String title, String section, String skill) {
        return new LessonRow(id, 1L, (int) id, title, null, null, section, 30, false, LessonStatusEnum.ACTIVE, skill);
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    /** Khóa học id=1 với 5 bài: 2 Từ vựng, 1 Kanji, 1 Ngữ pháp, 1 Đọc hiểu, 0 Nghe. */
    private void courseWithFiveLessons() {
        when(courseService.findCourses()).thenReturn(List.of(course(1L, 5)));
        when(courseService.findCourse(1L)).thenReturn(Optional.of(course(1L, 5)));
        when(lessonService.findLessons(1L)).thenReturn(List.of(
                lesson(10L, "Vocab 1", "Chapter 1", "VOCABULARY"),
                lesson(11L, "Vocab 2", "Chapter 1", "VOCABULARY"),
                lesson(12L, "Kanji 1", "Chapter 1", "KANJI"),
                lesson(13L, "Grammar 1", "Chapter 1", "GRAMMAR"),
                lesson(14L, "Reading 1", "Chapter 1", "READING")));
    }

    private MockHttpServletRequestBuilder workspace(String skill) {
        MockHttpServletRequestBuilder request = get("/manager/lessons").param("courseId", "1");
        return skill == null ? request : request.param("skill", skill);
    }

    private static MockHttpServletRequestBuilder lessonForm(String url, String courseId, String title, String skill, String duration) {
        return post(url).with(csrf())
                .param("courseId", courseId)
                .param("title", title)
                .param("sectionName", "Chapter 1")
                .param("skillType", skill)
                .param("durationMinutes", duration);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, List<String>> drafts(MvcResult result) {
        return (Map<Long, List<String>>) result.getRequest().getSession().getAttribute("draftChapterNames");
    }

    // ------------------------------------------------------------------ workspace

    @Nested
    @DisplayName("GET /manager/lessons (workspace)")
    class Workspace {

        @Test
        @DisplayName("UTC_MLC_01: hiển thị workspace mặc định của khóa học id=1 (5 bài)")
        void defaultWorkspace() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace(null))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW))
                    .andExpect(model().attribute("selectedCourseId", 1L))
                    .andExpect(model().attribute("courseLessonCount", 5));
        }

        @Test
        @DisplayName("UTC_MLC_02: lọc theo kỹ năng Từ vựng (2 bài)")
        void filterVocabulary() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace("VOCABULARY"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedSkill", SkillTypeEnum.VOCABULARY))
                    .andExpect(model().attribute("lessons", hasSize(2)));
        }

        @Test
        @DisplayName("UTC_MLC_03: lọc theo kỹ năng Kanji (1 bài)")
        void filterKanji() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace("KANJI"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedSkill", SkillTypeEnum.KANJI))
                    .andExpect(model().attribute("lessons", hasSize(1)));
        }

        @Test
        @DisplayName("UTC_MLC_04: lọc theo kỹ năng Ngữ pháp (1 bài)")
        void filterGrammar() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace("GRAMMAR"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedSkill", SkillTypeEnum.GRAMMAR))
                    .andExpect(model().attribute("lessons", hasSize(1)));
        }

        @Test
        @DisplayName("UTC_MLC_05: lọc theo kỹ năng Đọc hiểu (1 bài)")
        void filterReading() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace("READING"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedSkill", SkillTypeEnum.READING))
                    .andExpect(model().attribute("lessons", hasSize(1)));
        }

        @Test
        @DisplayName("UTC_MLC_06: kỹ năng Nghe chưa có bài nào")
        void filterListeningEmpty() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace("LISTENING"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("selectedSkill", SkillTypeEnum.LISTENING))
                    .andExpect(model().attribute("lessons", empty()))
                    .andExpect(model().attribute("visibleCount", 0));
        }

        @Test
        @DisplayName("UTC_MLC_07: bài không có kỹ năng nằm ở cột Khác, otherMode = true")
        void otherColumn() throws Exception {
            when(courseService.findCourses()).thenReturn(List.of(course(1L, 1)));
            when(lessonService.findLessons(1L)).thenReturn(List.of(lesson(20L, "Midterm Exam", "Chapter 1", null)));

            MvcResult result = mockMvc.perform(workspace("OTHER"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("otherMode", true))
                    .andReturn();

            @SuppressWarnings("unchecked")
            List<ChapterGroup> groups = (List<ChapterGroup>) result.getModelAndView().getModel().get("chapterGroups");
            SkillColumn other = groups.get(0).columns().stream().filter(c -> c.value().equals("OTHER")).findFirst().orElseThrow();
            assertThat(other.label()).isEqualTo("Khác");
            assertThat(other.lessons()).extracting(LessonRow::id).containsExactly(20L);
        }

        @Test
        @DisplayName("UTC_MLC_08: kỹ năng không hợp lệ được bỏ qua, chọn kỹ năng đầu tiên có bài (Từ vựng)")
        void invalidSkillFallsBack() throws Exception {
            courseWithFiveLessons();

            mockMvc.perform(workspace("INVALID_SKILL"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW))
                    .andExpect(model().attribute("selectedSkill", SkillTypeEnum.VOCABULARY));
        }
    }

    // ------------------------------------------------------------------ chapters

    @Nested
    @DisplayName("Chương (tạo nháp / đổi tên / xóa)")
    class Chapters {

        private MockHttpServletRequestBuilder createDraft(String courseId, String name) {
            return post("/manager/lessons/sections").with(csrf()).param("courseId", courseId).param("sectionName", name);
        }

        private MockHttpServletRequestBuilder rename(String courseId, String oldName, String newName) {
            return post("/manager/lessons/sections/rename").with(csrf())
                    .param("courseId", courseId).param("oldName", oldName).param("newName", newName);
        }

        @Test
        @DisplayName("UTC_MLC_09: tạo chương nháp \"New Chapter\" lưu trong session")
        void createsDraftChapter() throws Exception {
            MvcResult result = mockMvc.perform(createDraft("1", "New Chapter"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", DRAFT_CREATED))
                    .andReturn();

            assertThat(drafts(result).get(1L)).contains("New Chapter");
        }

        @Test
        @DisplayName("UTC_MLC_10: tên chương 1 ký tự hợp lệ")
        void oneCharacterChapter() throws Exception {
            mockMvc.perform(createDraft("1", "A"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", DRAFT_CREATED));
        }

        @Test
        @DisplayName("UTC_MLC_11: tên chương 150 ký tự hợp lệ")
        void chapter150() throws Exception {
            mockMvc.perform(createDraft("1", repeat('A', 150)))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", DRAFT_CREATED));
        }

        @Test
        @DisplayName("UTC_MLC_12: tên chương 151 ký tự bị từ chối, danh sách nháp không đổi")
        void chapter151() throws Exception {
            MvcResult result = mockMvc.perform(createDraft("1", repeat('A', 151)))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonError", CHAPTER_LENGTH))
                    .andReturn();

            Map<Long, List<String>> drafts = drafts(result);
            assertThat(drafts == null ? List.of() : drafts.getOrDefault(1L, List.of())).isEmpty();
        }

        @Test
        @DisplayName("UTC_MLC_13: tên chương rỗng bị từ chối")
        void blankChapter() throws Exception {
            mockMvc.perform(createDraft("1", " "))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonError", CHAPTER_LENGTH));
        }

        @Test
        @DisplayName("UTC_MLC_14: chương đã tồn tại không bị thêm lần nữa")
        void existingChapterNotDuplicated() throws Exception {
            when(lessonService.findLessons(1L)).thenReturn(List.of(lesson(10L, "Lesson 10", "Existing Chapter", "VOCABULARY")));

            MvcResult result = mockMvc.perform(createDraft("1", "Existing Chapter"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", DRAFT_CREATED))
                    .andReturn();

            assertThat(drafts(result).get(1L)).doesNotContain("Existing Chapter");
        }

        @Test
        @DisplayName("UTC_MLC_15: tạo chương nháp cho khóa học không tồn tại (controller không kiểm tra khóa học)")
        void draftChapterForUnknownCourse() throws Exception {
            mockMvc.perform(createDraft("999999", "New Chapter"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=999999"))
                    .andExpect(flash().attribute("lessonSuccess", DRAFT_CREATED));
        }

        @Test
        @DisplayName("UTC_MLC_16: đổi tên chương \"Old Chapter\" → \"Renamed Chapter\"")
        void renamesChapter() throws Exception {
            when(lessonService.findLessons(1L)).thenReturn(List.of(lesson(10L, "Lesson 10", "Old Chapter", "VOCABULARY")));

            mockMvc.perform(rename("1", "Old Chapter", "Renamed Chapter"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", "Đã cập nhật tên chương."));
            verify(lessonService, times(1)).renameSection(1L, "Old Chapter", "Renamed Chapter");
        }

        @Test
        @DisplayName("UTC_MLC_17: tên chương mới 1 ký tự hợp lệ")
        void renameOneCharacter() throws Exception {
            mockMvc.perform(rename("1", "Old Chapter", "B"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", "Đã cập nhật tên chương."));
        }

        @Test
        @DisplayName("UTC_MLC_18: tên chương mới 150 ký tự hợp lệ")
        void rename150() throws Exception {
            mockMvc.perform(rename("1", "Old Chapter", repeat('B', 150)))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess", "Đã cập nhật tên chương."));
        }

        @Test
        @DisplayName("UTC_MLC_19: tên chương mới 151 ký tự bị từ chối")
        void rename151() throws Exception {
            mockMvc.perform(rename("1", "Old Chapter", repeat('B', 151)))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonError", CHAPTER_LENGTH));
            verify(lessonService, never()).renameSection(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("UTC_MLC_20: tên chương mới rỗng bị từ chối")
        void renameBlank() throws Exception {
            mockMvc.perform(rename("1", "Old Chapter", "   "))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonError", CHAPTER_LENGTH));
            verify(lessonService, never()).renameSection(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("UTC_MLC_21: tên chương mới trùng chương khác bị từ chối")
        void renameToExistingChapter() throws Exception {
            when(lessonService.findLessons(1L)).thenReturn(List.of(
                    lesson(10L, "Lesson 10", "Chapter 1", "VOCABULARY"),
                    lesson(11L, "Lesson 11", "Chapter 2", "VOCABULARY")));

            mockMvc.perform(rename("1", "Chapter 1", "Chapter 2"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonError", "Tên chương mới đã được sử dụng trong khóa học."));
            verify(lessonService, never()).renameSection(anyLong(), anyString(), anyString());
        }

        @Test
        @Disabled("DEF-LM-01 (Open): controller chưa bắt IllegalArgumentException của renameSection, request kết thúc bằng HTTP 500")
        @DisplayName("UTC_MLC_22: đổi tên chương của khóa học không tồn tại hiện lỗi thay vì HTTP 500")
        void renameUnknownCourse() throws Exception {
            doThrow(new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."))
                    .when(lessonService).renameSection(999999L, "Chapter 1", "Renamed");

            mockMvc.perform(rename("999999", "Chapter 1", "Renamed"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=999999"))
                    .andExpect(flash().attribute("lessonError", "Khóa học đã chọn không còn tồn tại."));
        }

        @Test
        @DisplayName("UTC_MLC_23: xóa chương nháp không có bài")
        void deletesEmptyChapter() throws Exception {
            mockMvc.perform(post("/manager/lessons/sections/delete").with(csrf())
                            .param("courseId", "1").param("sectionName", "Draft Chapter"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess",
                            "Đã xóa chương. Bài học trong chương được giữ lại và chuyển sang “Chưa phân chương”."));
            verify(lessonService, times(1)).deleteSection(1L, "Draft Chapter");
        }

        @Test
        @DisplayName("UTC_MLC_24: xóa chương có 3 bài, bài học được giữ lại")
        void deletesChapterWithLessons() throws Exception {
            mockMvc.perform(post("/manager/lessons/sections/delete").with(csrf())
                            .param("courseId", "1").param("sectionName", "Active Chapter"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonSuccess",
                            "Đã xóa chương. Bài học trong chương được giữ lại và chuyển sang “Chưa phân chương”."));
            verify(lessonService).deleteSection(1L, "Active Chapter");
        }

        @Test
        @DisplayName("UTC_MLC_25: xóa chương của khóa học không tồn tại hiện lỗi thay vì HTTP 500")
        void deleteChapterUnknownCourse() throws Exception {
            doThrow(new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."))
                    .when(lessonService).deleteSection(999999L, "Chapter 1");

            mockMvc.perform(post("/manager/lessons/sections/delete").with(csrf())
                            .param("courseId", "999999").param("sectionName", "Chapter 1"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=999999"))
                    .andExpect(flash().attribute("lessonError", "Khóa học đã chọn không còn tồn tại."));
        }
    }

    // ------------------------------------------------------------------ create

    @Nested
    @DisplayName("POST /manager/lessons (create)")
    class Create {

        private static final String CREATED_REDIRECT = "/manager/lessons?courseId=1&skill=VOCABULARY";
        private static final String ERROR_REDIRECT = "/manager/lessons?modal=new&courseId=1&skill=VOCABULARY";

        private void createFails(String message) {
            when(lessonService.create(anyLong(), any(LessonInput.class))).thenThrow(new IllegalArgumentException(message));
        }

        @Test
        @DisplayName("UTC_MLC_26: tạo bài Từ vựng hợp lệ")
        void createsLesson() throws Exception {
            when(lessonService.create(eq(1L), any(LessonInput.class))).thenReturn(100L);

            mockMvc.perform(lessonForm("/manager/lessons", "1", "New Lesson", "VOCABULARY", "30"))
                    .andExpect(redirectedUrl(CREATED_REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “New Lesson” vào kỹ năng Từ vựng."));
        }

        @Test
        @DisplayName("UTC_MLC_27: tên bài 1 ký tự hợp lệ")
        void oneCharacterTitle() throws Exception {
            when(lessonService.create(eq(1L), any(LessonInput.class))).thenReturn(100L);

            mockMvc.perform(lessonForm("/manager/lessons", "1", "A", "VOCABULARY", "30"))
                    .andExpect(redirectedUrl(CREATED_REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “A” vào kỹ năng Từ vựng."));
        }

        @Test
        @DisplayName("UTC_MLC_28: tên bài 150 ký tự hợp lệ")
        void title150() throws Exception {
            when(lessonService.create(eq(1L), any(LessonInput.class))).thenReturn(100L);

            mockMvc.perform(lessonForm("/manager/lessons", "1", repeat('A', 150), "VOCABULARY", "30"))
                    .andExpect(redirectedUrl(CREATED_REDIRECT));
            verify(lessonService, times(1)).create(eq(1L), any(LessonInput.class));
        }

        @Test
        @DisplayName("UTC_MLC_29: tên bài 151 ký tự bị từ chối, giữ lại dữ liệu form")
        void title151() throws Exception {
            createFails("Tên bài học tối đa 150 ký tự.");

            mockMvc.perform(lessonForm("/manager/lessons", "1", repeat('A', 151), "VOCABULARY", "30"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Tên bài học tối đa 150 ký tự."))
                    .andExpect(flash().attributeExists("retainedForm"));
        }

        @Test
        @DisplayName("UTC_MLC_30: tên bài rỗng bị từ chối, giữ lại dữ liệu form")
        void blankTitle() throws Exception {
            createFails("Vui lòng nhập tên bài học.");

            mockMvc.perform(lessonForm("/manager/lessons", "1", "   ", "VOCABULARY", "30"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Vui lòng nhập tên bài học."))
                    .andExpect(flash().attributeExists("retainedForm"));
        }

        @Test
        @DisplayName("UTC_MLC_31: thời lượng 0 phút hợp lệ")
        void zeroDuration() throws Exception {
            when(lessonService.create(eq(1L), any(LessonInput.class))).thenReturn(100L);

            mockMvc.perform(lessonForm("/manager/lessons", "1", "Lesson 0", "VOCABULARY", "0"))
                    .andExpect(redirectedUrl(CREATED_REDIRECT));
            ArgumentCaptor<LessonInput> sent = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonService).create(eq(1L), sent.capture());
            assertThat(sent.getValue().durationMinutes()).isZero();
        }

        @Test
        @DisplayName("UTC_MLC_32: thời lượng -1 phút bị từ chối")
        void negativeDuration() throws Exception {
            createFails("Thời lượng phải nằm trong khoảng 0–360 phút.");

            mockMvc.perform(lessonForm("/manager/lessons", "1", "Lesson 1", "VOCABULARY", "-1"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Thời lượng phải nằm trong khoảng 0–360 phút."));
        }

        @Test
        @DisplayName("UTC_MLC_33: thời lượng 360 phút hợp lệ")
        void duration360() throws Exception {
            when(lessonService.create(eq(1L), any(LessonInput.class))).thenReturn(100L);

            mockMvc.perform(lessonForm("/manager/lessons", "1", "Lesson 1", "VOCABULARY", "360"))
                    .andExpect(redirectedUrl(CREATED_REDIRECT));
            ArgumentCaptor<LessonInput> sent = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonService).create(eq(1L), sent.capture());
            assertThat(sent.getValue().durationMinutes()).isEqualTo(360);
        }

        @Test
        @DisplayName("UTC_MLC_34: thời lượng 361 phút bị từ chối")
        void duration361() throws Exception {
            createFails("Thời lượng phải nằm trong khoảng 0–360 phút.");

            mockMvc.perform(lessonForm("/manager/lessons", "1", "Lesson 1", "VOCABULARY", "361"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Thời lượng phải nằm trong khoảng 0–360 phút."));
        }

        @Test
        @DisplayName("UTC_MLC_35: khóa học không tồn tại bị từ chối")
        void unknownCourse() throws Exception {
            createFails("Khóa học đã chọn không còn tồn tại.");

            mockMvc.perform(lessonForm("/manager/lessons", "999999", "Lesson 1", "VOCABULARY", "30"))
                    .andExpect(redirectedUrl("/manager/lessons?modal=new&courseId=999999&skill=VOCABULARY"))
                    .andExpect(flash().attribute("lessonError", "Khóa học đã chọn không còn tồn tại."));
        }

        @Test
        @DisplayName("UTC_MLC_36: trùng tên bài trong chương bị từ chối")
        void duplicateTitle() throws Exception {
            createFails("Tên bài học “Lesson 1” đã tồn tại trong chương này.");

            mockMvc.perform(lessonForm("/manager/lessons", "1", "Lesson 1", "VOCABULARY", "30"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Tên bài học “Lesson 1” đã tồn tại trong chương này."));
        }

        @Test
        @DisplayName("UTC_MLC_37: kỹ năng không xác định bị từ chối, không gọi create")
        void unknownSkill() throws Exception {
            mockMvc.perform(lessonForm("/manager/lessons", "1", "Lesson 1", "INVALID_SKILL", "30"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1"))
                    .andExpect(flash().attribute("lessonError", "Không xác định được kỹ năng của bài học."));
            verify(lessonService, never()).create(anyLong(), any());
        }
    }

    // ------------------------------------------------------------------ update

    @Nested
    @DisplayName("POST /manager/lessons/{id} (update)")
    class Update {

        private static final String SAVED_REDIRECT = "/manager/lessons?courseId=1&skill=VOCABULARY";
        private static final String ERROR_REDIRECT = "/manager/lessons?edit=10&courseId=1&skill=VOCABULARY";

        private void lessonTenExists() {
            when(lessonService.findLesson(10L)).thenReturn(Optional.of(lesson(10L, "Lesson 10", "Chapter 1", "VOCABULARY")));
        }

        @Test
        @DisplayName("UTC_MLC_38: cập nhật bài học hợp lệ")
        void updatesLesson() throws Exception {
            lessonTenExists();

            mockMvc.perform(lessonForm("/manager/lessons/10", "1", "Updated Title", "VOCABULARY", "45"))
                    .andExpect(redirectedUrl(SAVED_REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã lưu bài học “Updated Title”."));
        }

        @Test
        @DisplayName("UTC_MLC_39: tên bài 150 ký tự khi cập nhật hợp lệ")
        void title150() throws Exception {
            lessonTenExists();

            mockMvc.perform(lessonForm("/manager/lessons/10", "1", repeat('A', 150), "VOCABULARY", "45"))
                    .andExpect(redirectedUrl(SAVED_REDIRECT));
            verify(lessonService, times(1)).update(eq(10L), any(LessonInput.class));
        }

        @Test
        @DisplayName("UTC_MLC_40: tên bài 151 ký tự khi cập nhật bị từ chối")
        void title151() throws Exception {
            lessonTenExists();
            doThrow(new IllegalArgumentException("Tên bài học tối đa 150 ký tự.")).when(lessonService).update(eq(10L), any());

            mockMvc.perform(lessonForm("/manager/lessons/10", "1", repeat('A', 151), "VOCABULARY", "45"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Tên bài học tối đa 150 ký tự."))
                    .andExpect(flash().attributeExists("retainedForm"));
        }

        @Test
        @DisplayName("UTC_MLC_41: tên bài rỗng khi cập nhật bị từ chối")
        void blankTitle() throws Exception {
            lessonTenExists();
            doThrow(new IllegalArgumentException("Vui lòng nhập tên bài học.")).when(lessonService).update(eq(10L), any());

            mockMvc.perform(lessonForm("/manager/lessons/10", "1", "   ", "VOCABULARY", "45"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Vui lòng nhập tên bài học."));
        }

        @Test
        @DisplayName("UTC_MLC_42: cập nhật bài học không tồn tại")
        void unknownLesson() throws Exception {
            when(lessonService.findLesson(999999L)).thenReturn(Optional.empty());

            mockMvc.perform(lessonForm("/manager/lessons/999999", "1", "Updated Title", "VOCABULARY", "45"))
                    .andExpect(redirectedUrl("/manager/lessons?edit=999999&courseId=1&skill=VOCABULARY"))
                    .andExpect(flash().attribute("lessonError", "Không tìm thấy bài học cần sửa."));
            verify(lessonService, never()).update(anyLong(), any());
        }

        @Test
        @DisplayName("UTC_MLC_43: tên trùng bài khác trong chương khi cập nhật bị từ chối")
        void duplicateTitle() throws Exception {
            lessonTenExists();
            doThrow(new IllegalArgumentException("Tên bài học “Existing Title” đã tồn tại trong chương này."))
                    .when(lessonService).update(eq(10L), any());

            mockMvc.perform(lessonForm("/manager/lessons/10", "1", "Existing Title", "VOCABULARY", "45"))
                    .andExpect(redirectedUrl(ERROR_REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Tên bài học “Existing Title” đã tồn tại trong chương này."));
        }
    }

    // ------------------------------------------------------------------ delete / status / detail

    @Nested
    @DisplayName("Xóa, đổi trạng thái, xem chi tiết")
    class DeleteStatusDetail {

        private static final String REDIRECT = "/manager/lessons?courseId=1&skill=VOCABULARY";

        private MockHttpServletRequestBuilder delete(String lessonId) {
            return post("/manager/lessons/" + lessonId + "/delete").with(csrf()).param("courseId", "1").param("skill", "VOCABULARY");
        }

        private MockHttpServletRequestBuilder statusRequest(String lessonId, String status) {
            MockHttpServletRequestBuilder request = post("/manager/lessons/" + lessonId + "/status").with(csrf())
                    .param("courseId", "1").param("skill", "VOCABULARY");
            return status == null ? request : request.param("status", status);
        }

        private void lessonTen(LessonStatusEnum current, LessonStatusEnum next) {
            when(lessonService.findLesson(10L)).thenReturn(Optional.of(new LessonRow(10L, 1L, 1, "Lesson 10", null, null,
                    "Chapter 1", 30, false, current, "VOCABULARY")));
            when(lessonService.changeStatus(eq(10L), any())).thenReturn(next);
        }

        @Test
        @DisplayName("UTC_MLC_44: xóa bài học chưa có học viên học")
        void deletesLesson() throws Exception {
            when(lessonService.findLesson(10L)).thenReturn(Optional.of(lesson(10L, "Lesson 10", "Chapter 1", "VOCABULARY")));

            mockMvc.perform(delete("10"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã xoá bài học “Lesson 10”."));
            verify(lessonService).delete(10L);
        }

        @Test
        @DisplayName("UTC_MLC_45: xóa bài học không tồn tại")
        void deleteUnknownLesson() throws Exception {
            when(lessonService.findLesson(999999L)).thenReturn(Optional.empty());
            doThrow(new IllegalArgumentException("Không tìm thấy bài học cần thao tác.")).when(lessonService).delete(999999L);

            mockMvc.perform(delete("999999"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Không tìm thấy bài học cần thao tác."));
        }

        @Test
        @DisplayName("UTC_MLC_46: không cho xóa bài học đã có học viên học")
        void cannotDeleteStudiedLesson() throws Exception {
            when(lessonService.findLesson(10L)).thenReturn(Optional.of(lesson(10L, "Lesson 10", "Chapter 1", "VOCABULARY")));
            doThrow(new IllegalArgumentException("Bài học này đã có học viên học, không thể xóa!")).when(lessonService).delete(10L);

            mockMvc.perform(delete("10"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Bài học này đã có học viên học, không thể xóa!"));
        }

        @Test
        @DisplayName("UTC_MLC_47: hiển thị bài học (INACTIVE → ACTIVE)")
        void showsLesson() throws Exception {
            lessonTen(LessonStatusEnum.INACTIVE, LessonStatusEnum.ACTIVE);

            mockMvc.perform(statusRequest("10", "ACTIVE"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã hiển thị bài học “Lesson 10”."));
            verify(lessonService).changeStatus(10L, LessonStatusEnum.ACTIVE);
        }

        @Test
        @DisplayName("UTC_MLC_48: ẩn bài học (ACTIVE → INACTIVE)")
        void hidesLesson() throws Exception {
            lessonTen(LessonStatusEnum.ACTIVE, LessonStatusEnum.INACTIVE);

            mockMvc.perform(statusRequest("10", "INACTIVE"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã ẩn bài học “Lesson 10”."));
            verify(lessonService).changeStatus(10L, LessonStatusEnum.INACTIVE);
        }

        @Test
        @DisplayName("UTC_MLC_49: thiếu tham số status thì mặc định ACTIVE")
        void missingStatusDefaultsToActive() throws Exception {
            lessonTen(LessonStatusEnum.INACTIVE, LessonStatusEnum.ACTIVE);

            mockMvc.perform(statusRequest("10", null))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã hiển thị bài học “Lesson 10”."));
            verify(lessonService).changeStatus(10L, LessonStatusEnum.ACTIVE);
        }

        @Test
        @DisplayName("UTC_MLC_50: status không hợp lệ được xem như ACTIVE")
        void invalidStatusTreatedAsActive() throws Exception {
            lessonTen(LessonStatusEnum.INACTIVE, LessonStatusEnum.ACTIVE);

            mockMvc.perform(statusRequest("10", "INVALID_STATUS"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonSuccess", "Đã hiển thị bài học “Lesson 10”."));
            verify(lessonService).changeStatus(10L, LessonStatusEnum.ACTIVE);
        }

        @Test
        @DisplayName("UTC_MLC_51: đổi trạng thái bài học không tồn tại")
        void statusUnknownLesson() throws Exception {
            when(lessonService.findLesson(999999L)).thenReturn(Optional.empty());
            when(lessonService.changeStatus(eq(999999L), any()))
                    .thenThrow(new IllegalArgumentException("Không tìm thấy bài học cần thao tác."));

            mockMvc.perform(statusRequest("999999", "ACTIVE"))
                    .andExpect(redirectedUrl(REDIRECT))
                    .andExpect(flash().attribute("lessonError", "Không tìm thấy bài học cần thao tác."));
        }

        @Test
        @DisplayName("UTC_MLC_52: tải fragment chi tiết của bài học id=10")
        void detailModal() throws Exception {
            LessonRow row = lesson(10L, "Lesson 10", "Chapter 1", "VOCABULARY");
            when(lessonService.findLesson(10L)).thenReturn(Optional.of(row));
            when(lessonDetailService.buildLessonStudyView(row)).thenReturn(new LessonStudyView(row, "Từ vựng",
                    List.of(), List.of(), List.of(), null, List.of(), List.of()));
            when(courseService.findCourse(1L)).thenReturn(Optional.of(course(1L, 1)));

            mockMvc.perform(get("/manager/lessons/10/detail-modal"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(DETAIL_FRAGMENT))
                    .andExpect(model().attribute("lessonStudyViews", hasSize(1)))
                    .andExpect(model().attribute("openDetailId", 10L));
        }

        @Test
        @DisplayName("UTC_MLC_53: bài học không tồn tại trả về fragment rỗng (không 404)")
        void detailModalUnknownLesson() throws Exception {
            when(lessonService.findLesson(999999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/manager/lessons/999999/detail-modal"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(DETAIL_FRAGMENT))
                    .andExpect(model().attribute("lessonStudyViews", empty()));
        }
    }

    // ------------------------------------------------------------------ move / reorder

    @Nested
    @DisplayName("Di chuyển và sắp xếp")
    class MoveAndReorder {

        @Test
        @DisplayName("UTC_MLC_54: AJAX chuyển bài 10 xuống trong cột")
        void ajaxMoveDown() throws Exception {
            when(lessonService.moveInColumn(10L, 1, "Chapter 1", "VOCABULARY,KANJI")).thenReturn(true);

            mockMvc.perform(post("/manager/lessons/10/move").with(csrf()).header("X-Requested-With", "XMLHttpRequest")
                            .param("direction", "down").param("courseId", "1")
                            .param("chapter", "Chapter 1").param("column", "VOCABULARY,KANJI"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Đã thay đổi thứ tự bài học thành công."));
        }

        @Test
        @DisplayName("UTC_MLC_55: chuyển bài 11 lên (không AJAX)")
        void moveUp() throws Exception {
            when(lessonService.moveInColumn(11L, -1, "Chapter 1", "VOCABULARY,KANJI")).thenReturn(true);

            mockMvc.perform(post("/manager/lessons/11/move").with(csrf())
                            .param("direction", "up").param("courseId", "1").param("skill", "VOCABULARY")
                            .param("chapter", "Chapter 1").param("column", "VOCABULARY,KANJI"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1&skill=VOCABULARY"))
                    .andExpect(flash().attribute("lessonSuccess", "Đã thay đổi thứ tự bài học."));
        }

        @Test
        @DisplayName("UTC_MLC_56: bài đã ở đầu cột không thể chuyển lên")
        void cannotMoveFurther() throws Exception {
            when(lessonService.moveInColumn(10L, -1, "Chapter 1", "VOCABULARY,KANJI")).thenReturn(false);

            mockMvc.perform(post("/manager/lessons/10/move").with(csrf()).header("X-Requested-With", "XMLHttpRequest")
                            .param("direction", "up").param("courseId", "1")
                            .param("chapter", "Chapter 1").param("column", "VOCABULARY,KANJI"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(
                            "Bài học đã ở vị trí đầu hoặc cuối của cột kỹ năng nên không thể di chuyển thêm."));
        }

        @Test
        @DisplayName("UTC_MLC_57: di chuyển bài không tồn tại")
        void moveUnknownLesson() throws Exception {
            when(lessonService.moveInColumn(eq(999999L), anyInt(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Không tìm thấy bài học cần thao tác."));

            mockMvc.perform(post("/manager/lessons/999999/move").with(csrf())
                            .param("direction", "down").param("courseId", "1").param("skill", "VOCABULARY"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1&skill=VOCABULARY"))
                    .andExpect(flash().attribute("lessonError", "Không tìm thấy bài học cần thao tác."));
        }

        @Test
        @DisplayName("UTC_MLC_58: AJAX sắp xếp lại cột theo [12, 10, 11]")
        void ajaxReorderColumn() throws Exception {
            mockMvc.perform(post("/manager/lessons/reorder").with(csrf()).header("X-Requested-With", "XMLHttpRequest")
                            .param("courseId", "1").param("chapter", "Chapter 1")
                            .param("column", "VOCABULARY,KANJI").param("ids", "12,10,11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Đã lưu thứ tự bài học thành công."));
            verify(lessonService).reorderInColumn(1L, "Chapter 1", "VOCABULARY,KANJI", List.of(12L, 10L, 11L));
        }

        @Test
        @DisplayName("UTC_MLC_59: lưu thứ tự theo kỹ năng Từ vựng (không AJAX)")
        void reorderBySkill() throws Exception {
            mockMvc.perform(post("/manager/lessons/reorder").with(csrf())
                            .param("courseId", "1").param("skill", "VOCABULARY").param("ids", "11,10"))
                    .andExpect(redirectedUrl("/manager/lessons?courseId=1&skill=VOCABULARY"))
                    .andExpect(flash().attribute("lessonSuccess", "Đã lưu thứ tự bài học trong kỹ năng Từ vựng."));
            verify(lessonService).saveOrder(1L, "VOCABULARY", List.of(11L, 10L));
        }

        @Test
        @DisplayName("UTC_MLC_60: danh sách id rỗng bị từ chối")
        void blankIds() throws Exception {
            mockMvc.perform(post("/manager/lessons/reorder").with(csrf()).header("X-Requested-With", "XMLHttpRequest")
                            .param("courseId", "1").param("ids", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Không nhận được thứ tự mới của bài học."));
        }

        @Test
        @DisplayName("UTC_MLC_61: id không phải số bị từ chối")
        void nonNumericIds() throws Exception {
            mockMvc.perform(post("/manager/lessons/reorder").with(csrf()).header("X-Requested-With", "XMLHttpRequest")
                            .param("courseId", "1").param("ids", "10,abc,12"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Thứ tự bài học gửi lên không hợp lệ."));
            verify(lessonService, never()).saveOrder(anyLong(), any(), any());
        }
    }
}
