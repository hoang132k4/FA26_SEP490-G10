package com.jlpp.modules.quiz.service;

import com.jlpp.common.constants.*;
import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.service.QuestionImportService;
import com.jlpp.modules.quiz.dto.*;
import com.jlpp.modules.quiz.repository.ManagerQuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class ManagerQuizService {

    private final ManagerQuizRepository repository;
    private final QuestionImportService questionImportService;

    public ManagerQuizService(ManagerQuizRepository repository, QuestionImportService questionImportService) {
        this.repository = repository;
        this.questionImportService = questionImportService;
    }

    @Transactional(readOnly = true)
    public List<QuizOption> search(String search, Long lessonId, String status) {
        return repository.search(search == null ? "" : search.trim(), lessonId, status == null ? "" : status.trim());
    }

    @Transactional(readOnly = true)
    public List<LessonOption> lessons() { return repository.findLessons(); }

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
        validate(form);
        validatePublishReady(null, form);
        repository.insert(form.getLessonId(), clean(form.getTitle()), form.getTotalQuestions(), form.getPassingScore(), form.getStatus());
    }

    @Transactional
    public void update(long id, ManagerQuizForm form) {
        validate(form);
        validatePublishReady(id, form);
        if (repository.update(id, form.getLessonId(), clean(form.getTitle()), form.getTotalQuestions(), form.getPassingScore(), form.getStatus()) == 0) {
            throw new IllegalArgumentException("Không tìm thấy quiz cần sửa.");
        }
    }

    public ManagerQuizForm toForm(QuizOption quiz) {
        ManagerQuizForm form = new ManagerQuizForm();
        form.setId(quiz.id());
        form.setLessonId(quiz.lessonId());
        form.setTitle(quiz.title());
        form.setTotalQuestions(quiz.totalQuestions());
        form.setPassingScore(quiz.passingScore());
        form.setStatus(quiz.status());
        return form;
    }

    private void validate(ManagerQuizForm form) {
        if (form.getLessonId() == null || !repository.lessonExists(form.getLessonId())) {
            throw new IllegalArgumentException("Bài học được chọn không tồn tại.");
        }
        if (form.getStatus() == null || Arrays.stream(ExerciseStatusEnum.values()).noneMatch(value -> value.name().equals(form.getStatus()))) {
            throw new IllegalArgumentException("Trạng thái quiz không hợp lệ.");
        }
    }

    private void validatePublishReady(Long quizId, ManagerQuizForm form) {
        if (!"PUBLISHED".equals(form.getStatus())) return;
        if (quizId == null) {
            throw new IllegalArgumentException("Không thể tạo quiz với trạng thái PUBLISHED. Hãy tạo ở trạng thái DRAFT trước, gắn câu hỏi, rồi chuyển trạng thái.");
        }
        long linked = repository.countExerciseQuestions(quizId);
        int required = form.getTotalQuestions();
        if (linked < required) {
            throw new IllegalArgumentException("Không thể chuyển sang PUBLISHED: quiz cần " + required + " câu hỏi nhưng mới gắn " + linked + " câu. Hãy thêm câu hỏi vào quiz trước.");
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

    @Transactional(readOnly = true)
    public List<ExerciseQuestionRow> findAvailableQuestions(long quizId, String jlptLevel, String skillType) {
        return repository.findAvailableQuestions(quizId, norm(jlptLevel), norm(skillType));
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
    }

    @Transactional
    public int importQuestions(long quizId, MultipartFile file) {
        findById(quizId);
        List<Long> ids = questionImportService.importFile(file, "ACTIVE").stream()
                .map(QuestionImportService.ImportedRow::question)
                .filter(q -> !"PASSAGE".equals(q.getQuestionType()))
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
        if (repository.deleteExerciseQuestion(quizId, questionId) == 0) {
            throw new IllegalArgumentException("Câu hỏi này không nằm trong quiz.");
        }
    }

    public QuizQuestionOptions questionFilterOptions() {
        return new QuizQuestionOptions(
                Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList(),
                Arrays.stream(SkillTypeEnum.values()).map(Enum::name).toList());
    }

    public record QuizQuestionOptions(List<String> jlptLevels, List<String> skillTypes) {}

    private String norm(String v) { return v == null ? "" : v.trim(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
