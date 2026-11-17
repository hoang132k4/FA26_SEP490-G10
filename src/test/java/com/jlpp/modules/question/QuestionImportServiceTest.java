package com.jlpp.modules.question;

import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import com.jlpp.modules.question.service.ManagerQuestionService;
import com.jlpp.modules.question.service.QuestionImportService;
import com.jlpp.support.QuestionExcel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionImportServiceTest {

    @Mock private ManagerQuestionService questionService;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionImportService importService;

    // ── readRows: file validation ──

    @Test
    void nullFile_throwsImportException() {
        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(null, null));
        assertEquals(List.of("Vui lòng chọn file Excel (.xlsx)."), ex.getErrors());
    }

    @Test
    void emptyFile_throwsImportException() {
        MockMultipartFile empty = new MockMultipartFile("file", "questions.xlsx", null, new byte[0]);

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(empty, null));
        assertEquals(List.of("Vui lòng chọn file Excel (.xlsx)."), ex.getErrors());
    }

    @Test
    void nonXlsxExtension_throwsImportException() {
        MockMultipartFile csv = new MockMultipartFile("file", "questions.csv", "text/csv", "a,b".getBytes());

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(csv, null));
        assertTrue(ex.getErrors().get(0).contains(".xlsx"));
    }

    @Test
    void missingContentColumn_throwsImportException() {
        var file = QuestionExcel.xlsx(
                List.of("question_type", "option_a"),
                List.of(List.of("MULTI_CHOICE", "A")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().get(0).contains("content"));
    }

    @Test
    void allEmptyRows_throwsNoDataException() {
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                List.of(List.of("", "", "", "")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().get(0).contains("không có dòng dữ liệu"));
    }

    // ── toForm logic (tested via importFile) ──

    @Test
    void blankQuestionType_defaultsToMultiChoice() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("question_type", "content", "option_a", "option_b", "answer"),
                List.of(List.of("", "Câu hỏi gì đó", "A", "B", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, "DRAFT");

        assertEquals(1, rows.size());
        assertEquals("MULTI_CHOICE", rows.get(0).question().getQuestionType());
    }

    @Test
    void forcedStatus_overridesColumnStatus() {
        stubCreateReturns("MULTI_CHOICE", "ACTIVE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "status"),
                List.of(List.of("Câu hỏi", "A", "B", "A", "DRAFT")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, "ACTIVE");

        assertEquals("ACTIVE", rows.get(0).question().getStatus());
    }

    @Test
    void answerSplitting_commaAndSemicolon() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                List.of(List.of("Câu hỏi", "A", "B", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
    }

    @Test
    void blankContent_throwsErrorForThatRow() {
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                List.of(List.of("  ", "A", "B", "A")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().get(0).contains("content"));
    }

    // ── group logic ──

    @Test
    void passageThenChildren_setsParentIdCorrectly() {
        Question passage = question(10L, "PASSAGE");
        Question child = question(11L, "MULTI_CHOICE");
        when(questionService.create(any())).thenReturn(passage, child);

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("1", "PASSAGE", "Đoạn văn", "", "", ""),
                        List.of("1", "MULTI_CHOICE", "Câu con", "A", "B", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(2, rows.size());
        assertEquals(10L, rows.get(0).question().getId());
        assertEquals(11L, rows.get(1).question().getId());
    }

    @Test
    void duplicatePassageInSameGroup_throwsError() {
        Question passage = question(10L, "PASSAGE");
        when(questionService.create(any())).thenReturn(passage);

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("1", "PASSAGE", "Đoạn văn 1", "", "", ""),
                        List.of("1", "PASSAGE", "Đoạn văn 2", "", "", "")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Nhóm '1' chỉ được có 1 dòng PASSAGE")));
    }

    @Test
    void childBeforePassage_throwsError() {
        Question passage = question(10L, "PASSAGE");
        when(questionService.create(any())).thenReturn(passage);

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("1", "MULTI_CHOICE", "Câu con", "A", "B", "A"),
                        List.of("1", "PASSAGE", "Đoạn văn", "", "", "")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("cần có dòng PASSAGE nằm phía trên")));
    }

    @Test
    void failedPassageCascadesToChildren() {
        when(questionService.create(any())).thenThrow(new IllegalArgumentException("Lỗi passage"));

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("1", "PASSAGE", "Đoạn lỗi", "", "", ""),
                        List.of("1", "MULTI_CHOICE", "Câu con", "A", "B", "A")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertEquals(2, ex.getErrors().size());
        assertTrue(ex.getErrors().get(1).contains("Dòng PASSAGE của nhóm '1' bị lỗi"));
    }

    @Test
    void differentGroups_independentPassages() {
        Question p1 = question(10L, "PASSAGE");
        Question c1 = question(11L, "MULTI_CHOICE");
        Question p2 = question(20L, "PASSAGE");
        Question c2 = question(21L, "MULTI_CHOICE");
        when(questionService.create(any())).thenReturn(p1, c1, p2, c2);

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("A", "PASSAGE", "Đoạn A", "", "", ""),
                        List.of("A", "MULTI_CHOICE", "Câu A", "X", "Y", "X"),
                        List.of("B", "PASSAGE", "Đoạn B", "", "", ""),
                        List.of("B", "MULTI_CHOICE", "Câu B", "X", "Y", "Y")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);
        assertEquals(4, rows.size());
    }

    @Test
    void mixedValidAndInvalid_rollsBackAll() {
        Question good = question(1L, "MULTI_CHOICE");
        when(questionService.create(any()))
                .thenReturn(good)
                .thenThrow(new IllegalArgumentException("Câu lỗi"));

        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("Câu đúng", "A", "B", "A"),
                        List.of("Câu sai", "A", "B", "")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertNotNull(ex.getErrors());
    }

    // ── buildTemplate ──

    @Test
    void templateWithoutExamColumns_excludesSection() {
        byte[] template = importService.buildTemplate(false);
        assertNotNull(template);
        assertTrue(template.length > 0);
    }

    @Test
    void templateWithExamColumns_includesSection() {
        byte[] template = importService.buildTemplate(true);
        assertNotNull(template);
        assertTrue(template.length > 0);
    }

    // ── extra values map ──

    @Test
    void importedRowCarriesExtraColumnValues() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "section"),
                List.of(List.of("Câu hỏi", "A", "B", "A", "READING")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals("READING", rows.get(0).values().get("section"));
    }

    @Test
    void passageType_dropsOptionsAndAnswerFromForm() {
        Question passage = question(10L, "PASSAGE");
        when(questionService.create(any())).thenReturn(passage);

        var file = QuestionExcel.xlsx(
                List.of("question_type", "content", "option_a", "option_b", "answer"),
                List.of(List.of("PASSAGE", "Đoạn văn dài", "Fake A", "Fake B", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).question().getOptions());
    }

    // ── file validation: additional ──

    @Test
    void corruptedXlsxFile_throwsImportException() {
        MockMultipartFile corrupt = new MockMultipartFile("file", "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03});

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(corrupt, null));
        assertTrue(ex.getErrors().get(0).contains("Không đọc được file Excel"));
    }

    @Test
    void nullFilename_throwsImportException() {
        MockMultipartFile noName = new MockMultipartFile("file", null, null, "data".getBytes());

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(noName, null));
        assertTrue(ex.getErrors().get(0).contains(".xlsx"));
    }

    @Test
    void xls_notXlsx_throwsImportException() {
        MockMultipartFile xls = new MockMultipartFile("file", "questions.xls", null, "data".getBytes());

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(xls, null));
        assertTrue(ex.getErrors().get(0).contains(".xlsx"));
    }

    @Test
    void headerOnly_noDataRows_throwsNoData() {
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                List.of());

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().get(0).contains("không có dòng dữ liệu") || ex.getErrors().get(0).contains("File không có dòng"));
    }

    @Test
    void over1000Rows_throwsMaxRowsException() throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < 1002; i++) {
            rows.add(List.of("Câu " + i, "A", "B", "A"));
        }
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                rows);

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().get(0).contains("1000"));
    }

    @Test
    void missingHeaderRow_throwsException() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet("Questions");
            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "questions.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            QuestionImportException ex = assertThrows(QuestionImportException.class,
                    () -> importService.importFile(file, null));
            assertTrue(ex.getErrors().get(0).contains("tiêu đề"));
        }
    }

    // ── toForm logic: additional ──

    @Test
    void questionTypeCase_lowercaseMultiChoice_normalized() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("question_type", "content", "option_a", "option_b", "answer"),
                List.of(List.of("multi_choice", "Câu hỏi", "A", "B", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
        assertEquals("MULTI_CHOICE", rows.get(0).question().getQuestionType());
    }

    @Test
    void statusBlankAndNoForced_defaultsToDraft() {
        stubCreateReturns("MULTI_CHOICE", "DRAFT");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "status"),
                List.of(List.of("Câu hỏi", "A", "B", "A", "")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals("DRAFT", rows.get(0).question().getStatus());
    }

    @Test
    void jlptLevelAndSkillType_normalizedToUppercase() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "jlpt_level", "skill_type"),
                List.of(List.of("Câu hỏi", "A", "B", "A", "n4", "grammar")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
    }

    @Test
    void audioUrl_preserved_inForm() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "audio_url"),
                List.of(List.of("Câu hỏi", "A", "B", "A", "https://example.com/audio.mp3")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
        assertEquals("https://example.com/audio.mp3", rows.get(0).values().get("audio_url"));
    }

    @Test
    void explanationColumn_preserved_inForm() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "explanation"),
                List.of(List.of("Câu hỏi", "A", "B", "A", "Giải thích chi tiết")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
        assertEquals("Giải thích chi tiết", rows.get(0).values().get("explanation"));
    }

    @Test
    void optionC_optionD_blank_onlyABInForm() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "option_c", "option_d", "answer"),
                List.of(List.of("Câu hỏi", "A", "B", "", "", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
    }

    // ── group logic: additional ──

    @Test
    void singleGroupWithMultipleChildren_allGetParentId() {
        Question passage = question(10L, "PASSAGE");
        Question c1 = question(11L, "MULTI_CHOICE");
        Question c2 = question(12L, "MULTI_CHOICE");
        Question c3 = question(13L, "MULTI_CHOICE");
        when(questionService.create(any())).thenReturn(passage, c1, c2, c3);

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("1", "PASSAGE", "Đoạn văn dài", "", "", ""),
                        List.of("1", "MULTI_CHOICE", "Câu 1", "A", "B", "A"),
                        List.of("1", "MULTI_CHOICE", "Câu 2", "C", "D", "C"),
                        List.of("1", "MULTI_CHOICE", "Câu 3", "E", "F", "E")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(4, rows.size());
        assertEquals(10L, rows.get(0).question().getId());
    }

    @Test
    void standaloneQuestion_noGroup_noParentId() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("group", "content", "option_a", "option_b", "answer"),
                List.of(List.of("", "Câu đơn", "A", "B", "A")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
    }

    @Test
    void threeGroups_mixedValidAndInvalid_reportsAllErrors() {
        Question p1 = question(10L, "PASSAGE");
        when(questionService.create(any()))
                .thenReturn(p1)
                .thenThrow(new IllegalArgumentException("Lỗi child A"))
                .thenThrow(new IllegalArgumentException("Lỗi passage B"));

        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("A", "PASSAGE", "Đoạn A", "", "", ""),
                        List.of("A", "MULTI_CHOICE", "Câu A lỗi", "X", "Y", "X"),
                        List.of("B", "PASSAGE", "Đoạn B lỗi", "", "", ""),
                        List.of("B", "MULTI_CHOICE", "Câu B con", "X", "Y", "X")));

        QuestionImportException ex = assertThrows(QuestionImportException.class,
                () -> importService.importFile(file, null));
        assertTrue(ex.getErrors().size() >= 3);
    }

    // ── importedRow / template: additional ──

    @Test
    void importedRow_rowNumberMatchesExcelRow() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                List.of(
                        List.of("Câu 1", "A", "B", "A"),
                        List.of("Câu 2", "C", "D", "C")));

        List<QuestionImportService.ImportedRow> rows = importService.importFile(file, null);

        assertEquals(2, rows.get(0).rowNumber());
        assertEquals(3, rows.get(1).rowNumber());
    }

    @Test
    void template_baseOnly_hasCorrectColumnCount() throws IOException {
        byte[] template = importService.buildTemplate(false);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals(13, header.getLastCellNum());
        }
    }

    @Test
    void template_withExam_hasCorrectColumnCount() throws IOException {
        byte[] template = importService.buildTemplate(true);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals(14, header.getLastCellNum());
        }
    }

    @Test
    void template_firstRowIsHeader() throws IOException {
        byte[] template = importService.buildTemplate(false);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("group", header.getCell(0).getStringCellValue());
            assertEquals("content", header.getCell(2).getStringCellValue());
        }
    }

    @Test
    void template_hasSampleDataRows() throws IOException {
        byte[] template = importService.buildTemplate(false);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Sheet sheet = wb.getSheetAt(0);
            assertTrue(sheet.getLastRowNum() >= 5);
            assertNotNull(sheet.getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void template_hasGuideSheet() throws IOException {
        byte[] template = importService.buildTemplate(false);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("Huong dan", wb.getSheetAt(1).getSheetName());
        }
    }

    // ── remaining branches ──

    @Test
    void secondPassageInGroupWhosePassageFailed_isReportedAsDuplicate() {
        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content"),
                List.of(List.of("1", "PASSAGE", ""), List.of("1", "PASSAGE", "Đoạn văn thứ hai")));

        QuestionImportException ex = assertThrows(QuestionImportException.class, () -> importService.importFile(file, null));

        assertEquals(List.of("Dòng 2: Cột content không được để trống.",
                "Dòng 3: Nhóm '1' chỉ được có 1 dòng PASSAGE."), ex.getErrors());
    }

    @Test
    void failedStandalonePassage_doesNotBlockLaterGroups() {
        stubCreateReturns("PASSAGE");
        var file = QuestionExcel.xlsx(
                List.of("group", "question_type", "content"),
                List.of(List.of("", "PASSAGE", ""), List.of("", "PASSAGE", "Đoạn văn hợp lệ")));

        QuestionImportException ex = assertThrows(QuestionImportException.class, () -> importService.importFile(file, null));

        assertEquals(List.of("Dòng 2: Cột content không được để trống."), ex.getErrors());
    }

    @Test
    void fileWithoutOriginalName_isRejectedAsNotXlsx() {
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        QuestionImportException ex = assertThrows(QuestionImportException.class, () -> importService.importFile(file, null));

        assertEquals(List.of("Chỉ hỗ trợ file .xlsx. Hãy tải file mẫu và điền dữ liệu."), ex.getErrors());
    }

    @Test
    void blankHeaderColumn_isIgnored() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "", "option_a", "option_b", "answer"),
                List.of(List.of("Câu hỏi", "ghi chú bị bỏ qua", "A", "B", "A")));

        var rows = importService.importFile(file, null);

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).values().keySet().stream().noneMatch(String::isEmpty));
    }

    @Test
    void missingSheetRowInTheMiddle_isSkippedAndRowNumbersStayAccurate() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer"),
                java.util.Arrays.asList(List.of("Câu 1", "A", "B", "A"), null, List.of("Câu 3", "A", "B", "A")));

        var rows = importService.importFile(file, null);

        assertEquals(List.of(2, 4), rows.stream().map(QuestionImportService.ImportedRow::rowNumber).toList());
    }

    @Test
    void rowShorterThanHeader_treatsMissingCellsAsEmpty() {
        stubCreateReturns("MULTI_CHOICE");
        var file = QuestionExcel.xlsx(
                List.of("content", "option_a", "option_b", "answer", "explanation"),
                List.of(List.of("Câu hỏi", "A", "B", "A")));

        var rows = importService.importFile(file, null);

        assertEquals("", rows.get(0).values().get("explanation"));
    }

    // ── helpers ──

    private void stubCreateReturns(String questionType) {
        stubCreateReturns(questionType, "DRAFT");
    }

    private void stubCreateReturns(String questionType, String status) {
        when(questionService.create(any())).thenAnswer(inv -> {
            var form = inv.getArgument(0, com.jlpp.modules.question.dto.ManagerQuestionForm.class);
            Question q = Question.builder()
                    .content(form.getContent())
                    .questionType(questionType)
                    .status(status)
                    .build();
            q.setId((long) (Math.random() * 10000));
            return q;
        });
    }

    private static Question question(long id, String type) {
        Question q = Question.builder().content("Q" + id).questionType(type).status("DRAFT").build();
        q.setId(id);
        return q;
    }
}
