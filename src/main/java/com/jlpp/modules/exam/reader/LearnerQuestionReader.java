package com.jlpp.modules.exam.reader;

import com.jlpp.modules.exam.dto.QuestionDto;
import java.util.List;

/**
 * Read-Only Interface for Learner Exam module to retrieve questions and options.
 * Decouples the Learner Exam module from JPA entities, ensuring no DDL alterations
 * and enabling clean migration if Question Management team exposes an API or service later.
 */
public interface LearnerQuestionReader {

    /**
     * Đếm số câu hỏi thực tế liên kết với đề thi trong database Supabase.
     */
    long countQuestionsByExamId(Long examId);

    /**
     * Lấy danh sách câu hỏi phục vụ học viên làm bài thi (KHÔNG gửi kèm đáp án đúng hoặc giải thích).
     */
    List<QuestionDto> findQuestionsForTaking(Long examId);

    /**
     * Lấy danh sách câu hỏi phục vụ chấm điểm và hiển thị lời giải (CÓ chứa cờ đáp án đúng và lời giải).
     */
    List<QuestionDto> findQuestionsForEvaluation(Long examId);
}
