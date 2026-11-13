package com.jlpp.support;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class QuestionExcel {

    private QuestionExcel() {}

    public static MockMultipartFile xlsx(List<String> header, List<List<String>> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Questions");
            write(sheet.createRow(0), header);
            for (int i = 0; i < rows.size(); i++) {
                write(sheet.createRow(i + 1), rows.get(i));
            }
            wb.write(out);
            return new MockMultipartFile("file", "questions.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Row row, List<String> values) {
        for (int c = 0; c < values.size(); c++) {
            row.createCell(c).setCellValue(values.get(c));
        }
    }
}
