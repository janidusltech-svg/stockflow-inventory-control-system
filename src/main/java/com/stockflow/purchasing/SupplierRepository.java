package com.stockflow.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsBySupplierCodeIgnoreCase(String supplierCode);

    Optional<Supplier> findBySupplierCodeIgnoreCase(String supplierCode);

    List<Supplier> findBySupplierCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String supplierCode,
            String name,
            String email
    );

    List<Supplier> findByActiveTrueOrderByNameAsc();

    // Safe-delete checks: a supplier with any purchase order (draft included) is kept.

    @Query("select count(o) from PurchaseOrder o where o.supplier.id = :supplierId")
    long countPurchaseOrdersBySupplierId(@Param("supplierId") Long supplierId);

    @Query("select distinct o.supplier.id from PurchaseOrder o")
    List<Long> findSupplierIdsWithPurchaseOrders();
}
