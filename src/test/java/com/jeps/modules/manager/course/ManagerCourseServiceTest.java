package com.jeps.modules.manager.course;

import com.jeps.common.constants.LessonStatusEnum;
import com.jeps.modules.manager.courses.dto.CourseOption;
import com.jeps.modules.manager.courses.repository.CourseRepository;
import com.jeps.modules.manager.courses.service.CourseService;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jeps.modules.manager.lessons.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Report 5.1 – sheet ManagerCourseService (UTC_MCS_01 → UTC_MCS_18).
 * Service layer: repository là mock, không kết nối database.
 */
@ExtendWith(MockitoExtension.class)
class ManagerCourseServiceTest {

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

    private static LessonRow lesson(long id, LessonStatusEnum status) {
        return new LessonRow(id, 1L, (int) id, "Lesson " + id, null, null, "Chapter 1", 30, false, status, "VOCABULARY");
    }

    private static CourseOption course(long id) {
        return new CourseOption(id, "Course " + id, "CODE_" + id, "N5", "DRAFT", 0,
                "Desc", "Full", null, 500000.0, null, null, null, false);
    }

    @Nested
    @DisplayName("createCourse")
    class CreateCourse {

        @Test
        @DisplayName("UTC_MCS_01: tạo khóa học mặc định DRAFT và tự sinh bài thi giữa kì")
        void createsDraftCourseWithMilestoneExam() {
            when(courseRepository.insert(eq("N5 Online Course"), eq("N5_ONLINE"), eq("N5"), eq("DRAFT"), eq(500000.0),
                    isNull(), eq("Description"), isNull(), isNull(), eq("Objectives"), isNull(), isNull()))
                    .thenReturn(10L);

            long id = courseService.createCourse("N5 Online Course", "N5_ONLINE", "N5", 500000.0, null,
                    "Description", null, null, "Objectives", null, null);

            assertThat(id).isEqualTo(10L);
            ArgumentCaptor<LessonInput> milestone = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonRepository).insert(eq(10L), milestone.capture(), eq(1));
            assertThat(milestone.getValue().title()).isEqualTo("Bài thi giữa kì: Đánh giá năng lực giai đoạn 1");
            assertThat(milestone.getValue().skill()).isEqualTo("EXAM");
            assertThat(milestone.getValue().status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("UTC_MCS_02: lỗi khi sinh bài thi giữa kì được ném ra (không bị nuốt) để rollback cả khóa học")
        void milestoneFailureIsPropagated() {
            when(courseRepository.insert(anyString(), anyString(), anyString(), eq("DRAFT"), any(), any(),
                    any(), any(), any(), any(), any(), any())).thenReturn(10L);
            DataAccessResourceFailureException failure = new DataAccessResourceFailureException("insert lesson failed");
            when(lessonRepository.insert(eq(10L), any(LessonInput.class), eq(1))).thenThrow(failure);

            assertThatThrownBy(() -> courseService.createCourse("N5 Online Course", "N5_ONLINE", "N5", 500000.0, null,
                    "Description", null, null, "Objectives", null, null))
                    .isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("updateCourse")
    class UpdateCourse {

        @Test
        @DisplayName("UTC_MCS_03: cập nhật khóa học hợp lệ")
        void updatesExistingCourse() {
            when(courseRepository.update(eq(1L), eq("N5 Course Updated"), eq("N5_ONLINE"), eq("N5"), eq(600000.0),
                    any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

            courseService.updateCourse(1L, "N5 Course Updated", "N5_ONLINE", "N5", 600000.0, null,
                    null, null, null, null, null, null);

            verify(courseRepository).update(eq(1L), eq("N5 Course Updated"), eq("N5_ONLINE"), eq("N5"), eq(600000.0),
                    any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("UTC_MCS_04: cập nhật khóa học không tồn tại bị từ chối")
        void rejectsUnknownCourse() {
            when(courseRepository.update(eq(999999L), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> courseService.updateCourse(999999L, "N5 Course", "N5_ONLINE", "N5", 600000.0, null,
                    null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy khóa học cần sửa.");
        }
    }

    @Nested
    @DisplayName("transitionCourseStatus")
    class TransitionCourseStatus {

        @Test
        @DisplayName("UTC_MCS_05: DRAFT → PUBLISHED khi khóa học có ít nhất 1 bài ACTIVE")
        void publishesDraftCourse() {
            when(courseRepository.findStatusById(1L)).thenReturn(Optional.of("DRAFT"));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(lesson(10L, LessonStatusEnum.ACTIVE)));
            when(courseRepository.transitionStatus(1L, "PUBLISHED")).thenReturn(1);

            courseService.transitionCourseStatus(1L, "PUBLISHED");

            verify(courseRepository).transitionStatus(1L, "PUBLISHED");
        }

        @Test
        @DisplayName("UTC_MCS_06: status \" active \" chuẩn hóa thành PUBLISHED trùng trạng thái hiện tại thì không cập nhật")
        void normalizedSameStatusIsNoOp() {
            when(courseRepository.findStatusById(1L)).thenReturn(Optional.of("PUBLISHED"));

            courseService.transitionCourseStatus(1L, " active ");

            verify(courseRepository, never()).transitionStatus(anyLong(), anyString());
        }

        @Test
        @DisplayName("UTC_MCS_07: trạng thái không hợp lệ bị từ chối")
        void rejectsInvalidStatus() {
            assertThatThrownBy(() -> courseService.transitionCourseStatus(1L, "INVALID_STATUS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Vui lòng chọn trạng thái khóa học hợp lệ.");
            verify(courseRepository, never()).transitionStatus(anyLong(), anyString());
        }

        @Test
        @DisplayName("UTC_MCS_08: trạng thái null bị từ chối")
        void rejectsNullStatus() {
            assertThatThrownBy(() -> courseService.transitionCourseStatus(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Vui lòng chọn trạng thái khóa học hợp lệ.");
        }

        @Test
        @DisplayName("UTC_MCS_09: chuyển trạng thái khóa học không tồn tại bị từ chối")
        void rejectsUnknownCourse() {
            when(courseRepository.findStatusById(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.transitionCourseStatus(999999L, "PUBLISHED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy khóa học cần chuyển trạng thái.");
        }

        @Test
        @DisplayName("UTC_MCS_10: transitionStatus cập nhật 0 dòng thì báo không tìm thấy khóa học")
        void zeroUpdatedRowsIsRejected() {
            when(courseRepository.findStatusById(1L)).thenReturn(Optional.of("DRAFT"));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(lesson(10L, LessonStatusEnum.ACTIVE)));
            when(courseRepository.transitionStatus(1L, "PUBLISHED")).thenReturn(0);

            assertThatThrownBy(() -> courseService.transitionCourseStatus(1L, "PUBLISHED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy khóa học cần chuyển trạng thái.");
        }

        @Test
        @DisplayName("UTC_MCS_11: không cho xuất bản khi khóa học chỉ có bài INACTIVE")
        void rejectsPublishingWithoutActiveLesson() {
            when(courseRepository.findStatusById(1L)).thenReturn(Optional.of("DRAFT"));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(
                    lesson(10L, LessonStatusEnum.INACTIVE), lesson(11L, LessonStatusEnum.INACTIVE)));

            assertThatThrownBy(() -> courseService.transitionCourseStatus(1L, "PUBLISHED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không thể xuất bản khóa học khi chưa có ít nhất 1 bài học đang hoạt động.");
            verify(courseRepository, never()).transitionStatus(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Truy vấn khóa học")
    class Queries {

        @Test
        @DisplayName("UTC_MCS_11 (isCodeInUse): mã N5_ONLINE đã thuộc khóa học khác")
        void codeInUseByAnotherCourse() {
            when(courseRepository.existsByCode("N5_ONLINE", 1L)).thenReturn(true);

            assertThat(courseService.isCodeInUse("N5_ONLINE", 1L)).isTrue();
        }

        @Test
        @DisplayName("UTC_MCS_12: findCourses trả về 3 khóa học")
        void findsAllCourses() {
            when(courseRepository.findAll()).thenReturn(List.of(course(1L), course(2L), course(3L)));

            assertThat(courseService.findCourses()).hasSize(3);
        }

        @Test
        @DisplayName("UTC_MCS_13: findCourse tìm thấy khóa học id=1")
        void findsCourseById() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L)));

            assertThat(courseService.findCourse(1L)).get().extracting(CourseOption::id).isEqualTo(1L);
        }

        @Test
        @DisplayName("UTC_MCS_14: findCourse với id không tồn tại trả về rỗng")
        void unknownCourseIsEmpty() {
            when(courseRepository.findById(999999L)).thenReturn(Optional.empty());

            assertThat(courseService.findCourse(999999L)).isEmpty();
        }

        @Test
        @DisplayName("UTC_MCS_15: findCourseDetails trả về map thông tin khóa học")
        void findsCourseDetails() {
            Map<String, Object> details = Map.of("course_id", 1L, "course_title", "N5 Course", "status", "DRAFT");
            when(courseRepository.findDetailsById(1L)).thenReturn(Optional.of(details));

            assertThat(courseService.findCourseDetails(1L)).contains(details);
        }

        @Test
        @DisplayName("UTC_MCS_16: findCourseDetails với id không tồn tại trả về rỗng")
        void unknownCourseDetailsIsEmpty() {
            when(courseRepository.findDetailsById(999999L)).thenReturn(Optional.empty());

            assertThat(courseService.findCourseDetails(999999L)).isEmpty();
        }

        @Test
        @DisplayName("UTC_MCS_17: tên \"N5 Course\" đã được khóa học khác sử dụng")
        void titleInUse() {
            when(courseRepository.existsByTitle("N5 Course", 1L)).thenReturn(true);

            assertThat(courseService.isTitleInUse("N5 Course", 1L)).isTrue();
        }

        @Test
        @DisplayName("UTC_MCS_18: tên \"Unique N4 Course\" chưa được sử dụng")
        void titleAvailable() {
            when(courseRepository.existsByTitle("Unique N4 Course", 1L)).thenReturn(false);

            assertThat(courseService.isTitleInUse("Unique N4 Course", 1L)).isFalse();
        }
    }
}
