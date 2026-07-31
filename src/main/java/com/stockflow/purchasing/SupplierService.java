package com.stockflow.purchasing;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }

        String cleanedKeyword = keyword.trim();
        return supplierRepository
                .findBySupplierCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        cleanedKeyword,
                        cleanedKeyword,
                        cleanedKeyword
                );
    }

    @Transactional(readOnly = true)
    public List<Supplier> findActive() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    public Supplier create(Supplier supplier) {
        if (supplierRepository.existsBySupplierCodeIgnoreCase(supplier.getSupplierCode())) {
            throw new IllegalArgumentException("A supplier with this code already exists");
        }
        return supplierRepository.save(supplier);
    }

    public Supplier update(Long id, Supplier formSupplier) {
        Supplier supplier = findById(id);
        supplierRepository.findBySupplierCodeIgnoreCase(formSupplier.getSupplierCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A supplier with this code already exists");
                });

        supplier.setSupplierCode(formSupplier.getSupplierCode());
        supplier.setName(formSupplier.getName());
        supplier.setContactPerson(formSupplier.getContactPerson());
        supplier.setPhone(formSupplier.getPhone());
        supplier.setEmail(formSupplier.getEmail());
        supplier.setAddress(formSupplier.getAddress());
        supplier.setActive(formSupplier.isActive());
        return supplierRepository.save(supplier);
    }

    public void deactivate(Long id) {
        Supplier supplier = findById(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }
}
