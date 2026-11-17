package com.jlpp.modules.exam.service;

import com.jlpp.common.constants.*;
import com.jlpp.modules.exam.dto.ExamQuestionForm;
import com.jlpp.modules.exam.dto.ExamQuestionRow;
import com.jlpp.modules.exam.dto.GeneratePreview;
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
import com.jlpp.modules.question.service.QuestionPicker;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static com.jlpp.modules.exam.service.JlptExamStructure.LISTENING;
import static com.jlpp.modules.exam.service.JlptExamStructure.READING;
import static com.jlpp.modules.exam.service.JlptExamStructure.SECTIONS;
import static com.jlpp.modules.exam.service.JlptExamStructure.SECTION_LABELS;

@Service
public class ManagerExamService {

    private final EntityManager entityManager;

    private static final List<String> JLPT_LEVELS = Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList();
    private static final List<String> LANGUAGE_SKILL_ORDER = List.of(
            SkillTypeEnum.KANJI.name(), SkillTypeEnum.VOCABULARY.name(), SkillTypeEnum.GRAMMAR.name());

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionImportService questionImportService;
    private final Random random = new Random();

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
        exam.setTotalPoints(JlptExamStructure.TOTAL_SCORE);
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
        exam.setTotalPoints(JlptExamStructure.TOTAL_SCORE);
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

    public static final int PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public Page<Question> findAvailableQuestions(long examId, String jlptLevel, String skillType, int page) {
        return questionRepository.findAvailableForExam(examId, norm(jlptLevel), norm(skillType),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
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

        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(form.getQuestionIds()));
        Map<Long, Question> found = new HashMap<>();
        questionRepository.findAllById(ids).forEach(q -> found.put(q.getId(), q));
        Set<Long> alreadyLinked = new HashSet<>(examQuestionRepository.findLinkedQuestionIds(examId, ids));

        Map<String, List<Question>> bySection = new LinkedHashMap<>();
        for (Long questionId : ids) {
            Question question = found.get(questionId);
            if (question == null) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " không tồn tại.");
            }
            if (!"ACTIVE".equals(question.getStatus())) {
                throw new IllegalArgumentException("Câu hỏi #" + questionId + " chưa được kích hoạt (ACTIVE).");
            }
            if (alreadyLinked.contains(questionId)) {
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
            List<ExamQuestion> links = new ArrayList<>();
            for (Question question : entry.getValue()) {
                links.add(ExamQuestion.builder()
                        .id(new ExamQuestionId(examId, question.getId()))
                        .exam(exam)
                        .question(question)
                        .section(section)
                        .orderIndex(++order)
                        .points(1)
                        .build());
            }
            examQuestionRepository.saveAll(links);
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
                || !new LinkedHashSet<>(orderedQuestionIds).equals(new LinkedHashSet<>(currentIds))) {
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

    /** Preview: chọn câu ngẫu nhiên nhưng chưa lưu. */
    @Transactional(readOnly = true)
    public GeneratePreview previewQuestions(long examId) {
        Exam exam = findById(examId);
        requireDraft(exam);
        Map<Long, Question> bank = loadBank(examId, exam.getJlptLevel());
        Map<String, Map<String, List<Long>>> unitsBySection = buildUnits(bank);
        JlptExamStructure jlpt = JlptExamStructure.of(exam.getJlptLevel());

        Map<String, List<GeneratePreview.PreviewQuestion>> sections = new LinkedHashMap<>();
        for (String section : SECTIONS) {
            List<Long> ids = pickSection(examId, section, jlpt, unitsBySection, bank, exam.getJlptLevel());
            sections.put(section, ids.stream().map(id -> toPreviewQuestion(bank.get(id))).toList());
        }
        int total = sections.values().stream().mapToInt(List::size).sum();
        if (total == 0) throw new IllegalArgumentException("Đề thi đã đủ câu theo cấu trúc JLPT " + exam.getJlptLevel() + ".");
        return new GeneratePreview(examId, exam.getJlptLevel(), sections);
    }

    /** Chọn lại một phần duy nhất; phần còn lại giữ nguyên từ preview trước. */
    @Transactional(readOnly = true)
    public GeneratePreview regenerateSection(long examId, String section, GeneratePreview current) {
        if (!SECTIONS.contains(section)) throw new IllegalArgumentException("Section không hợp lệ.");
        Exam exam = findById(examId);
        requireDraft(exam);
        Map<Long, Question> bank = loadBank(examId, exam.getJlptLevel());
        Map<String, Map<String, List<Long>>> unitsBySection = buildUnits(bank);
        JlptExamStructure jlpt = JlptExamStructure.of(exam.getJlptLevel());

        List<Long> ids = pickSection(examId, section, jlpt, unitsBySection, bank, exam.getJlptLevel());
        Map<String, List<GeneratePreview.PreviewQuestion>> sections = new LinkedHashMap<>(current.sections());
        sections.put(section, ids.stream().map(id -> toPreviewQuestion(bank.get(id))).toList());
        return new GeneratePreview(examId, exam.getJlptLevel(), sections);
    }

    /** Lưu kết quả preview đã xác nhận. */
    @Transactional
    public int confirmGenerate(long examId, GeneratePreview preview) {
        Exam exam = findById(examId);
        requireDraft(exam);
        JlptExamStructure jlpt = JlptExamStructure.of(exam.getJlptLevel());
        // The preview lives in the session; questions may have been linked by hand or archived since it was made.
        Map<Long, Question> bank = loadBank(examId, exam.getJlptLevel());
        Map<String, List<Long>> idMap = preview.toIdMap();
        for (Map.Entry<String, List<Long>> entry : idMap.entrySet()) {
            long linked = examQuestionRepository.countByExamIdAndSection(examId, entry.getKey());
            if (linked + entry.getValue().size() > jlpt.questions(entry.getKey())
                    || !bank.keySet().containsAll(entry.getValue())) {
                throw new IllegalArgumentException(STALE_PREVIEW);
            }
        }
        exam.setLanguageQuestions(jlpt.languageQuestions());
        exam.setReadingQuestions(jlpt.readingQuestions());
        exam.setListeningQuestions(jlpt.listeningQuestions());
        exam.setTotalQuestions(jlpt.totalQuestions());
        exam.setDurationMinutes(jlpt.durationMinutes());
        exam.setPassingScore(jlpt.passingScore());
        examRepository.save(exam);

        idMap.forEach((section, ids) -> {
            if (ids.isEmpty()) return;
            int order = examQuestionRepository.maxOrderIndex(examId, section);
            List<ExamQuestion> links = new ArrayList<>();
            for (Long id : ids) {
                links.add(ExamQuestion.builder()
                        .id(new ExamQuestionId(examId, id))
                        .exam(exam)
                        .question(bank.get(id))
                        .section(section)
                        .orderIndex(++order)
                        .points(1)
                        .build());
            }
            examQuestionRepository.saveAll(links);
        });
        return preview.totalAdded();
    }

    private static final String STALE_PREVIEW =
            "Kết quả xem trước đã cũ vì câu hỏi trong đề hoặc ngân hàng vừa thay đổi. Hãy bấm Tự động chọn câu để chọn lại.";

    static String idList(List<Long> ids) {
        return String.join(", ", ids.stream().map(id -> "#" + id).toList());
    }

    private static void requireDraft(Exam exam) {
        if (!ExamStatusEnum.DRAFT.name().equals(exam.getStatus())) {
            throw new IllegalArgumentException("Chỉ tự động chọn câu cho đề thi ở trạng thái DRAFT.");
        }
    }

    private Map<Long, Question> loadBank(long examId, String level) {
        Map<Long, Question> bank = new HashMap<>();
        for (Question q : questionRepository.findAvailableForExam(examId, level, "")) {
            bank.put(q.getId(), q);
        }
        return bank;
    }

    private Map<String, Map<String, List<Long>>> buildUnits(Map<Long, Question> bank) {
        Map<String, Map<String, List<Long>>> unitsBySection = new LinkedHashMap<>();
        for (Question q : bank.values()) {
            String unitKey = q.getParentId() != null ? "p" + q.getParentId() : "q" + q.getId();
            unitsBySection.computeIfAbsent(sectionOf(q.getSkillType()), k -> new LinkedHashMap<>())
                    .computeIfAbsent(unitKey, k -> new ArrayList<>()).add(q.getId());
        }
        return unitsBySection;
    }

    private List<Long> pickSection(long examId, String section, JlptExamStructure jlpt,
                                    Map<String, Map<String, List<Long>>> unitsBySection,
                                    Map<Long, Question> bank, String level) {
        String label = SECTION_LABELS.get(section);
        long linked = examQuestionRepository.countByExamIdAndSection(examId, section);
        int required = jlpt.questions(section);
        if (linked > required) {
            throw new IllegalArgumentException("Phần " + label + " đã gán " + linked + " câu, vượt cấu trúc JLPT " + level
                    + " (" + required + " câu). Hãy gỡ bớt trước.");
        }
        int missing = (int) (required - linked);
        if (missing == 0) return List.of();
        List<List<Long>> units = unitsBySection.getOrDefault(section, Map.of()).values().stream()
                .map(unit -> unit.stream().sorted().toList()).toList();
        int available = units.stream().mapToInt(List::size).sum();
        if (available < missing) {
            throw new IllegalArgumentException("Phần " + label + " cần thêm " + missing + " câu cấp " + level
                    + " nhưng ngân hàng chỉ còn " + available + " câu ACTIVE phù hợp.");
        }
        return QuestionPicker.pickBalanced(groupBySkill(units, bank), missing, random)
                .orElseThrow(() -> new IllegalArgumentException("Phần " + label + " không ghép được đúng " + missing
                        + " câu vì số câu con của các bài đọc/nghe không khớp. Hãy chọn tay hoặc bổ sung câu đơn lẻ."));
    }

    private static GeneratePreview.PreviewQuestion toPreviewQuestion(Question q) {
        return new GeneratePreview.PreviewQuestion(q.getId(), q.getContent(), q.getQuestionType(), q.getJlptLevel(), q.getSkillType());
    }

    /** Nhóm unit theo skill_type, thứ tự giống đề JLPT: Hán tự → Từ vựng → Ngữ pháp → còn lại. */
    private static Map<String, List<List<Long>>> groupBySkill(List<List<Long>> units, Map<Long, Question> bank) {
        Map<String, List<List<Long>>> groups = new TreeMap<>(Comparator.comparingInt(ManagerExamService::skillRank)
                .thenComparing(Comparator.naturalOrder()));
        for (List<Long> unit : units) {
            String skill = Objects.requireNonNullElse(bank.get(unit.get(0)).getSkillType(), "");
            groups.computeIfAbsent(skill, k -> new ArrayList<>()).add(unit);
        }
        return groups;
    }

    private static int skillRank(String skill) {
        int rank = LANGUAGE_SKILL_ORDER.indexOf(skill);
        return rank < 0 ? LANGUAGE_SKILL_ORDER.size() : rank;
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
        // A question used only by DRAFT exams may be archived, so re-check before learners can see the exam.
        List<Long> inactive = examQuestionRepository.findInactiveQuestionIds(examId);
        if (!inactive.isEmpty()) {
            throw new IllegalArgumentException("Không thể chuyển sang " + status + ": đề còn câu hỏi không ở trạng thái ACTIVE ("
                    + idList(inactive) + "). Hãy gỡ các câu này khỏi đề hoặc kích hoạt lại trước.");
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

    /** Only called after validateSectionCounts, so the count is never null. */
    private static int formQuestions(ManagerExamForm form, String section) {
        return formCount(form, section);
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
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    public record ManagerExamOptions(List<String> examTypes, List<String> jlptLevels, List<String> statuses) {}
}
