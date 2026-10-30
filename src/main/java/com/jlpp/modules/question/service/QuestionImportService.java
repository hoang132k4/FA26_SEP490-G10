package com.jlpp.modules.question.service;

import com.jlpp.modules.question.dto.ManagerQuestionForm;
import com.jlpp.modules.question.dto.QuestionImportException;
import com.jlpp.modules.question.entity.Question;
import com.jlpp.modules.question.repository.QuestionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class QuestionImportService {

    public static final List<String> BASE_COLUMNS = List.of(
            "group", "question_type", "content", "jlpt_level", "skill_type", "difficulty", "category",
            "option_a", "option_b", "option_c", "option_d", "answer", "explanation", "audio_url", "status");
    public static final List<String> EXAM_COLUMNS = List.of("section", "points");

    private static final int MAX_ROWS = 1000;

    private final ManagerQuestionService questionService;
    private final QuestionRepository questionRepository;

    public QuestionImportService(ManagerQuestionService questionService, QuestionRepository questionRepository) {
        this.questionService = questionService;
        this.questionRepository = questionRepository;
    }

    public record ImportedRow(int rowNumber, Question question, Map<String, String> values) {}

    /**
     * All-or-nothing: any invalid row rolls back the whole file.
     * forcedStatus overrides the status column (quiz/exam imports need ACTIVE to link).
     */
    @Transactional
    public List<ImportedRow> importFile(MultipartFile file, String forcedStatus) {
        List<Map<String, String>> rows = readRows(file);
        if (rows.isEmpty()) throw new QuestionImportException(List.of("File không có dòng dữ liệu nào."));

        List<String> errors = new ArrayList<>();
        List<ImportedRow> imported = new ArrayList<>();
        Map<String, Long> passageByGroup = new HashMap<>();
        Set<String> failedGroups = new HashSet<>();

        for (Map<String, String> row : rows) {
            int rowNumber = Integer.parseInt(row.get("_row"));
            String group = row.getOrDefault("group", "");
            boolean isPassage = "PASSAGE".equals(upper(row.get("question_type")));
            try {
                ManagerQuestionForm form = toForm(row, forcedStatus);
                if (!group.isEmpty()) {
                    if (isPassage) {
                        if (passageByGroup.containsKey(group) || failedGroups.contains(group)) {
                            throw new IllegalArgumentException("Nhóm '" + group + "' chỉ được có 1 dòng PASSAGE.");
                        }
                    } else {
                        if (failedGroups.contains(group)) {
                            throw new IllegalArgumentException("Dòng PASSAGE của nhóm '" + group + "' bị lỗi.");
                        }
                        Long parentId = passageByGroup.get(group);
                        if (parentId == null) {
                            throw new IllegalArgumentException("Nhóm '" + group + "' cần có dòng PASSAGE nằm phía trên các câu hỏi con.");
                        }
                        form.setParentId(parentId);
                    }
                }
                Question saved = questionService.create(form);
                if (isPassage && !group.isEmpty()) passageByGroup.put(group, saved.getId());
                imported.add(new ImportedRow(rowNumber, saved, row));
            } catch (IllegalArgumentException e) {
                if (isPassage && !group.isEmpty()) failedGroups.add(group);
                errors.add("Dòng " + rowNumber + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) throw new QuestionImportException(errors);
        questionRepository.flush();
        return imported;
    }

    public byte[] buildTemplate(boolean includeExamColumns) {
        List<String> columns = new ArrayList<>(BASE_COLUMNS);
        if (includeExamColumns) columns.addAll(EXAM_COLUMNS);

        String[][] samples = {
                {"", "SINGLE_CHOICE", "日本語の新聞を＿＿ことができます。", "N4", "VOCABULARY", "EASY", "", "読む", "書く", "聞く", "話す", "A", "「新聞を読む」が自然です。", "", "DRAFT"},
                {"", "MULTI_CHOICE", "正しい文をすべて選んでください。", "N5", "GRAMMAR", "MEDIUM", "", "私は学生です。", "私が学生ですか。", "これは本です。", "あれ本です。", "A,C", "", "", "DRAFT"},
                {"1", "PASSAGE", "次の文章を読んで、質問に答えてください。\n日本には四季があります。春には桜が咲き、多くの人が花見をします。", "N4", "READING", "MEDIUM", "", "", "", "", "", "", "", "", "DRAFT"},
                {"1", "SINGLE_CHOICE", "この文章のテーマは何ですか。", "N4", "READING", "MEDIUM", "", "季節", "食べ物", "旅行", "仕事", "A", "", "", "DRAFT"},
                {"1", "SINGLE_CHOICE", "春に何が咲きますか。", "N4", "READING", "EASY", "", "桜", "梅", "菊", "ひまわり", "A", "", "", "DRAFT"},
        };
        String[][] examExtras = {
                {"LANGUAGE", "1"}, {"LANGUAGE", "2"}, {"READING", ""}, {"READING", "2"}, {"READING", "2"},
        };

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle wrap = wb.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet sheet = wb.createSheet("Questions");
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = new ArrayList<>(Arrays.asList(samples[r]));
                if (includeExamColumns) values.addAll(Arrays.asList(examExtras[r]));
                for (int c = 0; c < values.size(); c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(values.get(c));
                    cell.setCellStyle(wrap);
                }
            }
            for (int i = 0; i < columns.size(); i++) {
                String col = columns.get(i);
                sheet.setColumnWidth(i, ("content".equals(col) ? 50 : col.startsWith("option") || "explanation".equals(col) ? 20 : 14) * 256);
            }
            sheet.createFreezePane(0, 1);

            Sheet guide = wb.createSheet("Huong dan");
            String[] lines = guideLines(includeExamColumns);
            for (int i = 0; i < lines.length; i++) {
                guide.createRow(i).createCell(0).setCellValue(lines[i]);
            }
            guide.setColumnWidth(0, 120 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file mẫu.", e);
        }
    }

    private String[] guideLines(boolean exam) {
        List<String> lines = new ArrayList<>(List.of(
                "HƯỚNG DẪN IMPORT CÂU HỎI",
                "- Giữ nguyên dòng tiêu đề (dòng 1) của sheet Questions, xoá các dòng ví dụ rồi nhập dữ liệu. Chỉ đọc sheet đầu tiên.",
                "- Nếu có bất kỳ dòng nào lỗi, toàn bộ file sẽ KHÔNG được lưu. Hệ thống báo lỗi theo số dòng.",
                "- question_type: SINGLE_CHOICE (mặc định nếu để trống) | MULTI_CHOICE | PASSAGE",
                "- content: bắt buộc. Với PASSAGE là toàn bộ đoạn văn.",
                "- jlpt_level: N1..N5 | skill_type: VOCABULARY, GRAMMAR, READING, LISTENING | difficulty: EASY, MEDIUM, HARD (có thể để trống)",
                "- option_a, option_b: bắt buộc với câu trắc nghiệm; option_c, option_d: tuỳ chọn. PASSAGE để trống các cột đáp án.",
                "- answer: chữ cái đáp án đúng, ví dụ A. MULTI_CHOICE có thể nhiều đáp án: A,C",
                "- audio_url: link file nghe đã upload sẵn (có thể để trống rồi bổ sung sau ở trang sửa câu hỏi).",
                "",
                "BÀI ĐỌC HIỂU / BÀI NGHE NHIỀU CÂU HỎI (cột group)",
                "- Các dòng cùng giá trị group thuộc cùng một bài. Dòng PASSAGE (đoạn văn) phải nằm TRÊN các câu hỏi con.",
                "- Mỗi group chỉ có 1 dòng PASSAGE. Câu hỏi đơn lẻ thì để trống group."
        ));
        if (exam) {
            lines.add("");
            lines.add("CỘT RIÊNG CHO ĐỀ THI");
            lines.add("- section: LANGUAGE | READING | LISTENING. Để trống: tự suy ra từ skill_type (READING→READING, LISTENING→LISTENING, còn lại LANGUAGE).");
            lines.add("- points: điểm của câu (số nguyên >= 1), mặc định 1. Dòng PASSAGE không được gắn vào đề nên bỏ qua 2 cột này.");
            lines.add("- Khi import vào đề thi, cột status bị bỏ qua: câu hỏi được lưu ACTIVE vào ngân hàng và gắn luôn vào đề.");
        } else {
            lines.add("");
            lines.add("- status: DRAFT (mặc định) | ACTIVE | ARCHIVED. Khi import vào quiz, cột status bị bỏ qua: câu hỏi được lưu ACTIVE và gắn luôn vào quiz.");
        }
        return lines.toArray(String[]::new);
    }

    private ManagerQuestionForm toForm(Map<String, String> row, String forcedStatus) {
        String type = upper(row.get("question_type"));
        if (type.isEmpty()) type = "SINGLE_CHOICE";
        String content = row.getOrDefault("content", "");
        if (content.isBlank()) throw new IllegalArgumentException("Cột content không được để trống.");

        ManagerQuestionForm form = new ManagerQuestionForm();
        form.setQuestionType(type);
        form.setContent(content);
        form.setJlptLevel(upper(row.get("jlpt_level")));
        form.setSkillType(upper(row.get("skill_type")));
        form.setDifficulty(upper(row.get("difficulty")));
        form.setCategory(row.get("category"));
        form.setExplanation(row.get("explanation"));
        form.setAudioUrl(row.get("audio_url"));
        String status = forcedStatus != null ? forcedStatus : upper(row.get("status"));
        form.setStatus(status.isEmpty() ? "DRAFT" : status);

        if (!"PASSAGE".equals(type)) {
            form.setOptionA(row.get("option_a"));
            form.setOptionB(row.get("option_b"));
            form.setOptionC(row.get("option_c"));
            form.setOptionD(row.get("option_d"));
            form.setAnswerKeys(Arrays.stream(row.getOrDefault("answer", "").split("[,;\\s]+"))
                    .map(this::upper).filter(s -> !s.isEmpty()).distinct().toList());
        }
        return form;
    }

    private List<Map<String, String>> readRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new QuestionImportException(List.of("Vui lòng chọn file Excel (.xlsx)."));
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new QuestionImportException(List.of("Chỉ hỗ trợ file .xlsx. Hãy tải file mẫu và điền dữ liệu."));
        }
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new QuestionImportException(List.of("File thiếu dòng tiêu đề."));

            Map<Integer, String> columns = new HashMap<>();
            for (Cell cell : header) {
                String key = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
                if (!key.isEmpty()) columns.put(cell.getColumnIndex(), key);
            }
            if (!columns.containsValue("content")) {
                throw new QuestionImportException(List.of("File thiếu cột 'content'. Hãy dùng file mẫu."));
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                columns.forEach((idx, key) -> {
                    Cell cell = row.getCell(idx);
                    values.put(key, cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim());
                });
                if (values.values().stream().allMatch(String::isEmpty)) continue;
                values.put("_row", String.valueOf(r + 1));
                rows.add(values);
                if (rows.size() > MAX_ROWS) {
                    throw new QuestionImportException(List.of("Tối đa " + MAX_ROWS + " dòng mỗi lần import."));
                }
            }
            return rows;
        } catch (QuestionImportException e) {
            throw e;
        } catch (Exception e) {
            throw new QuestionImportException(List.of("Không đọc được file Excel (file hỏng hoặc sai định dạng)."));
        }
    }

    private String upper(String v) { return v == null ? "" : v.trim().toUpperCase(Locale.ROOT); }
}
