package com.jlpp.modules.manager.lessons.controller;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonDetail;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LibraryItem;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.SkillTab;
import com.jlpp.modules.manager.lessons.service.LessonDetailService;
import com.jlpp.modules.manager.lessons.service.LessonService;
import com.jlpp.modules.manager.courses.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manager workspace for the lessons of a course. Lessons are grouped by the five fixed skills,
 * ordered automatically inside their skill, and created or edited through a popup whose fields
 * depend on the skill.
 */
@Controller
@RequestMapping("/manager/lessons")
public class ManagerLessonController {

    private static final Logger log = LoggerFactory.getLogger(ManagerLessonController.class);
    private static final String VIEW = "pages/manager/lesson/manager-lessons";
    private static final String REDIRECT = "redirect:/manager/lessons";

    private final LessonService lessonService;
    private final CourseService courseService;
    private final LessonDetailService lessonDetailService;

    public ManagerLessonController(LessonService lessonService, CourseService courseService, LessonDetailService lessonDetailService) {
        this.lessonService = lessonService;
        this.courseService = courseService;
        this.lessonDetailService = lessonDetailService;
    }

    @GetMapping
    public String workspace(@RequestParam(required = false) Long courseId,
                            @RequestParam(required = false) String skill,
                            @RequestParam(name = "edit", required = false) Long editLessonId,
                            @RequestParam(name = "modal", required = false) String modalMode,
                            @RequestParam(name = "tab", required = false) String libraryTab,
                            @RequestParam(name = "entryId", required = false) Long libraryEntryId,
                            @RequestParam(name = "q", required = false) String libraryQuery,
                            @RequestParam(name = "level", required = false) String libraryLevel,
                            @RequestParam(name = "sort", required = false, defaultValue = "false") boolean sortMode,
                            Model model) {

        List<CourseOption> courses;
        try {
            courses = courseService.findCourses();
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not read the courses table", exception);
            courses = List.of();
            model.addAttribute("loadError", "Không kết nối được Supabase. Kiểm tra host, port, username và password trong cấu hình kết nối rồi tải lại.");
        }
        model.addAttribute("courses", courses);
        final List<CourseOption> availableCourses = courses;

        CourseOption selectedCourse = availableCourses.stream()
                .filter(course -> courseId != null && course.id() == courseId)
                .findFirst()
                .orElseGet(() -> availableCourses.stream()
                        .filter(course -> course.lessonCount() > 0)
                        .findFirst()
                        .orElse(availableCourses.isEmpty() ? null : availableCourses.get(0)));
        Long selectedCourseId = selectedCourse == null ? null : selectedCourse.id();
        String courseLevel = selectedCourse == null ? "N5" : selectedCourse.level();
        model.addAttribute("selectedCourse", selectedCourse);
        model.addAttribute("selectedCourseId", selectedCourseId);
        model.addAttribute("courseLessonCount", selectedCourse == null ? 0 : selectedCourse.lessonCount());
        model.addAttribute("courseLevel", courseLevel);

        List<LessonRow> courseLessons = List.of();
        if (selectedCourseId != null) {
            try {
                courseLessons = lessonService.findLessons(selectedCourseId);
            } catch (DataAccessException | TransactionException exception) {
                log.error("Could not read lessons of course {}", selectedCourseId, exception);
                model.addAttribute("loadError", "Không đọc được danh sách bài học từ database.");
            }
        }
        final List<LessonRow> allLessons = courseLessons;

        SkillTypeEnum selectedSkill = resolveSelectedSkill(skill, allLessons);
        model.addAttribute("selectedSkill", selectedSkill);
        model.addAttribute("selectedSkillLabel", selectedSkill.getLabel());

        List<SkillTab> skillTabs = new ArrayList<>();
        for (SkillTypeEnum candidate : SkillTypeEnum.orderedSkills()) {
            int count = (int) allLessons.stream()
                    .filter(lesson -> candidate.getValue().equals(lesson.skillValue()))
                    .count();
            skillTabs.add(new SkillTab(candidate.getValue(), candidate.getLabel(), candidate.getGlyph(),
                    count, candidate == selectedSkill));
        }
        model.addAttribute("skillTabs", skillTabs);

        List<LessonRow> visibleLessons = allLessons.stream()
                .filter(lesson -> selectedSkill.getValue().equals(lesson.skillValue()))
                .toList();
        model.addAttribute("lessons", visibleLessons);
        model.addAttribute("visibleCount", visibleLessons.size());
        model.addAttribute("totalMinutes", visibleLessons.stream()
                .mapToInt(lesson -> lesson.durationMinutes() == null ? 0 : lesson.durationMinutes())
                .sum());
        model.addAttribute("hiddenCount", visibleLessons.stream()
                .filter(lesson -> !lesson.isVisible())
                .count());
        model.addAttribute("sortMode", sortMode);
        model.addAttribute("statusOptions", LessonStatusEnum.orderedStatuses());
        model.addAttribute("skillOrderCsv", visibleLessons.stream()
                .map(lesson -> String.valueOf(lesson.id()))
                .collect(Collectors.joining(",")));

        LessonRow editingLesson = null;
        if (editLessonId != null) {
            try {
                editingLesson = lessonService.findLesson(editLessonId).orElse(null);
            } catch (DataAccessException exception) {
                log.error("Could not read lesson {}", editLessonId, exception);
            }
        }

        boolean modalOpen = editingLesson != null || "new".equalsIgnoreCase(modalMode);
        model.addAttribute("modalOpen", modalOpen);
        model.addAttribute("modalEditing", editingLesson != null);
        model.addAttribute("editingLesson", editingLesson);
        model.addAttribute("modalTitle", (editingLesson != null ? "Sửa bài học " : "Thêm bài học ")
                + selectedSkill.getLabel());

        boolean librarySupported = lessonDetailService.supportsLibrary(selectedSkill);
        List<LibraryItem> libraryItems = List.of();
        String resolvedLevel = libraryLevel == null || libraryLevel.isBlank() ? courseLevel : libraryLevel;
        if (modalOpen && librarySupported) {
            try {
                libraryItems = lessonDetailService.findLibrary(selectedSkill, resolvedLevel, libraryQuery);
            } catch (DataAccessException exception) {
                log.error("Could not read the {} library", selectedSkill.getValue(), exception);
                model.addAttribute("libraryError", "Không đọc được thư viện " + selectedSkill.getLabel() + ".");
            }
        }
        model.addAttribute("librarySupported", librarySupported);
        model.addAttribute("libraryItems", libraryItems);
        model.addAttribute("libraryLevel", resolvedLevel == null ? "" : resolvedLevel);
        model.addAttribute("libraryQuery", libraryQuery == null ? "" : libraryQuery);
        model.addAttribute("libraryTab", resolveLibraryTab(libraryTab, libraryItems, modalOpen));
        model.addAttribute("selectedEntryId", libraryEntryId);
        model.addAttribute("libraryEntryTitle", lessonDetailService
                .findLibraryTitle(selectedSkill, libraryEntryId).orElse(null));

        LessonDetail detail = null;
        if (editingLesson != null) {
            try {
                detail = lessonDetailService.findDetail(editingLesson.id()).orElse(null);
            } catch (DataAccessException exception) {
                log.error("Could not read details of lesson {}", editingLesson.id(), exception);
            }
        }
        Map<String, Object> form = modalOpen
                ? lessonDetailService.buildForm(selectedSkill, libraryEntryId, editingLesson, detail)
                : Map.of();
        model.addAttribute("form", form);

        return VIEW;
    }

    @PostMapping
    public String create(@RequestParam MultiValueMap<String, String> params,
                         RedirectAttributes redirectAttributes) {

        Long courseId = longValue(params, "courseId");
        SkillTypeEnum skill = SkillTypeEnum.fromParam(params.getFirst("skillType"));
        if (courseId == null) {
            redirectAttributes.addFlashAttribute("lessonError", "Vui lòng chọn khóa học trước khi thêm bài học.");
            return REDIRECT;
        }
        if (skill == null) {
            redirectAttributes.addFlashAttribute("lessonError", "Không xác định được kỹ năng của bài học.");
            return redirectToWorkspace(courseId, null, redirectAttributes);
        }

        long createdLessonId = 0;
        try {
            String title = params.getFirst("title");
            LessonInput input = new LessonInput(
                    title,
                    params.getFirst("sectionName"),
                    params.getFirst("description"),
                    buildLessonContent(skill, params),
                    skill.getValue(),
                    params.getFirst("status") == null ? LessonStatusEnum.ACTIVE.name() : params.getFirst("status"),
                    null,
                    integerValue(params, "durationMinutes"),
                    booleanValue(params, "freePreview"));

            createdLessonId = lessonService.create(courseId, input);
            lessonDetailService.save(createdLessonId, skill, longValue(params, "entryId"), params,
                    booleanValue(params, "saveToLibrary"), courseLevelOf(courseId));
            redirectAttributes.addFlashAttribute("lessonSuccess",
                    "Đã lưu bài học “" + trimmed(title) + "” vào kỹ năng " + skill.getLabel() + ".");
        } catch (IllegalArgumentException exception) {
            deleteQuietly(createdLessonId);
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            deleteQuietly(createdLessonId);
            redirectAttributes.addFlashAttribute("lessonError", "Không lưu được bài học: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill.getValue(), redirectAttributes);
    }

    @PostMapping("/{lessonId}")
    public String update(@PathVariable long lessonId,
                         @RequestParam MultiValueMap<String, String> params,
                         RedirectAttributes redirectAttributes) {

        Long courseId = longValue(params, "courseId");
        SkillTypeEnum skill = SkillTypeEnum.fromParam(params.getFirst("skillType"));
        try {
            LessonRow existing = lessonService.findLesson(lessonId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học cần sửa."));
            SkillTypeEnum resolvedSkill = skill == null
                    ? SkillTypeEnum.fromParam(existing.skillValue())
                    : skill;
            if (resolvedSkill == null) {
                resolvedSkill = SkillTypeEnum.VOCABULARY;
            }

            String title = params.getFirst("title");
            LessonInput input = new LessonInput(
                    title,
                    params.getFirst("sectionName"),
                    params.getFirst("description"),
                    buildLessonContent(resolvedSkill, params),
                    resolvedSkill.getValue(),
                    params.getFirst("status") == null ? existing.status().name() : params.getFirst("status"),
                    null,
                    integerValue(params, "durationMinutes"),
                    booleanValue(params, "freePreview"));

            lessonService.update(lessonId, input);
            lessonDetailService.save(lessonId, resolvedSkill, longValue(params, "entryId"), params,
                    booleanValue(params, "saveToLibrary"), courseLevelOf(courseId));
            redirectAttributes.addFlashAttribute("lessonSuccess", "Đã lưu bài học “" + trimmed(title) + "”.");
            return redirectToWorkspace(courseId, resolvedSkill.getValue(), redirectAttributes);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("lessonError", "Không lưu được bài học: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill == null ? null : skill.getValue(), redirectAttributes);
    }

    @PostMapping("/{lessonId}/delete")
    public String delete(@PathVariable long lessonId,
                         @RequestParam(required = false) Long courseId,
                         @RequestParam(required = false) String skill,
                         RedirectAttributes redirectAttributes) {
        try {
            LessonRow lesson = lessonService.findLesson(lessonId).orElse(null);
            lessonService.delete(lessonId);
            redirectAttributes.addFlashAttribute("lessonSuccess",
                    "Đã xoá bài học “" + (lesson == null ? "đã chọn" : lesson.title()) + "”.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("lessonError", "Không xoá được bài học: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    @PostMapping("/{lessonId}/duplicate")
    public String duplicate(@PathVariable long lessonId,
                            @RequestParam(required = false) Long courseId,
                            @RequestParam(required = false) String skill,
                            RedirectAttributes redirectAttributes) {
        try {
            LessonRow source = lessonService.findLesson(lessonId).orElse(null);
            long copyId = lessonService.duplicate(lessonId);
            copyDetail(lessonId, copyId);
            redirectAttributes.addFlashAttribute("lessonSuccess",
                    "Đã nhân bản “" + (source == null ? "bài học" : source.title())
                            + "”. Bản sao đang ẩn, sửa lại rồi hiển thị khi đã sẵn sàng.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("lessonError",
                    "Không nhân bản được bài học: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    @PostMapping("/{lessonId}/status")
    public String changeStatus(@PathVariable long lessonId,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) Long courseId,
                               @RequestParam(required = false) String skill,
                               RedirectAttributes redirectAttributes) {
        try {
            LessonRow lesson = lessonService.findLesson(lessonId).orElse(null);
            LessonStatusEnum nextStatus = lessonService.changeStatus(lessonId, LessonStatusEnum.fromParam(status));
            String title = lesson == null ? "bài học" : "“" + lesson.title() + "”";
            redirectAttributes.addFlashAttribute("lessonSuccess", nextStatus.isVisible()
                    ? "Đã hiển thị bài học " + title + "."
                    : "Đã ẩn bài học " + title + ".");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("lessonError",
                    "Không đổi được trạng thái bài học: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    @PostMapping("/{lessonId}/move")
    public String move(@PathVariable long lessonId,
                       @RequestParam(defaultValue = "up") String direction,
                       @RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String skill,
                       RedirectAttributes redirectAttributes) {
        try {
            boolean moved = lessonService.move(lessonId, "down".equalsIgnoreCase(direction) ? 1 : -1);
            if (!moved) {
                redirectAttributes.addFlashAttribute("lessonError",
                        "Bài học đã ở vị trí đầu hoặc cuối của kỹ năng nên không thể di chuyển thêm.");
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("lessonError", "Không đổi được thứ tự: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    @PostMapping("/reorder")
    public String reorder(@RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) String skill,
                          @RequestParam(required = false) String ids,
                          RedirectAttributes redirectAttributes) {
        if (courseId == null || ids == null || ids.isBlank()) {
            redirectAttributes.addFlashAttribute("lessonError", "Không nhận được thứ tự mới của bài học.");
            return redirectToWorkspace(courseId, skill, redirectAttributes);
        }
        try {
            List<Long> orderedIds = new ArrayList<>();
            for (String rawId : ids.split(",")) {
                String candidate = rawId.trim();
                if (!candidate.isEmpty()) {
                    orderedIds.add(Long.parseLong(candidate));
                }
            }
            lessonService.saveOrder(courseId, skill, orderedIds);
            redirectAttributes.addFlashAttribute("lessonSuccess",
                    "Đã lưu thứ tự bài học trong kỹ năng " + labelOf(skill) + ".");
        } catch (NumberFormatException exception) {
            redirectAttributes.addFlashAttribute("lessonError", "Thứ tự bài học gửi lên không hợp lệ.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("lessonError", "Không lưu được thứ tự: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    /**
     * Keeps {@code lessons.content} readable for the learner pages even though the structured data
     * now lives in {@code lesson_details}.
     */
    private String buildLessonContent(SkillTypeEnum skill, MultiValueMap<String, String> params) {
        return switch (skill) {
            case VOCABULARY -> joinCells(params, "word", "wordReading", "wordMeaning");
            case GRAMMAR -> joinNonBlank(params.getFirst("pattern"), params.getFirst("meaning"));
            case KANJI -> joinNonBlank(params.getFirst("character"), params.getFirst("kanjiMeaning"));
            case LISTENING -> params.getFirst("transcript");
            case READING -> params.getFirst("passage");
        };
    }

    private String joinCells(MultiValueMap<String, String> params, String... keys) {
        List<String> cells = params.get(keys[0]);
        if (cells == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < cells.size(); index++) {
            String word = valueAt(params.get(keys[0]), index);
            String reading = valueAt(params.get(keys[1]), index);
            String meaning = valueAt(params.get(keys[2]), index);
            if (word.isEmpty() && meaning.isEmpty()) {
                continue;
            }
            StringBuilder line = new StringBuilder(word);
            if (!reading.isEmpty()) {
                line.append("（").append(reading).append("）");
            }
            if (!meaning.isEmpty()) {
                line.append(": ").append(meaning);
            }
            lines.add(line.toString());
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return "";
        }
        return values.get(index).trim();
    }

    private String joinNonBlank(String first, String second) {
        List<String> parts = new ArrayList<>(2);
        if (first != null && !first.isBlank()) {
            parts.add(first.trim());
        }
        if (second != null && !second.isBlank()) {
            parts.add(second.trim());
        }
        return parts.isEmpty() ? null : String.join(" — ", parts);
    }

    private void copyDetail(long sourceLessonId, long targetLessonId) {
        try {
            lessonDetailService.copyDetail(sourceLessonId, targetLessonId);
        } catch (Exception exception) {
            log.warn("Could not copy the skill detail of lesson {}", sourceLessonId, exception);
        }
    }

    private void deleteQuietly(long lessonId) {
        if (lessonId <= 0) {
            return;
        }
        try {
            lessonService.delete(lessonId);
        } catch (Exception exception) {
            log.warn("Could not roll back lesson {}", lessonId, exception);
        }
    }

    private String courseLevelOf(Long courseId) {
        if (courseId == null) {
            return "N5";
        }
        return courseService.findCourse(courseId).map(CourseOption::level).orElse("N5");
    }

    private String resolveLibraryTab(String requestedTab, List<LibraryItem> items, boolean modalOpen) {
        if (!modalOpen) {
            return "create";
        }
        if ("create".equalsIgnoreCase(requestedTab)) {
            return "create";
        }
        if ("library".equalsIgnoreCase(requestedTab)) {
            return "library";
        }
        return items.isEmpty() ? "create" : "library";
    }

    private SkillTypeEnum resolveSelectedSkill(String requestedSkill, List<LessonRow> lessons) {
        SkillTypeEnum explicitSkill = SkillTypeEnum.fromParam(requestedSkill);
        if (explicitSkill != null) {
            return explicitSkill;
        }
        for (SkillTypeEnum skill : SkillTypeEnum.orderedSkills()) {
            boolean hasLessons = lessons.stream()
                    .anyMatch(lesson -> skill.getValue().equals(lesson.skillValue()));
            if (hasLessons) {
                return skill;
            }
        }
        return SkillTypeEnum.VOCABULARY;
    }

    private String redirectToWorkspace(Long courseId, String skill, RedirectAttributes redirectAttributes) {
        if (courseId != null) {
            redirectAttributes.addAttribute("courseId", courseId);
        }
        SkillTypeEnum resolvedSkill = SkillTypeEnum.fromParam(skill);
        if (resolvedSkill != null) {
            redirectAttributes.addAttribute("skill", resolvedSkill.getValue());
        }
        return REDIRECT;
    }

    private Long longValue(MultiValueMap<String, String> params, String key) {
        String value = params.getFirst(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer integerValue(MultiValueMap<String, String> params, String key) {
        String value = params.getFirst(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean booleanValue(MultiValueMap<String, String> params, String key) {
        String value = params.getFirst(key);
        return value != null && ("true".equalsIgnoreCase(value.trim()) || "on".equalsIgnoreCase(value.trim()));
    }

    private String labelOf(String skill) {
        SkillTypeEnum resolved = SkillTypeEnum.fromParam(skill);
        return resolved == null ? "đã chọn" : resolved.getLabel();
    }

    private String trimmed(String value) {
        return value == null || value.isBlank() ? "bài học mới" : value.trim();
    }

    private String describe(DataAccessException exception) {
        Throwable rootCause = exception.getMostSpecificCause();
        String message = rootCause.getMessage();
        log.error("Lesson database operation failed: {}", message, exception);
        return message == null || message.isBlank() ? "database từ chối dữ liệu." : message;
    }
}
