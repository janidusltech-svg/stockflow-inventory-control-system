package com.stockflow.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
