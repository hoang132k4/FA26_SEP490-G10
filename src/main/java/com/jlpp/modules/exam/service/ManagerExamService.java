package com.jlpp.modules.exam.service;

import com.jlpp.common.constants.*;
import com.jlpp.modules.exam.dto.ExamQuestionForm;
import com.jlpp.modules.exam.dto.ExamQuestionRow;
import com.jlpp.modules.exam.dto.ManagerExamForm;
import com.jlpp.modules.exam.dto.SectionProgress;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.jlpp.modules.exam.service.JlptExamStructure.LISTENING;
import static com.jlpp.modules.exam.service.JlptExamStructure.READING;
import static com.jlpp.modules.exam.service.JlptExamStructure.SECTIONS;
import static com.jlpp.modules.exam.service.JlptExamStructure.SECTION_LABELS;

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
        validateSectionCounts(form);
        validatePublishReady(null, form);
        JlptExamStructure jlpt = JlptExamStructure.of(form.getJlptLevel());
        Exam exam = new Exam(null, clean(form.getExamTitle()), form.getExamType(), form.getJlptLevel(),
                jlpt.durationMinutes(), totalQuestions(form), jlpt.passingScore(),
                form.getStatus(), optional(form.getDescription()));
        applySectionCounts(exam, form);
        return examRepository.save(exam);
    }

    @Transactional
    public Exam update(long examId, ManagerExamForm form) {
        validateDomainValues(form);
        validateSectionCounts(form);
        validatePublishReady(examId, form);
        Exam exam = findById(examId);
        JlptExamStructure jlpt = JlptExamStructure.of(form.getJlptLevel());
        exam.setExamTitle(clean(form.getExamTitle()));
        exam.setExamType(form.getExamType());
        exam.setJlptLevel(form.getJlptLevel());
        exam.setDurationMinutes(jlpt.durationMinutes());
        exam.setTotalQuestions(totalQuestions(form));
        exam.setPassingScore(jlpt.passingScore());
        exam.setStatus(form.getStatus());
        exam.setDescription(optional(form.getDescription()));
        applySectionCounts(exam, form);
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

    public ManagerExamForm newForm() {
        JlptExamStructure jlpt = JlptExamStructure.of(JLPT_LEVELS.get(0));
        ManagerExamForm form = new ManagerExamForm();
        form.setJlptLevel(jlpt.level());
        form.setStatus(ExamStatusEnum.DRAFT.name());
        form.setLanguageQuestions(jlpt.languageQuestions());
        form.setReadingQuestions(jlpt.readingQuestions());
        form.setListeningQuestions(jlpt.listeningQuestions());
        return form;
    }

    public ManagerExamForm toForm(Exam exam) {
        ManagerExamForm form = new ManagerExamForm();
        form.setExamId(exam.getExamId());
        form.setExamTitle(exam.getExamTitle());
        form.setExamType(exam.getExamType());
        form.setJlptLevel(exam.getJlptLevel());
        form.setLanguageQuestions(requiredQuestions(exam, JlptExamStructure.LANGUAGE));
        form.setReadingQuestions(requiredQuestions(exam, READING));
        form.setListeningQuestions(requiredQuestions(exam, LISTENING));
        form.setStatus(exam.getStatus());
        form.setDescription(exam.getDescription());
        return form;
    }

    @Transactional(readOnly = true)
    public List<SectionProgress> sectionProgress(long examId) {
        Exam exam = findById(examId);
        return SECTIONS.stream()
                .map(section -> new SectionProgress(section, SECTION_LABELS.get(section),
                        examQuestionRepository.countByExamIdAndSection(examId, section),
                        requiredQuestions(exam, section)))
                .toList();
    }

    // ── Exam-Question Management ──

    @Transactional(readOnly = true)
    public List<ExamQuestionRow> getExamQuestions(long examId) {
        return examQuestionRepository.findByExamIdWithQuestion(examId).stream()
                .map(eq -> {
                    Question q = eq.getQuestion();
                    return new ExamQuestionRow(
                            q.getId(), truncate(q.getContent(), 80), q.getQuestionType(),
                            q.getJlptLevel(), q.getSkillType(),
                            eq.getSection(), eq.getOrderIndex());
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

    /** Gắn câu hỏi vào đề; mỗi phần không được vượt số câu đã cấu hình. */
    @Transactional
    public void addQuestionsToExam(long examId, ExamQuestionForm form) {
        Exam exam = findById(examId);
        if (form.getQuestionIds() == null || form.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một câu hỏi.");
        }
        String forcedSection = form.getSection() == null || form.getSection().isBlank() ? null : form.getSection();
        if (forcedSection != null && !SECTIONS.contains(forcedSection)) {
            throw new IllegalArgumentException("Section không hợp lệ.");
        }

        Map<String, List<Question>> bySection = new LinkedHashMap<>();
        for (Long questionId : new LinkedHashSet<>(form.getQuestionIds())) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Câu hỏi #" + questionId + " không tồn tại."));
            if (!"ACTIVE".equals(question.getStatus())) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " chưa được kích hoạt (ACTIVE).");
            }
            if (examQuestionRepository.existsById(new ExamQuestionId(examId, questionId))) {
                continue;
            }
            String section = forcedSection != null ? forcedSection : sectionOf(question.getSkillType());
            bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(question);
        }

        for (Map.Entry<String, List<Question>> entry : bySection.entrySet()) {
            String section = entry.getKey();
            long linked = examQuestionRepository.countByExamIdAndSection(examId, section);
            int required = requiredQuestions(exam, section);
            if (linked + entry.getValue().size() > required) {
                throw new IllegalArgumentException(quotaMessage(section, required, linked, entry.getValue().size()));
            }
        }

        for (Map.Entry<String, List<Question>> entry : bySection.entrySet()) {
            String section = entry.getKey();
            int order = examQuestionRepository.maxOrderIndex(examId, section);
            for (Question question : entry.getValue()) {
                order++;
                examQuestionRepository.save(ExamQuestion.builder()
                        .id(new ExamQuestionId(examId, question.getId()))
                        .exam(exam)
                        .question(question)
                        .section(section)
                        .orderIndex(order)
                        .points(1)
                        .build());
            }
        }
    }

    @Transactional
    public void reorderSection(long examId, String section, List<Long> orderedQuestionIds) {
        if (!SECTIONS.contains(section)) {
            throw new IllegalArgumentException("Phần đề thi không hợp lệ.");
        }
        List<ExamQuestion> current = examQuestionRepository.findByExamIdAndSectionOrdered(examId, section);
        List<Long> currentIds = current.stream().map(eq -> eq.getId().getQuestionId()).toList();
        if (orderedQuestionIds == null
                || orderedQuestionIds.size() != currentIds.size()
                || !new LinkedHashSet<>(orderedQuestionIds).equals(new LinkedHashSet<>(currentIds))
                || new LinkedHashSet<>(orderedQuestionIds).size() != orderedQuestionIds.size()) {
            throw new IllegalArgumentException("Danh sách câu hỏi của phần này đã thay đổi. Hãy tải lại trang rồi kéo thả lại.");
        }
        // Two passes so no intermediate row breaks CHECK(order_index > 0) or UNIQUE(exam_id, section, order_index):
        // first park every row above the current max, then write the final 1..n.
        int offset = current.stream().mapToInt(ExamQuestion::getOrderIndex).max().orElse(0);
        String sql = "UPDATE exam_questions SET order_index = ? WHERE exam_id = ? AND question_id = ?";
        for (int i = 0; i < orderedQuestionIds.size(); i++) {
            entityManager.createNativeQuery(sql).setParameter(1, offset + i + 1).setParameter(2, examId)
                    .setParameter(3, orderedQuestionIds.get(i)).executeUpdate();
        }
        for (int i = 0; i < orderedQuestionIds.size(); i++) {
            entityManager.createNativeQuery(sql).setParameter(1, i + 1).setParameter(2, examId)
                    .setParameter(3, orderedQuestionIds.get(i)).executeUpdate();
        }
    }

    @Transactional
    public int importQuestions(long examId, MultipartFile file) {
        Exam exam = findById(examId);
        List<QuestionImportService.ImportedRow> rows = questionImportService.importFile(file, "ACTIVE");
        List<String> errors = new ArrayList<>();
        Map<String, List<Long>> bySection = new LinkedHashMap<>();
        for (QuestionImportService.ImportedRow row : rows) {
            Question question = row.question();
            if ("PASSAGE".equals(question.getQuestionType()) || question.getParentId() != null) continue;
            String section = row.values().getOrDefault("section", "").trim().toUpperCase(Locale.ROOT);
            if (section.isEmpty()) section = sectionOf(question.getSkillType());
            if (!SECTIONS.contains(section)) {
                errors.add("Dòng " + row.rowNumber() + ": section '" + section + "' không hợp lệ (LANGUAGE, READING, LISTENING).");
                continue;
            }
            bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(question.getId());
        }
        if (!errors.isEmpty()) throw new QuestionImportException(errors);
        if (bySection.isEmpty()) throw new QuestionImportException(List.of("File không có câu hỏi trắc nghiệm nào để gắn vào đề thi."));

        for (Map.Entry<String, List<Long>> entry : bySection.entrySet()) {
            String section = entry.getKey();
            long linked = examQuestionRepository.countByExamIdAndSection(examId, section);
            int required = requiredQuestions(exam, section);
            if (linked + entry.getValue().size() > required) {
                errors.add(quotaMessage(section, required, linked, entry.getValue().size()));
            }
        }
        if (!errors.isEmpty()) throw new QuestionImportException(errors);

        int linkedCount = 0;
        for (Map.Entry<String, List<Long>> entry : bySection.entrySet()) {
            ExamQuestionForm form = new ExamQuestionForm();
            form.setQuestionIds(entry.getValue());
            form.setSection(entry.getKey());
            addQuestionsToExam(examId, form);
            linkedCount += entry.getValue().size();
        }
        return linkedCount;
    }

    public byte[] importTemplate() {
        return questionImportService.buildTemplate(true);
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
        List<String> mismatched = new ArrayList<>();
        for (String section : SECTIONS) {
            long linked = examQuestionRepository.countByExamIdAndSection(examId, section);
            int required = formQuestions(form, section);
            if (linked != required) {
                mismatched.add(SECTION_LABELS.get(section) + " " + linked + "/" + required);
            }
        }
        if (!mismatched.isEmpty()) {
            throw new IllegalArgumentException("Không thể chuyển sang " + status + ": số câu đã gán chưa khớp cấu trúc đề ("
                    + String.join(", ", mismatched) + "). Hãy gắn đủ câu hoặc chỉnh lại số câu từng phần.");
        }
    }

    private void validateSectionCounts(ManagerExamForm form) {
        for (String section : SECTIONS) {
            Integer count = formCount(form, section);
            if (count == null || count < 0) {
                throw new IllegalArgumentException("Số câu phần " + SECTION_LABELS.get(section) + " không hợp lệ.");
            }
        }
        if (totalQuestions(form) == 0) {
            throw new IllegalArgumentException("Đề thi phải có ít nhất 1 câu hỏi.");
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

    private static String quotaMessage(String section, int required, long linked, int adding) {
        long remaining = Math.max(0, required - linked);
        return "Phần " + SECTION_LABELS.get(section) + " chỉ cần " + required + " câu, đã gán " + linked
                + " nên chỉ thêm được " + remaining + " câu nữa (đang thêm " + adding
                + "). Hãy bớt câu hoặc tăng số câu phần này trong cấu hình đề.";
    }

    /** Đề cũ chưa lưu số câu theo phần thì dùng mặc định của cấp độ. */
    private static int requiredQuestions(Exam exam, String section) {
        Integer value = switch (section) {
            case READING -> exam.getReadingQuestions();
            case LISTENING -> exam.getListeningQuestions();
            default -> exam.getLanguageQuestions();
        };
        return value != null ? value : JlptExamStructure.of(exam.getJlptLevel()).questions(section);
    }

    private static Integer formCount(ManagerExamForm form, String section) {
        return switch (section) {
            case READING -> form.getReadingQuestions();
            case LISTENING -> form.getListeningQuestions();
            default -> form.getLanguageQuestions();
        };
    }

    private static int formQuestions(ManagerExamForm form, String section) {
        Integer value = formCount(form, section);
        return value != null ? value : 0;
    }

    private static int totalQuestions(ManagerExamForm form) {
        return SECTIONS.stream().mapToInt(section -> formQuestions(form, section)).sum();
    }

    private static void applySectionCounts(Exam exam, ManagerExamForm form) {
        exam.setLanguageQuestions(form.getLanguageQuestions());
        exam.setReadingQuestions(form.getReadingQuestions());
        exam.setListeningQuestions(form.getListeningQuestions());
    }

    private static String sectionOf(String skillType) {
        if (READING.equals(skillType)) return READING;
        if (LISTENING.equals(skillType)) return LISTENING;
        return JlptExamStructure.LANGUAGE;
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
