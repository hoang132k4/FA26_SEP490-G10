package com.jlpp.modules.question.service;

import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagerQuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;

    public ManagerQuestionService(QuestionRepository questionRepository, ExamRepository examRepository) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
    }

    @Transactional(readOnly = true)
    public List<Question> search(String search, String category, Long examId) {
        return questionRepository.searchForManager(normalize(search), normalize(category), examId);
    }

    @Transactional(readOnly = true)
    public Question findById(long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi."));
    }

    @Transactional(readOnly = true)
    public List<String> categories() { return questionRepository.findDistinctCategories(); }

    @Transactional(readOnly = true)
    public List<Exam> exams() { return examRepository.findAll(); }

    @Transactional(readOnly = true)
    public long count() { return questionRepository.count(); }

    @Transactional
    public Question create(ManagerQuestionForm form) {
        validateExam(form.getExamId());
        return questionRepository.save(toEntity(form));
    }

    @Transactional
    public Question update(long id, ManagerQuestionForm form) {
        validateExam(form.getExamId());
        Question question = findById(id);
        question.setContent(clean(form.getContent()));
        question.setCategory(cleanOptional(form.getCategory()));
        question.setAudioUrl(cleanOptional(form.getAudioUrl()));
        question.setExplanation(cleanOptional(form.getExplanation()));
        question.setOrderIndex(form.getOrderIndex());
        question.setPoints(form.getPoints());
        question.setExamId(form.getExamId());
        return questionRepository.save(question);
    }

    public ManagerQuestionForm toForm(Question question) {
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setId(question.getId());
        form.setContent(question.getContent());
        form.setCategory(question.getCategory());
        form.setAudioUrl(question.getAudioUrl());
        form.setExplanation(question.getExplanation());
        form.setOrderIndex(question.getOrderIndex());
        form.setPoints(question.getPoints());
        form.setExamId(question.getExamId());
        return form;
    }

    private Question toEntity(ManagerQuestionForm form) {
        return new Question(clean(form.getContent()), cleanOptional(form.getCategory()), cleanOptional(form.getAudioUrl()),
                cleanOptional(form.getExplanation()), form.getOrderIndex(), form.getPoints(), form.getExamId());
    }

    private void validateExam(Long examId) {
        if (examId != null && !examRepository.existsById(examId)) {
            throw new IllegalArgumentException("Đề thi được chọn không tồn tại.");
        }
    }

    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String cleanOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
