package com.stockflow.shared.reporting;

/** One interchangeable algorithm for generating a StockFlow CSV report. */
public interface CsvReportStrategy {
    ReportType type();

    byte[] generate();
}
