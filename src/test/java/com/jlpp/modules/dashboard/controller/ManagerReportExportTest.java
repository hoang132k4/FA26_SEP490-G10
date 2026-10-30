package com.jlpp.modules.dashboard.controller;

import com.jlpp.modules.manager.reports.dto.ManagerReportData;
import com.jlpp.modules.manager.reports.service.ManagerReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagerReportExportTest {

    @Test
    void exportsCsvWithEscapedCellsAndFormulaProtection() {
        ManagerReportService reportService = mock(ManagerReportService.class);
        when(reportService.getReport("exam-results", null, null)).thenReturn(new ManagerReportData(
                "exam-results", "Báo cáo kết quả thi", List.of("Học viên", "Bài thi"),
                List.of(List.of("=1+1", "Bài thi \"N5\""))));
        ManagerController controller = new ManagerController(reportService);

        ResponseEntity<byte[]> response = controller.exportReport("exam-results", null, null);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"manager-report-exam-results.csv\"");
        assertThat(csv).startsWith("\uFEFF\"Học viên\",\"Bài thi\"");
        assertThat(csv).contains("\"'=1+1\",\"Bài thi \"\"N5\"\"\"");
    }
}
