package com.stockflow.shared.search;

public record GlobalSearchResult(
        String category,
        String title,
        String subtitle,
        String status,
        String targetUrl
) {
}
