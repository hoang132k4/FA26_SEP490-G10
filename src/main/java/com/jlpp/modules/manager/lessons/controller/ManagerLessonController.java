package com.jlpp.modules.manager.lessons.controller;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.course.entity.StudyMaterial;
import com.jlpp.modules.course.service.StudyMaterialService;
import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetCardForm;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetForm;
import com.jlpp.modules.manager.flashcards.service.ManagerFlashcardService;
import com.jlpp.modules.lesson.entity.Lesson;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonDetail;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.ChapterGroup;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.SkillColumn;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonStudyView;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.PracticeQuizView;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LibraryItem;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.SkillTab;
import com.jlpp.modules.manager.lessons.service.LessonDetailService;
import com.jlpp.modules.manager.lessons.service.LessonAudioStorageService;
import com.jlpp.modules.manager.lessons.service.LessonService;
import com.jlpp.modules.manager.courses.service.CourseService;
import com.jlpp.modules.quiz.dto.ManagerQuizForm;
import com.jlpp.modules.quiz.entity.Exercise;
import com.jlpp.modules.quiz.repository.ExerciseRepository;
import com.jlpp.modules.quiz.service.ManagerQuizService;
import com.jlpp.modules.quiz.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manager workspace for the lessons of a course. Lessons are grouped by the five fixed skills,
 * ordered automatically inside their skill, and created or edited through a popup whose fields
 * depend on the skill.
 */
@Controller
@RequestMapping("/manager/lessons")
@SessionAttributes("draftChapterNames")
public class ManagerLessonController {

    private static final Logger log = LoggerFactory.getLogger(ManagerLessonController.class);
    private static final String VIEW = "pages/manager/lesson/manager-lessons";
    private static final String REDIRECT = "redirect:/manager/lessons";

    private final LessonService lessonService;
    private final CourseService courseService;
    private final LessonDetailService lessonDetailService;
    private final LessonAudioStorageService lessonAudioStorageService;
    private final StudyMaterialService studyMaterialService;
    private final FlashcardRepository flashcardRepository;
    private final ExerciseRepository exerciseRepository;
    private final QuizService quizService;
    private final ManagerFlashcardService managerFlashcardService;
    private final ManagerQuizService managerQuizService;

    public ManagerLessonController(LessonService lessonService, CourseService courseService,
                                   LessonDetailService lessonDetailService,
                                   LessonAudioStorageService lessonAudioStorageService,
                                   StudyMaterialService studyMaterialService,
                                   FlashcardRepository flashcardRepository,
                                   ExerciseRepository exerciseRepository,
                                   QuizService quizService,
                                   ManagerFlashcardService managerFlashcardService,
                                   ManagerQuizService managerQuizService) {
        this.lessonService = lessonService;
        this.courseService = courseService;
        this.lessonDetailService = lessonDetailService;
        this.lessonAudioStorageService = lessonAudioStorageService;
        this.studyMaterialService = studyMaterialService;
        this.flashcardRepository = flashcardRepository;
        this.exerciseRepository = exerciseRepository;
        this.quizService = quizService;
        this.managerFlashcardService = managerFlashcardService;
        this.managerQuizService = managerQuizService;
    }

    @ModelAttribute("draftChapterNames")
    public Map<Long, List<String>> draftChapterNames() { return new java.util.LinkedHashMap<>(); }

    @GetMapping
    public String workspace(@RequestParam(name = "courseId", required = false) Long courseId,
                            @RequestParam(name = "skill", required = false) String skill,
                            @RequestParam(name = "edit", required = false) Long editLessonId,
                            @RequestParam(name = "modal", required = false) String modalMode,
                            @RequestParam(name = "tab", required = false) String libraryTab,
                            @RequestParam(name = "entryId", required = false) Long libraryEntryId,
                            @RequestParam(name = "q", required = false) String libraryQuery,
                            @RequestParam(name = "level", required = false) String libraryLevel,
                            @RequestParam(name = "sectionName", required = false) String requestedSectionName,
                            @RequestParam(name = "detailId", required = false) Long detailId,
                            @RequestParam(name = "sort", required = false, defaultValue = "false") boolean sortMode,
                            Model model,
                            HttpServletRequest request,
                            @ModelAttribute("draftChapterNames") Map<Long, List<String>> draftChapterNames) {

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
        model.addAttribute("openDetailId", detailId);
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
        List<Long> lessonIds = allLessons.stream().map(LessonRow::id).toList();
        java.util.Set<Long> flashcardLessonIds = lessonService.findLessonIdsWithFlashcards(lessonIds);
        java.util.Set<Long> practiceLessonIds = lessonService.findLessonIdsWithExercises(lessonIds);

        Map<Long, List<String>> lessonTags = new java.util.LinkedHashMap<>();
        for (LessonRow lesson : allLessons) {
            SkillTypeEnum skillType = SkillTypeEnum.fromParam(lesson.skillValue());
            List<String> tags = new ArrayList<>();
            String titleLower = lesson.title() == null ? "" : lesson.title().toLowerCase();
            boolean isFlashcard = flashcardLessonIds.contains(lesson.id())
                    || titleLower.startsWith("flashcard");
            boolean isPractice = practiceLessonIds.contains(lesson.id())
                    || titleLower.startsWith("luyện tập") || titleLower.startsWith("quiz");
            if (isFlashcard || isPractice) {
                if (isFlashcard) tags.add("FLASHCARD");
                if (isPractice) tags.add("PRACTICE");
            } else if (skillType != null) {
                tags.add(skillType.getValue());
            }
            lessonTags.put(lesson.id(), List.copyOf(tags));
        }

        // Lazy load: chi nap LessonStudyView khi co detailId duoc yeu cau
        List<LessonStudyView> lessonStudyViews = new ArrayList<>();
        if (detailId != null) {
            allLessons.stream()
                    .filter(l -> l.id() == detailId)
                    .findFirst()
                    .or(() -> lessonService.findLesson(detailId))
                    .ifPresent(lesson -> lessonStudyViews.add(buildLessonStudyView(lesson)));
        }
        model.addAttribute("lessonStudyViews", lessonStudyViews);
        model.addAttribute("lessonTags", lessonTags);
        model.addAttribute("courseLessonCount", allLessons.size());

        LinkedHashSet<String> chapterNames = new LinkedHashSet<>();
        allLessons.stream().map(this::chapterName).forEach(chapterNames::add);
        if (selectedCourseId != null) {
            chapterNames.addAll(draftChapterNames.getOrDefault(selectedCourseId, List.of()));
        }
        List<ChapterGroup> chapterGroups = chapterNames.stream().map(name -> new ChapterGroup(name, 0, List.of(
                chapterColumn(name, "VOCABULARY,KANJI", "Từ vựng & Kanji", "語", allLessons),
                chapterColumn(name, "GRAMMAR", "Ngữ pháp", "文", allLessons),
                chapterColumn(name, "READING,LISTENING", "Đọc hiểu & Nghe hiểu", "聴", allLessons)
        ))).map(group -> new ChapterGroup(group.name(),
                (int) allLessons.stream().filter(lesson -> group.name().equals(chapterName(lesson))).count(),
                group.columns())).collect(Collectors.toCollection(ArrayList::new));
        for (int index = 0; index < chapterGroups.size(); index++) {
            ChapterGroup group = chapterGroups.get(index);
            List<LessonRow> unclassified = allLessons.stream()
                    .filter(lesson -> chapterName(lesson).equals(group.name()))
                    .filter(lesson -> !isKnownSkill(lesson.skillValue())).toList();
            List<SkillColumn> columns = new ArrayList<>(group.columns());
            columns.add(new SkillColumn("OTHER", "Khác", "▤", unclassified));
            chapterGroups.set(index, new ChapterGroup(group.name(), group.lessonCount(), columns));
        }
        chapterGroups = List.copyOf(chapterGroups);
        model.addAttribute("chapterGroups", chapterGroups);
        model.addAttribute("chapterNames", chapterNames);

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
        model.addAttribute("skillOrderCsv", visibleLessons.stream()
                .map(lesson -> String.valueOf(lesson.id()))
                .collect(Collectors.joining(",")));

        LessonRow editingLesson = null;
        if (editLessonId != null) {
            LessonRow checkLesson = allLessons.stream().filter(l -> l.id() == editLessonId).findFirst().orElse(null);
            String titleLower = checkLesson != null && checkLesson.title() != null ? checkLesson.title().toLowerCase() : "";
            if (flashcardLessonIds.contains(editLessonId) || titleLower.startsWith("flashcard")) {
                List<Flashcard> cards = flashcardRepository.findByLessonId(editLessonId);
                if (cards != null && !cards.isEmpty()) {
                    Long fcId = cards.get(0).getFlashcardId() != null ? cards.get(0).getFlashcardId() : cards.get(0).getId();
                    return "redirect:/manager/flashcards/" + fcId + "/edit";
                }
                return "redirect:/manager/flashcards";
            }
            if (practiceLessonIds.contains(editLessonId) || titleLower.startsWith("luyện tập") || titleLower.startsWith("quiz")) {
                Optional<Exercise> ex = exerciseRepository.findByLessonId(editLessonId);
                if (ex.isPresent()) {
                    return "redirect:/manager/quizzes/" + ex.get().getExerciseId() + "/edit";
                }
                return "redirect:/manager/quizzes?lessonId=" + editLessonId;
            }
            try {
                editingLesson = lessonService.findLesson(editLessonId).orElse(null);
            } catch (DataAccessException exception) {
                log.error("Could not read lesson {}", editLessonId, exception);
            }
        }

        boolean modalOpen = editingLesson != null || "new".equalsIgnoreCase(modalMode);
        boolean otherMode = "OTHER".equalsIgnoreCase(skill)
                || (editingLesson != null && !isKnownSkill(editingLesson.skillValue()));
        model.addAttribute("modalOpen", modalOpen);
        model.addAttribute("otherMode", otherMode);
        model.addAttribute("modalEditing", editingLesson != null);
        model.addAttribute("editingLesson", editingLesson);
        model.addAttribute("modalTitle", (editingLesson != null ? "Sửa bài học " : "Thêm bài học ")
                + selectedSkill.getLabel());

        boolean librarySupported = !otherMode && lessonDetailService.supportsLibrary(selectedSkill);
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
        if (editingLesson != null && !otherMode) {
            try {
                detail = lessonDetailService.findDetail(editingLesson.id()).orElse(null);
            } catch (DataAccessException exception) {
                log.error("Could not read details of lesson {}", editingLesson.id(), exception);
            }
        }
        Map<String, Object> form;
        if (!modalOpen) {
            form = Map.of();
        } else if (otherMode) {
            form = new java.util.LinkedHashMap<>();
            form.put("title", editingLesson == null ? "" : editingLesson.title());
            form.put("content", editingLesson == null ? "" : editingLesson.content());
            form.put("durationMinutes", editingLesson == null ? "" : editingLesson.durationMinutes());
            form.put("freePreview", editingLesson != null && editingLesson.freePreview());
            form.put("sectionName", editingLesson == null ? (requestedSectionName == null ? "" : requestedSectionName)
                    : editingLesson.sectionName());
        } else {
            form = lessonDetailService.buildForm(selectedSkill, libraryEntryId, editingLesson, detail);
        }
        if (modalOpen && editingLesson == null) {
            if (requestedSectionName != null && !requestedSectionName.isBlank()) {
                form.put("sectionName", requestedSectionName.trim());
            } else if ((form.get("sectionName") == null || form.get("sectionName").toString().isBlank()) && !chapterNames.isEmpty()) {
                String firstChap = chapterNames.stream().filter(c -> !c.equals("Chưa phân chương")).findFirst().orElse(null);
                if (firstChap != null) form.put("sectionName", firstChap);
            }
        }
        model.addAttribute("form", form);
        model.addAttribute("requestedSectionName", form.get("sectionName"));

        if (detailId != null && request != null && "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "pages/manager/lesson/manager-lessons :: lessonDetailContainer";
        }

        return VIEW;
    }

    private SkillColumn chapterColumn(String chapter, String skillValues, String label, String glyph,
                                      List<LessonRow> lessons) {
        List<String> accepted = List.of(skillValues.split(","));
        return new SkillColumn(skillValues, label, glyph, lessons.stream()
                .filter(lesson -> chapter.equals(chapterName(lesson)))
                .filter(lesson -> {
                    SkillTypeEnum skill = SkillTypeEnum.fromParam(lesson.skillValue());
                    return skill != null && accepted.contains(skill.getValue());
                }).toList());
    }

    private String chapterName(LessonRow lesson) {
        String sectionName = lesson.sectionName();
        return sectionName == null || sectionName.isBlank() ? "Chưa phân chương" : sectionName.trim();
    }

    private boolean isKnownSkill(String skillValue) {
        return SkillTypeEnum.fromParam(skillValue) != null;
    }

    @PostMapping("/sections")
    public String createDraftSection(@RequestParam(name = "courseId") Long courseId,
                                     @RequestParam(name = "sectionName") String sectionName,
                                     @ModelAttribute("draftChapterNames") Map<Long, List<String>> draftChapterNames,
                                     RedirectAttributes redirectAttributes) {
        String name = sectionName == null ? "" : sectionName.trim();
        if (name.isBlank() || name.length() > 150) {
            redirectAttributes.addFlashAttribute("lessonError", "Tên chương phải từ 1 đến 150 ký tự.");
        } else {
            List<String> names = new ArrayList<>(draftChapterNames.getOrDefault(courseId, List.of()));
            boolean exists = lessonService.findLessons(courseId).stream().anyMatch(row -> name.equals(row.sectionName()))
                    || names.stream().anyMatch(name::equalsIgnoreCase);
            if (!exists) names.add(name);
            draftChapterNames.put(courseId, names);
            redirectAttributes.addFlashAttribute("lessonSuccess", "Đã tạo chương trong phiên làm việc. Chương được lưu cùng bài học đầu tiên.");
        }
        redirectAttributes.addAttribute("courseId", courseId);
        return REDIRECT;
    }

    @PostMapping("/sections/rename")
    public String renameSection(@RequestParam(name = "courseId") Long courseId,
                                @RequestParam(name = "oldName") String oldName,
                                @RequestParam(name = "newName") String newName,
                                @ModelAttribute("draftChapterNames") Map<Long, List<String>> draftChapterNames,
                                RedirectAttributes redirectAttributes) {
        String oldValue = oldName == null ? "" : oldName.trim();
        String nextValue = newName == null ? "" : newName.trim();
        if (oldValue.isBlank() || nextValue.isBlank() || nextValue.length() > 150) {
            redirectAttributes.addFlashAttribute("lessonError", "Tên chương phải từ 1 đến 150 ký tự.");
        } else {
            boolean duplicate = lessonService.findLessons(courseId).stream()
                    .anyMatch(row -> nextValue.equalsIgnoreCase(row.sectionName()) && !oldValue.equalsIgnoreCase(row.sectionName()))
                    || draftChapterNames.getOrDefault(courseId, List.of()).stream()
                    .anyMatch(name -> nextValue.equalsIgnoreCase(name) && !oldValue.equalsIgnoreCase(name));
            if (duplicate) {
                redirectAttributes.addFlashAttribute("lessonError", "Tên chương mới đã được sử dụng trong khóa học.");
            } else {
                lessonService.renameSection(courseId, oldValue, nextValue);
                List<String> names = new ArrayList<>(draftChapterNames.getOrDefault(courseId, List.of()));
                names.removeIf(oldValue::equals);
                if (names.stream().noneMatch(nextValue::equalsIgnoreCase) && !lessonService.findLessons(courseId).stream()
                        .anyMatch(row -> nextValue.equals(row.sectionName()))) names.add(nextValue);
                draftChapterNames.put(courseId, names);
                redirectAttributes.addFlashAttribute("lessonSuccess", "Đã cập nhật tên chương.");
            }
        }
        redirectAttributes.addAttribute("courseId", courseId);
        return REDIRECT;
    }

    @PostMapping("/sections/delete")
    public String deleteSection(@RequestParam(name = "courseId") Long courseId,
                                @RequestParam(name = "sectionName") String sectionName,
                                @ModelAttribute("draftChapterNames") Map<Long, List<String>> draftChapterNames,
                                RedirectAttributes redirectAttributes) {
        String name = sectionName == null ? "" : sectionName.trim();
        lessonService.deleteSection(courseId, name);
        List<String> names = new ArrayList<>(draftChapterNames.getOrDefault(courseId, List.of()));
        names.removeIf(name::equals);
        draftChapterNames.put(courseId, names);
        redirectAttributes.addFlashAttribute("lessonSuccess", "Đã xóa chương. Bài học trong chương được giữ lại và chuyển sang “Chưa phân chương”.");
        redirectAttributes.addAttribute("courseId", courseId);
        return REDIRECT;
    }

    @PostMapping
    public String create(@RequestParam MultiValueMap<String, String> params,
                         @RequestParam(name = "questionAudioFile", required = false) List<MultipartFile> questionAudioFiles,
                         RedirectAttributes redirectAttributes) {

        Long courseId = longValue(params, "courseId");
        String requestedSkill = params.getFirst("skillType");
        SkillTypeEnum skill = SkillTypeEnum.fromParam(requestedSkill);
        if (courseId == null) {
            redirectAttributes.addFlashAttribute("lessonError", "Vui lòng chọn khóa học trước khi thêm bài học.");
            return REDIRECT;
        }
        if ("OTHER".equalsIgnoreCase(requestedSkill)) {
            long createdLessonId = 0;
            try {
                String title = params.getFirst("title");
                String sectionName = params.getFirst("sectionName");
                if (sectionName == null || sectionName.isBlank()) {
                    sectionName = lessonService.findLessons(courseId).stream()
                            .map(LessonRow::sectionName)
                            .filter(s -> s != null && !s.isBlank() && !"Chưa phân chương".equalsIgnoreCase(s.trim()))
                            .findFirst().orElse(null);
                }
                LessonInput input = new LessonInput(title, sectionName, null,
                        params.getFirst("content"), null, LessonStatusEnum.ACTIVE.name(), null,
                        integerValue(params, "durationMinutes"), booleanValue(params, "freePreview"));
                createdLessonId = lessonService.createOther(courseId, input);
                redirectAttributes.addFlashAttribute("lessonSuccess", "Đã lưu bài học “" + trimmed(title) + "” vào cột Khác.");
            } catch (IllegalArgumentException exception) {
                deleteQuietly(createdLessonId);
                redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
            } catch (DataAccessException exception) {
                deleteQuietly(createdLessonId);
                redirectAttributes.addFlashAttribute("lessonError", "Không lưu được bài học: " + describe(exception));
            }
            redirectAttributes.addAttribute("courseId", courseId);
            redirectAttributes.addAttribute("skill", "OTHER");
            return REDIRECT;
        }
        if (skill == null) {
            redirectAttributes.addFlashAttribute("lessonError", "Không xác định được kỹ năng của bài học.");
            return redirectToWorkspace(courseId, null, redirectAttributes);
        }

        long createdLessonId = 0;
        try {
            if (skill == SkillTypeEnum.LISTENING) lessonAudioStorageService.applyQuestionAudioFiles(params, questionAudioFiles);
            String title = params.getFirst("title");
            String sectionName = params.getFirst("sectionName");
            if (sectionName == null || sectionName.isBlank()) {
                sectionName = lessonService.findLessons(courseId).stream()
                        .map(LessonRow::sectionName)
                        .filter(s -> s != null && !s.isBlank() && !"Chưa phân chương".equalsIgnoreCase(s.trim()))
                        .findFirst().orElse(null);
            }
            LessonInput input = new LessonInput(
                    title,
                    sectionName,
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
        } catch (java.io.IOException exception) {
            deleteQuietly(createdLessonId);
            redirectAttributes.addFlashAttribute("lessonError", "Không thể lưu file nghe đã chọn. Vui lòng thử lại.");
        }
        return redirectToWorkspace(courseId, skill.getValue(), redirectAttributes);
    }

    @PostMapping("/{lessonId}")
    public String update(@PathVariable(name = "lessonId") long lessonId,
                         @RequestParam MultiValueMap<String, String> params,
                         @RequestParam(name = "questionAudioFile", required = false) List<MultipartFile> questionAudioFiles,
                         RedirectAttributes redirectAttributes) {

        Long courseId = longValue(params, "courseId");
        SkillTypeEnum skill = SkillTypeEnum.fromParam(params.getFirst("skillType"));
        try {
            LessonRow existing = lessonService.findLesson(lessonId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học cần sửa."));
            if (existing.skillValue() == null || "OTHER".equalsIgnoreCase(params.getFirst("skillType"))) {
                String title = params.getFirst("title");
                String rawSectionName = params.getFirst("sectionName");
                String sectionName = (rawSectionName == null || rawSectionName.isBlank())
                        ? existing.sectionName() : rawSectionName;
                LessonInput otherInput = new LessonInput(title, sectionName, existing.description(),
                        params.getFirst("content"), null, existing.status().name(), null,
                        integerValue(params, "durationMinutes"), booleanValue(params, "freePreview"));
                lessonService.updateOther(lessonId, otherInput);
                redirectAttributes.addFlashAttribute("lessonSuccess", "Đã lưu bài học “" + trimmed(title) + "”.");
                if (courseId != null) redirectAttributes.addAttribute("courseId", courseId);
                redirectAttributes.addAttribute("skill", "OTHER");
                return REDIRECT;
            }
            SkillTypeEnum resolvedSkill = skill == null
                    ? SkillTypeEnum.fromParam(existing.skillValue())
                    : skill;
            if (resolvedSkill == null) {
                resolvedSkill = SkillTypeEnum.VOCABULARY;
            }

            String title = params.getFirst("title");
            if (resolvedSkill == SkillTypeEnum.LISTENING) lessonAudioStorageService.applyQuestionAudioFiles(params, questionAudioFiles);
            String updateSectionName = params.getFirst("sectionName");
            if (updateSectionName == null || updateSectionName.isBlank()) {
                updateSectionName = existing.sectionName();
            }
            LessonInput input = new LessonInput(
                    title,
                    updateSectionName,
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
        } catch (java.io.IOException exception) {
            redirectAttributes.addFlashAttribute("lessonError", "Không thể lưu file nghe đã chọn. Vui lòng thử lại.");
        }
        return redirectToWorkspace(courseId, skill == null ? null : skill.getValue(), redirectAttributes);
    }

    @PostMapping("/{lessonId}/delete")
    public String delete(@PathVariable(name = "lessonId") long lessonId,
                         @RequestParam(name = "courseId", required = false) Long courseId,
                         @RequestParam(name = "skill", required = false) String skill,
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
    public String duplicate(@PathVariable(name = "lessonId") long lessonId,
                            @RequestParam(name = "courseId", required = false) Long courseId,
                            @RequestParam(name = "skill", required = false) String skill,
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
    public String changeStatus(@PathVariable(name = "lessonId") long lessonId,
                               @RequestParam(name = "status", required = false) String status,
                               @RequestParam(name = "courseId", required = false) Long courseId,
                               @RequestParam(name = "skill", required = false) String skill,
                               @RequestParam(name = "detailId", required = false) Long detailId,
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
        String destination = redirectToWorkspace(courseId, skill, redirectAttributes);
        if (detailId != null) redirectAttributes.addAttribute("detailId", detailId);
        return destination;
    }

    @PostMapping("/{lessonId}/move")
    public Object move(@PathVariable(name = "lessonId") long lessonId,
                       @RequestParam(name = "direction", defaultValue = "up") String direction,
                       @RequestParam(name = "courseId", required = false) Long courseId,
                       @RequestParam(name = "skill", required = false) String skill,
                       @RequestParam(name = "chapter", required = false) String chapter,
                       @RequestParam(name = "column", required = false) String column,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        boolean isAjax = isAjaxRequest(request);
        try {
            boolean moved = lessonService.moveInColumn(lessonId,
                    "down".equalsIgnoreCase(direction) ? 1 : -1, chapter, column);
            if (!moved) {
                if (isAjax) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Bài học đã ở vị trí đầu hoặc cuối của cột kỹ năng nên không thể di chuyển thêm."));
                }
                redirectAttributes.addFlashAttribute("lessonError",
                        "Bài học đã ở vị trí đầu hoặc cuối của cột kỹ năng nên không thể di chuyển thêm.");
            } else {
                if (isAjax) {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Đã thay đổi thứ tự bài học thành công."));
                }
                redirectAttributes.addFlashAttribute("lessonSuccess", "Đã thay đổi thứ tự bài học.");
            }
        } catch (IllegalArgumentException exception) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", exception.getMessage()));
            }
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            if (isAjax) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "success", false,
                        "message", "Không đổi được thứ tự: " + describe(exception)));
            }
            redirectAttributes.addFlashAttribute("lessonError", "Không đổi được thứ tự: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    @PostMapping("/reorder")
    public Object reorder(@RequestParam(name = "courseId", required = false) Long courseId,
                          @RequestParam(name = "skill", required = false) String skill,
                          @RequestParam(name = "chapter", required = false) String chapter,
                          @RequestParam(name = "column", required = false) String column,
                          @RequestParam(name = "ids", required = false) String ids,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        boolean isAjax = isAjaxRequest(request);
        if (courseId == null || ids == null || ids.isBlank()) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không nhận được thứ tự mới của bài học."));
            }
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
            if (chapter != null && !chapter.isBlank() && column != null && !column.isBlank()) {
                lessonService.reorderInColumn(courseId, chapter, column, orderedIds);
            } else {
                lessonService.saveOrder(courseId, skill, orderedIds);
            }
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đã lưu thứ tự bài học thành công."));
            }
            redirectAttributes.addFlashAttribute("lessonSuccess",
                    "Đã lưu thứ tự bài học trong kỹ năng " + labelOf(skill) + ".");
        } catch (NumberFormatException exception) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thứ tự bài học gửi lên không hợp lệ."));
            }
            redirectAttributes.addFlashAttribute("lessonError", "Thứ tự bài học gửi lên không hợp lệ.");
        } catch (IllegalArgumentException exception) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", exception.getMessage()));
            }
            redirectAttributes.addFlashAttribute("lessonError", exception.getMessage());
        } catch (DataAccessException exception) {
            if (isAjax) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "success", false,
                        "message", "Không lưu được thứ tự: " + describe(exception)));
            }
            redirectAttributes.addFlashAttribute("lessonError", "Không lưu được thứ tự: " + describe(exception));
        }
        return redirectToWorkspace(courseId, skill, redirectAttributes);
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) return false;
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }

    /**
     * Keeps {@code lessons.content} readable for the learner pages even though the structured data
     * now lives in {@code lesson_details}.
     */
    private String buildLessonContent(SkillTypeEnum skill, MultiValueMap<String, String> params) {
        return switch (skill) {
            case VOCABULARY -> buildVocabularyLessonContent(params);
            case GRAMMAR -> buildGrammarLessonContent(params);
            case KANJI -> buildKanjiLessonContent(params);
            case LISTENING -> params.getFirst("transcript");
            case READING -> params.getFirst("passage");
        };
    }

    private String buildGrammarLessonContent(MultiValueMap<String, String> params) {
        String pattern = params.getFirst("pattern");
        if (pattern == null || pattern.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(pattern.trim());
        String meaning = params.getFirst("meaning");
        if (meaning != null && !meaning.isBlank()) {
            sb.append(" : ").append(meaning.trim());
        }
        sb.append("\n\n");
        String formation = params.getFirst("formation");
        if (formation != null && !formation.isBlank()) {
            sb.append("### Cấu trúc kết hợp\n\n```\n").append(formation.trim()).append("\n```\n\n");
        }
        String desc = params.getFirst("description");
        if (desc != null && !desc.isBlank()) {
            sb.append("### Cách dùng & Giải thích\n\n").append(desc.trim()).append("\n\n");
        }
        List<String> jps = params.get("exampleJp");
        List<String> vis = params.get("exampleVi");
        if (jps != null && !jps.isEmpty()) {
            sb.append("### Ví dụ\n\n");
            for (int i = 0; i < jps.size(); i++) {
                String jp = jps.get(i) == null ? "" : jps.get(i).trim();
                if (jp.isEmpty()) continue;
                String vi = vis != null && i < vis.size() && vis.get(i) != null ? vis.get(i).trim() : "";
                sb.append("- **").append(jp).append("**");
                if (!vi.isEmpty()) sb.append(" (").append(vi).append(")");
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String buildVocabularyLessonContent(MultiValueMap<String, String> params) {
        List<String> words = params.get("word");
        if (words == null || words.isEmpty()) return "";
        List<String> readings = params.get("wordReading");
        List<String> meanings = params.get("wordMeaning");
        List<String> poses = params.get("partOfSpeech");
        List<String> hanviets = params.get("hanviet");
        List<String> exJps = params.get("wordExample");
        List<String> exVis = params.get("wordExampleVi");

        StringBuilder sb = new StringBuilder();
        sb.append("| Từ vựng | Cách đọc | Từ loại | Hán Việt | Ý nghĩa | Ví dụ |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i) == null ? "" : words.get(i).trim();
            if (w.isEmpty()) continue;
            String r = readings != null && i < readings.size() && readings.get(i) != null ? readings.get(i).trim() : "-";
            String m = meanings != null && i < meanings.size() && meanings.get(i) != null ? meanings.get(i).trim() : "-";
            String p = poses != null && i < poses.size() && poses.get(i) != null ? poses.get(i).trim() : "-";
            String hv = hanviets != null && i < hanviets.size() && hanviets.get(i) != null ? hanviets.get(i).trim() : "-";
            String ex = exJps != null && i < exJps.size() && exJps.get(i) != null ? exJps.get(i).trim() : "";
            String exv = exVis != null && i < exVis.size() && exVis.get(i) != null ? exVis.get(i).trim() : "";
            String exFull = ex.isEmpty() ? "-" : ex + (!exv.isEmpty() ? " (" + exv + ")" : "");
            sb.append("| **").append(w).append("** | ").append(r).append(" | ").append(p).append(" | ")
                    .append(hv).append(" | ").append(m).append(" | ").append(exFull).append(" |\n");
        }
        return sb.toString();
    }

    private String buildKanjiLessonContent(MultiValueMap<String, String> params) {
        String character = params.getFirst("character");
        if (character == null || character.isBlank()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(character.trim());
        String hanviet = params.getFirst("hanviet");
        if (hanviet != null && !hanviet.isBlank()) {
            sb.append(" (").append(hanviet.trim()).append(")");
        }
        String meaning = params.getFirst("kanjiMeaning");
        if (meaning != null && !meaning.isBlank()) {
            sb.append(" : ").append(meaning.trim());
        }
        sb.append("\n\n");

        sb.append("### Thông tin Hán tự\n\n");
        String onReading = params.getFirst("onReading");
        if (onReading != null && !onReading.isBlank()) {
            sb.append("- **Âm On (Onyomi)**: `").append(onReading.trim()).append("`\n");
        }
        String kunReading = params.getFirst("kunReading");
        if (kunReading != null && !kunReading.isBlank()) {
            sb.append("- **Âm Kun (Kunyomi)**: `").append(kunReading.trim()).append("`\n");
        }
        String strokeCount = params.getFirst("strokeCount");
        if (strokeCount != null && !strokeCount.isBlank()) {
            sb.append("- **Số nét**: ").append(strokeCount.trim()).append(" nét\n");
        }
        String radical = params.getFirst("radical");
        if (radical != null && !radical.isBlank()) {
            sb.append("- **Bộ thủ**: ").append(radical.trim()).append("\n");
        }

        List<String> compoundWords = params.get("compoundWord");
        List<String> compoundReadings = params.get("compoundReading");
        List<String> compoundMeanings = params.get("compoundMeaning");
        if (compoundWords != null && !compoundWords.isEmpty()) {
            boolean hasAny = false;
            for (String cw : compoundWords) {
                if (cw != null && !cw.isBlank()) {
                    hasAny = true;
                    break;
                }
            }
            if (hasAny) {
                sb.append("\n### Từ ghép thường gặp\n\n");
                sb.append("| Từ ghép | Cách đọc | Ý nghĩa |\n");
                sb.append("| :--- | :--- | :--- |\n");
                for (int i = 0; i < compoundWords.size(); i++) {
                    String cw = compoundWords.get(i) == null ? "" : compoundWords.get(i).trim();
                    if (cw.isEmpty()) continue;
                    String cr = compoundReadings != null && i < compoundReadings.size() && compoundReadings.get(i) != null
                            ? compoundReadings.get(i).trim() : "";
                    String cm = compoundMeanings != null && i < compoundMeanings.size() && compoundMeanings.get(i) != null
                            ? compoundMeanings.get(i).trim() : "";
                    sb.append("| **").append(cw).append("** | ").append(cr.isEmpty() ? "-" : "`" + cr + "`")
                            .append(" | ").append(cm.isEmpty() ? "-" : cm).append(" |\n");
                }
            }
        }
        return sb.toString();
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

    /**
     * Hiển thị trang tạo flashcard với thông tin bài học đã được điền trước:
     * - Bài học (lessonId và tên bộ thẻ)
     * - Cấp độ JLPT của khóa học chứa bài học
     * - Danh sách thẻ từ vựng / kanji / ngữ pháp trích xuất từ bài học
     */
    @GetMapping("/flashcard/new")
    public String newFlashcardFromLesson(@RequestParam(name = "lessonId", required = false) Long lessonId, Model model) {
        ManagerFlashcardSetForm form = new ManagerFlashcardSetForm();
        if (lessonId != null) {
            form.setLessonId(lessonId);
            try {
                lessonService.findLesson(lessonId).ifPresent(lesson -> {
                    String title = lesson.title();
                    if (title != null && !title.isBlank()) {
                        form.setDeckName(title.trim());
                    }

                    // Tự động điền cấp độ JLPT từ khóa học chứa bài học
                    String jlptLevel = courseLevelOf(lesson.courseId());
                    if (jlptLevel != null && !jlptLevel.isBlank()) {
                        form.setJlptLevel(jlptLevel.trim().toUpperCase());
                    }

                    // Tự động điền từ vựng / kanji / ngữ pháp của bài học
                    List<ManagerFlashcardSetCardForm> cards = extractCardsFromLesson(lesson);
                    if (!cards.isEmpty()) {
                        form.setCards(cards);
                    }
                });
            } catch (DataAccessException | TransactionException exception) {
                log.warn("Could not pre-fill lesson data for flashcard form (lessonId={})", lessonId, exception);
            }
        }
        model.addAttribute("flashcardSetForm", form);
        try {
            List<Lesson> allLessons = managerFlashcardService.findLessons();
            java.util.LinkedHashMap<Long, String> lessonTitles = new java.util.LinkedHashMap<>();
            List<FlashcardLessonOption> lessonOptions = new ArrayList<>();
            for (Lesson l : allLessons) {
                String t = l.getTitle() != null && !l.getTitle().isBlank() ? l.getTitle().trim()
                         : l.getLessonTitle() != null && !l.getLessonTitle().isBlank() ? l.getLessonTitle().trim()
                         : "Bài học #" + l.getLessonId();
                lessonTitles.put(l.getLessonId(), t);
                lessonOptions.add(new FlashcardLessonOption(l.getLessonId(), t));
            }
            model.addAttribute("lessons", lessonOptions);
            model.addAttribute("lessonTitles", lessonTitles);
            model.addAttribute("lessonsAvailable", true);
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not load lessons for flashcard prefill form", exception);
            model.addAttribute("lessons", List.of());
            model.addAttribute("lessonTitles", Map.of());
            model.addAttribute("lessonsAvailable", false);
            model.addAttribute("lessonsError", "Không tải được danh sách bài học.");
        }
        return "pages/manager/flashcard/manager-flashcard-create";
    }

    /**
     * Hiển thị trang tạo bài luyện tập (quiz) với thông tin bài học đã được chọn sẵn từ lesson workspace.
     */
    @GetMapping({"/quiz/new", "/quizzes/new"})
    public String newQuizFromLesson(@RequestParam(name = "lessonId", required = false) Long lessonId, Model model) {
        ManagerQuizForm form = new ManagerQuizForm();
        if (lessonId != null) {
            form.setLessonId(lessonId);
            try {
                lessonService.findLesson(lessonId).ifPresent(lesson -> {
                    String title = lesson.title();
                    if (title != null && !title.isBlank()) {
                        form.setTitle("Luyện tập: " + title.trim());
                    }
                });
            } catch (DataAccessException | TransactionException exception) {
                log.warn("Could not pre-fill lesson data for quiz form (lessonId={})", lessonId, exception);
            }
            form.setStatus("PUBLISHED");
            form.setTotalQuestions(10);
            form.setPassingScore(80);
        }
        model.addAttribute("quizForm", form);
        try {
            model.addAttribute("lessons", managerQuizService.lessons());
            model.addAttribute("statuses", managerQuizService.statuses());
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not load options for quiz prefill form", exception);
            model.addAttribute("lessons", List.of());
            model.addAttribute("statuses", List.of("DRAFT", "PUBLISHED", "ARCHIVED"));
        }
        model.addAttribute("isEdit", false);
        return "pages/manager/quiz/form";
    }

    @GetMapping("/{lessonId}/flashcard/edit")
    public String editFlashcardForLesson(@PathVariable(name = "lessonId") long lessonId) {
        List<Flashcard> cards = flashcardRepository.findByLessonId(lessonId);
        if (cards != null && !cards.isEmpty()) {
            return "redirect:/manager/flashcards/" + cards.get(0).getFlashcardId() + "/edit";
        }
        return "redirect:/manager/flashcards";
    }

    @GetMapping("/{lessonId}/quiz/edit")
    public String editQuizForLesson(@PathVariable(name = "lessonId") long lessonId) {
        Optional<Exercise> exercise = exerciseRepository.findByLessonId(lessonId);
        if (exercise.isPresent()) {
            return "redirect:/manager/quizzes/" + exercise.get().getExerciseId() + "/edit";
        }
        return "redirect:/manager/quizzes?lessonId=" + lessonId;
    }

    /**
     * Endpoint AJAX fragment: nạp LessonStudyView và render duy nhất modal chi tiết của 1 bài học.
     */
    @GetMapping({"/{lessonId}/detail-modal", "/detail-modal"})
    public String detailModalFragment(@PathVariable(name = "lessonId", required = false) Long lessonId,
                                      @RequestParam(name = "id", required = false) Long id,
                                      @RequestParam(name = "courseId", required = false) Long courseId,
                                      Model model) {
        Long targetId = lessonId != null ? lessonId : id;
        if (targetId == null) {
            model.addAttribute("lessonStudyViews", List.of());
            return "pages/manager/lesson/manager-lessons :: lessonDetailContainer";
        }
        LessonRow lesson = lessonService.findLesson(targetId).orElse(null);
        if (lesson != null) {
            LessonStudyView studyView = buildLessonStudyView(lesson);
            model.addAttribute("lessonStudyViews", List.of(studyView));
            Long actualCourseId = courseId != null ? courseId : lesson.courseId();
            model.addAttribute("selectedCourseId", actualCourseId);
            model.addAttribute("courseLevel", courseLevelOf(actualCourseId));
            model.addAttribute("openDetailId", targetId);
        } else {
            model.addAttribute("lessonStudyViews", List.of());
        }
        return "pages/manager/lesson/manager-lessons :: lessonDetailContainer";
    }

    /**
     * Nạp dữ liệu chi tiết (materials, flashcards, quizzes, listening/reading questions)
     * cho đúng 1 bài học khi được yêu cầu.
     */
    private LessonStudyView buildLessonStudyView(LessonRow lesson) {
        return lessonDetailService.buildLessonStudyView(lesson);
    }

    /** Trích xuất thẻ flashcard từ bài học (từ vựng, kanji, hoặc ngữ pháp). */
    private List<ManagerFlashcardSetCardForm> extractCardsFromLesson(LessonRow lesson) {
        List<ManagerFlashcardSetCardForm> cards = new ArrayList<>();
        SkillTypeEnum skill = SkillTypeEnum.fromParam(lesson.skillValue());
        LessonDetail detail = null;
        try {
            detail = lessonDetailService.findDetail(lesson.id()).orElse(null);
        } catch (Exception exception) {
            log.warn("Could not read lesson detail for flashcard prefill (lessonId={})", lesson.id(), exception);
        }

        Map<String, Object> detailForm = Map.of();
        if (skill != null) {
            try {
                detailForm = lessonDetailService.buildForm(skill, detail != null ? detail.sourceEntryId() : null, lesson, detail);
            } catch (Exception exception) {
                log.warn("Could not build skill form for flashcard prefill (lessonId={})", lesson.id(), exception);
            }
        }

        if (skill == SkillTypeEnum.VOCABULARY) {
            extractVocabularyCards(cards, detailForm, lesson);
        } else if (skill == SkillTypeEnum.KANJI) {
            extractKanjiCards(cards, detailForm, lesson);
        } else if (skill == SkillTypeEnum.GRAMMAR) {
            extractGrammarCards(cards, detailForm, lesson);
        }

        // Nếu kỹ năng khác hoặc chưa trích xuất được thẻ, kiểm tra flashcard đã lưu trong DB của bài học
        if (cards.isEmpty()) {
            try {
                List<Flashcard> existingFlashcards = flashcardRepository.findByLessonId(lesson.id());
                if (existingFlashcards != null && !existingFlashcards.isEmpty()) {
                    for (Flashcard fc : existingFlashcards) {
                        if (fc.getVocabulary() != null && !fc.getVocabulary().isBlank()) {
                            ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
                            card.setVocabulary(fc.getVocabulary().trim());
                            card.setReading(fc.getReading() != null ? fc.getReading().trim() : "");
                            card.setMeaning(fc.getMeaning() != null && !fc.getMeaning().isBlank()
                                    ? fc.getMeaning().trim() : card.getVocabulary());
                            card.setExampleSentence(fc.getExampleSentence() != null ? fc.getExampleSentence().trim() : "");
                            cards.add(card);
                        }
                    }
                }
            } catch (Exception exception) {
                log.warn("Could not read existing flashcards for prefill (lessonId={})", lesson.id(), exception);
            }
        }

        // Fallback: tối thiểu 1 thẻ với tên bài học và mô tả/nội dung
        if (cards.isEmpty()) {
            ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
            String title = lesson.title() != null && !lesson.title().isBlank() ? lesson.title().trim() : "Từ vựng";
            String desc = lesson.description() != null && !lesson.description().isBlank() ? lesson.description().trim()
                    : lesson.content() != null && !lesson.content().isBlank() ? lesson.content().trim() : title;
            card.setVocabulary(title);
            card.setMeaning(desc);
            cards.add(card);
        }

        return cards;
    }

    private void extractVocabularyCards(List<ManagerFlashcardSetCardForm> cards, Map<String, Object> form, LessonRow lesson) {
        Object wordsObj = form.get("words");
        if (wordsObj instanceof List<?> wordsList) {
            for (Object obj : wordsList) {
                if (obj instanceof Map<?, ?> map) {
                    String vocab = stringValue(map.get("word"));
                    String reading = stringValue(map.get("wordReading"));
                    if (reading.isBlank()) reading = stringValue(map.get("reading"));
                    String meaning = stringValue(map.get("wordMeaning"));
                    if (meaning.isBlank()) meaning = stringValue(map.get("meaning"));
                    String example = stringValue(map.get("wordExample"));
                    if (example.isBlank()) example = stringValue(map.get("example_sentence"));
                    if (!vocab.isBlank()) {
                        ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
                        card.setVocabulary(vocab);
                        card.setReading(reading);
                        card.setMeaning(!meaning.isBlank() ? meaning : vocab);
                        card.setExampleSentence(example);
                        cards.add(card);
                    }
                }
            }
        }

        if (cards.isEmpty()) {
            Object wordObj = form.get("word");
            if (wordObj instanceof List<?> wordList) {
                List<?> readingList = form.get("wordReading") instanceof List<?> l ? l : List.of();
                List<?> meaningList = form.get("wordMeaning") instanceof List<?> l ? l : List.of();
                List<?> exampleList = form.get("wordExample") instanceof List<?> l ? l : List.of();
                for (int i = 0; i < wordList.size(); i++) {
                    String w = stringValue(wordList.get(i));
                    if (!w.isBlank()) {
                        String r = i < readingList.size() ? stringValue(readingList.get(i)) : "";
                        String m = i < meaningList.size() ? stringValue(meaningList.get(i)) : "";
                        String ex = i < exampleList.size() ? stringValue(exampleList.get(i)) : "";
                        ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
                        card.setVocabulary(w);
                        card.setReading(r);
                        card.setMeaning(!m.isBlank() ? m : w);
                        card.setExampleSentence(ex);
                        cards.add(card);
                    }
                }
            } else if (wordObj instanceof String w && !w.isBlank()) {
                ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
                card.setVocabulary(w);
                card.setReading(stringValue(form.get("wordReading")));
                String m = stringValue(form.get("wordMeaning"));
                card.setMeaning(!m.isBlank() ? m : w);
                card.setExampleSentence(stringValue(form.get("wordExample")));
                cards.add(card);
            }
        }

        if (cards.isEmpty() && lesson.content() != null && !lesson.content().isBlank()) {
            for (String line : lesson.content().split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String vocab = "", reading = "", meaning = "";
                if (line.contains("（") && line.contains("）") && line.contains(":")) {
                    int p1 = line.indexOf("（");
                    int p2 = line.indexOf("）");
                    int p3 = line.indexOf(":");
                    vocab = line.substring(0, p1).trim();
                    reading = line.substring(p1 + 1, p2).trim();
                    meaning = line.substring(p3 + 1).trim();
                } else if (line.contains("(") && line.contains(")") && line.contains(":")) {
                    int p1 = line.indexOf("(");
                    int p2 = line.indexOf(")");
                    int p3 = line.indexOf(":");
                    vocab = line.substring(0, p1).trim();
                    reading = line.substring(p1 + 1, p2).trim();
                    meaning = line.substring(p3 + 1).trim();
                } else if (line.contains(":")) {
                    int p3 = line.indexOf(":");
                    vocab = line.substring(0, p3).trim();
                    meaning = line.substring(p3 + 1).trim();
                } else if (line.contains("—")) {
                    int p3 = line.indexOf("—");
                    vocab = line.substring(0, p3).trim();
                    meaning = line.substring(p3 + 1).trim();
                } else if (line.contains("-")) {
                    int p3 = line.indexOf("-");
                    vocab = line.substring(0, p3).trim();
                    meaning = line.substring(p3 + 1).trim();
                } else {
                    vocab = line;
                    meaning = lesson.title();
                }
                if (!vocab.isBlank()) {
                    ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
                    card.setVocabulary(vocab);
                    card.setReading(reading);
                    card.setMeaning(!meaning.isBlank() ? meaning : vocab);
                    cards.add(card);
                }
            }
        }
    }

    private void extractKanjiCards(List<ManagerFlashcardSetCardForm> cards, Map<String, Object> form, LessonRow lesson) {
        String character = stringValue(form.get("character"));
        String onReading = stringValue(form.get("onReading"));
        if (onReading.isBlank()) onReading = stringValue(form.get("on_reading"));
        String kunReading = stringValue(form.get("kunReading"));
        if (kunReading.isBlank()) kunReading = stringValue(form.get("kun_reading"));
        String kanjiMeaning = stringValue(form.get("kanjiMeaning"));
        if (kanjiMeaning.isBlank()) kanjiMeaning = stringValue(form.get("meaning"));
        Object strokeCount = form.get("strokeCount");
        if (strokeCount == null) strokeCount = form.get("stroke_count");

        String radical = stringValue(form.get("radical"));

        if (!character.isBlank()) {
            ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
            card.setVocabulary(character);
            List<String> readings = new ArrayList<>();
            if (!onReading.isBlank()) readings.add("Âm On: " + onReading);
            if (!kunReading.isBlank()) readings.add("Âm Kun: " + kunReading);
            card.setReading(String.join(" | ", readings));
            card.setMeaning(!kanjiMeaning.isBlank() ? kanjiMeaning : lesson.title());
            StringBuilder meta = new StringBuilder();
            if (strokeCount != null && !strokeCount.toString().isBlank()) {
                meta.append("Số nét: ").append(strokeCount);
            }
            if (!radical.isBlank()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append("Bộ: ").append(radical);
            }
            if (meta.length() > 0) {
                card.setExampleSentence(meta.toString());
            }
            cards.add(card);
        }

        Object compoundsObj = form.get("compounds");
        if (compoundsObj instanceof List<?> compList) {
            for (Object obj : compList) {
                if (obj instanceof Map<?, ?> compMap) {
                    String cWord = stringValue(compMap.get("compoundWord"));
                    if (cWord.isBlank()) cWord = stringValue(compMap.get("word"));
                    String cReading = stringValue(compMap.get("compoundReading"));
                    if (cReading.isBlank()) cReading = stringValue(compMap.get("reading"));
                    String cMeaning = stringValue(compMap.get("compoundMeaning"));
                    if (cMeaning.isBlank()) cMeaning = stringValue(compMap.get("meaning"));
                    if (!cWord.isBlank()) {
                        ManagerFlashcardSetCardForm compCard = new ManagerFlashcardSetCardForm();
                        compCard.setVocabulary(cWord);
                        compCard.setReading(cReading);
                        compCard.setMeaning(!cMeaning.isBlank() ? cMeaning : cWord);
                        cards.add(compCard);
                    }
                }
            }
        }

        if (cards.isEmpty() && lesson.content() != null && !lesson.content().isBlank()) {
            String content = lesson.content().trim();
            String vocab = content, meaning = "";
            if (content.contains("—")) {
                String[] parts = content.split("—", 2);
                vocab = parts[0].trim();
                meaning = parts[1].trim();
            } else if (content.contains("-")) {
                String[] parts = content.split("-", 2);
                vocab = parts[0].trim();
                meaning = parts[1].trim();
            }
            if (!vocab.isBlank()) {
                ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
                card.setVocabulary(vocab);
                card.setMeaning(!meaning.isBlank() ? meaning : lesson.title());
                cards.add(card);
            }
        }
    }

    private void extractGrammarCards(List<ManagerFlashcardSetCardForm> cards, Map<String, Object> form, LessonRow lesson) {
        String pattern = stringValue(form.get("pattern"));
        String grammarMeaning = stringValue(form.get("meaning"));
        String usageNote = stringValue(form.get("description"));
        if (usageNote.isBlank()) usageNote = stringValue(form.get("usage_note"));

        StringBuilder exampleText = new StringBuilder();
        Object examplesObj = form.get("examples");
        if (examplesObj instanceof List<?> exList) {
            for (Object obj : exList) {
                if (obj instanceof Map<?, ?> exMap) {
                    String jp = stringValue(exMap.get("exampleJp"));
                    if (jp.isBlank()) jp = stringValue(exMap.get("example_jp"));
                    String vi = stringValue(exMap.get("exampleVi"));
                    if (vi.isBlank()) vi = stringValue(exMap.get("example_vi"));
                    if (!jp.isBlank()) {
                        if (exampleText.length() > 0) exampleText.append("\n");
                        exampleText.append(jp);
                        if (!vi.isBlank()) exampleText.append(" (").append(vi).append(")");
                    }
                }
            }
        }

        if (!pattern.isBlank() || (lesson.title() != null && !lesson.title().isBlank())) {
            ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
            card.setVocabulary(!pattern.isBlank() ? pattern : lesson.title());
            card.setReading(usageNote);
            card.setMeaning(!grammarMeaning.isBlank() ? grammarMeaning
                    : (lesson.description() != null && !lesson.description().isBlank() ? lesson.description() : card.getVocabulary()));
            if (exampleText.length() > 0) {
                card.setExampleSentence(exampleText.toString());
            }
            cards.add(card);
        }

        if (cards.isEmpty() && lesson.content() != null && !lesson.content().isBlank()) {
            String content = lesson.content().trim();
            String vocab = content, meaning = "";
            if (content.contains("—")) {
                String[] parts = content.split("—", 2);
                vocab = parts[0].trim();
                meaning = parts[1].trim();
            } else if (content.contains("-")) {
                String[] parts = content.split("-", 2);
                vocab = parts[0].trim();
                meaning = parts[1].trim();
            }
            ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
            card.setVocabulary(!vocab.isBlank() ? vocab : lesson.title());
            card.setMeaning(!meaning.isBlank() ? meaning : lesson.title());
            cards.add(card);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Projection dùng để hiển thị dropdown lesson trong trang tạo flashcard từ lesson. */
    private record FlashcardLessonOption(Long id, String title) {}
}
