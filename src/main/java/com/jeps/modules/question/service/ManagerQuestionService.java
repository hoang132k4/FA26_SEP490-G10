package com.jeps.modules.question.service;

import com.jeps.common.constants.*;
import com.jeps.modules.question.dto.ManagerQuestionForm;
import com.jeps.modules.question.dto.QuestionListRow;
import com.jeps.modules.question.dto.QuestionUsageStats;
import com.jeps.modules.question.entity.Question;
import com.jeps.modules.question.repository.QuestionRepository;
import com.jeps.modules.quiz.entity.QuestionOption;
import com.jeps.modules.quiz.repository.QuestionOptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ManagerQuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ObjectMapper objectMapper;

    public ManagerQuestionService(QuestionRepository questionRepository,
                                  QuestionOptionRepository questionOptionRepository,
                                  ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.objectMapper = objectMapper;
    }

    public static final int PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public List<Long> searchAllIds(String search, String questionType,
                                   String jlptLevel, String skillType, String status) {
        return questionRepository.searchIdsForManager(
                norm(search), norm(questionType),
                norm(jlptLevel), norm(skillType), norm(status));
    }

    @Transactional(readOnly = true)
    public Page<QuestionListRow> search(String search, String questionType,
                                        String jlptLevel, String skillType, String status, int page) {
        Page<Question> questions = questionRepository.searchForManager(
                norm(search), norm(questionType),
                norm(jlptLevel), norm(skillType), norm(status),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        if (questions.isEmpty()) return new PageImpl<>(List.of(), questions.getPageable(), questions.getTotalElements());

        List<Long> ids = questions.stream().map(Question::getId).toList();
        Map<Long, Long> examCounts = toBatchMap(questionRepository.batchCountExamUsages(ids));
        Map<Long, Long> quizCounts = toBatchMap(questionRepository.batchCountQuizUsages(ids));
        Map<Long, Long> childCounts = toBatchMap(questionRepository.batchCountChildren(ids));

        return questions.map(q -> new QuestionListRow(
                q.getId(), q.getContent(), q.getQuestionType(),
                q.getJlptLevel(), q.getSkillType(), q.getStatus(),
                examCounts.getOrDefault(q.getId(), 0L),
                quizCounts.getOrDefault(q.getId(), 0L),
                childCounts.getOrDefault(q.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public Question findById(long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi."));
    }

    @Transactional(readOnly = true)
    public List<Question> findChildren(long parentId) {
        return questionRepository.findByParentIdOrderById(parentId);
    }

    @Transactional(readOnly = true)
    public long count() {
        return questionRepository.countTopLevel();
    }

    @Transactional
    public Question create(ManagerQuestionForm form) {
        validateDomain(form);
        inheritParentAudio(form);
        Question question = new Question();
        applyForm(question, form);
        Question saved = questionRepository.save(question);
        syncQuestionOptions(saved, form);
        return saved;
    }

    @Transactional
    public Question update(long id, ManagerQuestionForm form) {
        validateDomain(form);
        Question question = findById(id);
        validateStatusTransition(question, form.getStatus());
        if ("PASSAGE".equals(question.getQuestionType()) && !question.getStatus().equals(form.getStatus())) {
            cascadeStatusToChildren(id, form.getStatus());
        }
        applyForm(question, form);
        Question saved = questionRepository.save(question);
        syncQuestionOptions(saved, form);
        return saved;
    }

    public record BulkUpdateResult(int updated, int skipped) {}

    @Transactional
    public BulkUpdateResult bulkUpdateStatus(List<Long> ids, String newStatus) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một câu hỏi.");
        }
        if (Arrays.stream(QuestionStatusEnum.values()).noneMatch(v -> v.name().equals(newStatus))) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        int totalRequested = ids.size();

        if ("ACTIVE".equals(newStatus)) {
            Set<Long> excluded = new HashSet<>();
            excluded.addAll(questionRepository.findIdsWithIncompleteInfo(ids));
            excluded.addAll(questionRepository.findIdsWithMissingAnswers(ids));

            List<Long> childIds = questionRepository.findChildIdsByParentIds(ids);
            if (!childIds.isEmpty()) {
                List<Long> badChildIds = new ArrayList<>();
                badChildIds.addAll(questionRepository.findIdsWithIncompleteInfo(childIds));
                badChildIds.addAll(questionRepository.findIdsWithMissingAnswers(childIds));
                if (!badChildIds.isEmpty()) {
                    excluded.addAll(questionRepository.findParentIdsOfChildren(badChildIds));
                }
            }

            ids = ids.stream().filter(id -> !excluded.contains(id)).toList();
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("Không có câu hỏi nào đủ điều kiện để chuyển sang ACTIVE (thiếu cấp độ JLPT, kỹ năng hoặc đáp án).");
            }
        }

        if (!"ACTIVE".equals(newStatus)) {
            List<Long> allIdsToCheck = new ArrayList<>(ids);
            List<Long> childIds = questionRepository.findChildIdsByParentIds(ids);
            allIdsToCheck.addAll(childIds);

            List<Long> blockedByExam = questionRepository.findIdsUsedInActiveExams(allIdsToCheck);
            List<Long> blockedByQuiz = questionRepository.findIdsUsedInActiveQuizzes(allIdsToCheck);
            Set<Long> blocked = new HashSet<>(blockedByExam);
            blocked.addAll(blockedByQuiz);
            if (!blocked.isEmpty()) {
                Long first = blocked.iterator().next();
                throw new IllegalArgumentException("Câu hỏi #" + first
                        + " đang được sử dụng trong đề thi/quiz đang hoạt động. Không thể chuyển sang " + newStatus + ".");
            }
        }

        int updated = questionRepository.bulkSetStatus(ids, newStatus);
        questionRepository.bulkSetStatusByParentIds(ids, newStatus);
        int skipped = totalRequested - updated;
        return new BulkUpdateResult(updated, skipped);
    }

    @Transactional
    public void delete(long id) {
        Question question = findById(id);
        if (questionRepository.isUsedAnywhere(id)) {
            throw new IllegalArgumentException("Không thể xóa câu hỏi đang được sử dụng trong đề thi hoặc quiz. Hãy gỡ câu hỏi khỏi tất cả đề thi/quiz trước.");
        }
        List<Question> children = questionRepository.findByParentIdOrderById(id);
        if (!children.isEmpty()) {
            for (Question child : children) {
                if (questionRepository.isUsedAnywhere(child.getId())) {
                    throw new IllegalArgumentException("Không thể xóa: câu hỏi con #" + child.getId() + " đang được sử dụng trong đề thi hoặc quiz.");
                }
            }
            List<Long> childIds = children.stream().map(Question::getId).toList();
            questionOptionRepository.deleteByQuestionIdIn(childIds);
            questionRepository.deleteAll(children);
        }
        questionOptionRepository.deleteByQuestionId(id);
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public QuestionUsageStats getUsageStats(long questionId) {
        return new QuestionUsageStats(
                questionRepository.countExamUsages(questionId),
                questionRepository.countQuizUsages(questionId),
                questionRepository.countActiveExamUsages(questionId),
                questionRepository.countActiveQuizUsages(questionId));
    }

    public ManagerQuestionForm toForm(Question question) {
        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setId(question.getId());
        form.setQuestionType(question.getQuestionType());
        form.setContent(question.getContent());
        form.setAudioUrl(question.getAudioUrl());
        form.setExplanation(question.getExplanation());
        form.setJlptLevel(question.getJlptLevel());
        form.setSkillType(question.getSkillType());
        form.setStatus(question.getStatus());
        form.setParentId(question.getParentId());

        if (question.getOptions() != null) {
            List<Map<String, String>> opts = parseOptions(question.getOptions());
            for (Map<String, String> opt : opts) {
                switch (opt.getOrDefault("key", "")) {
                    case "A" -> form.setOptionA(opt.get("text"));
                    case "B" -> form.setOptionB(opt.get("text"));
                    case "C" -> form.setOptionC(opt.get("text"));
                    case "D" -> form.setOptionD(opt.get("text"));
                }
            }
        }
        if (question.getAnswerKey() != null) {
            form.setAnswerKeys(parseAnswerKey(question.getAnswerKey()));
        }
        return form;
    }

    public ManagerQuestionOptions options() {
        return new ManagerQuestionOptions(
                Arrays.stream(QuestionTypeEnum.values()).map(Enum::name).toList(),
                Arrays.stream(JlptLevelEnum.values()).map(Enum::name).toList(),
                SkillTypeEnum.questionSkillNames(),
                Arrays.stream(QuestionStatusEnum.values()).map(Enum::name).toList()
        );
    }

    private void applyForm(Question question, ManagerQuestionForm form) {
        question.setQuestionType(form.getQuestionType());
        question.setContent(clean(form.getContent()));
        question.setAudioUrl(optional(form.getAudioUrl()));
        question.setExplanation(optional(form.getExplanation()));
        question.setJlptLevel(optional(form.getJlptLevel()));
        question.setSkillType(optional(form.getSkillType()));
        question.setStatus(form.getStatus());
        question.setParentId(form.getParentId());

        if ("PASSAGE".equals(form.getQuestionType())) {
            question.setOptions(null);
            question.setAnswerKey(null);
        } else {
            question.setOptions(buildOptionsJson(form));
            question.setAnswerKey(buildAnswerKeyJson(form));
        }
    }

    private void cascadeStatusToChildren(long parentId, String newStatus) {
        List<Question> children = questionRepository.findByParentIdOrderById(parentId);
        if ("ACTIVE".equals(newStatus) && !children.isEmpty()) {
            List<Long> childIds = children.stream().map(Question::getId).toList();
            Set<Long> incomplete = new TreeSet<>(questionRepository.findIdsWithIncompleteInfo(childIds));
            incomplete.addAll(questionRepository.findIdsWithMissingAnswers(childIds));
            if (!incomplete.isEmpty()) {
                throw new IllegalArgumentException("Câu hỏi con #" + incomplete.iterator().next()
                        + " chưa đủ cấp độ, kỹ năng hoặc đáp án nên không thể chuyển sang ACTIVE.");
            }
        }
        for (Question child : children) {
            if (!child.getStatus().equals(newStatus)) {
                if (!"ACTIVE".equals(newStatus)) {
                    long activeExams = questionRepository.countActiveExamUsages(child.getId());
                    long activeQuizzes = questionRepository.countActiveQuizUsages(child.getId());
                    if (activeExams > 0 || activeQuizzes > 0) {
                        throw new IllegalArgumentException("Câu hỏi con #" + child.getId()
                                + " đang được sử dụng trong đề thi/quiz đang hoạt động. Không thể chuyển sang " + newStatus + ".");
                    }
                }
                child.setStatus(newStatus);
            }
        }
        if (!children.isEmpty()) {
            questionRepository.saveAll(children);
        }
    }

    private void validateStatusTransition(Question current, String newStatus) {
        if (!"ACTIVE".equals(newStatus) && !newStatus.equals(current.getStatus())) {
            long activeExams = questionRepository.countActiveExamUsages(current.getId());
            long activeQuizzes = questionRepository.countActiveQuizUsages(current.getId());
            if (activeExams > 0 || activeQuizzes > 0) {
                StringBuilder msg = new StringBuilder("Không thể chuyển sang " + newStatus + ": câu hỏi đang được sử dụng trong ");
                if (activeExams > 0) msg.append(activeExams).append(" đề thi đang hoạt động");
                if (activeExams > 0 && activeQuizzes > 0) msg.append(" và ");
                if (activeQuizzes > 0) msg.append(activeQuizzes).append(" quiz đang hoạt động");
                msg.append(". Hãy gỡ câu hỏi hoặc chuyển đề thi/quiz sang trạng thái khác trước.");
                throw new IllegalArgumentException(msg.toString());
            }
        }
    }

    private void validateDomain(ManagerQuestionForm form) {
        if (Arrays.stream(QuestionTypeEnum.values()).noneMatch(v -> v.name().equals(form.getQuestionType()))) {
            throw new IllegalArgumentException("Loại câu hỏi không hợp lệ.");
        }
        if (Arrays.stream(QuestionStatusEnum.values()).noneMatch(v -> v.name().equals(form.getStatus()))) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        if (form.getJlptLevel() != null && !form.getJlptLevel().isBlank()
                && Arrays.stream(JlptLevelEnum.values()).noneMatch(v -> v.name().equals(form.getJlptLevel()))) {
            throw new IllegalArgumentException("Cấp độ JLPT không hợp lệ.");
        }
        if (form.getSkillType() != null && !form.getSkillType().isBlank()
                && !SkillTypeEnum.questionSkillNames().contains(form.getSkillType())) {
            throw new IllegalArgumentException("Loại kỹ năng không hợp lệ.");
        }
        boolean listeningPassage = "PASSAGE".equals(form.getQuestionType()) && "LISTENING".equals(form.getSkillType());
        if (!listeningPassage && isBlank(form.getContent())) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống.");
        }
        if (!"PASSAGE".equals(form.getQuestionType())) {
            if (isBlank(form.getOptionA()) || isBlank(form.getOptionB())) {
                throw new IllegalArgumentException("Cần ít nhất đáp án A và B.");
            }
            if (form.getAnswerKeys() == null || form.getAnswerKeys().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn đáp án đúng.");
            }
            List<String> validKeys = new ArrayList<>(List.of("A", "B"));
            if (!isBlank(form.getOptionC())) validKeys.add("C");
            if (!isBlank(form.getOptionD())) validKeys.add("D");
            for (String key : form.getAnswerKeys()) {
                if (!validKeys.contains(key)) {
                    throw new IllegalArgumentException("Đáp án đúng '" + key + "' không có trong danh sách đáp án.");
                }
            }
            if (form.getAnswerKeys().size() > 1) {
                throw new IllegalArgumentException("Câu hỏi trắc nghiệm chỉ được chọn 1 đáp án đúng.");
            }
        }
        if ("ACTIVE".equals(form.getStatus())) {
            if (isBlank(form.getJlptLevel())) {
                throw new IllegalArgumentException("Câu hỏi ACTIVE phải có cấp độ JLPT.");
            }
            if (isBlank(form.getSkillType())) {
                throw new IllegalArgumentException("Câu hỏi ACTIVE phải có loại kỹ năng.");
            }
            if (listeningPassage && isBlank(form.getAudioUrl())) {
                throw new IllegalArgumentException("Bài nghe ACTIVE phải có file audio.");
            }
        }
        if (form.getParentId() != null) {
            if ("PASSAGE".equals(form.getQuestionType())) {
                throw new IllegalArgumentException("Câu hỏi con không được là loại PASSAGE.");
            }
            Question parent = questionRepository.findById(form.getParentId()).orElse(null);
            if (parent == null || !"PASSAGE".equals(parent.getQuestionType())) {
                throw new IllegalArgumentException("Câu hỏi cha phải là loại PASSAGE.");
            }
        }
    }

    /** validateDomain has already required options A and B. */
    private String buildOptionsJson(ManagerQuestionForm form) {
        List<Map<String, String>> opts = new ArrayList<>();
        opts.add(Map.of("key", "A", "text", form.getOptionA().trim()));
        opts.add(Map.of("key", "B", "text", form.getOptionB().trim()));
        if (!isBlank(form.getOptionC())) opts.add(Map.of("key", "C", "text", form.getOptionC().trim()));
        if (!isBlank(form.getOptionD())) opts.add(Map.of("key", "D", "text", form.getOptionD().trim()));
        return toJson(opts);
    }

    private String buildAnswerKeyJson(ManagerQuestionForm form) {
        return toJson(form.getAnswerKeys());
    }

    private List<Map<String, String>> parseOptions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<String> parseAnswerKey(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        return objectMapper.valueToTree(value).toString();
    }

    private Map<Long, Long> toBatchMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private void inheritParentAudio(ManagerQuestionForm form) {
        if (form.getParentId() == null) return;
        if (form.getAudioUrl() != null && !form.getAudioUrl().isBlank()) return;
        Question parent = questionRepository.findById(form.getParentId()).orElse(null);
        if (parent != null && parent.getAudioUrl() != null && !parent.getAudioUrl().isBlank()) {
            form.setAudioUrl(parent.getAudioUrl());
        }
    }

    private void syncQuestionOptions(Question saved, ManagerQuestionForm form) {
        if ("PASSAGE".equals(saved.getQuestionType())) return;

        List<QuestionOption> existing = questionOptionRepository.findByQuestionIdOrderByLabelAsc(saved.getId());
        Map<String, QuestionOption> byLabel = new LinkedHashMap<>();
        for (QuestionOption opt : existing) byLabel.put(opt.getLabel(), opt);

        Set<String> validAnswers = form.getAnswerKeys() != null ? new HashSet<>(form.getAnswerKeys()) : Set.of();
        Map<String, String> formOptions = new LinkedHashMap<>();
        if (!isBlank(form.getOptionA())) formOptions.put("A", form.getOptionA().trim());
        if (!isBlank(form.getOptionB())) formOptions.put("B", form.getOptionB().trim());
        if (!isBlank(form.getOptionC())) formOptions.put("C", form.getOptionC().trim());
        if (!isBlank(form.getOptionD())) formOptions.put("D", form.getOptionD().trim());

        List<QuestionOption> toSave = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, String> entry : formOptions.entrySet()) {
            String label = entry.getKey();
            seen.add(label);
            QuestionOption opt = byLabel.getOrDefault(label, new QuestionOption());
            opt.setQuestionId(saved.getId());
            opt.setLabel(label);
            opt.setOptionText(entry.getValue());
            opt.setIsCorrect(validAnswers.contains(label));
            toSave.add(opt);
        }
        List<QuestionOption> toDelete = existing.stream().filter(o -> !seen.contains(o.getLabel())).toList();
        if (!toDelete.isEmpty()) questionOptionRepository.deleteAll(toDelete);
        if (!toSave.isEmpty()) questionOptionRepository.saveAll(toSave);
    }

    private String norm(String v) { return v == null ? "" : v.trim(); }
    private String clean(String v) { return v == null ? "" : v.trim(); }
    private String optional(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private boolean isBlank(String v) { return v == null || v.isBlank(); }

    public record ManagerQuestionOptions(
            List<String> questionTypes,
            List<String> jlptLevels,
            List<String> skillTypes,
            List<String> statuses
    ) {}
}
