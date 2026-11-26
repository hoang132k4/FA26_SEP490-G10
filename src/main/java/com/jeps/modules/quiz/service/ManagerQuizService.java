package com.jeps.modules.quiz.service;

import com.jeps.common.constants.*;
import com.jeps.modules.quiz.dto.*;
import com.jeps.modules.quiz.repository.ManagerQuizRepository;
import com.jeps.modules.quiz.repository.ManagerQuizRepository.QuestionCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class ManagerQuizService {

    private final ManagerQuizRepository repository;
    private final Random random = new Random();

    public ManagerQuizService(ManagerQuizRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<QuizOption> search(String search, String level, String section, String status, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        String text = norm(search), lvl = norm(level), sec = norm(section), st = norm(status);
        return new PageImpl<>(repository.search(text, lvl, sec, st, PAGE_SIZE, pageable.getOffset()),
                pageable, repository.countSearch(text, lvl, sec, st));
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
        form.setLessonId(quiz.lessonId() > 0 ? quiz.lessonId() : null);
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
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(form.getQuestionIds()));
        Map<Long, QuestionCheck> checks = repository.findQuestionChecks(ids);
        for (Long questionId : ids) {
            QuestionCheck check = checks.get(questionId);
            if (check == null) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " không tồn tại.");
            }
            if (!"ACTIVE".equals(check.status())) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " chưa được kích hoạt (ACTIVE).");
            }
            if ("PASSAGE".equals(check.questionType())) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " là bài đọc/nghe (PASSAGE), quiz chỉ gắn được câu trắc nghiệm.");
            }
        }
        Set<Long> linked = repository.findLinkedQuestionIds(quizId, ids);
        List<Long> toInsert = ids.stream().filter(id -> !linked.contains(id)).toList();
        if (!toInsert.isEmpty()) {
            repository.insertExerciseQuestions(quizId, toInsert);
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

    @Transactional
    public int bulkUpdateStatus(List<Long> ids, String newStatus) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một quiz.");
        }
        if (Arrays.stream(ExerciseStatusEnum.values()).noneMatch(v -> v.name().equals(newStatus))) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        int updated = 0;
        for (Long id : ids) {
            QuizOption quiz = findById(id);
            if (quiz.status().equals(newStatus)) continue;
            if ("PUBLISHED".equals(newStatus)) {
                if (repository.countExerciseQuestions(id) == 0) {
                    throw new IllegalArgumentException("Quiz #" + id + " chưa có câu hỏi. Không thể chuyển sang PUBLISHED.");
                }
                List<Long> inactive = repository.findInactiveQuestionIds(id);
                if (!inactive.isEmpty()) {
                    throw new IllegalArgumentException("Quiz #" + id + " còn câu hỏi không ACTIVE. Không thể chuyển sang PUBLISHED.");
                }
            }
            repository.updateStatus(id, newStatus);
            updated++;
        }
        return updated;
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
