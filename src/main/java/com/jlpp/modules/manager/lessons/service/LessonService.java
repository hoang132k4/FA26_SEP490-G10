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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business rules of the lesson workspace.
 *
 * <p>Ordering is scoped to a single (course, skill) pair and is always kept as a gapless
 * {@code 1..n} sequence. Every mutation that can disturb it calls {@link #normalizeOrders}.</p>
 */
@Service
public class LessonService {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_SECTION_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MAX_DURATION_MINUTES = 360;

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

    @Transactional(readOnly = true)
    public Set<Long> findLessonIdsWithFlashcards(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return Set.of();
        try {
            return lessonRepository.findLessonIdsWithFlashcards(lessonIds);
        } catch (Exception exception) {
            return Set.of();
        }
    }

    @Transactional(readOnly = true)
    public Set<Long> findLessonIdsWithExercises(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return Set.of();
        try {
            return lessonRepository.findLessonIdsWithExercises(lessonIds);
        } catch (Exception exception) {
            return Set.of();
        }
    }

    @Transactional
    public long create(long courseId, LessonInput rawInput) {
        LessonInput input = validate(rawInput, false);
        courseService.findCourse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."));

        boolean duplicate = lessonRepository.findByCourse(courseId).stream()
                .anyMatch(r -> Objects.equals(r.sectionName(), input.sectionName()) && r.title().equalsIgnoreCase(input.title()));
        if (duplicate) {
            throw new IllegalArgumentException("Tên bài học “" + input.title() + "” đã tồn tại trong chương này.");
        }

        int orderIndex = input.orderIndex() == null || input.orderIndex() < 1
                ? lessonRepository.nextOrderIndex(courseId, input.skill())
                : input.orderIndex();
        long lessonId = lessonRepository.insert(courseId, input, orderIndex);
        normalizeOrders(courseId, input.skill());
        return lessonId;
    }

    @Transactional
    public long createOther(long courseId, LessonInput rawInput) {
        LessonInput input = validate(rawInput, true);
        courseService.findCourse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."));

        boolean duplicate = lessonRepository.findByCourse(courseId).stream()
                .anyMatch(r -> Objects.equals(r.sectionName(), input.sectionName()) && r.title().equalsIgnoreCase(input.title()));
        if (duplicate) {
            throw new IllegalArgumentException("Tên bài học “" + input.title() + "” đã tồn tại trong chương này.");
        }

        int orderIndex = lessonRepository.nextOrderIndex(courseId, null);
        long lessonId = lessonRepository.insert(courseId, input, orderIndex);
        normalizeOrders(courseId, null);
        return lessonId;
    }

    @Transactional
    public void update(long lessonId, LessonInput rawInput) {
        LessonRow existing = requireLesson(lessonId);
        LessonInput input = validate(rawInput, false);

        boolean duplicate = lessonRepository.findByCourse(existing.courseId()).stream()
                .anyMatch(r -> r.id() != lessonId && Objects.equals(r.sectionName(), input.sectionName()) && r.title().equalsIgnoreCase(input.title()));
        if (duplicate) {
            throw new IllegalArgumentException("Tên bài học “" + input.title() + "” đã tồn tại trong chương này.");
        }

        int orderIndex = input.orderIndex() == null || input.orderIndex() < 1
                ? existing.order()
                : input.orderIndex();
        lessonRepository.update(lessonId, input, orderIndex);

        boolean orderChanged = orderIndex != existing.order();
        boolean skillChanged = !Objects.equals(existing.skillValue(), input.skill());
        if (orderChanged || skillChanged) {
            normalizeOrders(existing.courseId(), input.skill());
            if (skillChanged) {
                normalizeOrders(existing.courseId(), existing.skillValue());
            }
        }
    }

    @Transactional
    public void updateOther(long lessonId, LessonInput rawInput) {
        LessonRow existing = requireLesson(lessonId);
        if (SkillTypeEnum.fromParam(existing.skillValue()) != null) {
            throw new IllegalArgumentException("Bài học này không thuộc cột Khác.");
        }
        LessonInput input = validate(rawInput, true);

        boolean duplicate = lessonRepository.findByCourse(existing.courseId()).stream()
                .anyMatch(r -> r.id() != lessonId && Objects.equals(r.sectionName(), input.sectionName()) && r.title().equalsIgnoreCase(input.title()));
        if (duplicate) {
            throw new IllegalArgumentException("Tên bài học “" + input.title() + "” đã tồn tại trong chương này.");
        }

        lessonRepository.update(lessonId, input, existing.order());
    }

    @Transactional
    public void delete(long lessonId) {
        LessonRow existing = requireLesson(lessonId);
        lessonRepository.delete(lessonId);
        normalizeOrders(existing.courseId(), existing.skillValue());
    }

    @Transactional
    public void renameSection(long courseId, String oldName, String newName) {
        courseService.findCourse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."));
        lessonRepository.renameSection(courseId, oldName, newName);
    }

    @Transactional
    public void deleteSection(long courseId, String sectionName) {
        courseService.findCourse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học đã chọn không còn tồn tại."));
        lessonRepository.clearSection(courseId, sectionName);
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

    /**
     * Moves a lesson one position up (direction < 0) or down (direction > 0)
     * inside its specific chapter and skill column. Returns {@code false} when it cannot move.
     */
    @Transactional
    public boolean moveInColumn(long lessonId, int direction, String chapter, String columnSkillValues) {
        LessonRow existing = requireLesson(lessonId);
        long courseId = existing.courseId();
        List<LessonRow> allLessons = new ArrayList<>(lessonRepository.findByCourse(courseId));

        String targetChapter = (chapter != null && !chapter.isBlank())
                ? chapter.trim()
                : getChapterName(existing);

        List<String> acceptedSkills = (columnSkillValues != null && !columnSkillValues.isBlank())
                ? List.of(columnSkillValues.split(","))
                : defaultColumnForSkill(existing.skillValue());

        List<LessonRow> columnLessons = allLessons.stream()
                .filter(l -> targetChapter.equals(getChapterName(l)))
                .filter(l -> matchesColumn(l.skillValue(), acceptedSkills))
                .toList();

        List<Long> columnIds = columnLessons.stream().map(LessonRow::id).toList();
        int currentIndex = columnIds.indexOf(lessonId);
        int targetIndex = currentIndex + direction;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= columnIds.size()) {
            return false;
        }

        List<Long> reorderedIds = new ArrayList<>(columnIds);
        Collections.swap(reorderedIds, currentIndex, targetIndex);

        applyColumnOrder(allLessons, reorderedIds);
        return true;
    }

    /**
     * Reorders lessons within a specific chapter and skill column based on submittedIds.
     */
    @Transactional
    public void reorderInColumn(long courseId, String chapter, String columnSkillValues, List<Long> submittedIds) {
        List<LessonRow> allLessons = new ArrayList<>(lessonRepository.findByCourse(courseId));

        String targetChapter = chapter == null || chapter.isBlank() ? "Chưa phân chương" : chapter.trim();
        List<String> acceptedSkills = (columnSkillValues != null && !columnSkillValues.isBlank())
                ? List.of(columnSkillValues.split(","))
                : List.of();

        List<LessonRow> columnLessons = allLessons.stream()
                .filter(l -> targetChapter.equals(getChapterName(l)))
                .filter(l -> matchesColumn(l.skillValue(), acceptedSkills))
                .toList();

        List<Long> currentIds = columnLessons.stream().map(LessonRow::id).toList();
        List<Long> uniqueSubmitted = new ArrayList<>(new LinkedHashSet<>(submittedIds));

        if (uniqueSubmitted.size() != currentIds.size() || !uniqueSubmitted.containsAll(currentIds)) {
            throw new IllegalArgumentException(
                    "Danh sách bài học đã thay đổi trong lúc sắp xếp. Vui lòng tải lại trang rồi thử lại.");
        }

        applyColumnOrder(allLessons, uniqueSubmitted);
    }

    private void applyColumnOrder(List<LessonRow> allLessons, List<Long> reorderedIds) {
        Map<Long, LessonRow> lessonById = allLessons.stream()
                .collect(Collectors.toMap(LessonRow::id, l -> l));

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < allLessons.size(); i++) {
            if (reorderedIds.contains(allLessons.get(i).id())) {
                slots.add(i);
            }
        }

        for (int k = 0; k < slots.size(); k++) {
            int slotIndex = slots.get(k);
            Long lessonId = reorderedIds.get(k);
            allLessons.set(slotIndex, lessonById.get(lessonId));
        }

        List<Long> orderedIds = allLessons.stream().map(LessonRow::id).toList();
        lessonRepository.batchUpdateOrders(orderedIds);
    }

    private String getChapterName(LessonRow lesson) {
        String sectionName = lesson.sectionName();
        return sectionName == null || sectionName.isBlank() ? "Chưa phân chương" : sectionName.trim();
    }

    private boolean matchesColumn(String skillValue, List<String> acceptedSkills) {
        if (acceptedSkills.isEmpty() || acceptedSkills.contains("OTHER")) {
            return SkillTypeEnum.fromParam(skillValue) == null;
        }
        SkillTypeEnum skill = SkillTypeEnum.fromParam(skillValue);
        return skill != null && acceptedSkills.contains(skill.getValue());
    }

    private List<String> defaultColumnForSkill(String skillValue) {
        SkillTypeEnum skill = SkillTypeEnum.fromParam(skillValue);
        if (skill == null) {
            return List.of("OTHER");
        }
        return switch (skill) {
            case VOCABULARY, KANJI -> List.of("VOCABULARY", "KANJI");
            case GRAMMAR -> List.of("GRAMMAR");
            case READING, LISTENING -> List.of("READING", "LISTENING");
        };
    }

    /** Moves a lesson one place up or down inside its skill or chapter column. Returns {@code false} when it cannot move. */
    @Transactional
    public boolean move(long lessonId, int direction) {
        return moveInColumn(lessonId, direction, null, null);
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
        if (orderedIds == null || orderedIds.isEmpty()) return;
        lessonRepository.batchUpdateOrders(orderedIds);
    }

    private void normalizeOrders(long courseId, String skill) {
        String skillValue = skill == null ? null : resolveSkillValue(skill);
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

    private LessonInput validate(LessonInput rawInput, boolean allowOtherSkill) {
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
        boolean explicitOther = rawInput.skill() == null || "OTHER".equalsIgnoreCase(rawInput.skill().trim());
        if (skill == null && (!allowOtherSkill || !explicitOther)) {
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
                skill == null ? null : skill.getValue(),
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
