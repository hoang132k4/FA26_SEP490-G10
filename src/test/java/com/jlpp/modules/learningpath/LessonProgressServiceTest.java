package com.jlpp.modules.learningpath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.learningpath.dto.LearningPathProgressDto;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.impl.LessonProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonProgressServiceTest {

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @Mock
    private LessonRepository lessonRepository;

    private ObjectMapper objectMapper;
    private LessonProgressServiceImpl lessonProgressService;

    private final Long userId = 42L;
    private final Long courseId = 100L;
    private final Long lesson1Id = 1001L;
    private final Long lesson2Id = 1002L;

    private Course course;
    private Lesson lesson1;
    private Lesson lesson2;
    private CourseRegistration registration;
    private LearningPath learningPath;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lessonProgressService = new LessonProgressServiceImpl(
                learningPathRepository,
                courseRegistrationRepository,
                lessonRepository,
                objectMapper
        );

        course = Course.builder().title("Khóa N4").level("N4").lessonCount(2).build();
        course.setId(courseId);

        lesson1 = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        lesson1.setId(lesson1Id);

        lesson2 = Lesson.builder().course(course).title("Bài 2").orderIndex(2).build();
        lesson2.setId(lesson2Id);

        registration = CourseRegistration.builder()
                .status("ACTIVE")
                .progressPercent(0)
                .build();
        registration.setId(500L);

        learningPath = LearningPath.builder()
                .userId(userId)
                .courseId(courseId)
                .lessonProgress("[]")
                .build();
        learningPath.setId(888L);
    }

    @Test
    @DisplayName("markLessonCompleted: Hoàn thành bài 1/2 -> Cập nhật tiến độ khóa học thành 50%")
    void testMarkLessonCompleted_FirstLesson_Sets50Percent() {
        when(courseRegistrationRepository.findForUpdateByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(registration));
        when(lessonRepository.findById(lesson1Id)).thenReturn(Optional.of(lesson1));
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(lesson1, lesson2));
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId))
                .thenReturn(Optional.of(learningPath));

        lessonProgressService.markLessonCompleted(userId, courseId, lesson1Id);

        verify(courseRegistrationRepository, times(1)).saveAndFlush(registration);
        assertThat(registration.getProgressPercent()).isEqualTo(50);
        assertThat(registration.getStatus()).isEqualTo("ACTIVE");

        verify(learningPathRepository, times(1)).saveAndFlush(learningPath);
        assertThat(learningPath.getLessonProgress()).contains(String.valueOf(lesson1Id));
    }

    @Test
    @DisplayName("markLessonCompleted: Hoàn thành bài 2/2 -> Đạt 100% tiến độ và chuyển trạng thái sang COMPLETED")
    void testMarkLessonCompleted_LastLesson_Sets100PercentAndCompleted() {
        // Giả lập bài 1 đã có trong JSON format đúng của hệ thống
        learningPath.setLessonProgress("{\"version\":1,\"lessons\":{\"" + lesson1Id + "\":{\"status\":\"COMPLETED\"}}}");
        registration.setProgressPercent(50);

        when(courseRegistrationRepository.findForUpdateByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(registration));
        when(lessonRepository.findById(lesson2Id)).thenReturn(Optional.of(lesson2));
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(lesson1, lesson2));
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId))
                .thenReturn(Optional.of(learningPath));

        lessonProgressService.markLessonCompleted(userId, courseId, lesson2Id);

        verify(courseRegistrationRepository, times(1)).saveAndFlush(registration);
        assertThat(registration.getProgressPercent()).isEqualTo(100);
        assertThat(registration.getStatus()).isEqualTo("COMPLETED");

        assertThat(learningPath.getLessonProgress()).contains(String.valueOf(lesson1Id));
        assertThat(learningPath.getLessonProgress()).contains(String.valueOf(lesson2Id));
    }

    @Test
    @DisplayName("markLessonCompleted: Gọi lại cho bài đã hoàn thành sẽ không bị nhân đôi tiến độ (Idempotent)")
    void testMarkLessonCompleted_DuplicateLesson_DoesNotInflateProgress() {
        learningPath.setLessonProgress("{\"version\":1,\"lessons\":{\"" + lesson1Id + "\":{\"status\":\"COMPLETED\"}}}");
        registration.setProgressPercent(50);

        when(courseRegistrationRepository.findForUpdateByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(registration));
        when(lessonRepository.findById(lesson1Id)).thenReturn(Optional.of(lesson1));
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(lesson1, lesson2));
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId))
                .thenReturn(Optional.of(learningPath));

        lessonProgressService.markLessonCompleted(userId, courseId, lesson1Id);

        assertThat(registration.getProgressPercent()).isEqualTo(50);
        assertThat(registration.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("markLessonCompleted: Người dùng chưa đăng ký khóa học sẽ không ghi nhận tiến độ")
    void testMarkLessonCompleted_NotEnrolled_DoesNothing() {
        when(courseRegistrationRepository.findForUpdateByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.empty());
        when(courseRegistrationRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(false);

        lessonProgressService.markLessonCompleted(userId, courseId, lesson1Id);

        verify(learningPathRepository, never()).save(any());
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("isLessonCompleted: Trả về true khi lessonId nằm trong JSON lộ trình học tập")
    void testIsLessonCompleted_TrueWhenInJson() {
        learningPath.setLessonProgress("[{\"lessonId\":" + lesson1Id + ",\"completedAt\":\"2026-11-17T00:00:00Z\"}]");
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId))
                .thenReturn(Optional.of(learningPath));

        boolean completed = lessonProgressService.isLessonCompleted(userId, courseId, lesson1Id, 1, 2, 50);
        assertThat(completed).isTrue();

        boolean notCompleted = lessonProgressService.isLessonCompleted(userId, courseId, lesson2Id, 2, 2, 50);
        assertThat(notCompleted).isFalse();
    }

    @Test
    @DisplayName("isLessonCompleted: Suy diễn theo orderIndex khi JSON tiến độ rỗng")
    void testIsLessonCompleted_FallbackLinearOrder() {
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId))
                .thenReturn(Optional.empty());

        // Tiến độ 50% trong 2 bài = bài 1 đã xong, bài 2 chưa xong
        boolean lesson1Completed = lessonProgressService.isLessonCompleted(userId, courseId, lesson1Id, 1, 2, 50);
        assertThat(lesson1Completed).isTrue();

        boolean lesson2Completed = lessonProgressService.isLessonCompleted(userId, courseId, lesson2Id, 2, 2, 50);
        assertThat(lesson2Completed).isFalse();
    }

    @Test
    @DisplayName("getLessonProgress: Đọc đúng danh sách bài học đã hoàn thành từ JSON")
    void testGetLessonProgress_ParsesSuccessfully() {
        learningPath.setLessonProgress("{\"lessons\":{\"" + lesson1Id + "\":{\"status\":\"COMPLETED\",\"completedAt\":\"2026-11-17T10:00:00Z\"}}}");
        when(learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(userId, courseId))
                .thenReturn(Optional.of(learningPath));

        LearningPathProgressDto dto = lessonProgressService.getLessonProgress(userId, courseId);

        assertThat(dto).isNotNull();
        assertThat(dto.getLessons()).containsKey(String.valueOf(lesson1Id));
        assertThat(dto.isLessonCompleted(lesson1Id)).isTrue();
    }
}
