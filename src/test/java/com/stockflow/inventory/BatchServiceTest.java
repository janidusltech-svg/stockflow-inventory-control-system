package com.stockflow.inventory;

import com.stockflow.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchServiceTest {

    private BatchRepository batchRepository;
    private BatchService batchService;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        batchService = new BatchService(batchRepository);
    }

    @Test
    void createsBatchForBatchTrackedProduct() {
        Product product = mock(Product.class);
        when(product.isBatchTracked()).thenReturn(true);
        when(product.getName()).thenReturn("Milk Powder 400g");
        when(batchRepository.findByProductIdAndBatchNumberIgnoreCase(null, "B-100"))
                .thenReturn(Optional.empty());
        when(batchRepository.save(any(Batch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Batch batch = batchService.resolveBatch(
                product,
                "b-100",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2027, 7, 1),
                LocalDate.of(2026, 7, 30)
        );

        assertThat(batch.getBatchNumber()).isEqualTo("B-100");
        assertThat(batch.getProduct()).isEqualTo(product);
    }

    @Test
    void rejectsBatchThatExpiredBeforeReceivingDate() {
        Product product = mock(Product.class);
        when(product.isBatchTracked()).thenReturn(true);
        when(product.getName()).thenReturn("Milk Powder 400g");

        assertThatThrownBy(() -> batchService.resolveBatch(
                product,
                "B-100",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 30)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An expired batch cannot be accepted into inventory");
    }
}
