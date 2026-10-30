package com.jlpp.modules.dashboard.controller;

import com.jlpp.modules.manager.reports.dto.ManagerReportData;
import com.jlpp.modules.manager.reports.dto.ManagerReportType;
import com.jlpp.modules.manager.reports.service.ManagerReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager area pages. Lesson management lives in
 * {@link com.jlpp.modules.manager.lessons.controller.ManagerLessonController}.
 */
@Controller
@RequestMapping("/manager")
public class ManagerController {

    private static final Logger log = LoggerFactory.getLogger(ManagerController.class);
    private final ManagerReportService reportService;

    public ManagerController(ManagerReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String managerHome() {
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String type,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        ManagerReportType selectedType;
        try {
            selectedType = ManagerReportType.from(type);
        } catch (IllegalArgumentException exception) {
            selectedType = ManagerReportType.EXAM_RESULTS;
            model.addAttribute("reportError", exception.getMessage());
        }
        model.addAttribute("reportTypes", ManagerReportType.values());
        model.addAttribute("selectedType", selectedType.value());
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);

        if (from != null && to != null && from.isAfter(to)) {
            model.addAttribute("reportError", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            model.addAttribute("report", emptyReport(selectedType));
            return "pages/manager/report/manager-reports";
        }
        try {
            model.addAttribute("report", reportService.getReport(selectedType.value(), from, to));
        } catch (DataAccessException exception) {
            log.error("Could not load manager report type {}", selectedType.value(), exception);
            model.addAttribute("reportError", "Không thể tải báo cáo từ cơ sở dữ liệu. Vui lòng thử lại sau.");
            model.addAttribute("report", emptyReport(selectedType));
        }
        return "pages/manager/report/manager-reports";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ManagerReportType selectedType;
        try {
            selectedType = ManagerReportType.from(type);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body(exception.getMessage().getBytes(StandardCharsets.UTF_8));
        }
        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.".getBytes(StandardCharsets.UTF_8));
        }
        try {
            ManagerReportData report = reportService.getReport(selectedType.value(), from, to);
            byte[] csv = toCsv(report).getBytes(StandardCharsets.UTF_8);
            String filename = "manager-report-" + selectedType.value() + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv);
        } catch (DataAccessException exception) {
            log.error("Could not export manager report type {}", selectedType.value(), exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.TEXT_PLAIN)
                    .body("Không thể xuất báo cáo từ cơ sở dữ liệu. Vui lòng thử lại sau.".getBytes(StandardCharsets.UTF_8));
        }
    }

    private ManagerReportData emptyReport(ManagerReportType type) {
        return new ManagerReportData(type.value(), type.label(), type.headers(), List.of());
    }

    private String toCsv(ManagerReportData report) {
        List<String> records = new ArrayList<>();
        records.add(report.headers().stream().map(this::csvValue).collect(java.util.stream.Collectors.joining(",")));
        for (List<String> row : report.rows()) {
            records.add(row.stream().map(this::csvValue).collect(java.util.stream.Collectors.joining(",")));
        }
        return "\uFEFF" + String.join("\r\n", records) + "\r\n";
    }

    private String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.stripLeading().matches("^[=+@-].*")) {
            safeValue = "'" + safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/questions")
    public String questions() {
        return "pages/manager/question/list";
    }

    @GetMapping("/quizzes")
    public String quizzes() {
        return "pages/manager/quiz/list";
    }

    @GetMapping("/exams")
    public String exams() {
        return "pages/manager/exam/list";
    }

}
