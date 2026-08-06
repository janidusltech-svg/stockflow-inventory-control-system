package com.stockflow.shared.audit;

public record AuditSummary(long totalEvents, long successfulEvents,
                           long failedEvents, long eventsLastTwentyFourHours) {
}
