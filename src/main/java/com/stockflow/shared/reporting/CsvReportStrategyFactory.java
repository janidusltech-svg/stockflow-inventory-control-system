package com.stockflow.shared.reporting;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Pattern: returns the correct CSV strategy for a requested report type.
 */
@Component
public class CsvReportStrategyFactory {

    private final Map<ReportType, CsvReportStrategy> strategies;

    public CsvReportStrategyFactory(List<CsvReportStrategy> availableStrategies) {
        Map<ReportType, CsvReportStrategy> registered = new EnumMap<>(ReportType.class);
        for (CsvReportStrategy strategy : availableStrategies) {
            CsvReportStrategy previous = registered.put(strategy.reportType(), strategy);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple CSV strategies are registered for " + strategy.reportType());
            }
        }
        for (ReportType type : ReportType.values()) {
            if (!registered.containsKey(type)) {
                throw new IllegalStateException("No CSV strategy is registered for " + type);
            }
        }
        this.strategies = Map.copyOf(registered);
    }

    public CsvReportStrategy getStrategy(ReportType type) {
        if (type == null) {
            throw new IllegalArgumentException("Report type is required");
        }
        CsvReportStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported report type: " + type);
        }
        return strategy;
    }

    public byte[] generate(ReportType type) {
        return getStrategy(type).generate();
    }
}
