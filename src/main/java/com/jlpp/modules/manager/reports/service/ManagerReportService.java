package com.jlpp.modules.manager.reports.service;

import com.jlpp.modules.manager.reports.dto.ManagerReportData;
import com.jlpp.modules.manager.reports.dto.ManagerReportType;
import com.jlpp.modules.manager.reports.repository.ManagerReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ManagerReportService {

    private final ManagerReportRepository reportRepository;

    public ManagerReportService(ManagerReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public ManagerReportData getReport(String reportType, LocalDate from, LocalDate to) {
        ManagerReportType type = ManagerReportType.from(reportType);
        return new ManagerReportData(type.value(), type.label(), type.headers(),
                reportRepository.findReportRows(type, from, to));
    }
}
