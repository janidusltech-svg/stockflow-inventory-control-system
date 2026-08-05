package com.stockflow.purchasing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierServiceTest {

    private SupplierRepository supplierRepository;
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierRepository = mock(SupplierRepository.class);
        supplierService = new SupplierService(supplierRepository);
    }

    @Test
    void createsSupplierWhenCodeIsUnique() {
        Supplier supplier = supplier("SUP-001");
        when(supplierRepository.existsBySupplierCodeIgnoreCase("SUP-001"))
                .thenReturn(false);
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Supplier saved = supplierService.create(supplier);

        assertThat(saved.getSupplierCode()).isEqualTo("SUP-001");
        verify(supplierRepository).save(supplier);
    }

    @Test
    void rejectsDuplicateSupplierCode() {
        Supplier supplier = supplier("SUP-001");
        when(supplierRepository.existsBySupplierCodeIgnoreCase("SUP-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> supplierService.create(supplier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A supplier with this code already exists");
    }

    @Test
    void deactivatesSupplierWithoutDeletingHistory() {
        Supplier supplier = supplier("SUP-001");
        supplier.setActive(true);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deactivate(1L);

        assertThat(supplier.isActive()).isFalse();
        verify(supplierRepository).save(supplier);
    }

    private Supplier supplier(String code) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(code);
        supplier.setName("Fresh Foods Lanka");
        supplier.setPhone("0771234567");
        supplier.setEmail("orders@freshfoodslanka.lk");
        supplier.setActive(true);
        return supplier;
    }
}
