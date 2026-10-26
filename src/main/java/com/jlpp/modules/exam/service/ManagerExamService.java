package com.jlpp.modules.exam.service;

import com.jlpp.common.constants.ExamStatusEnum;
import com.jlpp.common.constants.ExamTypeEnum;
import com.jlpp.common.constants.JlptLevelEnum;
import com.jlpp.modules.exam.dto.ManagerExamForm;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class ManagerExamService {

    private static final List<String> JLPT_LEVELS = Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList();

    private final ExamRepository examRepository;

    public ManagerExamService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional(readOnly = true)
    public List<Exam> search(String search, String examType, String jlptLevel, String status) {
        return examRepository.searchForManager(normalize(search), normalize(examType), normalize(jlptLevel), normalize(status));
    }

    @Transactional(readOnly = true)
    public Exam findById(long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề thi."));
    }

    @Transactional
    public Exam create(ManagerExamForm form) {
        validateDomainValues(form);
        Exam exam = new Exam(null, clean(form.getExamTitle()), form.getExamType(), form.getJlptLevel(),
                form.getDurationMinutes(), form.getTotalQuestions(), form.getPassingScore(), form.getStatus(),
                cleanOptional(form.getDescription()));
        return examRepository.save(exam);
    }

    @Transactional
    public Exam update(long examId, ManagerExamForm form) {
        validateDomainValues(form);
        Exam exam = findById(examId);
        exam.setExamTitle(clean(form.getExamTitle()));
        exam.setExamType(form.getExamType());
        exam.setJlptLevel(form.getJlptLevel());
        exam.setDurationMinutes(form.getDurationMinutes());
        exam.setTotalQuestions(form.getTotalQuestions());
        exam.setPassingScore(form.getPassingScore());
        exam.setStatus(form.getStatus());
        exam.setDescription(cleanOptional(form.getDescription()));
        return examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    public ManagerExamOptions options() {
        return new ManagerExamOptions(Arrays.stream(ExamTypeEnum.values()).map(Enum::name).toList(),
                JLPT_LEVELS, Arrays.stream(ExamStatusEnum.values()).map(Enum::name).toList());
    }

    @Transactional(readOnly = true)
    public long countByType(String examType) {
        return examRepository.countByExamTypeIgnoreCase(examType);
    }

    public ManagerExamForm toForm(Exam exam) {
        ManagerExamForm form = new ManagerExamForm();
        form.setExamId(exam.getExamId());
        form.setExamTitle(exam.getExamTitle());
        form.setExamType(exam.getExamType());
        form.setJlptLevel(exam.getJlptLevel());
        form.setDurationMinutes(exam.getDurationMinutes());
        form.setTotalQuestions(exam.getTotalQuestions());
        form.setPassingScore(exam.getPassingScore());
        form.setStatus(exam.getStatus());
        form.setDescription(exam.getDescription());
        return form;
    }

    private void validateDomainValues(ManagerExamForm form) {
        if (form.getExamType() == null || !Arrays.stream(ExamTypeEnum.values()).map(Enum::name).anyMatch(form.getExamType()::equals)) {
            throw new IllegalArgumentException("Loại đề thi không hợp lệ.");
        }
        if (form.getJlptLevel() == null || !JLPT_LEVELS.contains(form.getJlptLevel())) {
            throw new IllegalArgumentException("Cấp độ JLPT không hợp lệ.");
        }
        if (form.getStatus() == null || !Arrays.stream(ExamStatusEnum.values()).map(Enum::name).anyMatch(form.getStatus()::equals)) {
            throw new IllegalArgumentException("Trạng thái đề thi không hợp lệ.");
        }
    }

    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String cleanOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record ManagerExamOptions(List<String> examTypes, List<String> jlptLevels, List<String> statuses) { }
}
