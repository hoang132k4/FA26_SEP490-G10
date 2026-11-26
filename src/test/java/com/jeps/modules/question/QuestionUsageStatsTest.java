package com.jeps.modules.question;

import com.jeps.modules.question.dto.QuestionUsageStats;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionUsageStatsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, false",
            "1, 0, true",
            "0, 2, true",
            "1, 2, true"
    })
    void usedInActiveExamOrQuizWhenEitherCountIsPositive(long activeExams, long activeQuizzes, boolean expected) {
        QuestionUsageStats stats = new QuestionUsageStats(3, 4, activeExams, activeQuizzes);

        assertEquals(expected, stats.isUsedInActiveExamOrQuiz());
        assertEquals(7, stats.totalUsages());
    }
}
