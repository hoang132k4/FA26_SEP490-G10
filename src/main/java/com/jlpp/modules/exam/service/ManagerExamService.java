package com.jlpp.modules.exam.service;

import com.jlpp.common.constants.*;
import com.jlpp.modules.exam.dto.ExamQuestionForm;
import com.jlpp.modules.exam.dto.ExamQuestionRow;
import com.jlpp.modules.exam.dto.ManagerExamForm;
import com.jlpp.modules.exam.entity.Exam;
import com.jlpp.modules.exam.entity.ExamQuestion;
import com.jlpp.modules.exam.entity.ExamQuestionId;
import com.jlpp.modules.exam.repository.ExamQuestionRepository;
import com.jlpp.modules.exam.repository.ExamRepository;
import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.jlpp.modules.question.service.QuestionImportService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class ManagerExamService {

    private final EntityManager entityManager;

    private static final List<String> JLPT_LEVELS = Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList();

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionImportService questionImportService;

    public ManagerExamService(ExamRepository examRepository,
                              ExamQuestionRepository examQuestionRepository,
                              QuestionRepository questionRepository,
                              EntityManager entityManager,
                              QuestionImportService questionImportService) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
        this.entityManager = entityManager;
        this.questionImportService = questionImportService;
    }

    // ── Exam CRUD ──

    @Transactional(readOnly = true)
    public List<Exam> search(String search, String examType, String jlptLevel, String status) {
        return examRepository.searchForManager(norm(search), norm(examType), norm(jlptLevel), norm(status));
    }

    @Transactional(readOnly = true)
    public Exam findById(long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề thi."));
    }

    @Transactional
    public Exam create(ManagerExamForm form) {
        validateDomainValues(form);
        validatePublishReady(null, form);
        Exam exam = new Exam(null, clean(form.getExamTitle()), form.getExamType(), form.getJlptLevel(),
                form.getDurationMinutes(), form.getTotalQuestions(), form.getPassingScore(), form.getStatus(),
                optional(form.getDescription()));
        return examRepository.save(exam);
    }

    @Transactional
    public Exam update(long examId, ManagerExamForm form) {
        validateDomainValues(form);
        validatePublishReady(examId, form);
        Exam exam = findById(examId);
        exam.setExamTitle(clean(form.getExamTitle()));
        exam.setExamType(form.getExamType());
        exam.setJlptLevel(form.getJlptLevel());
        exam.setDurationMinutes(form.getDurationMinutes());
        exam.setTotalQuestions(form.getTotalQuestions());
        exam.setPassingScore(form.getPassingScore());
        exam.setStatus(form.getStatus());
        exam.setDescription(optional(form.getDescription()));
        return examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    public ManagerExamOptions options() {
        return new ManagerExamOptions(
                Arrays.stream(ExamTypeEnum.values()).map(Enum::name).toList(),
                JLPT_LEVELS,
                Arrays.stream(ExamStatusEnum.values()).map(Enum::name).toList());
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

    // ── Exam-Question Management ──

    @Transactional(readOnly = true)
    public List<ExamQuestionRow> getExamQuestions(long examId) {
        return examQuestionRepository.findByExamIdWithQuestion(examId).stream()
                .map(eq -> {
                    Question q = eq.getQuestion();
                    return new ExamQuestionRow(
                            q.getId(), truncate(q.getContent(), 80), q.getQuestionType(),
                            q.getJlptLevel(), q.getSkillType(), q.getDifficulty(),
                            eq.getSection(), eq.getOrderIndex(), eq.getPoints());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long countExamQuestions(long examId) {
        return examQuestionRepository.countByExamId(examId);
    }

    @Transactional(readOnly = true)
    public List<Question> findAvailableQuestions(long examId, String jlptLevel, String skillType) {
        return questionRepository.findAvailableForExam(examId, norm(jlptLevel), norm(skillType));
    }

    @Transactional
    public void addQuestionsToExam(long examId, ExamQuestionForm form) {
        Exam exam = findById(examId);
        String section = form.getSection();
        if (Arrays.stream(ExamSectionEnum.values()).noneMatch(v -> v.name().equals(section))) {
            throw new IllegalArgumentException("Section không hợp lệ.");
        }
        int currentMax = examQuestionRepository.maxOrderIndex(examId, section);
        int order = currentMax;
        for (Long questionId : form.getQuestionIds()) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Câu hỏi #" + questionId + " không tồn tại."));
            if (!"ACTIVE".equals(question.getStatus())) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " chưa được kích hoạt (ACTIVE).");
            }
            ExamQuestionId eqId = new ExamQuestionId(examId, questionId);
            if (examQuestionRepository.existsById(eqId)) {
                continue;
            }
            order++;
            ExamQuestion eq = ExamQuestion.builder()
                    .id(eqId)
                    .exam(exam)
                    .question(question)
                    .section(section)
                    .orderIndex(order)
                    .points(form.getPoints() != null ? form.getPoints() : 1)
                    .build();
            examQuestionRepository.save(eq);
        }
    }

    @Transactional
    public void moveQuestion(long examId, long questionId, String direction) {
        ExamQuestionId eqId = new ExamQuestionId(examId, questionId);
        ExamQuestion target = examQuestionRepository.findById(eqId)
                .orElseThrow(() -> new IllegalArgumentException("Câu hỏi không nằm trong đề thi."));
        List<ExamQuestion> sectionQuestions = examQuestionRepository.findByExamIdAndSectionOrdered(examId, target.getSection());
        int idx = -1;
        for (int i = 0; i < sectionQuestions.size(); i++) {
            if (sectionQuestions.get(i).getId().getQuestionId().equals(questionId)) {
                idx = i;
                break;
            }
        }
        int swapIdx = "up".equals(direction) ? idx - 1 : idx + 1;
        if (swapIdx < 0 || swapIdx >= sectionQuestions.size()) return;
        ExamQuestion other = sectionQuestions.get(swapIdx);
        long otherQid = other.getId().getQuestionId();
        int orderA = target.getOrderIndex();
        int orderB = other.getOrderIndex();
        String sql = "UPDATE exam_questions SET order_index = ? WHERE exam_id = ? AND question_id = ?";
        entityManager.createNativeQuery(sql).setParameter(1, -1).setParameter(2, examId).setParameter(3, questionId).executeUpdate();
        entityManager.createNativeQuery(sql).setParameter(1, orderA).setParameter(2, examId).setParameter(3, otherQid).executeUpdate();
        entityManager.createNativeQuery(sql).setParameter(1, orderB).setParameter(2, examId).setParameter(3, questionId).executeUpdate();
    }

    @Transactional
    public int importQuestions(long examId, MultipartFile file) {
        findById(examId);
        List<QuestionImportService.ImportedRow> rows = questionImportService.importFile(file, "ACTIVE");
        List<String> errors = new ArrayList<>();
        int linked = 0;
        for (QuestionImportService.ImportedRow row : rows) {
            Question question = row.question();
            if ("PASSAGE".equals(question.getQuestionType())) continue;
            String section = row.values().getOrDefault("section", "").trim().toUpperCase(Locale.ROOT);
            if (section.isEmpty()) section = defaultSection(question.getSkillType());
            String finalSection = section;
            if (Arrays.stream(ExamSectionEnum.values()).noneMatch(v -> v.name().equals(finalSection))) {
                errors.add("Dòng " + row.rowNumber() + ": section '" + section + "' không hợp lệ (LANGUAGE, READING, LISTENING).");
                continue;
            }
            Integer points = parsePoints(row.values().getOrDefault("points", ""));
            if (points == null) {
                errors.add("Dòng " + row.rowNumber() + ": points phải là số nguyên >= 1.");
                continue;
            }
            ExamQuestionForm form = new ExamQuestionForm();
            form.setQuestionIds(List.of(question.getId()));
            form.setSection(section);
            form.setPoints(points);
            addQuestionsToExam(examId, form);
            linked++;
        }
        if (!errors.isEmpty()) throw new QuestionImportException(errors);
        if (linked == 0) throw new QuestionImportException(List.of("File không có câu hỏi trắc nghiệm nào để gắn vào đề thi."));
        return linked;
    }

    public byte[] importTemplate() {
        return questionImportService.buildTemplate(true);
    }

    private String defaultSection(String skillType) {
        if ("READING".equals(skillType)) return "READING";
        if ("LISTENING".equals(skillType)) return "LISTENING";
        return "LANGUAGE";
    }

    private Integer parsePoints(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 1 || value != Math.floor(value)) return null;
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public void removeQuestionFromExam(long examId, long questionId) {
        ExamQuestionId eqId = new ExamQuestionId(examId, questionId);
        if (!examQuestionRepository.existsById(eqId)) {
            throw new IllegalArgumentException("Câu hỏi này không nằm trong đề thi.");
        }
        examQuestionRepository.deleteById(eqId);
    }

    // ── Private helpers ──

    private void validatePublishReady(Long examId, ManagerExamForm form) {
        String status = form.getStatus();
        if (!"PUBLISHED".equals(status) && !"ACTIVE".equals(status)) return;
        if (examId == null) {
            throw new IllegalArgumentException("Không thể tạo đề thi với trạng thái " + status + ". Hãy tạo ở trạng thái DRAFT trước, gắn câu hỏi, rồi chuyển trạng thái.");
        }
        long linked = examQuestionRepository.countByExamId(examId);
        int required = form.getTotalQuestions();
        if (linked < required) {
            throw new IllegalArgumentException("Không thể chuyển sang " + status + ": đề thi cần " + required + " câu hỏi nhưng mới gắn " + linked + " câu. Hãy thêm câu hỏi vào đề trước.");
        }
    }

    private void validateDomainValues(ManagerExamForm form) {
        if (form.getExamType() == null || Arrays.stream(ExamTypeEnum.values()).map(Enum::name).noneMatch(form.getExamType()::equals)) {
            throw new IllegalArgumentException("Loại đề thi không hợp lệ.");
        }
        if (form.getJlptLevel() == null || !JLPT_LEVELS.contains(form.getJlptLevel())) {
            throw new IllegalArgumentException("Cấp độ JLPT không hợp lệ.");
        }
        if (form.getStatus() == null || Arrays.stream(ExamStatusEnum.values()).map(Enum::name).noneMatch(form.getStatus()::equals)) {
            throw new IllegalArgumentException("Trạng thái đề thi không hợp lệ.");
        }
    }

    private String norm(String v) { return v == null ? "" : v.trim(); }
    private String clean(String v) { return v == null ? "" : v.trim(); }
    private String optional(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    public record ManagerExamOptions(List<String> examTypes, List<String> jlptLevels, List<String> statuses) {}
}
