package com.stockflow.control;

public record ReorderAlert(
        ReorderRule rule,
        long currentQuantity,
        String levelStatus,
        int shortageQuantity
) {
    public boolean isOutOfStock() {
        return "OUT_OF_STOCK".equals(levelStatus);
    }

    public boolean isLowStock() {
        return "LOW_STOCK".equals(levelStatus);
    }
}
