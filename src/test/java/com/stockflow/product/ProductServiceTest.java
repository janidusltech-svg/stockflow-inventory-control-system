package com.stockflow.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductService(
                productRepository,
                mock(CategoryRepository.class),
                mock(BrandRepository.class),
                mock(UnitRepository.class)
        );
    }

    @Test
    void neverUsedProductIsDeletedPermanently() {
        Product product = product("SKU-NEW");
        when(productRepository.findByIdWithDetails(7L)).thenReturn(Optional.of(product));

        productService.delete(7L);

        verify(productRepository).delete(product);
    }

    @Test
    void productWithStockHistoryIsKept() {
        Product product = product("SKU-OLD");
        when(productRepository.findByIdWithDetails(7L)).thenReturn(Optional.of(product));
        when(productRepository.findProductIdsUsedInStockMovements()).thenReturn(List.of(7L));
        when(productRepository.findProductIdsUsedInPurchaseOrders()).thenReturn(List.of(7L));

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purchase orders")
                .hasMessageContaining("stock movements")
                .hasMessageContaining("Deactivate it instead");

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void productWithReorderRuleIsKept() {
        Product product = product("SKU-RULE");
        when(productRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(product));
        when(productRepository.findProductIdsUsedInReorderRules()).thenReturn(List.of(9L));

        assertThatThrownBy(() -> productService.delete(9L))
                .hasMessageContaining("reorder rules");

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void linkedProductIdsCombineEveryUsageSource() {
        when(productRepository.findProductIdsUsedInBatches()).thenReturn(List.of(1L));
        when(productRepository.findProductIdsUsedInStockIssues()).thenReturn(List.of(2L, 1L));

        assertThat(productService.findLinkedProductIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    private Product product(String sku) {
        Product product = new Product();
        product.setSku(sku);
        product.setName("Test product");
        return product;
    }
}
