package com.jlpp.modules.manager.reports.dto;

import java.util.List;

public record ManagerReportData(
        String type,
        String title,
        List<String> headers,
        List<List<String>> rows
) {
}
