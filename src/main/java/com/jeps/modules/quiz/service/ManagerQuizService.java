package com.jeps.modules.quiz.service;

import com.jeps.common.constants.*;
import com.jeps.modules.question.dto.QuestionImportException;
import com.jeps.modules.question.entity.Question;
import com.jeps.modules.question.service.QuestionImportService;
import com.jeps.modules.quiz.dto.*;
import com.jeps.modules.quiz.repository.ManagerQuizRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class ManagerQuizService {

    private final ManagerQuizRepository repository;
    private final QuestionImportService questionImportService;
    private final Random random = new Random();

    public ManagerQuizService(ManagerQuizRepository repository, QuestionImportService questionImportService) {
        this.repository = repository;
        this.questionImportService = questionImportService;
    }

    @Transactional(readOnly = true)
    public List<QuizOption> search(String search, String level, String section, String status) {
        return repository.search(norm(search), norm(level), norm(section), norm(status));
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> levelSections() { return repository.findLevelSections(); }

    @Transactional(readOnly = true)
    public Map<String, List<LessonOption>> lessonGroups() {
        Map<String, List<LessonOption>> groups = new LinkedHashMap<>();
        for (LessonOption lesson : repository.findLessons()) {
            groups.computeIfAbsent(lesson.group(), k -> new ArrayList<>()).add(lesson);
        }
        return groups;
    }

    @Transactional(readOnly = true)
    public List<String> statuses() { return Arrays.stream(ExerciseStatusEnum.values()).map(Enum::name).toList(); }

    @Transactional(readOnly = true)
    public long count() { return repository.count(); }

    @Transactional(readOnly = true)
    public QuizOption findById(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quiz."));
    }

    @Transactional
    public void create(ManagerQuizForm form) {
        validate(null, form);
        validatePublishReady(null, form);
        repository.insert(form.getLessonId(), clean(form.getTitle()), form.getPassingScore(), form.getStatus());
    }

    @Transactional
    public void update(long id, ManagerQuizForm form) {
        validate(id, form);
        validatePublishReady(id, form);
        if (repository.update(id, form.getLessonId(), clean(form.getTitle()), form.getPassingScore(), form.getStatus()) == 0) {
            throw new IllegalArgumentException("Không tìm thấy quiz cần sửa.");
        }
    }

    public ManagerQuizForm toForm(QuizOption quiz) {
        ManagerQuizForm form = new ManagerQuizForm();
        form.setId(quiz.id());
        form.setLessonId(quiz.lessonId());
        form.setTitle(quiz.title());
        form.setPassingScore(quiz.passingScore());
        form.setStatus(quiz.status());
        return form;
    }

    private void validate(Long quizId, ManagerQuizForm form) {
        if (form.getLessonId() == null || !repository.lessonExists(form.getLessonId())) {
            throw new IllegalArgumentException("Bài học được chọn không tồn tại.");
        }
        if (form.getStatus() == null || Arrays.stream(ExerciseStatusEnum.values()).noneMatch(value -> value.name().equals(form.getStatus()))) {
            throw new IllegalArgumentException("Trạng thái quiz không hợp lệ.");
        }
        if (repository.lessonHasOtherQuiz(form.getLessonId(), quizId)) {
            throw new IllegalArgumentException("Bài học này đã có quiz. Mỗi bài học chỉ được gắn một quiz.");
        }
    }

    private void validatePublishReady(Long quizId, ManagerQuizForm form) {
        if (!"PUBLISHED".equals(form.getStatus())) return;
        if (quizId == null) {
            throw new IllegalArgumentException("Không thể tạo quiz với trạng thái PUBLISHED. Hãy tạo ở trạng thái DRAFT trước, gắn câu hỏi, rồi chuyển trạng thái.");
        }
        if (repository.countExerciseQuestions(quizId) == 0) {
            throw new IllegalArgumentException("Không thể chuyển sang PUBLISHED: quiz chưa có câu hỏi nào. Hãy gắn câu hỏi vào quiz trước.");
        }
        List<Long> inactive = repository.findInactiveQuestionIds(quizId);
        if (!inactive.isEmpty()) {
            throw new IllegalArgumentException("Không thể chuyển sang PUBLISHED: quiz còn câu hỏi không ở trạng thái ACTIVE ("
                    + String.join(", ", inactive.stream().map(id -> "#" + id).toList())
                    + "). Hãy gỡ các câu này khỏi quiz hoặc kích hoạt lại trước.");
        }
    }

    // ── Quiz-Question Management ──

    @Transactional(readOnly = true)
    public List<ExerciseQuestionRow> getQuizQuestions(long quizId) {
        findById(quizId);
        return repository.findExerciseQuestions(quizId);
    }

    @Transactional(readOnly = true)
    public long countQuizQuestions(long quizId) {
        return repository.countExerciseQuestions(quizId);
    }

    public static final int PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public Page<ExerciseQuestionRow> findAvailableQuestions(long quizId, String jlptLevel, String skillType, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        String level = norm(jlptLevel);
        String skill = norm(skillType);
        return new PageImpl<>(repository.findAvailableQuestions(quizId, level, skill, PAGE_SIZE, pageable.getOffset()),
                pageable, repository.countAvailableQuestions(quizId, level, skill));
    }

    @Transactional
    public void addQuestionsToQuiz(long quizId, ExerciseQuestionForm form) {
        findById(quizId);
        if (form.getQuestionIds() == null || form.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một câu hỏi.");
        }
        for (Long questionId : form.getQuestionIds()) {
            if (!repository.questionExists(questionId)) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " không tồn tại.");
            }
            if (!repository.questionIsActive(questionId)) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " chưa được kích hoạt (ACTIVE).");
            }
            if (repository.exerciseQuestionExists(quizId, questionId)) {
                continue;
            }
            repository.insertExerciseQuestion(quizId, questionId);
        }
        repository.syncTotalQuestions(quizId);
    }

    /** Lấy ngẫu nhiên câu ACTIVE trong ngân hàng theo cấp độ và số câu từng kỹ năng do manager chọn. */
    @Transactional
    public int generateQuestions(long quizId, QuizGenerateForm form) {
        findById(quizId);
        String level = form.getJlptLevel();
        if (level == null || Arrays.stream(JlptLevelEnum.values()).noneMatch(v -> v.name().equals(level))) {
            throw new IllegalArgumentException("Vui lòng chọn cấp độ JLPT.");
        }
        List<String> errors = new ArrayList<>();
        List<Long> picked = new ArrayList<>();
        for (SkillTypeEnum skill : SkillTypeEnum.questionSkills()) {
            Integer requested = form.getCounts() == null ? null : form.getCounts().get(skill.name());
            if (requested == null || requested == 0) continue;
            if (requested < 0) {
                errors.add("Số câu " + skill.getLabel() + " không được âm.");
                continue;
            }
            List<Long> pool = new ArrayList<>(repository.findGeneratableQuestionIds(quizId, level, skill.name()));
            if (pool.size() < requested) {
                errors.add(skill.getLabel() + " cần " + requested + " câu cấp " + level
                        + " nhưng ngân hàng chỉ còn " + pool.size() + " câu ACTIVE phù hợp.");
                continue;
            }
            Collections.shuffle(pool, random);
            picked.addAll(pool.subList(0, requested));
        }
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(" ", errors));
        if (picked.isEmpty()) throw new IllegalArgumentException("Hãy nhập số câu cho ít nhất một kỹ năng.");

        ExerciseQuestionForm addForm = new ExerciseQuestionForm();
        addForm.setQuestionIds(picked);
        addQuestionsToQuiz(quizId, addForm);
        return picked.size();
    }

    @Transactional
    public int importQuestions(long quizId, MultipartFile file) {
        findById(quizId);
        List<Long> ids = questionImportService.importFile(file, "ACTIVE").stream()
                .map(QuestionImportService.ImportedRow::question)
                .filter(q -> !"PASSAGE".equals(q.getQuestionType()) && q.getParentId() == null)
                .map(Question::getId)
                .toList();
        if (ids.isEmpty()) {
            throw new QuestionImportException(List.of("File không có câu hỏi trắc nghiệm nào để gắn vào quiz."));
        }
        ExerciseQuestionForm form = new ExerciseQuestionForm();
        form.setQuestionIds(ids);
        addQuestionsToQuiz(quizId, form);
        return ids.size();
    }

    public byte[] importTemplate() {
        return questionImportService.buildTemplate(false);
    }

    @Transactional
    public void removeQuestionFromQuiz(long quizId, long questionId) {
        QuizOption quiz = findById(quizId);
        if ("PUBLISHED".equals(quiz.status()) && repository.countExerciseQuestions(quizId) <= 1) {
            throw new IllegalArgumentException("Không thể gỡ câu hỏi cuối cùng của quiz đang PUBLISHED. Hãy chuyển quiz về DRAFT trước.");
        }
        if (repository.deleteExerciseQuestion(quizId, questionId) == 0) {
            throw new IllegalArgumentException("Câu hỏi này không nằm trong quiz.");
        }
        repository.syncTotalQuestions(quizId);
    }

    public QuizQuestionOptions questionFilterOptions() {
        return new QuizQuestionOptions(
                Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList(),
                SkillTypeEnum.questionSkillNames());
    }

    public record QuizQuestionOptions(List<String> jlptLevels, List<String> skillTypes) {}

    private String norm(String v) { return v == null ? "" : v.trim(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
