package com.stockflow.shared.reporting;

/**
 * Strategy Pattern: every report supplies its own CSV generation algorithm.
 */
public interface CsvReportStrategy {

    ReportType reportType();

    byte[] generate();
}
