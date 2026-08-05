package com.stockflow.control;

import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ReorderRuleServiceTest {

    private ReorderRuleService reorderRuleService;

    @BeforeEach
    void setUp() {
        reorderRuleService = new ReorderRuleService(
                mock(ReorderRuleRepository.class),
                mock(ProductRepository.class),
                mock(InventoryBalanceRepository.class)
        );
    }

    @Test
    void maximumLevelMustBeGreaterThanMinimumLevel() {
        ReorderRuleForm form = new ReorderRuleForm();
        form.setProductId(1L);
        form.setMinimumLevel(20);
        form.setMaximumLevel(20);
        form.setReorderQuantity(30);

        assertThatThrownBy(() -> reorderRuleService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum level");
    }
}
