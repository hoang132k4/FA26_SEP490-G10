package com.jlpp.modules.manager.lessons.service;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.manager.courses.service.CourseService;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Business rules of the lesson workspace.
 *
 * <p>Ordering is scoped to a single (course, skill) pair and is always kept as a gapless
 * {@code 1..n} sequence. Every mutation that can disturb it calls {@link #normalizeOrders}.</p>
 */
@Service
public class LessonService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SECTION_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_DURATION_MINUTES = 600;

    private final LessonRepository lessonRepository;
    private final CourseService courseService;

    public LessonService(LessonRepository lessonRepository, CourseService courseService) {
        this.lessonRepository = lessonRepository;
        this.courseService = courseService;
    }

    @Transactional(readOnly = true)
    public List<LessonRow> findLessons(long courseId) {
        return lessonRepository.findByCourse(courseId);
    }

    @Transactional(readOnly = true)
    public Optional<LessonRow> findLesson(long lessonId) {
        return lessonRepository.findById(lessonId);
    }

    @Transactional
    public long create(long courseId, LessonInput rawInput) {
        LessonInput input = validate(rawInput);
        courseService.findCourse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."));

        int orderIndex = input.orderIndex() == null || input.orderIndex() < 1
                ? lessonRepository.nextOrderIndex(courseId, input.skill())
                : input.orderIndex();
        long lessonId = lessonRepository.insert(courseId, input, orderIndex);
        normalizeOrders(courseId, input.skill());
        return lessonId;
    }

    @Transactional
    public void update(long lessonId, LessonInput rawInput) {
        LessonRow existing = requireLesson(lessonId);
        LessonInput input = validate(rawInput);

        int orderIndex = input.orderIndex() == null || input.orderIndex() < 1
                ? existing.order()
                : input.orderIndex();
        lessonRepository.update(lessonId, input, orderIndex);
        normalizeOrders(existing.courseId(), input.skill());
        if (!Objects.equals(existing.skillValue(), input.skill())) {
            normalizeOrders(existing.courseId(), existing.skillValue());
        }
    }

    @Transactional
    public void delete(long lessonId) {
        LessonRow existing = requireLesson(lessonId);
        lessonRepository.delete(lessonId);
        normalizeOrders(existing.courseId(), existing.skillValue());
    }

    /**
     * Creates a hidden copy right after the source lesson. The copy is never published
     * automatically, so a duplicate can be reviewed before it reaches learners.
     */
    @Transactional
    public long duplicate(long lessonId) {
        LessonRow source = requireLesson(lessonId);
        String skill = resolveSkillValue(source.skillValue());

        LessonInput copy = new LessonInput(
                duplicateTitle(source.title()),
                source.sectionName(),
                source.description(),
                source.content(),
                skill,
                LessonStatusEnum.INACTIVE.name(),
                source.order(),
                source.durationMinutes(),
                source.freePreview());

        List<Long> order = new ArrayList<>(lessonRepository.findIdsInOrder(source.courseId(), skill));
        long copyId = lessonRepository.insert(source.courseId(), copy, source.order());
        int sourceIndex = order.indexOf(source.id());
        order.add(sourceIndex < 0 ? order.size() : sourceIndex + 1, copyId);
        applyOrder(order);
        return copyId;
    }

    @Transactional
    public LessonStatusEnum changeStatus(long lessonId, LessonStatusEnum requestedStatus) {
        LessonRow existing = requireLesson(lessonId);
        LessonStatusEnum nextStatus = requestedStatus == null
                ? existing.status().toggled()
                : requestedStatus;
        lessonRepository.updateStatus(lessonId, nextStatus);
        return nextStatus;
    }

    /** Moves a lesson one place up or down inside its skill. Returns {@code false} when it cannot move. */
    @Transactional
    public boolean move(long lessonId, int direction) {
        LessonRow existing = requireLesson(lessonId);
        List<Long> order = new ArrayList<>(
                lessonRepository.findIdsInOrder(existing.courseId(), resolveSkillValue(existing.skillValue())));
        int currentIndex = order.indexOf(lessonId);
        int targetIndex = currentIndex + direction;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= order.size()) {
            return false;
        }
        Collections.swap(order, currentIndex, targetIndex);
        applyOrder(order);
        return true;
    }

    /**
     * Persists the order produced by the sort mode. The submitted ids must match the current
     * lessons of the skill exactly, otherwise the whole request is rejected.
     */
    @Transactional
    public void saveOrder(long courseId, String skill, List<Long> submittedIds) {
        String skillValue = resolveSkillValue(skill);
        List<Long> currentIds = lessonRepository.findIdsInOrder(courseId, skillValue);
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(submittedIds));
        if (uniqueIds.size() != currentIds.size() || !uniqueIds.containsAll(currentIds)) {
            throw new IllegalArgumentException(
                    "Danh sách bài học đã thay đổi trong lúc sắp xếp. Vui lòng tải lại trang rồi thử lại.");
        }
        applyOrder(uniqueIds);
    }

    private void applyOrder(List<Long> orderedIds) {
        for (int index = 0; index < orderedIds.size(); index++) {
            lessonRepository.updateOrder(orderedIds.get(index), index + 1);
        }
    }

    private void normalizeOrders(long courseId, String skill) {
        String skillValue = resolveSkillValue(skill);
        applyOrder(lessonRepository.findIdsInOrder(courseId, skillValue));
    }

    private LessonRow requireLesson(long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học cần thao tác."));
    }

    private String resolveSkillValue(String rawSkill) {
        SkillTypeEnum skill = SkillTypeEnum.fromParam(rawSkill);
        return skill == null ? SkillTypeEnum.VOCABULARY.getValue() : skill.getValue();
    }

    private String duplicateTitle(String title) {
        String suffix = " (bản sao)";
        String base = title == null || title.isBlank() ? "Bài học" : title.trim();
        String candidate = base + suffix;
        if (candidate.length() <= MAX_TITLE_LENGTH) {
            return candidate;
        }
        return base.substring(0, MAX_TITLE_LENGTH - suffix.length()).trim() + suffix;
    }

    private LessonInput validate(LessonInput rawInput) {
        if (rawInput == null) {
            throw new IllegalArgumentException("Không nhận được dữ liệu bài học.");
        }
        String title = trim(rawInput.title());
        if (title == null) {
            throw new IllegalArgumentException("Vui lòng nhập tên bài học.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Tên bài học tối đa " + MAX_TITLE_LENGTH + " ký tự.");
        }

        SkillTypeEnum skill = SkillTypeEnum.fromParam(rawInput.skill());
        if (skill == null) {
            throw new IllegalArgumentException("Vui lòng chọn kỹ năng cho bài học.");
        }

        LessonStatusEnum status = LessonStatusEnum.fromParam(rawInput.status());

        String sectionName = trim(rawInput.sectionName());
        if (sectionName != null && sectionName.length() > MAX_SECTION_LENGTH) {
            throw new IllegalArgumentException("Chương / Phần tối đa " + MAX_SECTION_LENGTH + " ký tự.");
        }

        String description = trim(rawInput.description());
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả ngắn tối đa " + MAX_DESCRIPTION_LENGTH + " ký tự.");
        }

        String content = rawInput.content() == null ? null : rawInput.content().trim();
        if (content != null && content.isEmpty()) {
            content = null;
        }
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung bài học tối đa " + MAX_CONTENT_LENGTH + " ký tự.");
        }

        Integer duration = rawInput.durationMinutes();
        if (duration != null && (duration < 0 || duration > MAX_DURATION_MINUTES)) {
            throw new IllegalArgumentException(
                    "Thời lượng phải nằm trong khoảng 0–" + MAX_DURATION_MINUTES + " phút.");
        }

        Integer order = rawInput.orderIndex();
        if (order != null && order < 1) {
            order = null;
        }

        return new LessonInput(
                title,
                sectionName,
                description,
                content,
                skill.getValue(),
                status.name(),
                order,
                duration,
                rawInput.freePreview());
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
