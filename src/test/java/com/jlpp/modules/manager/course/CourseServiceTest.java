package com.jlpp.modules.manager.course;

import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.courses.repository.CourseRepository;
import com.jlpp.modules.manager.courses.service.CourseService;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository);
        courseService.setLessonRepository(lessonRepository);
    }

    private CourseOption sampleCourseOption(long id, String title, String code, String level, String status) {
        return new CourseOption(
                id, title, code, level, status, 5,
                "Short description", "Full description",
                "/thumb.jpg", 100000.0, 80000.0, 120, "N5 Grammar", true
        );
    }

    @Test
    @DisplayName("UT_MC_01: findCourses should return all courses from repository")
    void testFindCoursesReturnsList() {
        List<CourseOption> expected = List.of(
                sampleCourseOption(1L, "Khóa N5", "N5-01", "N5", "PUBLISHED"),
                sampleCourseOption(2L, "Khóa N4", "N4-01", "N4", "DRAFT")
        );
        when(courseRepository.findAll()).thenReturn(expected);

        List<CourseOption> actual = courseService.findCourses();

        assertThat(actual).hasSize(2).isEqualTo(expected);
        verify(courseRepository).findAll();
    }

    @Test
    @DisplayName("UT_MC_02: findCourseDetails should return course detail map when course exists")
    void testFindCourseDetailsWhenExists() {
        Map<String, Object> details = Map.of("course_id", 10L, "course_title", "Tiếng Nhật N5", "status", "PUBLISHED");
        when(courseRepository.findDetailsById(10L)).thenReturn(Optional.of(details));

        Optional<Map<String, Object>> result = courseService.findCourseDetails(10L);

        assertThat(result).isPresent();
        assertThat(result.get().get("course_title")).isEqualTo("Tiếng Nhật N5");
    }

    @Test
    @DisplayName("UT_MC_03: findCourseDetails should return empty when course not found")
    void testFindCourseDetailsWhenNotFound() {
        when(courseRepository.findDetailsById(999L)).thenReturn(Optional.empty());

        Optional<Map<String, Object>> result = courseService.findCourseDetails(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT_MC_04: isCodeInUse should return true when code already exists")
    void testIsCodeInUseWhenDuplicate() {
        when(courseRepository.existsByCode("JP-N5", null)).thenReturn(true);

        boolean inUse = courseService.isCodeInUse("JP-N5", null);

        assertThat(inUse).isTrue();
        verify(courseRepository).existsByCode("JP-N5", null);
    }

    @Test
    @DisplayName("UT_MC_05: isTitleInUse should return false when title is unique")
    void testIsTitleInUseWhenUnique() {
        when(courseRepository.existsByTitle("Khóa học mới", 1L)).thenReturn(false);

        boolean inUse = courseService.isTitleInUse("Khóa học mới", 1L);

        assertThat(inUse).isFalse();
        verify(courseRepository).existsByTitle("Khóa học mới", 1L);
    }

    @Test
    @DisplayName("UT_MC_06: createCourse should insert course and create default milestone lesson")
    void testCreateCourseSuccess() {
        when(courseRepository.insert(
                eq("Khóa học N5 Cấp tốc"), eq("N5-FAST"), eq("N5"), eq("DRAFT"),
                eq(500000.0), eq(400000.0), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()
        )).thenReturn(101L);

        long createdId = courseService.createCourse(
                "Khóa học N5 Cấp tốc", "N5-FAST", "N5", 500000.0, 400000.0,
                "Mô tả ngắn", "Mô tả đầy đủ", "/uploads/n5.jpg",
                "Mục tiêu", "Yêu cầu", "Đối tượng"
        );

        assertThat(createdId).isEqualTo(101L);
        verify(courseRepository).insert(
                eq("Khóa học N5 Cấp tốc"), eq("N5-FAST"), eq("N5"), eq("DRAFT"),
                eq(500000.0), eq(400000.0), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()
        );
        verify(lessonRepository).insert(eq(101L), any(LessonInput.class), eq(1));
    }

    @Test
    @DisplayName("UT_MC_07: updateCourse should throw exception when course does not exist")
    void testUpdateCourseNotFoundThrowsException() {
        when(courseRepository.update(
                eq(999L), anyString(), anyString(), anyString(), anyDouble(), anyDouble(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(0);

        assertThatThrownBy(() -> courseService.updateCourse(
                999L, "Tên sửa", "CODE", "N5", 200.0, 100.0,
                "Desc", "Full", "/thumb.png", "Obj", "Prereq", "Audience"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Không tìm thấy khóa học cần sửa");
    }

    @Test
    @DisplayName("UT_MC_08: transitionCourseStatus should throw exception on invalid status")
    void testTransitionCourseStatusInvalidStatus() {
        assertThatThrownBy(() -> courseService.transitionCourseStatus(1L, "UNKNOWN_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vui lòng chọn trạng thái khóa học hợp lệ");
    }

    @Test
    @DisplayName("UT_MC_09: transitionCourseStatus should throw exception when course not found")
    void testTransitionCourseStatusNotFound() {
        when(courseRepository.findStatusById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.transitionCourseStatus(999L, "PUBLISHED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy khóa học");
    }

    @Test
    @DisplayName("UT_MC_10: transitionCourseStatus should update status when valid and different")
    void testTransitionCourseStatusSuccess() {
        when(courseRepository.findStatusById(10L)).thenReturn(Optional.of("DRAFT"));
        when(courseRepository.transitionStatus(10L, "PUBLISHED")).thenReturn(1);

        courseService.transitionCourseStatus(10L, "PUBLISHED");

        verify(courseRepository).transitionStatus(10L, "PUBLISHED");
    }

    @Test
    @DisplayName("UT_MC_11: transitionCourseStatus should no-op when status is already identical")
    void testTransitionCourseStatusSameStatusNoOp() {
        when(courseRepository.findStatusById(10L)).thenReturn(Optional.of("PUBLISHED"));

        courseService.transitionCourseStatus(10L, "ACTIVE"); // ACTIVE normalizes to PUBLISHED

        verify(courseRepository, never()).transitionStatus(anyLong(), anyString());
    }
}

