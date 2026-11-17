package com.jlpp.modules.exam;

import com.jlpp.modules.exam.service.JlptExamStructure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class JlptExamStructureTest {

    @ParameterizedTest
    @CsvSource({"LANGUAGE,38", "READING,5", "LISTENING,24"})
    void questionsPerSection_N5(String section, int expected) {
        assertEquals(expected, JlptExamStructure.of("N5").questions(section));
    }

    @Test
    void totalQuestions_N5_sumOfAllSections() {
        JlptExamStructure n5 = JlptExamStructure.of("N5");
        assertEquals(n5.languageQuestions() + n5.readingQuestions() + n5.listeningQuestions(),
                n5.totalQuestions());
    }

    @Test
    void n4n5_languageReadingCombined_true() {
        assertTrue(JlptExamStructure.of("N4").languageReadingCombined());
        assertTrue(JlptExamStructure.of("N5").languageReadingCombined());
    }

    @Test
    void n1n2n3_languageReadingCombined_false() {
        assertFalse(JlptExamStructure.of("N1").languageReadingCombined());
        assertFalse(JlptExamStructure.of("N2").languageReadingCombined());
        assertFalse(JlptExamStructure.of("N3").languageReadingCombined());
    }

    @Test
    void unknownLevel_fallsBackToN5() {
        JlptExamStructure unknown = JlptExamStructure.of("N99");
        JlptExamStructure n5 = JlptExamStructure.of("N5");
        assertEquals(n5.totalQuestions(), unknown.totalQuestions());
        assertEquals(n5.passingScore(), unknown.passingScore());
    }

    @Test
    void allLevels_returnsFiveEntries() {
        assertEquals(5, JlptExamStructure.all().size());
    }

    @Test
    void questionsUnknownSection_defaultsToLanguage() {
        JlptExamStructure n3 = JlptExamStructure.of("N3");
        assertEquals(n3.languageQuestions(), n3.questions("UNKNOWN_SECTION"));
    }
}
