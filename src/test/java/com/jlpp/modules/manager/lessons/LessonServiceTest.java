package com.jlpp.modules.manager.lessons;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.modules.manager.courses.service.CourseService;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.repository.LessonRepository;
import com.jlpp.modules.manager.lessons.service.LessonService;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseService courseService;

    private LessonService lessonService;

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(lessonRepository, courseService);
    }

    private LessonRow createLesson(long id, long courseId, int order, String title, String sectionName, String skill) {
        return new LessonRow(id, courseId, order, title, "Desc", "Content", sectionName, 20, false, LessonStatusEnum.ACTIVE, skill);
    }

    @Test
    @DisplayName("moveInColumn: Should swap lesson with previous lesson when moving up")
    void testMoveUpInColumn() {
        long courseId = 1L;
        LessonRow l1 = createLesson(10L, courseId, 1, "Bài 1", "Chương 1", "VOCABULARY");
        LessonRow l2 = createLesson(20L, courseId, 2, "Bài 2", "Chương 1", "KANJI");
        LessonRow l3 = createLesson(30L, courseId, 3, "Bài 3", "Chương 1", "VOCABULARY");

        when(lessonRepository.findById(20L)).thenReturn(Optional.of(l2));
        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of(l1, l2, l3));

        boolean result = lessonService.moveInColumn(20L, -1, "Chương 1", "VOCABULARY,KANJI");

        assertThat(result).isTrue();
        // After swap, order should be l2 (id 20), l1 (id 10), l3 (id 30)
        verify(lessonRepository).batchUpdateOrders(List.of(20L, 10L, 30L));
    }

    @Test
    @DisplayName("moveInColumn: Should return false when moving up from first position")
    void testMoveUpWhenAlreadyFirst() {
        long courseId = 1L;
        LessonRow l1 = createLesson(10L, courseId, 1, "Bài 1", "Chương 1", "VOCABULARY");
        LessonRow l2 = createLesson(20L, courseId, 2, "Bài 2", "Chương 1", "KANJI");

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(l1));
        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of(l1, l2));

        boolean result = lessonService.moveInColumn(10L, -1, "Chương 1", "VOCABULARY,KANJI");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("moveInColumn: Should return false when moving down from last position")
    void testMoveDownWhenAlreadyLast() {
        long courseId = 1L;
        LessonRow l1 = createLesson(10L, courseId, 1, "Bài 1", "Chương 1", "VOCABULARY");
        LessonRow l2 = createLesson(20L, courseId, 2, "Bài 2", "Chương 1", "KANJI");

        when(lessonRepository.findById(20L)).thenReturn(Optional.of(l2));
        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of(l1, l2));

        boolean result = lessonService.moveInColumn(20L, 1, "Chương 1", "VOCABULARY,KANJI");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("reorderInColumn: Should reorder lessons according to submittedIds without affecting other columns")
    void testReorderInColumn() {
        long courseId = 1L;
        LessonRow l1 = createLesson(10L, courseId, 1, "Từ vựng 1", "Chương 1", "VOCABULARY");
        LessonRow l2 = createLesson(20L, courseId, 2, "Kanji 1", "Chương 1", "KANJI");
        LessonRow l3 = createLesson(30L, courseId, 3, "Ngữ pháp 1", "Chương 1", "GRAMMAR");
        LessonRow l4 = createLesson(40L, courseId, 4, "Từ vựng 2", "Chương 1", "VOCABULARY");

        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of(l1, l2, l3, l4));

        // In VOCABULARY,KANJI column, lessons are l1(10), l2(20), l4(40)
        // Reorder them as [40, 10, 20]
        lessonService.reorderInColumn(courseId, "Chương 1", "VOCABULARY,KANJI", List.of(40L, 10L, 20L));

        // Slot 0 (was 10) -> now 40
        // Slot 1 (was 20) -> now 10
        // Slot 2 (was 30) -> stays 30 (Grammar unchanged)
        // Slot 3 (was 40) -> now 20
        verify(lessonRepository).batchUpdateOrders(List.of(40L, 10L, 30L, 20L));
    }

    @Test
    @DisplayName("reorderInColumn: Should throw exception if submitted IDs do not match column lessons")
    void testReorderInColumnMismatchedIds() {
        long courseId = 1L;
        LessonRow l1 = createLesson(10L, courseId, 1, "Từ vựng 1", "Chương 1", "VOCABULARY");
        LessonRow l2 = createLesson(20L, courseId, 2, "Kanji 1", "Chương 1", "KANJI");

        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of(l1, l2));

        assertThatThrownBy(() -> lessonService.reorderInColumn(courseId, "Chương 1", "VOCABULARY,KANJI", List.of(10L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findLessonIdsWithFlashcards: Should delegate to repository and return IDs")
    void testFindLessonIdsWithFlashcards() {
        List<Long> lessonIds = List.of(1L, 2L, 3L);
        when(lessonRepository.findLessonIdsWithFlashcards(lessonIds)).thenReturn(Set.of(1L, 3L));

        Set<Long> result = lessonService.findLessonIdsWithFlashcards(lessonIds);
        assertThat(result).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("findLessonIdsWithExercises: Should delegate to repository and return IDs")
    void testFindLessonIdsWithExercises() {
        List<Long> lessonIds = List.of(1L, 2L, 3L);
        when(lessonRepository.findLessonIdsWithExercises(lessonIds)).thenReturn(Set.of(2L));

        Set<Long> result = lessonService.findLessonIdsWithExercises(lessonIds);
        assertThat(result).containsExactly(2L);
    }

    @Test
    @DisplayName("update: Should NOT call normalizeOrders when order and skill are unchanged")
    void testUpdateWithoutOrderChangeDoesNotNormalize() {
        LessonRow existing = createLesson(10L, 1L, 2, "Tên cũ", "Chương 1", "VOCABULARY");
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));

        LessonInput input = new LessonInput("Tên mới", "Chương 1", "Mô tả mới", "Nội dung mới",
                "VOCABULARY", LessonStatusEnum.ACTIVE.name(), null, 20, false);
        lessonService.update(10L, input);

        verify(lessonRepository).update(eq(10L), any(LessonInput.class), eq(2));
        verify(lessonRepository, never()).findIdsInOrder(anyLong(), anyString());
        verify(lessonRepository, never()).batchUpdateOrders(any());
    }

    @Test
    @DisplayName("update: Should call normalizeOrders when orderIndex changed")
    void testUpdateWithOrderChangeNormalizes() {
        LessonRow existing = createLesson(10L, 1L, 2, "Tên cũ", "Chương 1", "VOCABULARY");
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(lessonRepository.findIdsInOrder(1L, "VOCABULARY")).thenReturn(List.of(10L));

        LessonInput input = new LessonInput("Tên mới", "Chương 1", "Mô tả", "Nội dung",
                "VOCABULARY", LessonStatusEnum.ACTIVE.name(), 5, 20, false);
        lessonService.update(10L, input);

        verify(lessonRepository).update(eq(10L), any(LessonInput.class), eq(5));
        verify(lessonRepository).findIdsInOrder(1L, "VOCABULARY");
        verify(lessonRepository).batchUpdateOrders(List.of(10L));
    }

    @Test
    @DisplayName("updateOther: Should NOT call normalizeOrders")
    void testUpdateOtherDoesNotNormalize() {
        LessonRow existing = createLesson(10L, 1L, 2, "Bài khác", "Chương 1", null);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));

        LessonInput input = new LessonInput("Tên bài khác", "Chương 1", null, "Nội dung",
                null, LessonStatusEnum.ACTIVE.name(), null, 20, false);
        lessonService.updateOther(10L, input);

        verify(lessonRepository).update(eq(10L), any(LessonInput.class), eq(2));
        verify(lessonRepository, never()).findIdsInOrder(anyLong(), any());
        verify(lessonRepository, never()).batchUpdateOrders(any());
    }

    @Test
    @DisplayName("UT_ML_05: create should insert new lesson and normalize orders")
    void testCreateLessonSuccess() {
        long courseId = 1L;
        when(courseService.findCourse(courseId)).thenReturn(Optional.of(
                new com.jlpp.modules.manager.courses.dto.CourseOption(courseId, "Tiêu đề", "N5-01", "N5", "PUBLISHED", 0, null, null, null, null, null, null, null, false)
        ));
        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of());
        when(lessonRepository.nextOrderIndex(courseId, "VOCABULARY")).thenReturn(1);
        when(lessonRepository.insert(eq(courseId), any(LessonInput.class), eq(1))).thenReturn(100L);
        when(lessonRepository.findIdsInOrder(courseId, "VOCABULARY")).thenReturn(List.of(100L));

        LessonInput input = new LessonInput("Bài mới", "Chương 1", "Mô tả", "Nội dung",
                "VOCABULARY", LessonStatusEnum.ACTIVE.name(), null, 20, false);
        long lessonId = lessonService.create(courseId, input);

        assertThat(lessonId).isEqualTo(100L);
        verify(lessonRepository).insert(eq(courseId), any(LessonInput.class), eq(1));
        verify(lessonRepository).batchUpdateOrders(List.of(100L));
    }

    @Test
    @DisplayName("UT_ML_06: create should throw exception when lesson title already exists in section")
    void testCreateDuplicateTitleThrowsException() {
        long courseId = 1L;
        when(courseService.findCourse(courseId)).thenReturn(Optional.of(
                new com.jlpp.modules.manager.courses.dto.CourseOption(courseId, "Tiêu đề", "N5-01", "N5", "PUBLISHED", 0, null, null, null, null, null, null, null, false)
        ));
        LessonRow existing = createLesson(10L, courseId, 1, "Từ vựng 1", "Chương 1", "VOCABULARY");
        when(lessonRepository.findByCourse(courseId)).thenReturn(List.of(existing));

        LessonInput input = new LessonInput("Từ vựng 1", "Chương 1", "Mô tả", "Nội dung",
                "VOCABULARY", LessonStatusEnum.ACTIVE.name(), null, 20, false);

        assertThatThrownBy(() -> lessonService.create(courseId, input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại trong chương này");
    }

    @Test
    @DisplayName("UT_ML_07: delete should remove lesson and normalize remaining orders")
    void testDeleteLessonSuccess() {
        LessonRow existing = createLesson(10L, 1L, 1, "Bài xóa", "Chương 1", "VOCABULARY");
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(lessonRepository.findIdsInOrder(1L, "VOCABULARY")).thenReturn(List.of());

        lessonService.delete(10L);

        verify(lessonRepository).delete(10L);
        verify(lessonRepository).findIdsInOrder(1L, "VOCABULARY");
    }

    @Test
    @DisplayName("UT_ML_08: changeStatus should toggle status when requested status is null")
    void testChangeStatusToggle() {
        LessonRow existing = createLesson(10L, 1L, 1, "Bài test", "Chương 1", "VOCABULARY");
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));

        LessonStatusEnum toggled = lessonService.changeStatus(10L, null);

        assertThat(toggled).isEqualTo(LessonStatusEnum.INACTIVE);
        verify(lessonRepository).updateStatus(10L, LessonStatusEnum.INACTIVE);
    }
}

