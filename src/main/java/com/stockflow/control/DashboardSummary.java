package com.stockflow.control;

public record DashboardSummary(
        long activeProducts,
        long totalStockUnits,
        long lowStockProducts,
        long outOfStockProducts,
        long expiringBatches,
        long pendingAdjustments,
        long openStockCounts
) {
}
