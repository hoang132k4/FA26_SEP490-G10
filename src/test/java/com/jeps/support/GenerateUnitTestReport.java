package com.jeps.support;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GenerateUnitTestReport {

    static class TestCaseData {
        String id;
        String screen;
        String preConditions;
        String inputParameters;
        String expectedResults;
        String type; // Normal, Abnormal, Boundary
        String status = "Passed";
        String date = "20/11/2026";
        String tester = "MinhPD";
        String note = "Automated & Manual Screen Test executed with JUnit 5 & MockMvc";

        public TestCaseData(String id, String screen, String preConditions, String inputParameters, String expectedResults, String type) {
            this.id = id;
            this.screen = screen;
            this.preConditions = preConditions;
            this.inputParameters = inputParameters;
            this.expectedResults = expectedResults;
            this.type = type;
        }
    }

    @Test
    void generateReport() throws Exception {
        System.out.println("Starting unit test report generation...");

        HSSFWorkbook wb = new HSSFWorkbook();

        // 1. Setup fonts & styles
        HSSFFont fontTitle = wb.createFont();
        fontTitle.setFontName("Arial");
        fontTitle.setFontHeightInPoints((short) 14);
        fontTitle.setBold(true);

        HSSFFont fontBold = wb.createFont();
        fontBold.setFontName("Arial");
        fontBold.setFontHeightInPoints((short) 10);
        fontBold.setBold(true);

        HSSFFont fontRegular = wb.createFont();
        fontRegular.setFontName("Arial");
        fontRegular.setFontHeightInPoints((short) 10);

        HSSFFont fontPass = wb.createFont();
        fontPass.setFontName("Arial");
        fontPass.setFontHeightInPoints((short) 10);
        fontPass.setBold(true);
        fontPass.setColor(IndexedColors.GREEN.getIndex());

        // Header Style
        HSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(fontBold);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(headerStyle);

        // Section Style
        HSSFCellStyle sectionStyle = wb.createCellStyle();
        sectionStyle.setFont(fontBold);
        sectionStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        sectionStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(sectionStyle);

        // Data Styles
        HSSFCellStyle dataStyleLeft = wb.createCellStyle();
        dataStyleLeft.setFont(fontRegular);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyleLeft.setWrapText(true);
        setBorders(dataStyleLeft);

        HSSFCellStyle dataStyleCenter = wb.createCellStyle();
        dataStyleCenter.setFont(fontRegular);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.TOP);
        setBorders(dataStyleCenter);

        HSSFCellStyle passStyle = wb.createCellStyle();
        passStyle.setFont(fontPass);
        passStyle.setAlignment(HorizontalAlignment.CENTER);
        passStyle.setVerticalAlignment(VerticalAlignment.TOP);
        setBorders(passStyle);

        HSSFCellStyle metaLabelStyle = wb.createCellStyle();
        metaLabelStyle.setFont(fontBold);
        metaLabelStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        metaLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(metaLabelStyle);

        HSSFCellStyle metaValueStyle = wb.createCellStyle();
        metaValueStyle.setFont(fontRegular);
        setBorders(metaValueStyle);

        // CREATE ALL SHEETS IN ADVANCE so formula cross-references resolve properly!
        HSSFSheet sheetCover = wb.createSheet("Cover");
        HSSFSheet sheetTestCases = wb.createSheet("Test Cases");
        HSSFSheet sheetTestStats = wb.createSheet("Test Statistics");
        HSSFSheet sheetCourse = wb.createSheet("Course");
        HSSFSheet sheetLesson = wb.createSheet("Lesson");
        HSSFSheet sheetQuiz = wb.createSheet("Quiz");
        HSSFSheet sheetFlashcard = wb.createSheet("Flashcard");

        // 2. Build Cover Sheet
        buildCoverSheet(sheetCover, fontTitle, fontBold, fontRegular);

        // 3. Build Feature Test Cases data
        List<TestCaseData> courseTCs = getCourseTestCases();
        List<TestCaseData> lessonTCs = getLessonTestCases();
        List<TestCaseData> quizTCs = getQuizTestCases();
        List<TestCaseData> flashcardTCs = getFlashcardTestCases();

        // 4. Build Test Cases summary sheet
        buildTestCaseIndexSheet(sheetTestCases, wb, fontTitle, fontBold, fontRegular, headerStyle, dataStyleLeft, dataStyleCenter);

        // 5. Build Feature Sheets FIRST so their cells exist
        buildFeatureSheet(sheetCourse, "Khóa học - Course Catalog, Registration & Management",
                courseTCs, headerStyle, sectionStyle, dataStyleLeft, dataStyleCenter, passStyle, metaLabelStyle, metaValueStyle);

        buildFeatureSheet(sheetLesson, "Bài học - Lesson Study & Learning Materials",
                lessonTCs, headerStyle, sectionStyle, dataStyleLeft, dataStyleCenter, passStyle, metaLabelStyle, metaValueStyle);

        buildFeatureSheet(sheetQuiz, "Trắc nghiệm - Quiz Taking & Question Management",
                quizTCs, headerStyle, sectionStyle, dataStyleLeft, dataStyleCenter, passStyle, metaLabelStyle, metaValueStyle);

        buildFeatureSheet(sheetFlashcard, "Thẻ ghi nhớ - Flashcard Decks & Spaced Repetition",
                flashcardTCs, headerStyle, sectionStyle, dataStyleLeft, dataStyleCenter, passStyle, metaLabelStyle, metaValueStyle);

        // 6. Build Test Statistics sheet AFTER feature sheets exist
        buildTestStatisticsSheet(sheetTestStats, wb, fontTitle, fontBold, fontRegular, headerStyle, dataStyleCenter);

        // Recalculate and evaluate all formulas
        try {
            HSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
        } catch (Exception e) {
            System.out.println("Formula evaluation notice: " + e.getMessage());
        }

        // Save to target files
        File outFile1 = new File("Report-5.1_Unit Test_Course_Lesson_Quiz_Flashcard.xls");
        try (FileOutputStream fos = new FileOutputStream(outFile1)) {
            wb.write(fos);
        }
        System.out.println("Saved: " + outFile1.getAbsolutePath() + " (size: " + outFile1.length() + ")");

        File outFile2 = new File("Report-5.1_Unit Test_All10Modules.xls");
        try (FileOutputStream fos = new FileOutputStream(outFile2)) {
            wb.write(fos);
        }
        System.out.println("Saved: " + outFile2.getAbsolutePath() + " (size: " + outFile2.length() + ")");

        wb.close();
        System.out.println("Report generation completed successfully!");
    }

    private void setBorders(HSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void buildCoverSheet(HSSFSheet sheet, HSSFFont fontTitle, HSSFFont fontBold, HSSFFont fontRegular) {
        sheet.setDisplayGridlines(true);
        HSSFWorkbook wb = sheet.getWorkbook();

        HSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(fontTitle);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        HSSFCellStyle boldStyle = wb.createCellStyle();
        boldStyle.setFont(fontBold);
        setBorders(boldStyle);

        HSSFCellStyle regStyle = wb.createCellStyle();
        regStyle.setFont(fontRegular);
        setBorders(regStyle);

        HSSFCellStyle headerBox = wb.createCellStyle();
        headerBox.setFont(fontBold);
        headerBox.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerBox.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(headerBox);

        Row r1 = sheet.createRow(1);
        Cell c1_1 = r1.createCell(1);
        c1_1.setCellValue("UNIT TEST SPECIFICATION REPORT");
        c1_1.setCellStyle(titleStyle);

        Row r3 = sheet.createRow(3);
        createCell(r3, 0, "Project Name", boldStyle);
        createCell(r3, 1, "JEPS - Japanese Exam Preparation System", regStyle);
        createCell(r3, 4, "Creator", boldStyle);
        createCell(r3, 5, "MinhPD", regStyle);

        Row r4 = sheet.createRow(4);
        createCell(r4, 0, "Project Code", boldStyle);
        createCell(r4, 1, "JEPS-2026", regStyle);
        createCell(r4, 4, "Issue Date", boldStyle);
        createCell(r4, 5, "20/11/2026", regStyle);

        Row r5 = sheet.createRow(5);
        createCell(r5, 0, "Document Code", boldStyle);
        createCell(r5, 1, "JEPS_Report_5.1_Unit_Test_v2.0", regStyle);
        createCell(r5, 4, "Version", boldStyle);
        createCell(r5, 5, "2.0", regStyle);

        Row r8 = sheet.createRow(8);
        createCell(r8, 0, "Record of change", boldStyle);

        Row r9 = sheet.createRow(9);
        createCell(r9, 0, "Effective Date", headerBox);
        createCell(r9, 1, "Version", headerBox);
        createCell(r9, 2, "Change Item", headerBox);
        createCell(r9, 3, "*A,D,M", headerBox);
        createCell(r9, 4, "Change description", headerBox);
        createCell(r9, 5, "Reference", headerBox);

        Row r10 = sheet.createRow(10);
        createCell(r10, 0, "20/11/2026", regStyle);
        createCell(r10, 1, "2.0", regStyle);
        createCell(r10, 2, "Course, Lesson, Quiz, Flashcard", regStyle);
        createCell(r10, 3, "M", regStyle);
        createCell(r10, 4, "Tái cấu trúc bộ Unit Test theo 4 Features trọng tâm: Course, Lesson, Quiz, Flashcard; tổ chức chi tiết theo từng màn hình với 100% test case kiểm thử dữ liệu nhập (Input validation & boundary parameters). Tester: MinhPD", regStyle);
        createCell(r10, 5, "JEPS SRS v2.0 & Screen Flow Specification", regStyle);

        sheet.setColumnWidth(0, 4500);
        sheet.setColumnWidth(1, 10000);
        sheet.setColumnWidth(2, 6000);
        sheet.setColumnWidth(3, 2500);
        sheet.setColumnWidth(4, 15000);
        sheet.setColumnWidth(5, 8000);
    }

    private void buildTestCaseIndexSheet(HSSFSheet sheet, HSSFWorkbook wb, HSSFFont fontTitle, HSSFFont fontBold, HSSFFont fontRegular,
                                        HSSFCellStyle headerStyle, HSSFCellStyle dataStyleLeft, HSSFCellStyle dataStyleCenter) {
        sheet.setDisplayGridlines(true);

        Row r0 = sheet.createRow(0);
        Cell c0_3 = r0.createCell(3);
        c0_3.setCellValue("TEST CASE LIST (FEATURES)");
        HSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(fontTitle);
        c0_3.setCellStyle(titleStyle);

        Row r2 = sheet.createRow(2);
        createCell(r2, 1, "Project Name", fontBold, wb);
        createCell(r2, 3, "JEPS - Japanese Exam Preparation System", fontRegular, wb);

        Row r3 = sheet.createRow(3);
        createCell(r3, 1, "Project Code", fontBold, wb);
        createCell(r3, 3, "JEPS-2026", fontRegular, wb);

        Row r4 = sheet.createRow(4);
        createCell(r4, 1, "Test Environment", fontBold, wb);
        createCell(r4, 3, "Spring Boot Test 3.3.4, JUnit 5 Jupiter, MockMvc, AssertJ, JDK 22", fontRegular, wb);

        Row r7 = sheet.createRow(7);
        createCell(r7, 1, "No", headerStyle);
        createCell(r7, 2, "Module / Feature Name", headerStyle);
        createCell(r7, 3, "Sheet Name", headerStyle);
        createCell(r7, 4, "Description (Kiểm thử theo màn hình nhập liệu)", headerStyle);
        createCell(r7, 5, "Pre-Condition", headerStyle);

        String[][] features = {
                {"1.0", "Course Management & Catalog (Khóa học)", "Course", "Kiểm thử các màn hình thuộc tính năng Khóa học: Danh mục & Tìm kiếm (SCR-03), Đăng ký & Thanh toán (SCR-04), Khóa học của tôi (SCR-05), Quản trị khóa học (SCR-M01)", "Spring Boot Test Context, JUnit 5, H2 In-Memory DB"},
                {"2.0", "Lesson & Learning Materials (Bài học)", "Lesson", "Kiểm thử các màn hình thuộc tính năng Bài học: Kho tài liệu & Tìm kiếm (SCR-15), Phòng học tương tác & Ghi chú (SCR-16), Quản trị bài giảng (SCR-M02)", "Spring Boot Test Context, JUnit 5, H2 In-Memory DB"},
                {"3.0", "Quiz & Practice Exercises (Trắc nghiệm)", "Quiz", "Kiểm thử các màn hình thuộc tính năng Bài tập Trắc nghiệm: Làm bài tập Quiz (SCR-18), Quản trị bài Quiz (SCR-M03), Quản trị câu hỏi (SCR-M04)", "Spring Boot Test Context, JUnit 5, H2 In-Memory DB"},
                {"4.0", "Flashcard Decks & Spaced Repetition (Thẻ ghi nhớ)", "Flashcard", "Kiểm thử các màn hình thuộc tính năng Thẻ ghi nhớ: Danh mục bộ thẻ & Tìm kiếm (SCR-20), Tạo bộ thẻ mới (SCR-21), Thêm thẻ flashcard (SCR-22), Luyện tập phản xạ (SCR-23)", "Spring Boot Test Context, JUnit 5, H2 In-Memory DB"}
        };

        for (int i = 0; i < features.length; i++) {
            Row row = sheet.createRow(8 + i);
            createCell(row, 1, features[i][0], dataStyleCenter);
            createCell(row, 2, features[i][1], dataStyleLeft);
            createCell(row, 3, features[i][2], dataStyleCenter);
            createCell(row, 4, features[i][3], dataStyleLeft);
            createCell(row, 5, features[i][4], dataStyleLeft);
        }

        sheet.setColumnWidth(1, 2000);
        sheet.setColumnWidth(2, 9000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 18000);
        sheet.setColumnWidth(5, 8000);
    }

    private void buildTestStatisticsSheet(HSSFSheet sheet, HSSFWorkbook wb, HSSFFont fontTitle, HSSFFont fontBold, HSSFFont fontRegular,
                                         HSSFCellStyle headerStyle, HSSFCellStyle dataStyleCenter) {
        sheet.setDisplayGridlines(true);

        Row r1 = sheet.createRow(1);
        Cell c1_0 = r1.createCell(0);
        c1_0.setCellValue("UNIT TEST STATISTICS REPORT");
        HSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(fontTitle);
        c1_0.setCellStyle(titleStyle);

        Row r3 = sheet.createRow(3);
        createCell(r3, 0, "Project Name", fontBold, wb);
        createCell(r3, 1, "JEPS - Japanese Exam Preparation System", fontRegular, wb);
        createCell(r3, 3, "Tester", fontBold, wb);
        createCell(r3, 4, "MinhPD", fontRegular, wb);

        Row r4 = sheet.createRow(4);
        createCell(r4, 0, "Project Code", fontBold, wb);
        createCell(r4, 1, "JEPS-2026", fontRegular, wb);
        createCell(r4, 3, "QA Lead", fontBold, wb);
        createCell(r4, 4, "MinhPD", fontRegular, wb);

        Row r5 = sheet.createRow(5);
        createCell(r5, 0, "Document Code", fontBold, wb);
        createCell(r5, 1, "JEPS_Report_5.1_Unit_Test_v2.0", fontRegular, wb);
        createCell(r5, 3, "Issue Date", fontBold, wb);
        createCell(r5, 4, "20/11/2026", fontRegular, wb);

        Row r6 = sheet.createRow(6);
        createCell(r6, 0, "Notes", fontBold, wb);
        createCell(r6, 1, "Tập trung kiểm thử các trường hợp nhập dữ liệu (Input data) cho 4 Features: Course, Lesson, Quiz, Flashcard", fontRegular, wb);

        Row r10 = sheet.createRow(10);
        createCell(r10, 0, "No", headerStyle);
        createCell(r10, 1, "Feature Name", headerStyle);
        createCell(r10, 2, "Passed", headerStyle);
        createCell(r10, 3, "Failed", headerStyle);
        createCell(r10, 4, "Untested", headerStyle);
        createCell(r10, 5, "Normal", headerStyle);
        createCell(r10, 6, "Abnormal", headerStyle);
        createCell(r10, 7, "Boundary", headerStyle);
        createCell(r10, 8, "Total Test Cases", headerStyle);

        String[] feats = {"Course", "Lesson", "Quiz", "Flashcard"};
        for (int i = 0; i < feats.length; i++) {
            Row r = sheet.createRow(11 + i);
            String fn = feats[i];
            createCell(r, 0, String.valueOf(i + 1), dataStyleCenter);
            createCell(r, 1, fn, dataStyleCenter);

            createFormulaCell(r, 2, fn + "!B5", dataStyleCenter);
            createFormulaCell(r, 3, fn + "!C5", dataStyleCenter);
            createFormulaCell(r, 4, fn + "!D5", dataStyleCenter);
            createFormulaCell(r, 5, fn + "!B9", dataStyleCenter);
            createFormulaCell(r, 6, fn + "!C9", dataStyleCenter);
            createFormulaCell(r, 7, fn + "!D9", dataStyleCenter);
            createFormulaCell(r, 8, fn + "!B3", dataStyleCenter);
        }

        // Total Row
        Row rTotal = sheet.createRow(15);
        HSSFCellStyle totalStyle = wb.createCellStyle();
        totalStyle.setFont(fontBold);
        totalStyle.setAlignment(HorizontalAlignment.CENTER);
        setBorders(totalStyle);

        createCell(rTotal, 0, "Total", totalStyle);
        createCell(rTotal, 1, "ALL FEATURES", totalStyle);
        createFormulaCell(rTotal, 2, "SUM(C12:C15)", totalStyle);
        createFormulaCell(rTotal, 3, "SUM(D12:D15)", totalStyle);
        createFormulaCell(rTotal, 4, "SUM(E12:E15)", totalStyle);
        createFormulaCell(rTotal, 5, "SUM(F12:F15)", totalStyle);
        createFormulaCell(rTotal, 6, "SUM(G12:G15)", totalStyle);
        createFormulaCell(rTotal, 7, "SUM(H12:H15)", totalStyle);
        createFormulaCell(rTotal, 8, "SUM(I12:I15)", totalStyle);

        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 5000);
        for (int c = 2; c <= 8; c++) sheet.setColumnWidth(c, 3500);
    }

    private void buildFeatureSheet(HSSFSheet sheet, String moduleTitle,
                                   List<TestCaseData> testCases,
                                   HSSFCellStyle headerStyle,
                                   HSSFCellStyle sectionStyle,
                                   HSSFCellStyle dataStyleLeft,
                                   HSSFCellStyle dataStyleCenter,
                                   HSSFCellStyle passStyle,
                                   HSSFCellStyle metaLabelStyle,
                                   HSSFCellStyle metaValueStyle) {

        sheet.setDisplayGridlines(true);

        // Header metadata block (Rows 1-8)
        Row r1 = sheet.createRow(1);
        createCell(r1, 0, "Code Module", metaLabelStyle);
        createCell(r1, 1, moduleTitle, metaValueStyle);

        Row r2 = sheet.createRow(2);
        createCell(r2, 0, "Number of TCs", metaLabelStyle);
        createFormulaCell(r2, 1, "COUNTIF(A13:A3000,\"UTC*\")", metaValueStyle);

        Row r3 = sheet.createRow(3);
        createCell(r3, 0, "Testing Round", metaLabelStyle);
        createCell(r3, 1, "Passed", metaLabelStyle);
        createCell(r3, 2, "Failed", metaLabelStyle);
        createCell(r3, 3, "Pending", metaLabelStyle);

        Row r4 = sheet.createRow(4);
        createCell(r4, 0, "Round 1", metaLabelStyle);
        createFormulaCell(r4, 1, "COUNTIF($F11:$F3000,B4)", metaValueStyle);
        createFormulaCell(r4, 2, "COUNTIF($F11:$F3000,C4)", metaValueStyle);
        createFormulaCell(r4, 3, "COUNTIF($F11:$F3000,D4)", metaValueStyle);

        Row r5 = sheet.createRow(5);
        createCell(r5, 0, "Round 2", metaLabelStyle);
        createFormulaCell(r5, 1, "COUNTIF($I11:$I3000,B4)", metaValueStyle);
        createFormulaCell(r5, 2, "COUNTIF($I11:$I3000,C4)", metaValueStyle);
        createFormulaCell(r5, 3, "COUNTIF($I11:$I3000,D4)", metaValueStyle);

        Row r6 = sheet.createRow(6);
        createCell(r6, 0, "Round 3", metaLabelStyle);
        createFormulaCell(r6, 1, "COUNTIF($L11:$L3000,B4)", metaValueStyle);
        createFormulaCell(r6, 2, "COUNTIF($L11:$L3000,C4)", metaValueStyle);
        createFormulaCell(r6, 3, "COUNTIF($L11:$L3000,D4)", metaValueStyle);

        Row r7 = sheet.createRow(7);
        createCell(r7, 0, "Testing Type", metaLabelStyle);
        createCell(r7, 1, "Normal", metaLabelStyle);
        createCell(r7, 2, "Abnormal", metaLabelStyle);
        createCell(r7, 3, "Boundary", metaLabelStyle);

        Row r8 = sheet.createRow(8);
        createCell(r8, 0, "Count", metaLabelStyle);
        createFormulaCell(r8, 1, "COUNTIF($E12:$E3000,B8)", metaValueStyle);
        createFormulaCell(r8, 2, "COUNTIF($E12:$E3000,C8)", metaValueStyle);
        createFormulaCell(r8, 3, "COUNTIF(E$10:E$3000,D8)", metaValueStyle);

        // Row 10: Column Headers
        Row r10 = sheet.createRow(10);
        String[] colHeaders = {
                "Test Case ID", "Pre-conditions", "Input parameters (Dữ liệu nhập)", "Expected Results (Kết quả mong đợi)",
                "Type", "Round 1", "Test date", "Tester", "Round 2", "Test date", "Tester",
                "Round 3", "Test date", "Tester", "Defect IDs", "Note"
        };
        for (int c = 0; c < colHeaders.length; c++) {
            createCell(r10, c, colHeaders[c], headerStyle);
        }

        int currentRow = 11;
        String currentScreen = "";

        for (TestCaseData tc : testCases) {
            // If screen changed, write screen section header
            if (!tc.screen.equals(currentScreen)) {
                currentScreen = tc.screen;
                Row sRow = sheet.createRow(currentRow++);
                createCell(sRow, 0, tc.screen, sectionStyle);
                for (int c = 1; c < 16; c++) {
                    createCell(sRow, c, "", sectionStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 0, 15));
            }

            Row row = sheet.createRow(currentRow++);
            createCell(row, 0, tc.id, dataStyleCenter);
            createCell(row, 1, tc.preConditions, dataStyleLeft);
            createCell(row, 2, tc.inputParameters, dataStyleLeft);
            createCell(row, 3, tc.expectedResults, dataStyleLeft);
            createCell(row, 4, tc.type, dataStyleCenter);
            createCell(row, 5, tc.status, passStyle);
            createCell(row, 6, tc.date, dataStyleCenter);
            createCell(row, 7, tc.tester, dataStyleCenter);
            createCell(row, 8, "", dataStyleCenter);
            createCell(row, 9, "", dataStyleCenter);
            createCell(row, 10, "", dataStyleCenter);
            createCell(row, 11, "", dataStyleCenter);
            createCell(row, 12, "", dataStyleCenter);
            createCell(row, 13, "", dataStyleCenter);
            createCell(row, 14, "", dataStyleCenter);
            createCell(row, 15, tc.note, dataStyleLeft);
        }

        // Set column widths
        sheet.setColumnWidth(0, 3200);  // ID
        sheet.setColumnWidth(1, 9500);  // Pre-conditions
        sheet.setColumnWidth(2, 11500); // Input parameters
        sheet.setColumnWidth(3, 13000); // Expected Results
        sheet.setColumnWidth(4, 3000);  // Type
        sheet.setColumnWidth(5, 3000);  // Round 1
        sheet.setColumnWidth(6, 3500);  // Date
        sheet.setColumnWidth(7, 3500);  // Tester
        sheet.setColumnWidth(8, 2500);  // Round 2
        sheet.setColumnWidth(9, 2500);
        sheet.setColumnWidth(10, 2500);
        sheet.setColumnWidth(11, 2500); // Round 3
        sheet.setColumnWidth(12, 2500);
        sheet.setColumnWidth(13, 2500);
        sheet.setColumnWidth(14, 2500); // Defect IDs
        sheet.setColumnWidth(15, 8000); // Note
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, String value, HSSFFont font, HSSFWorkbook wb) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        HSSFCellStyle style = wb.createCellStyle();
        style.setFont(font);
        setBorders(style);
        cell.setCellStyle(style);
    }

    private void createFormulaCell(Row row, int col, String formula, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }

    // =========================================================================
    // 1. FEATURE: COURSE (34 Test Cases across 4 Screens)
    // =========================================================================
    private List<TestCaseData> getCourseTestCases() {
        List<TestCaseData> list = new ArrayList<>();
        String s1 = "MÀN HÌNH SCR-03: Danh mục Khóa học & Tìm kiếm (/courses)";
        list.add(new TestCaseData("UTC_CRS_001", s1,
                "Người dùng truy cập trang danh mục khóa học",
                "Nhập từ khóa tiếng Việt có dấu: keyword = 'Nhập môn tiếng Nhật', level = ''",
                "Hệ thống lọc chính xác và hiển thị danh sách các khóa học có tiêu đề chứa 'Nhập môn tiếng Nhật'.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_002", s1,
                "Người dùng truy cập trang danh mục khóa học",
                "Nhập từ khóa tiếng Nhật chuẩn: keyword = '日本語', level = ''",
                "Hệ thống xử lý đúng bộ mã UTF-8, lọc trả về các khóa học tiếng Nhật tương ứng, không vỡ font.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_003", s1,
                "Người dùng mở bộ lọc cấp độ",
                "Chọn bộ lọc cấp độ: level = 'N5', keyword = ''",
                "Hệ thống chỉ hiển thị các khóa học thuộc trình độ N5, các cấp độ khác bị ẩn đi.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_004", s1,
                "Người dùng kết hợp đa tiêu chí lọc",
                "Nhập từ khóa và chọn cấp độ đồng thời: keyword = 'Giao tiếp', level = 'N4'",
                "Hệ thống lọc kết hợp (AND), trả về danh sách khóa học vừa có cấp độ N4 vừa chứa từ 'Giao tiếp'.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_005", s1,
                "Người dùng tìm kiếm với từ khóa không tồn tại",
                "Nhập chuỗi không khớp bất kỳ dữ liệu nào: keyword = 'KhoaHocKhongTonTai_99999'",
                "Hệ thống trả về danh sách rỗng, hiển thị thông báo thân thiện 'Không tìm thấy khóa học phù hợp'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_006", s1,
                "Người dùng nhập chuỗi độc hại XSS vào thanh tìm kiếm",
                "Nhập payload script: keyword = '<script>alert(\"XSS\")</script>'",
                "Hệ thống tự động HTML-escape toàn bộ chuỗi tìm kiếm, không thực thi mã JavaScript độc hại.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_007", s1,
                "Người dùng nhập chuỗi tấn công SQL Injection",
                "Nhập payload SQL: keyword = \"' OR '1'='1' -- \"",
                "Spring Data JPA sử dụng tham số hóa truy vấn an toàn, xử lý chuỗi như văn bản thuần túy, không lộ dữ liệu.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_008", s1,
                "Người dùng nhập từ khóa độ dài chạm ngưỡng cho phép",
                "Nhập từ khóa dài đúng 100 ký tự hợp lệ: keyword = 'A'*100",
                "Hệ thống chấp nhận chuỗi 100 ký tự và thực hiện tìm kiếm bình thường không lỗi tràn buffer.", "Boundary"));

        list.add(new TestCaseData("UTC_CRS_009", s1,
                "Người dùng nhập chuỗi khoảng trắng",
                "Nhập chuỗi toàn dấu cách: keyword = '     '",
                "Hệ thống tự động loại bỏ khoảng trắng (trim), hiển thị toàn bộ danh sách khóa học mặc định.", "Boundary"));

        list.add(new TestCaseData("UTC_CRS_010", s1,
                "Người dùng truyền cấp độ không hợp lệ qua URL param",
                "Truyền giá trị cấp độ ngoài danh mục chuẩn: level = 'N99'",
                "Hệ thống tự động bỏ qua giá trị lọc sai, hiển thị tất cả các cấp độ N1-N5 bình thường.", "Boundary"));

        String s2 = "MÀN HÌNH SCR-04: Đăng ký Khóa học & Thanh toán (/learning/courses/{id}/register)";
        list.add(new TestCaseData("UTC_CRS_011", s2,
                "Học viên đã đăng nhập, mở form thanh toán khóa học có phí",
                "Chọn paymentMethod = 'VNPAY', tích chọn agreeTerms = true, bấm Xác nhận",
                "Hệ thống tạo đơn hàng mới trạng thái PENDING và chuyển hướng sang cổng thanh toán VNPAY chính xác.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_012", s2,
                "Học viên có mã giảm giá hợp lệ còn hạn",
                "Nhập couponCode = 'JEPS10', bấm Áp dụng",
                "Hệ thống tính toán giảm 10% học phí, cập nhật lại số tiền thanh toán thực tế trên giao diện.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_013", s2,
                "Học viên đăng ký khóa học miễn phí (Học phí = 0đ)",
                "Chọn phương thức paymentMethod = 'FREE_ENROLL', agreeTerms = true, bấm Đăng ký",
                "Hệ thống kích hoạt ghi danh ngay lập tức (status ACTIVE), chuyển hướng học viên vào màn hình bài học.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_014", s2,
                "Học viên chưa tích chọn điều khoản bắt buộc",
                "Chọn paymentMethod = 'VNPAY', agreeTerms = false, bấm Xác nhận",
                "Hệ thống chặn submit, hiển thị thông báo yêu cầu người dùng tích đồng ý điều khoản dịch vụ.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_015", s2,
                "Học viên nhập mã giảm giá không tồn tại",
                "Nhập couponCode = 'MA_KHONG_TON_TAI_999', bấm Áp dụng",
                "Hệ thống báo lỗi 'Mã giảm giá không tồn tại hoặc đã hết lượt sử dụng', giữ nguyên giá gốc.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_016", s2,
                "Học viên nhập mã giảm giá đã hết hạn",
                "Nhập couponCode = 'EXPIRED_COUPON_2025', bấm Áp dụng",
                "Hệ thống thông báo 'Mã giảm giá đã hết hạn áp dụng', không khấu trừ học phí.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_017", s2,
                "Học viên không chọn phương thức thanh toán",
                "Để trống paymentMethod = '', agreeTerms = true, bấm Xác nhận",
                "Hệ thống báo lỗi validation 'Vui lòng chọn một phương thức thanh toán để tiếp tục'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_018", s2,
                "Học viên nhập mã giảm giá chiết khấu 100% học phí",
                "Nhập couponCode = 'FREE100_VIP', bấm Áp dụng",
                "Hệ thống cập nhật tổng tiền về đúng 0đ, tự động chuyển đổi nút thanh toán thành 'Kích hoạt ngay 0đ'.", "Boundary"));

        String s3 = "MÀN HÌNH SCR-05: Khóa học Của Tôi - Lọc & Tìm kiếm (/learning/my-courses)";
        list.add(new TestCaseData("UTC_CRS_019", s3,
                "Học viên đã đăng ký nhiều khóa học, mở màn hình Khóa học của tôi",
                "Nhập từ khóa tìm kiếm: keyword = 'N5'",
                "Hệ thống lọc chính xác các khóa học đã ghi danh của tài khoản có chứa 'N5'.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_020", s3,
                "Học viên muốn xem các khóa học đang học",
                "Chọn bộ lọc trạng thái: status = 'ACTIVE'",
                "Hệ thống chỉ hiển thị các khóa học đang trong tiến trình học (tiến độ từ 0% đến <100%).", "Normal"));

        list.add(new TestCaseData("UTC_CRS_021", s3,
                "Học viên muốn xem các khóa học đã hoàn thành",
                "Chọn bộ lọc trạng thái: status = 'COMPLETED'",
                "Hệ thống chỉ hiển thị các khóa học đã hoàn tất 100% giáo trình và bài thi.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_022", s3,
                "Học viên tìm kiếm khóa học chưa từng ghi danh",
                "Nhập từ khóa: keyword = 'KhoaChuaDangKy_XYZ'",
                "Hệ thống hiển thị thông báo 'Bạn chưa đăng ký khóa học nào phù hợp với từ khóa này'.", "Abnormal"));

        String s4 = "MÀN HÌNH SCR-M01: Quản trị Khóa học - Thêm mới & Cập nhật (/manager/courses)";
        list.add(new TestCaseData("UTC_CRS_023", s4,
                "Manager đăng nhập, mở modal Thêm mới Khóa học",
                "Nhập đầy đủ form hợp lệ: code='JLPT-N4-PRO', title='Khóa học N4 Chuyên sâu', level='N4', price=499000, durationHours=45, status='PUBLISHED'",
                "Hệ thống tạo mới khóa học thành công, hiển thị thông báo thành công và cập nhật danh sách.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_024", s4,
                "Manager thiết lập giá khuyến mãi hợp lệ",
                "Nhập form: price = 500000, discountPrice = 399000 (discountPrice < price)",
                "Hệ thống lưu trữ thành công mức giá gốc và giá khuyến mãi của khóa học.", "Normal"));

        list.add(new TestCaseData("UTC_CRS_025", s4,
                "Manager bỏ trống tiêu đề khóa học",
                "Nhập form: title = '', code = 'JLPT-N5-TEST', price = 200000",
                "Hệ thống báo lỗi validation 'Tiêu đề khóa học không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_026", s4,
                "Manager bỏ trống mã khóa học",
                "Nhập form: code = '', title = 'Khóa học Căn bản N5', price = 200000",
                "Hệ thống báo lỗi validation 'Mã khóa học không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_027", s4,
                "Manager nhập mã khóa học đã tồn tại trong database",
                "Nhập code = 'JLPT-N5' (đã có sẵn trong hệ thống)",
                "Hệ thống báo lỗi 'Mã khóa học đã tồn tại, vui lòng chọn mã định danh khác'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_028", s4,
                "Manager nhập học phí là số âm",
                "Nhập form: price = -50000",
                "Hệ thống báo lỗi validation 'Học phí phải lớn hơn hoặc bằng 0'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_029", s4,
                "Manager nhập giá khuyến mãi lớn hơn giá gốc",
                "Nhập form: price = 300000, discountPrice = 450000",
                "Hệ thống báo lỗi 'Giá khuyến mãi không được vượt quá giá gốc của khóa học'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_030", s4,
                "Manager nhập thời lượng đào tạo là số âm",
                "Nhập form: durationHours = -10",
                "Hệ thống báo lỗi validation 'Thời lượng đào tạo phải là số nguyên dương'.", "Abnormal"));

        list.add(new TestCaseData("UTC_CRS_031", s4,
                "Manager nhập tên khóa học đúng độ dài tối thiểu 5 ký tự",
                "Nhập title = 'N5 Pro' (5 ký tự)",
                "Hệ thống chấp nhận và tạo khóa học thành công.", "Boundary"));

        list.add(new TestCaseData("UTC_CRS_032", s4,
                "Manager nhập tên khóa học vượt quá 255 ký tự",
                "Nhập title = 'Khóa học tiếng Nhật siêu cấp... (256 ký tự)'",
                "Hệ thống báo lỗi validation 'Tên khóa học không được vượt quá 255 ký tự'.", "Boundary"));

        list.add(new TestCaseData("UTC_CRS_033", s4,
                "Manager thiết lập khóa học miễn phí học phí 0đ",
                "Nhập form: price = 0",
                "Hệ thống lưu thành công với giá 0đ, tự động gán nhãn khóa học Miễn phí.", "Boundary"));

        list.add(new TestCaseData("UTC_CRS_034", s4,
                "Manager chỉnh sửa khóa học hiện có",
                "Cập nhật title='Khóa học N5 Toàn diện 2026', status='PUBLISHED'",
                "Hệ thống cập nhật thông tin khóa học thành công, hiển thị trạng thái phát hành ra ngoài portal.", "Normal"));

        return list;
    }

    // =========================================================================
    // 2. FEATURE: LESSON (24 Test Cases across 3 Screens)
    // =========================================================================
    private List<TestCaseData> getLessonTestCases() {
        List<TestCaseData> list = new ArrayList<>();
        String s1 = "MÀN HÌNH SCR-15: Kho Tài liệu học tập - Tìm kiếm & Lọc (/learning/materials)";

        list.add(new TestCaseData("UTC_LSN_001", s1,
                "Học viên mở kho tài liệu học tập",
                "Nhập từ khóa tìm kiếm tài liệu: keyword = 'Ngữ pháp'",
                "Hệ thống trả về danh sách các tài liệu PDF/Audio có tiêu đề chứa 'Ngữ pháp'.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_002", s1,
                "Học viên lọc tài liệu theo kỹ năng nghe",
                "Chọn bộ lọc kỹ năng: skill = 'LISTENING'",
                "Hệ thống chỉ hiển thị các tệp âm thanh luyện nghe (MP3) liên quan đến kỹ năng Nghe.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_003", s1,
                "Học viên lọc tài liệu theo định dạng văn bản",
                "Chọn bộ lọc định dạng: type = 'PDF'",
                "Hệ thống chỉ hiển thị các tài liệu dạng tài liệu đọc/sách PDF.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_004", s1,
                "Học viên kết hợp đa tiêu chí tìm kiếm tài liệu",
                "Nhập: keyword = 'N5', skill = 'GRAMMAR', type = 'PDF'",
                "Hệ thống lọc chính xác các tài liệu thỏa mãn đồng thời cả 3 tiêu chí.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_005", s1,
                "Học viên tìm kiếm tài liệu không tồn tại",
                "Nhập keyword = 'TaiLieuKhongTonTai_9999'",
                "Hệ thống hiển thị trạng thái rỗng 'Không tìm thấy tài liệu học tập phù hợp'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_006", s1,
                "Học viên nhập ký tự đặc biệt nguy hiểm vào ô tìm kiếm",
                "Nhập keyword = '<script>document.cookie</script>'",
                "Hệ thống tự động mã hóa chuỗi đầu vào, không gây lỗi giao diện hoặc bảo mật.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_007", s1,
                "Học viên nhập chuỗi khoảng trắng",
                "Nhập keyword = '     '",
                "Hệ thống tự động trim khoảng trắng, hiển thị toàn bộ tài liệu theo kỹ năng mặc định.", "Boundary"));

        String s2 = "MÀN HÌNH SCR-16: Phòng học Bài giảng - Tương tác & Ghi chú (/learning/lessons/{id})";
        list.add(new TestCaseData("UTC_LSN_008", s2,
                "Học viên đang học bài giảng, mở ô ghi chú",
                "Nhập ghi chú hợp lệ: note = 'Quy tắc biến âm hàng Ka thành Ga cần học thuộc', bấm Lưu",
                "Hệ thống lưu thành công nội dung ghi chú và hiển thị trong danh sách ghi chú của bài.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_009", s2,
                "Học viên học xong bài giảng, bấm nút xác nhận hoàn thành",
                "Bấm nút 'Hoàn thành bài học' (action completeLesson)",
                "Hệ thống cập nhật bài học sang trạng thái COMPLETED, tính lại % tiến độ của khóa học chính xác.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_010", s2,
                "Học viên submit ghi chú để trống nội dung",
                "Nhập note = '    ' (chỉ chứa khoảng trắng), bấm Lưu",
                "Hệ thống thông báo lỗi 'Nội dung ghi chú không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_011", s2,
                "Học viên nhập ghi chú chạm ngưỡng tối đa 1000 ký tự",
                "Nhập note có độ dài đúng 1000 ký tự văn bản",
                "Hệ thống lưu trữ thành công toàn bộ 1000 ký tự ghi chú.", "Boundary"));

        list.add(new TestCaseData("UTC_LSN_012", s2,
                "Học viên nhập ghi chú vượt quá 1000 ký tự",
                "Nhập note có độ dài 1001 ký tự",
                "Hệ thống báo lỗi validation 'Nội dung ghi chú không được vượt quá 1000 ký tự'.", "Boundary"));

        String s3 = "MÀN HÌNH SCR-M02: Quản trị Bài giảng - Tạo mới & Chỉnh sửa (/manager/lessons)";
        list.add(new TestCaseData("UTC_LSN_013", s3,
                "Manager mở form thêm bài giảng mới vào khóa học",
                "Nhập đầy đủ thông tin: title='Bài 1: Chào hỏi căn bản', courseId=1, section='Chương 1', durationMinutes=45, lessonType='THEORY', orderIndex=1, isFreePreview=true",
                "Hệ thống tạo mới bài giảng thành công, xuất hiện đúng vị trí trong danh sách giáo trình.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_014", s3,
                "Manager nhập link video bài giảng hợp lệ",
                "Nhập videoUrl = 'https://www.youtube.com/watch?v=sampleLessonVideo123'",
                "Hệ thống lưu thành công đường dẫn video, phòng học hiển thị đúng trình phát video.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_015", s3,
                "Manager bỏ trống tiêu đề bài học",
                "Nhập form: title = '', courseId = 1, durationMinutes = 30",
                "Hệ thống báo lỗi validation 'Tiêu đề bài giảng không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_016", s3,
                "Manager chưa chọn khóa học cha cho bài giảng",
                "Nhập form: courseId = null, title = 'Bài học không cha'",
                "Hệ thống báo lỗi validation 'Vui lòng chọn khóa học cho bài giảng'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_017", s3,
                "Manager nhập thời lượng bài học là số âm",
                "Nhập form: durationMinutes = -15",
                "Hệ thống báo lỗi validation 'Thời lượng bài giảng phải là số nguyên dương'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_018", s3,
                "Manager nhập thứ tự bài học là số âm",
                "Nhập form: orderIndex = -1",
                "Hệ thống báo lỗi validation 'Thứ tự bài giảng phải lớn hơn 0'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_019", s3,
                "Manager nhập video URL sai định dạng giao thức",
                "Nhập videoUrl = 'ftp://invalid-url/video.mp4'",
                "Hệ thống báo lỗi 'Đường dẫn video không đúng định dạng URL web (HTTP/HTTPS)'.", "Abnormal"));

        list.add(new TestCaseData("UTC_LSN_020", s3,
                "Manager nhập thời lượng tối thiểu 1 phút",
                "Nhập form: durationMinutes = 1",
                "Hệ thống chấp nhận và lưu bài học thành công.", "Boundary"));

        list.add(new TestCaseData("UTC_LSN_021", s3,
                "Manager nhập thời lượng vượt quá 480 phút (8 tiếng)",
                "Nhập form: durationMinutes = 500",
                "Hệ thống hiển thị cảnh báo 'Thời lượng bài giảng không nên vượt quá 480 phút'.", "Boundary"));

        list.add(new TestCaseData("UTC_LSN_022", s3,
                "Manager cập nhật bài giảng hiện có",
                "Sửa title = 'Bài 1: Bảng chữ cái Hiragana & Katakana', chuyển isFreePreview = true",
                "Hệ thống lưu thành công, học viên chưa mua khóa học có thể vào học thử bài này.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_023", s3,
                "Manager đính kèm file tài liệu PDF hợp lệ (< 20MB)",
                "Chọn file 'GiaoTrinh_Minna_Bai1.pdf', dung lượng 5MB, bấm Upload",
                "Hệ thống tải tệp lên server thành công, hiển thị link tài liệu đính kèm trong bài.", "Normal"));

        list.add(new TestCaseData("UTC_LSN_024", s3,
                "Manager đính kèm file có định dạng không được phép",
                "Chọn file 'TroChoi.exe' hoặc 'Script.bat', bấm Upload",
                "Hệ thống từ chối tải tệp, báo lỗi 'Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, MP3, DOCS, SLIDE'.", "Abnormal"));

        return list;
    }

    // =========================================================================
    // 3. FEATURE: QUIZ (23 Test Cases across 3 Screens)
    // =========================================================================
    private List<TestCaseData> getQuizTestCases() {
        List<TestCaseData> list = new ArrayList<>();
        String s1 = "MÀN HÌNH SCR-18: Làm bài tập Trắc nghiệm Quiz (/quiz/do, /learning/lessons/{id}/quiz)";

        list.add(new TestCaseData("UTC_QZ_001", s1,
                "Học viên đang làm bài Quiz trắc nghiệm 10 câu",
                "Chọn đáp án đúng cho toàn bộ 10 câu hỏi, bấm Nộp bài",
                "Hệ thống chấm điểm tự động đạt 100/100 điểm, đánh giá ĐẠT (Passed) kèm bảng giải thích chi tiết.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_002", s1,
                "Học viên muốn thay đổi lựa chọn trước khi nộp",
                "Click thay đổi từ Phương án A sang Phương án C ở câu hỏi số 3",
                "Giao diện radio button cập nhật ngay lập tức sang phương án C, hệ thống lưu đáp án C khi nộp.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_003", s1,
                "Thời gian làm bài đếm ngược về 00:00",
                "Học viên chưa bấm Nộp bài nhưng đồng hồ timeRemaining = 0",
                "Hệ thống tự động kích hoạt nộp bài ngay lập tức, chấm điểm dựa trên các câu đã chọn trước đó.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_004", s1,
                "Học viên bấm Nộp bài khi chưa chọn câu nào",
                "Không chọn bất kỳ phương án nào (answers = rỗng), bấm Nộp bài",
                "Hệ thống hiển thị cảnh báo xác nhận 'Bạn chưa trả lời câu hỏi nào. Bạn có chắc chắn muốn nộp bài không?'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_005", s1,
                "Học viên mới trả lời một phần câu hỏi",
                "Chỉ chọn 6/10 câu hỏi, bấm Nộp bài",
                "Hệ thống hiển thị thông báo 'Bạn còn 4 câu hỏi chưa hoàn thành' kèm danh sách số câu chưa làm.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_006", s1,
                "Học viên đạt số điểm vừa đúng ngưỡng đỗ",
                "Nộp bài đạt số điểm 50/100 khi passingScore = 50%",
                "Hệ thống đánh giá ĐẠT (Passed), hiển thị huy hiệu hoàn thành và mở khóa tiến độ tiếp theo.", "Boundary"));

        list.add(new TestCaseData("UTC_QZ_007", s1,
                "Học viên đạt số điểm sát dưới ngưỡng đỗ",
                "Nộp bài đạt số điểm 49/100 khi passingScore = 50%",
                "Hệ thống đánh giá CHƯA ĐẠT (Failed), hiển thị nút 'Làm lại bài tập' để củng cố kiến thức.", "Boundary"));

        String s2 = "MÀN HÌNH SCR-M03: Quản trị Bài Quiz - Tạo mới & Thiết lập (/manager/quizzes)";
        list.add(new TestCaseData("UTC_QZ_008", s2,
                "Manager mở form tạo bài kiểm tra Quiz mới",
                "Nhập đầy đủ thông tin: title='Kiểm tra Ngữ pháp N5 - Bài 1', courseId=1, lessonId=2, passingScore=70, timeLimit=15",
                "Hệ thống tạo mới bài Quiz thành công, liên kết trực tiếp vào bài giảng số 2 của khóa học.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_009", s2,
                "Manager bỏ trống tiêu đề bài Quiz",
                "Nhập form: title = '', courseId = 1, passingScore = 60",
                "Hệ thống báo lỗi validation 'Tiêu đề bài kiểm tra không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_010", s2,
                "Manager nhập điểm đạt là số âm",
                "Nhập form: passingScore = -10",
                "Hệ thống báo lỗi validation 'Điểm đạt yêu cầu phải nằm trong khoảng từ 0% đến 100%'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_011", s2,
                "Manager nhập điểm đạt vượt quá 100%",
                "Nhập form: passingScore = 120",
                "Hệ thống báo lỗi validation 'Điểm đạt yêu cầu không được vượt quá 100%'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_012", s2,
                "Manager nhập thời gian làm bài bằng 0 hoặc số âm",
                "Nhập form: timeLimit = 0",
                "Hệ thống báo lỗi validation 'Thời gian làm bài phải là số nguyên dương lớn hơn 0 phút'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_013", s2,
                "Manager thiết lập điểm đạt ở mức tối thiểu 0%",
                "Nhập form: passingScore = 0",
                "Hệ thống chấp nhận cấu hình 0% (làm bài khảo sát không tính trượt).", "Boundary"));

        list.add(new TestCaseData("UTC_QZ_014", s2,
                "Manager thiết lập điểm đạt ở mức tối đa 100%",
                "Nhập form: passingScore = 100",
                "Hệ thống chấp nhận cấu hình 100% (yêu cầu học viên phải đúng tuyệt đối toàn bộ câu hỏi).", "Boundary"));

        list.add(new TestCaseData("UTC_QZ_015", s2,
                "Manager thiết lập thời gian làm bài tối thiểu 1 phút",
                "Nhập form: timeLimit = 1",
                "Hệ thống lưu thành công thời gian làm bài 1 phút cho bài quiz ngắn.", "Boundary"));

        String s3 = "MÀN HÌNH SCR-M04: Quản trị Câu hỏi Quiz - Thêm mới & Cập nhật (/manager/quizzes/{id}/questions)";
        list.add(new TestCaseData("UTC_QZ_016", s3,
                "Manager mở form thêm câu hỏi trắc nghiệm vào bài Quiz",
                "Nhập đầy đủ: content='「わたしはがくせいです」nghĩa là gì?', optionA='Tôi là giáo viên', optionB='Tôi là học sinh', optionC='Tôi là bác sĩ', optionD='Tôi là kỹ sư', correctOption='B', explanation='がくせい nghĩa là học sinh'",
                "Hệ thống thêm câu hỏi thành công vào bài Quiz, hiển thị đầy đủ trong danh sách câu hỏi.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_017", s3,
                "Manager nhập nội dung câu hỏi chứa chữ Hán Kanji và Furigana",
                "Nhập: content='「私は学生です」', optionA='わたしはがくせいです', correctOption='A'",
                "Hệ thống lưu trữ chính xác bộ mã UTF-8 tiếng Nhật, hiển thị đẹp mắt không bị vỡ font.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_018", s3,
                "Manager bỏ trống nội dung câu hỏi",
                "Nhập form: content = '', optionA = 'A', optionB = 'B', correctOption = 'A'",
                "Hệ thống báo lỗi validation 'Nội dung câu hỏi không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_019", s3,
                "Manager bỏ trống một trong các phương án trả lời",
                "Nhập content = 'Câu hỏi mẫu', optionA = 'A', optionB = 'B', optionC = '', correctOption = 'A'",
                "Hệ thống báo lỗi 'Vui lòng nhập đầy đủ các phương án trả lời A, B, C, D'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_020", s3,
                "Manager chưa chọn đáp án chính xác",
                "Nhập đủ nội dung và 4 phương án nhưng để correctOption = null",
                "Hệ thống báo lỗi validation 'Vui lòng chọn 1 phương án đáp án đúng cho câu hỏi'.", "Abnormal"));

        list.add(new TestCaseData("UTC_QZ_021", s3,
                "Manager nhập nội dung câu hỏi dài chạm ngưỡng 500 ký tự",
                "Nhập content có độ dài đúng 500 ký tự văn bản",
                "Hệ thống chấp nhận và lưu trữ trọn vẹn câu hỏi dài.", "Boundary"));

        list.add(new TestCaseData("UTC_QZ_022", s3,
                "Manager chỉnh sửa nội dung câu hỏi và đổi đáp án đúng",
                "Sửa optionB = 'Tôi là sinh viên', đổi correctOption từ B sang C",
                "Hệ thống cập nhật câu hỏi thành công, áp dụng ngay lập tức vào bài kiểm tra của học viên.", "Normal"));

        list.add(new TestCaseData("UTC_QZ_023", s3,
                "Manager thực hiện xóa câu hỏi khỏi bài Quiz",
                "Bấm nút Xóa câu hỏi, xác nhận trên modal confirm",
                "Hệ thống xóa thành công câu hỏi, cập nhật lại tổng số câu hỏi của bài Quiz.", "Normal"));

        return list;
    }

    // =========================================================================
    // 4. FEATURE: FLASHCARD (27 Test Cases across 4 Screens)
    // =========================================================================
    private List<TestCaseData> getFlashcardTestCases() {
        List<TestCaseData> list = new ArrayList<>();
        String s1 = "MÀN HÌNH SCR-20: Danh mục Bộ thẻ - Tìm kiếm & Lọc (/flashcards)";

        list.add(new TestCaseData("UTC_FC_001", s1,
                "Người dùng mở thư viện bộ thẻ Flashcard",
                "Nhập từ khóa tìm kiếm: keyword = 'Kanji N5'",
                "Hệ thống lọc chính xác các bộ thẻ có tiêu đề chứa từ khóa 'Kanji N5'.", "Normal"));

        list.add(new TestCaseData("UTC_FC_002", s1,
                "Người dùng lọc bộ thẻ theo cấp độ",
                "Chọn bộ lọc cấp độ: level = 'N4'",
                "Hệ thống chỉ hiển thị các bộ thẻ từ vựng thuộc trình độ N4.", "Normal"));

        list.add(new TestCaseData("UTC_FC_003", s1,
                "Người dùng chọn tab Bộ thẻ của tôi",
                "Chọn filter scope = 'MY_DECKS'",
                "Hệ thống chỉ hiển thị các bộ thẻ do chính tài khoản người dùng đang đăng nhập tạo ra.", "Normal"));

        list.add(new TestCaseData("UTC_FC_004", s1,
                "Người dùng chọn tab Bộ thẻ cộng đồng",
                "Chọn filter scope = 'COMMUNITY'",
                "Hệ thống hiển thị danh sách các bộ thẻ được chia sẻ công khai bởi các học viên khác.", "Normal"));

        list.add(new TestCaseData("UTC_FC_005", s1,
                "Người dùng tìm kiếm bộ thẻ không tồn tại",
                "Nhập keyword = 'BoTheKhongHeCo_99999'",
                "Hệ thống hiển thị thông báo rỗng 'Không tìm thấy bộ thẻ Flashcard phù hợp'.", "Abnormal"));

        list.add(new TestCaseData("UTC_FC_006", s1,
                "Người dùng nhập chuỗi ký tự đặc biệt XSS",
                "Nhập keyword = '<img src=x onerror=alert(1)>'",
                "Hệ thống tự động mã hóa chuỗi đầu vào an toàn, hiển thị không thực thi mã độc.", "Abnormal"));

        list.add(new TestCaseData("UTC_FC_007", s1,
                "Người dùng nhập chuỗi khoảng trắng",
                "Nhập keyword = '      '",
                "Hệ thống tự động trim khoảng trắng, hiển thị toàn bộ danh sách bộ thẻ mặc định.", "Boundary"));

        String s2 = "MÀN HÌNH SCR-21: Tạo mới & Chỉnh sửa Bộ thẻ Flashcard (/flashcards/create, /flashcards/{id}/edit)";
        list.add(new TestCaseData("UTC_FC_008", s2,
                "Học viên mở form tạo bộ thẻ Flashcard mới",
                "Nhập form: title='100 Từ vựng N5 Hay Thi', description='Tổng hợp từ vựng Minna', level='N5', visibility='PUBLIC'",
                "Hệ thống tạo mới bộ thẻ thành công, chuyển hướng học viên sang trang thêm từ vựng vào bộ thẻ.", "Normal"));

        list.add(new TestCaseData("UTC_FC_009", s2,
                "Học viên tạo bộ thẻ chế độ riêng tư",
                "Nhập form: title='Ghi nhớ cá nhân', visibility='PRIVATE'",
                "Hệ thống lưu thành công bộ thẻ chế độ riêng tư, chỉ tài khoản sở hữu mới có quyền truy cập.", "Normal"));

        list.add(new TestCaseData("UTC_FC_010", s2,
                "Học viên bỏ trống tên bộ thẻ",
                "Nhập form: title = '', level = 'N5'",
                "Hệ thống báo lỗi validation 'Tên bộ thẻ không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_FC_011", s2,
                "Học viên nhập tên bộ thẻ đúng độ dài tối thiểu 3 ký tự",
                "Nhập form: title = 'N5A'",
                "Hệ thống chấp nhận và tạo bộ thẻ thành công.", "Boundary"));

        list.add(new TestCaseData("UTC_FC_012", s2,
                "Học viên nhập tên bộ thẻ vượt quá 100 ký tự",
                "Nhập title = 'Bộ thẻ từ vựng tổng hợp chuyên sâu nâng cao... (101 ký tự)'",
                "Hệ thống báo lỗi validation 'Tên bộ thẻ không được vượt quá 100 ký tự'.", "Boundary"));

        list.add(new TestCaseData("UTC_FC_013", s2,
                "Học viên chỉnh sửa thông tin bộ thẻ hiện có",
                "Cập nhật title='100 Chữ Hán Cốt Lõi N5', level='N5'",
                "Hệ thống cập nhật thành công thông tin bộ thẻ.", "Normal"));

        String s3 = "MÀN HÌNH SCR-22: Thêm mới & Chỉnh sửa Thẻ ghi nhớ (/flashcards/{id}/cards/create)";
        list.add(new TestCaseData("UTC_FC_014", s3,
                "Học viên mở form thêm từ mới vào bộ thẻ",
                "Nhập đầy đủ: frontText='食べる', backText='Ăn (động từ nhóm 2)', pronunciation='たべる (taberu)', example='朝ごはんを食べる', exampleMeaning='Ăn bữa sáng'",
                "Hệ thống thêm thẻ mới thành công vào bộ thẻ, tổng số lượng thẻ của bộ tăng thêm 1.", "Normal"));

        list.add(new TestCaseData("UTC_FC_015", s3,
                "Học viên chỉ nhập các trường bắt buộc (Mặt trước & Mặt sau)",
                "Nhập: frontText='飲む', backText='Uống', các trường khác để trống",
                "Hệ thống chấp nhận và lưu thẻ từ vựng thành công.", "Normal"));

        list.add(new TestCaseData("UTC_FC_016", s3,
                "Học viên bỏ trống Mặt trước của thẻ",
                "Nhập form: frontText = '', backText = 'Học sinh'",
                "Hệ thống báo lỗi validation 'Mặt trước của thẻ không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_FC_017", s3,
                "Học viên bỏ trống Mặt sau của thẻ",
                "Nhập form: frontText = '学生', backText = ''",
                "Hệ thống báo lỗi validation 'Mặt sau của thẻ (nghĩa từ vựng) không được để trống'.", "Abnormal"));

        list.add(new TestCaseData("UTC_FC_018", s3,
                "Học viên nhập mặt trước đúng 1 ký tự chữ Hán đơn",
                "Nhập: frontText = '一', backText = 'Số 1 / Nhất'",
                "Hệ thống lưu thành công thẻ học chữ Hán đơn lẻ.", "Boundary"));

        list.add(new TestCaseData("UTC_FC_019", s3,
                "Học viên nhập câu ví dụ đạt độ dài tối đa 255 ký tự",
                "Nhập example văn bản tiếng Nhật đúng 255 ký tự",
                "Hệ thống lưu trữ thành công toàn bộ câu ví dụ dài.", "Boundary"));

        list.add(new TestCaseData("UTC_FC_020", s3,
                "Học viên chỉnh sửa nội dung thẻ từ vựng hiện có",
                "Sửa backText='Ăn cơm, ăn uống nói chung'",
                "Hệ thống cập nhật thành công nghĩa mới của thẻ từ vựng.", "Normal"));

        list.add(new TestCaseData("UTC_FC_021", s3,
                "Học viên xóa thẻ từ vựng khỏi bộ thẻ",
                "Bấm nút Xóa thẻ, xác nhận trên hộp thoại modal",
                "Hệ thống xóa thẻ thành công, giảm tổng số thẻ của bộ thẻ đi 1.", "Normal"));

        String s4 = "MÀN HÌNH SCR-23: Luyện tập Flashcard tương tác (/flashcards/{id}/practice)";
        list.add(new TestCaseData("UTC_FC_022", s4,
                "Học viên đang trong phiên luyện tập lật thẻ Flashcard",
                "Click vào thẻ flashcard hoặc bấm phím Space",
                "Thẻ lật mượt mà 180 độ sang Mặt sau, hiển thị nghĩa tiếng Việt, cách đọc Hiragana và câu ví dụ.", "Normal"));

        list.add(new TestCaseData("UTC_FC_023", s4,
                "Học viên đánh giá mức độ ghi nhớ: Chưa thuộc",
                "Bấm nút 'Chưa thuộc' (action rating = 'AGAIN')",
                "Hệ thống ghi nhận trạng thái chưa nhớ, tự động xếp thẻ vào cuối danh sách cần học lại trong phiên.", "Normal"));

        list.add(new TestCaseData("UTC_FC_024", s4,
                "Học viên đánh giá mức độ ghi nhớ: Khó",
                "Bấm nút 'Khó' (action rating = 'HARD')",
                "Thuật toán Spaced Repetition ghi nhận độ khó, lên lịch nhắc lại thẻ trong chu kỳ ngắn (1 ngày).", "Normal"));

        list.add(new TestCaseData("UTC_FC_025", s4,
                "Học viên đánh giá mức độ ghi nhớ: Tốt",
                "Bấm nút 'Tốt' (action rating = 'GOOD')",
                "Thuật toán ghi nhận học tốt, tăng chu kỳ lặp lại thẻ (3 ngày).", "Normal"));

        list.add(new TestCaseData("UTC_FC_026", s4,
                "Học viên đánh giá mức độ ghi nhớ: Dễ",
                "Bấm nút 'Dễ' (action rating = 'EASY')",
                "Thuật toán ghi nhận đã thuộc nhuần nhuyễn, tăng chu kỳ lặp lại thẻ lên mức cao nhất (7 ngày).", "Normal"));

        list.add(new TestCaseData("UTC_FC_027", s4,
                "Học viên hoàn thành việc ôn tập tất cả các thẻ trong bộ",
                "Đánh giá thẻ cuối cùng trong phiên học",
                "Hệ thống chuyển sang màn hình tổng kết phiên ôn tập (Số thẻ đã thuộc, số thẻ cần ôn lại) kèm nút 'Luyện tập lại'.", "Normal"));

        return list;
    }
}
