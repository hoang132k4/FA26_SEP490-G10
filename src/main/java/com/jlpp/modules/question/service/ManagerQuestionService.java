package com.jlpp.modules.question.service;

import com.jlpp.common.constants.*;
import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.dto.QuestionListRow;
import com.jlpp.modules.question.dto.QuestionUsageStats;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ManagerQuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public ManagerQuestionService(QuestionRepository questionRepository, ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<QuestionListRow> search(String search, String category, String questionType,
                                         String jlptLevel, String skillType, String difficulty, String status) {
        List<Question> questions = questionRepository.searchForManager(
                norm(search), norm(category), norm(questionType),
                norm(jlptLevel), norm(skillType), norm(difficulty), norm(status));
        return questions.stream().map(q -> new QuestionListRow(
                q.getId(), q.getContent(), q.getQuestionType(),
                q.getJlptLevel(), q.getSkillType(), q.getDifficulty(), q.getStatus(),
                questionRepository.countExamUsages(q.getId()),
                questionRepository.countQuizUsages(q.getId())
        )).toList();
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
    public List<String> categories() {
        return questionRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public long count() {
        return questionRepository.countTopLevel();
    }

    @Transactional
    public Question create(ManagerQuestionForm form) {
        validateDomain(form);
        Question question = new Question();
        applyForm(question, form);
        return questionRepository.save(question);
    }

    @Transactional
    public Question update(long id, ManagerQuestionForm form) {
        validateDomain(form);
        Question question = findById(id);
        validateStatusTransition(question, form.getStatus());
        applyForm(question, form);
        return questionRepository.save(question);
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
            questionRepository.deleteAll(children);
        }
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
        form.setCategory(question.getCategory());
        form.setAudioUrl(question.getAudioUrl());
        form.setExplanation(question.getExplanation());
        form.setJlptLevel(question.getJlptLevel());
        form.setSkillType(question.getSkillType());
        form.setDifficulty(question.getDifficulty());
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
                Arrays.stream(SkillTypeEnum.values()).map(Enum::name).toList(),
                Arrays.stream(DifficultyEnum.values()).map(Enum::name).toList(),
                Arrays.stream(QuestionStatusEnum.values()).map(Enum::name).toList(),
                Arrays.stream(QuestionCategoryEnum.values()).map(Enum::name).toList()
        );
    }

    private void applyForm(Question question, ManagerQuestionForm form) {
        question.setQuestionType(form.getQuestionType());
        question.setContent(clean(form.getContent()));
        question.setCategory(optional(form.getCategory()));
        question.setAudioUrl(optional(form.getAudioUrl()));
        question.setExplanation(optional(form.getExplanation()));
        question.setJlptLevel(optional(form.getJlptLevel()));
        question.setSkillType(optional(form.getSkillType()));
        question.setDifficulty(optional(form.getDifficulty()));
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

    private void validateStatusTransition(Question current, String newStatus) {
        if ("ARCHIVED".equals(newStatus) && !"ARCHIVED".equals(current.getStatus())) {
            long activeExams = questionRepository.countActiveExamUsages(current.getId());
            long activeQuizzes = questionRepository.countActiveQuizUsages(current.getId());
            if (activeExams > 0 || activeQuizzes > 0) {
                StringBuilder msg = new StringBuilder("Không thể chuyển sang ARCHIVED: câu hỏi đang được sử dụng trong ");
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
                && Arrays.stream(SkillTypeEnum.values()).noneMatch(v -> v.name().equals(form.getSkillType()))) {
            throw new IllegalArgumentException("Loại kỹ năng không hợp lệ.");
        }
        if (form.getDifficulty() != null && !form.getDifficulty().isBlank()
                && Arrays.stream(DifficultyEnum.values()).noneMatch(v -> v.name().equals(form.getDifficulty()))) {
            throw new IllegalArgumentException("Độ khó không hợp lệ.");
        }
        if (!"PASSAGE".equals(form.getQuestionType())) {
            if (isBlank(form.getOptionA()) || isBlank(form.getOptionB())) {
                throw new IllegalArgumentException("Cần ít nhất đáp án A và B.");
            }
            if (form.getAnswerKeys() == null || form.getAnswerKeys().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn đáp án đúng.");
            }
            List<String> validKeys = new ArrayList<>();
            if (!isBlank(form.getOptionA())) validKeys.add("A");
            if (!isBlank(form.getOptionB())) validKeys.add("B");
            if (!isBlank(form.getOptionC())) validKeys.add("C");
            if (!isBlank(form.getOptionD())) validKeys.add("D");
            for (String key : form.getAnswerKeys()) {
                if (!validKeys.contains(key)) {
                    throw new IllegalArgumentException("Đáp án đúng '" + key + "' không có trong danh sách đáp án.");
                }
            }
            if ("SINGLE_CHOICE".equals(form.getQuestionType()) && form.getAnswerKeys().size() > 1) {
                throw new IllegalArgumentException("Câu hỏi trắc nghiệm 1 đáp án chỉ được chọn 1 đáp án đúng.");
            }
        }
        if (form.getParentId() != null) {
            Question parent = questionRepository.findById(form.getParentId()).orElse(null);
            if (parent == null || !"PASSAGE".equals(parent.getQuestionType())) {
                throw new IllegalArgumentException("Câu hỏi cha phải là loại PASSAGE.");
            }
        }
    }

    private String buildOptionsJson(ManagerQuestionForm form) {
        List<Map<String, String>> opts = new ArrayList<>();
        if (!isBlank(form.getOptionA())) opts.add(Map.of("key", "A", "text", form.getOptionA().trim()));
        if (!isBlank(form.getOptionB())) opts.add(Map.of("key", "B", "text", form.getOptionB().trim()));
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
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Lỗi tạo JSON.", e);
        }
    }

    private String norm(String v) { return v == null ? "" : v.trim(); }
    private String clean(String v) { return v == null ? "" : v.trim(); }
    private String optional(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private boolean isBlank(String v) { return v == null || v.isBlank(); }

    public record ManagerQuestionOptions(
            List<String> questionTypes,
            List<String> jlptLevels,
            List<String> skillTypes,
            List<String> difficulties,
            List<String> statuses,
            List<String> categories
    ) {}
}
