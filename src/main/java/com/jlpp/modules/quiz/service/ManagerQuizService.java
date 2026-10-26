package com.jlpp.modules.quiz.service;

import com.jlpp.common.constants.ExerciseStatusEnum;
import com.jlpp.modules.quiz.dto.LessonOption;
import com.jlpp.modules.quiz.dto.ManagerQuizForm;
import com.jlpp.modules.quiz.dto.QuizOption;
import com.jlpp.modules.quiz.repository.ManagerQuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class ManagerQuizService {

    private final ManagerQuizRepository repository;

    public ManagerQuizService(ManagerQuizRepository repository) { this.repository = repository; }

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
        repository.insert(form.getLessonId(), clean(form.getTitle()), form.getTotalQuestions(), form.getPassingScore(), form.getStatus());
    }

    @Transactional
    public void update(long id, ManagerQuizForm form) {
        validate(form);
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

    private String clean(String value) { return value == null ? "" : value.trim(); }
}
