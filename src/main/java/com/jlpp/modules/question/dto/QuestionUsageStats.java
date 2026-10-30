package com.jlpp.modules.question.dto;

public record QuestionUsageStats(long examCount, long quizCount, long activeExamCount, long activeQuizCount) {

    public long totalUsages() { return examCount + quizCount; }

    public boolean isUsedInActiveExamOrQuiz() { return activeExamCount > 0 || activeQuizCount > 0; }
}
