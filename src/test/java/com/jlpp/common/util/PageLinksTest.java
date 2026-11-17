package com.jlpp.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageLinksTest {

    @Test
    void keepsNonBlankFiltersAndDropsNullOrBlankOnes() {
        assertEquals("/manager/questions?jlptLevel=N5",
                PageLinks.of("/manager/questions", "search", null, "jlptLevel", "N5", "skillType", "  "));
    }

    @Test
    void encodesVietnameseAndSpacesInFilterValues() {
        assertEquals("/manager/questions?search=C%C3%A2u%20h%E1%BB%8Fi",
                PageLinks.of("/manager/questions", "search", "Câu hỏi"));
    }

    @Test
    void withoutFiltersReturnsPlainPath() {
        assertEquals("/manager/exams/7/questions", PageLinks.of("/manager/exams/7/questions"));
    }
}
