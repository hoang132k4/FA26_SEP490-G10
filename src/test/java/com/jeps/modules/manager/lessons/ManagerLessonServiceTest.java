package com.jeps.modules.manager.lessons;

import com.jeps.common.constants.LessonStatusEnum;
import com.jeps.modules.manager.courses.dto.CourseOption;
import com.jeps.modules.manager.courses.service.CourseService;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jeps.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jeps.modules.manager.lessons.repository.LessonRepository;
import com.jeps.modules.manager.lessons.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Report 5.1 – sheet ManagerLessonService (UTC_MLS_01 → UTC_MLS_44).
 * Service layer: LessonRepository và CourseService là mock, không kết nối database.
 */
@ExtendWith(MockitoExtension.class)
class ManagerLessonServiceTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private CourseService courseService;
    @InjectMocks private LessonService lessonService;

    private static final CourseOption COURSE = new CourseOption(1L, "N5 Course", "N5_ONLINE", "N5", "DRAFT", 0,
            "Desc", "Full", null, 0d, null, null, null, false);

    // ------------------------------------------------------------------ helpers

    private static LessonInput input(String title, String section, String skill, Integer duration) {
        return new LessonInput(title, section, null, null, skill, "ACTIVE", null, duration, false);
    }

    private static LessonRow row(long id, int order, String title, String section, String skill) {
        return new LessonRow(id, 1L, order, title, null, null, section, 30, false, LessonStatusEnum.ACTIVE, skill);
    }

    private static LessonRow row(long id, int order, String title, String section, String skill, LessonStatusEnum status) {
        return new LessonRow(id, 1L, order, title, null, null, section, 30, false, status, skill);
    }

    private void courseExists() {
        when(courseService.findCourse(1L)).thenReturn(Optional.of(COURSE));
    }

    /** Khóa học id=1 đang trống, bài mới được chèn với id=100 tại orderIndex 3. */
    private void insertSucceeds(long newId) {
        courseExists();
        when(lessonRepository.findByCourse(1L)).thenReturn(List.of());
        lenient().when(lessonRepository.nextOrderIndex(1L)).thenReturn(3);
        when(lessonRepository.insert(eq(1L), any(LessonInput.class), anyInt())).thenReturn(newId);
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    // ------------------------------------------------------------------ create

    @Nested
    @DisplayName("create(courseId, input)")
    class Create {

        @Test
        @DisplayName("UTC_MLS_01: tạo bài học, tên được trim, trạng thái mặc định INACTIVE, orderIndex 3")
        void createsInactiveLesson() {
            insertSucceeds(100L);

            long id = lessonService.create(1L, input("  Lesson 1  ", "Chapter 1", "VOCABULARY", 30));

            assertThat(id).isEqualTo(100L);
            ArgumentCaptor<LessonInput> saved = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonRepository).insert(eq(1L), saved.capture(), eq(3));
            assertThat(saved.getValue().title()).isEqualTo("Lesson 1");
            assertThat(saved.getValue().status()).isEqualTo(LessonStatusEnum.INACTIVE.name());
        }

        @Test
        @DisplayName("UTC_MLS_02: tên 150 ký tự hợp lệ")
        void title150() {
            insertSucceeds(100L);

            assertThat(lessonService.create(1L, input(repeat('A', 150), "Chapter 1", "VOCABULARY", 30))).isEqualTo(100L);
        }

        @Test
        @DisplayName("UTC_MLS_03: tên 151 ký tự bị từ chối")
        void title151() {
            assertThatThrownBy(() -> lessonService.create(1L, input(repeat('A', 151), "Chapter 1", "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tên bài học tối đa 150 ký tự.");
            verify(lessonRepository, never()).insert(anyLong(), any(), anyInt());
        }

        @Test
        @DisplayName("UTC_MLS_04: tên rỗng bị từ chối")
        void blankTitle() {
            assertThatThrownBy(() -> lessonService.create(1L, input("   ", "Chapter 1", "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Vui lòng nhập tên bài học.");
        }

        @Test
        @DisplayName("UTC_MLS_05: không có chương thì lưu sectionName = null (Chưa phân chương)")
        void nullSection() {
            insertSucceeds(100L);

            assertThat(lessonService.create(1L, input("Lesson 1", null, "VOCABULARY", 30))).isEqualTo(100L);
            ArgumentCaptor<LessonInput> saved = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonRepository).insert(eq(1L), saved.capture(), anyInt());
            assertThat(saved.getValue().sectionName()).isNull();
        }

        @Test
        @DisplayName("UTC_MLS_06: tên chương 150 ký tự hợp lệ")
        void section150() {
            insertSucceeds(100L);

            assertThat(lessonService.create(1L, input("Lesson 1", repeat('S', 150), "VOCABULARY", 30))).isEqualTo(100L);
        }

        @Test
        @DisplayName("UTC_MLS_07: tên chương 151 ký tự bị từ chối")
        void section151() {
            assertThatThrownBy(() -> lessonService.create(1L, input("Lesson 1", repeat('S', 151), "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Chương / Phần tối đa 150 ký tự.");
        }

        @Test
        @DisplayName("UTC_MLS_08: thời lượng 360 phút hợp lệ")
        void duration360() {
            insertSucceeds(100L);

            assertThat(lessonService.create(1L, input("Lesson 1", "Chapter 1", "VOCABULARY", 360))).isEqualTo(100L);
        }

        @Test
        @DisplayName("UTC_MLS_09: thời lượng 361 phút bị từ chối")
        void duration361() {
            assertThatThrownBy(() -> lessonService.create(1L, input("Lesson 1", "Chapter 1", "VOCABULARY", 361)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Thời lượng phải nằm trong khoảng 0–360 phút.");
        }

        @Test
        @DisplayName("UTC_MLS_10: khóa học không tồn tại bị từ chối")
        void unknownCourse() {
            when(courseService.findCourse(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.create(999999L, input("Lesson 1", "Chapter 1", "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Khóa học đã chọn không còn tồn tại.");
        }

        @Test
        @DisplayName("UTC_MLS_11: trùng tên bài trong cùng chương bị từ chối")
        void duplicateTitleInChapter() {
            courseExists();
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(row(10L, 1, "Lesson 1", "Chapter 1", "VOCABULARY")));

            assertThatThrownBy(() -> lessonService.create(1L, input("Lesson 1", "Chapter 1", "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tên bài học “Lesson 1” đã tồn tại trong chương này.");
            verify(lessonRepository, never()).insert(anyLong(), any(), anyInt());
        }
    }

    // ------------------------------------------------------------------ update / delete / changeStatus

    @Nested
    @DisplayName("update, delete, changeStatus")
    class UpdateDeleteStatus {

        @Test
        @DisplayName("UTC_MLS_12: cập nhật bài học đổi chương và kỹ năng")
        void updatesLesson() {
            LessonRow existing = row(10L, 1, "Old Title", "Chapter 1", "VOCABULARY");
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(existing));

            lessonService.update(10L, input("Updated Title", "Chapter 2", "KANJI", 45));

            ArgumentCaptor<LessonInput> saved = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonRepository).update(eq(10L), saved.capture(), eq(1));
            assertThat(saved.getValue().title()).isEqualTo("Updated Title");
            assertThat(saved.getValue().sectionName()).isEqualTo("Chapter 2");
            assertThat(saved.getValue().skill()).isEqualTo("KANJI");
            assertThat(saved.getValue().durationMinutes()).isEqualTo(45);
        }

        @Test
        @DisplayName("UTC_MLS_13: cập nhật bài học không tồn tại bị từ chối")
        void updateUnknownLesson() {
            when(lessonRepository.findById(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.update(999999L, input("Updated Title", "Chapter 1", "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy bài học cần thao tác.");
        }

        @Test
        @DisplayName("UTC_MLS_14: đổi tên trùng bài khác trong cùng chương bị từ chối")
        void updateDuplicateTitle() {
            LessonRow existing = row(10L, 1, "Lesson 10", "Chapter 1", "VOCABULARY");
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(existing,
                    row(11L, 2, "Duplicate Title", "Chapter 1", "VOCABULARY")));

            assertThatThrownBy(() -> lessonService.update(10L, input("Duplicate Title", "Chapter 1", "VOCABULARY", 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tên bài học “Duplicate Title” đã tồn tại trong chương này.");
            verify(lessonRepository, never()).update(anyLong(), any(), anyInt());
        }

        @Test
        @DisplayName("UTC_MLS_15: xóa bài học chưa có học viên học")
        void deletesLesson() {
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(row(10L, 1, "Lesson 10", "Chapter 1", "VOCABULARY")));
            when(lessonRepository.hasLearnersStudied(10L)).thenReturn(false);

            lessonService.delete(10L);

            verify(lessonRepository).delete(10L);
        }

        @Test
        @DisplayName("UTC_MLS_16: không cho xóa bài học đã có học viên học")
        void cannotDeleteStudiedLesson() {
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(row(10L, 1, "Lesson 10", "Chapter 1", "VOCABULARY")));
            when(lessonRepository.hasLearnersStudied(10L)).thenReturn(true);

            assertThatThrownBy(() -> lessonService.delete(10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bài học này đã có học viên học, không thể xóa!");
            verify(lessonRepository, never()).delete(anyLong());
        }

        @Test
        @DisplayName("UTC_MLS_17: đổi trạng thái sang ACTIVE")
        void changesStatus() {
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(
                    row(10L, 1, "Lesson 10", "Chapter 1", "VOCABULARY", LessonStatusEnum.INACTIVE)));

            assertThat(lessonService.changeStatus(10L, LessonStatusEnum.ACTIVE)).isEqualTo(LessonStatusEnum.ACTIVE);
            verify(lessonRepository).updateStatus(10L, LessonStatusEnum.ACTIVE);
        }

        @Test
        @DisplayName("UTC_MLS_18: đổi trạng thái bài học không tồn tại bị từ chối")
        void changeStatusUnknownLesson() {
            when(lessonRepository.findById(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.changeStatus(999999L, LessonStatusEnum.ACTIVE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy bài học cần thao tác.");
        }
    }

    // ------------------------------------------------------------------ saveOrder

    @Nested
    @DisplayName("saveOrder(courseId, skill, ids)")
    class SaveOrder {

        private void vocabularyLessons(List<Long> ids) {
            when(lessonRepository.findIdsInOrder(1L, "VOCABULARY")).thenReturn(ids);
            List<LessonRow> rows = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) rows.add(row(ids.get(i), i + 1, "Lesson " + ids.get(i), "Chapter 1", "VOCABULARY"));
            lenient().when(lessonRepository.findByCourse(1L)).thenReturn(rows);
        }

        @Test
        @DisplayName("UTC_MLS_19: lưu thứ tự mới [12, 10, 11]")
        void savesNewOrder() {
            vocabularyLessons(List.of(10L, 11L, 12L));

            lessonService.saveOrder(1L, "VOCABULARY", List.of(12L, 10L, 11L));

            verify(lessonRepository).batchUpdateOrders(List.of(12L, 10L, 11L));
        }

        @Test
        @DisplayName("UTC_MLS_20: danh sách thiếu bài (12 bị thiếu) bị từ chối")
        void rejectsIncompleteList() {
            vocabularyLessons(List.of(10L, 11L, 12L));

            assertThatThrownBy(() -> lessonService.saveOrder(1L, "VOCABULARY", List.of(10L, 11L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Danh sách bài học đã thay đổi trong lúc sắp xếp. Vui lòng tải lại trang rồi thử lại.");
            verify(lessonRepository, never()).batchUpdateOrders(anyList());
        }

        @Test
        @DisplayName("UTC_MLS_21: id trùng lặp được loại bỏ trước khi lưu")
        void removesDuplicateIds() {
            vocabularyLessons(List.of(10L, 11L, 12L));

            lessonService.saveOrder(1L, "VOCABULARY", List.of(10L, 10L, 11L, 12L));

            verify(lessonRepository).batchUpdateOrders(List.of(10L, 11L, 12L));
        }

        @Test
        @DisplayName("UTC_MLS_22: danh sách rỗng khi kỹ năng chưa có bài vẫn hợp lệ")
        void emptyListIsAccepted() {
            vocabularyLessons(List.of());

            lessonService.saveOrder(1L, "VOCABULARY", List.of());
        }
    }

    // ------------------------------------------------------------------ queries

    @Nested
    @DisplayName("Truy vấn bài học")
    class Queries {

        @Test
        @DisplayName("UTC_MLS_23: findLessons trả về 3 bài của khóa học")
        void findsLessons() {
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(
                    row(10L, 1, "A", "C1", "VOCABULARY"), row(11L, 2, "B", "C1", "KANJI"), row(12L, 3, "C", "C1", "GRAMMAR")));

            assertThat(lessonService.findLessons(1L)).hasSize(3);
        }

        @Test
        @DisplayName("UTC_MLS_24: findLesson tìm thấy id=10 và rỗng với id không tồn tại")
        void findsLessonById() {
            LessonRow lesson = row(10L, 1, "Lesson 10", "Chapter 1", "VOCABULARY");
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
            when(lessonRepository.findById(999999L)).thenReturn(Optional.empty());

            assertThat(lessonService.findLesson(10L)).contains(lesson);
            assertThat(lessonService.findLesson(999999L)).isEmpty();
        }

        @Test
        @DisplayName("UTC_MLS_25: bài có flashcard")
        void lessonsWithFlashcards() {
            when(lessonRepository.findLessonIdsWithFlashcards(List.of(10L, 11L))).thenReturn(Set.of(10L));

            assertThat(lessonService.findLessonIdsWithFlashcards(List.of(10L, 11L))).containsExactly(10L);
        }

        @Test
        @DisplayName("UTC_MLS_26: danh sách rỗng / null trả về tập rỗng, không gọi repository")
        void emptyInputReturnsEmptySet() {
            assertThat(lessonService.findLessonIdsWithFlashcards(List.of())).isEmpty();
            assertThat(lessonService.findLessonIdsWithFlashcards(null)).isEmpty();
            verifyNoInteractions(lessonRepository);
        }

        @Test
        @DisplayName("UTC_MLS_27: bài có bài luyện tập")
        void lessonsWithExercises() {
            when(lessonRepository.findLessonIdsWithExercises(List.of(10L, 11L))).thenReturn(Set.of(11L));

            assertThat(lessonService.findLessonIdsWithExercises(List.of(10L, 11L))).containsExactly(11L);
        }

        @Test
        @DisplayName("UTC_MLS_28: kiểm tra bài học đã có học viên học")
        void hasLearnersStudied() {
            when(lessonRepository.hasLearnersStudied(10L)).thenReturn(true);
            when(lessonRepository.hasLearnersStudied(11L)).thenReturn(false);

            assertThat(lessonService.hasLearnersStudied(10L)).isTrue();
            assertThat(lessonService.hasLearnersStudied(11L)).isFalse();
        }

        @Test
        @DisplayName("UTC_MLS_29: danh sách bài đã có học viên học trong khóa")
        void studiedLessonIds() {
            when(lessonRepository.findStudiedLessonIds(1L, List.of(10L, 11L))).thenReturn(Set.of(10L));

            assertThat(lessonService.findStudiedLessonIds(1L, List.of(10L, 11L))).containsExactly(10L);
        }
    }

    // ------------------------------------------------------------------ createOther / updateOther

    @Nested
    @DisplayName("Cột Khác (createOther / updateOther)")
    class OtherColumn {

        @Test
        @DisplayName("UTC_MLS_30: tạo bài ở cột Khác (skill = null, INACTIVE)")
        void createsOtherLesson() {
            courseExists();
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of());
            when(lessonRepository.nextOrderIndex(1L)).thenReturn(4);
            when(lessonRepository.insert(eq(1L), any(LessonInput.class), eq(4))).thenReturn(101L);

            assertThat(lessonService.createOther(1L, input("Midterm Exam", "Chapter 1", null, 45))).isEqualTo(101L);
            ArgumentCaptor<LessonInput> saved = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonRepository).insert(eq(1L), saved.capture(), eq(4));
            assertThat(saved.getValue().skill()).isNull();
            assertThat(saved.getValue().status()).isEqualTo(LessonStatusEnum.INACTIVE.name());
        }

        @Test
        @DisplayName("UTC_MLS_31: trùng tên bài ở cột Khác bị từ chối")
        void duplicateOtherLesson() {
            courseExists();
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(row(20L, 1, "Midterm Exam", "Chapter 1", null)));

            assertThatThrownBy(() -> lessonService.createOther(1L, input("Midterm Exam", "Chapter 1", null, 45)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tên bài học “Midterm Exam” đã tồn tại trong chương này.");
        }

        @Test
        @DisplayName("UTC_MLS_32: cập nhật bài ở cột Khác")
        void updatesOtherLesson() {
            LessonRow existing = row(10L, 5, "Other", "Chapter 1", null);
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(existing));
            when(lessonRepository.findRawSkillType(10L)).thenReturn(Optional.empty());

            lessonService.updateOther(10L, input("Updated Other", "Chapter 1", null, 60));

            ArgumentCaptor<LessonInput> saved = ArgumentCaptor.forClass(LessonInput.class);
            verify(lessonRepository).update(eq(10L), saved.capture(), eq(5));
            assertThat(saved.getValue().title()).isEqualTo("Updated Other");
            assertThat(saved.getValue().durationMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("UTC_MLS_33: bài kỹ năng thường không được sửa qua cột Khác")
        void rejectsSkillLesson() {
            when(lessonRepository.findById(10L)).thenReturn(Optional.of(row(10L, 1, "Vocab", "Chapter 1", "VOCABULARY")));

            assertThatThrownBy(() -> lessonService.updateOther(10L, input("Updated", "Chapter 1", null, 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bài học này không thuộc cột Khác.");
        }
    }

    // ------------------------------------------------------------------ chapters

    @Nested
    @DisplayName("Chương (renameSection / deleteSection)")
    class Chapters {

        @Test
        @DisplayName("UTC_MLS_34: đổi tên chương")
        void renamesSection() {
            courseExists();

            lessonService.renameSection(1L, "Chapter 1", "Chapter 1: Intro");

            verify(lessonRepository).renameSection(1L, "Chapter 1", "Chapter 1: Intro");
        }

        @Test
        @DisplayName("UTC_MLS_35: đổi tên chương của khóa học không tồn tại bị từ chối")
        void renameUnknownCourse() {
            when(courseService.findCourse(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.renameSection(999999L, "Chapter 1", "New"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Khóa học đã chọn không còn tồn tại.");
        }

        @Test
        @DisplayName("UTC_MLS_36: xóa chương (bài học chuyển về Chưa phân chương)")
        void deletesSection() {
            courseExists();

            lessonService.deleteSection(1L, "Chapter 1");

            verify(lessonRepository).clearSection(1L, "Chapter 1");
        }

        @Test
        @DisplayName("UTC_MLS_37: xóa chương của khóa học không tồn tại bị từ chối")
        void deleteSectionUnknownCourse() {
            when(courseService.findCourse(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.deleteSection(999999L, "Chapter 1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Khóa học đã chọn không còn tồn tại.");
        }
    }

    // ------------------------------------------------------------------ move / reorder

    @Nested
    @DisplayName("Di chuyển và sắp xếp trong cột")
    class MoveAndReorder {

        private void twoVocabularyLessons() {
            LessonRow first = row(10L, 1, "Lesson 10", "Chapter 1", "VOCABULARY");
            LessonRow second = row(11L, 2, "Lesson 11", "Chapter 1", "KANJI");
            lenient().when(lessonRepository.findById(10L)).thenReturn(Optional.of(first));
            lenient().when(lessonRepository.findById(11L)).thenReturn(Optional.of(second));
            when(lessonRepository.findByCourse(1L)).thenReturn(List.of(first, second));
        }

        @Test
        @DisplayName("UTC_MLS_38: chuyển bài 10 xuống dưới bài 11 trong cột Từ vựng & Kanji")
        void movesDown() {
            twoVocabularyLessons();

            assertThat(lessonService.moveInColumn(10L, 1, "Chapter 1", "VOCABULARY,KANJI")).isTrue();
            verify(lessonRepository).batchUpdateOrders(List.of(11L, 10L));
        }

        @Test
        @DisplayName("UTC_MLS_39: bài đầu cột không thể chuyển lên")
        void cannotMoveAboveTop() {
            twoVocabularyLessons();

            assertThat(lessonService.moveInColumn(10L, -1, "Chapter 1", "VOCABULARY,KANJI")).isFalse();
            verify(lessonRepository, never()).batchUpdateOrders(anyList());
        }

        @Test
        @DisplayName("UTC_MLS_40: di chuyển bài không tồn tại bị từ chối")
        void moveUnknownLesson() {
            when(lessonRepository.findById(999999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.moveInColumn(999999L, 1, "Chapter 1", "VOCABULARY"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không tìm thấy bài học cần thao tác.");
        }

        @Test
        @DisplayName("UTC_MLS_41: sắp xếp lại cột theo [11, 10]")
        void reordersColumn() {
            twoVocabularyLessons();

            lessonService.reorderInColumn(1L, "Chapter 1", "VOCABULARY,KANJI", List.of(11L, 10L));

            verify(lessonRepository).batchUpdateOrders(List.of(11L, 10L));
        }

        @Test
        @DisplayName("UTC_MLS_42: danh sách id không khớp cột bị từ chối")
        void rejectsStaleIds() {
            twoVocabularyLessons();

            assertThatThrownBy(() -> lessonService.reorderInColumn(1L, "Chapter 1", "VOCABULARY,KANJI", List.of(10L, 999L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Danh sách bài học đã thay đổi trong lúc sắp xếp. Vui lòng tải lại trang rồi thử lại.");
            verify(lessonRepository, never()).batchUpdateOrders(anyList());
        }

        @Test
        @DisplayName("UTC_MLS_43: move() dùng cột mặc định của kỹ năng và chuyển xuống được")
        void moveDelegatesToDefaultColumn() {
            twoVocabularyLessons();

            assertThat(lessonService.move(10L, 1)).isTrue();
        }

        @Test
        @DisplayName("UTC_MLS_44: move() ở vị trí cuối cột trả về false")
        void moveAtBottom() {
            twoVocabularyLessons();

            assertThat(lessonService.move(11L, 1)).isFalse();
        }
    }
}
