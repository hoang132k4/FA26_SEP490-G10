package com.jlpp.modules.exam.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cấu trúc đề JLPT chính thức (N4, N5 theo bản rút gọn từ kỳ 12/2020).
 * Số câu là mặc định, manager có thể chỉnh từng phần khi tạo đề.
 */
public record JlptExamStructure(String level,
                                int languageQuestions,
                                int readingQuestions,
                                int listeningQuestions,
                                int durationMinutes,
                                int passingScore,
                                boolean languageReadingCombined) {

    public static final int TOTAL_SCORE = 180;
    public static final int SECTION_MIN_SCORE = 19;

    public static final String LANGUAGE = "LANGUAGE";
    public static final String READING = "READING";
    public static final String LISTENING = "LISTENING";

    public static final Map<String, String> SECTION_LABELS = Map.of(
            LANGUAGE, "Kiến thức ngôn ngữ (Từ vựng · Ngữ pháp)",
            READING, "Đọc hiểu",
            LISTENING, "Nghe hiểu");

    public static final List<String> SECTIONS = List.of(LANGUAGE, READING, LISTENING);

    private static final Map<String, JlptExamStructure> LEVELS = new LinkedHashMap<>();

    static {
        add(new JlptExamStructure("N5", 38, 5, 24, 90, 80, true));
        add(new JlptExamStructure("N4", 49, 8, 28, 115, 90, true));
        add(new JlptExamStructure("N3", 58, 16, 28, 140, 95, false));
        add(new JlptExamStructure("N2", 54, 21, 32, 155, 90, false));
        add(new JlptExamStructure("N1", 45, 25, 37, 165, 100, false));
    }

    private static void add(JlptExamStructure structure) {
        LEVELS.put(structure.level(), structure);
    }

    public static JlptExamStructure of(String level) {
        return LEVELS.getOrDefault(level, LEVELS.get("N5"));
    }

    public static Map<String, JlptExamStructure> all() {
        return LEVELS;
    }

    public int questions(String section) {
        return switch (section) {
            case READING -> readingQuestions;
            case LISTENING -> listeningQuestions;
            default -> languageQuestions;
        };
    }

    public int totalQuestions() {
        return languageQuestions + readingQuestions + listeningQuestions;
    }
}
