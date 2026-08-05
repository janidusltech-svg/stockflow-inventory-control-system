package com.stockflow.warehouse;

import com.stockflow.inventory.InventoryBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            StorageLocationRepository storageLocationRepository,
                            InventoryBalanceRepository inventoryBalanceRepository) {
        this.warehouseRepository = warehouseRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> findAllWarehouses() {
        return warehouseRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Warehouse> findActiveWarehouses() {
        return warehouseRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<StorageLocation> findAllLocations() {
        return storageLocationRepository.findAllWithWarehouse();
    }

    @Transactional(readOnly = true)
    public Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
    }

    @Transactional(readOnly = true)
    public StorageLocation findLocation(Long id) {
        return storageLocationRepository.findByIdWithWarehouse(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage location not found"));
    }

    public Warehouse createWarehouse(WarehouseForm form) {
        validateWarehouseUniqueness(form, null);
        Warehouse warehouse = new Warehouse();
        applyWarehouseForm(warehouse, form);
        return warehouseRepository.save(warehouse);
    }

    public Warehouse updateWarehouse(Long id, WarehouseForm form) {
        Warehouse warehouse = findWarehouse(id);
        validateWarehouseUniqueness(form, id);
        if (warehouse.isActive() && !form.isActive()
                && inventoryBalanceRepository.countPositiveByWarehouseId(id) > 0) {
            throw new IllegalArgumentException(
                    "Move all stock out of this warehouse before deactivating it"
            );
        }
        applyWarehouseForm(warehouse, form);
        if (!warehouse.isActive()) {
            storageLocationRepository.findByWarehouseIdWithWarehouse(id).forEach(location -> {
                location.setActive(false);
                storageLocationRepository.save(location);
            });
        }
        return warehouseRepository.save(warehouse);
    }

    public void deactivateWarehouse(Long id) {
        Warehouse warehouse = findWarehouse(id);
        if (inventoryBalanceRepository.countPositiveByWarehouseId(id) > 0) {
            throw new IllegalArgumentException(
                    "Move all stock out of this warehouse before deactivating it"
            );
        }
        warehouse.setActive(false);
        storageLocationRepository.findByWarehouseIdWithWarehouse(id).forEach(location -> {
            location.setActive(false);
            storageLocationRepository.save(location);
        });
        warehouseRepository.save(warehouse);
    }

    public StorageLocation createLocation(StorageLocationForm form) {
        Warehouse warehouse = requireActiveWarehouse(form.getWarehouseId());
        validateLocationUniqueness(form, null);
        StorageLocation location = new StorageLocation();
        applyLocationForm(location, form, warehouse);
        return storageLocationRepository.save(location);
    }

    public StorageLocation updateLocation(Long id, StorageLocationForm form) {
        StorageLocation location = findLocation(id);
        Warehouse warehouse = requireActiveWarehouse(form.getWarehouseId());
        if (!location.getWarehouse().getId().equals(warehouse.getId())
                && inventoryBalanceRepository.countPositiveByLocationId(id) > 0) {
            throw new IllegalArgumentException(
                    "Move all stock out before changing the location's warehouse"
            );
        }
        if (location.isActive() && !form.isActive()
                && inventoryBalanceRepository.countPositiveByLocationId(id) > 0) {
            throw new IllegalArgumentException(
                    "Move all stock out of this location before deactivating it"
            );
        }
        validateLocationUniqueness(form, id);
        applyLocationForm(location, form, warehouse);
        return storageLocationRepository.save(location);
    }

    public void deactivateLocation(Long id) {
        StorageLocation location = findLocation(id);
        if (inventoryBalanceRepository.countPositiveByLocationId(id) > 0) {
            throw new IllegalArgumentException(
                    "Move all stock out of this location before deactivating it"
            );
        }
        location.setActive(false);
        storageLocationRepository.save(location);
    }

    public WarehouseForm toWarehouseForm(Warehouse warehouse) {
        WarehouseForm form = new WarehouseForm();
        form.setCode(warehouse.getCode());
        form.setName(warehouse.getName());
        form.setAddress(warehouse.getAddress());
        form.setActive(warehouse.isActive());
        return form;
    }

    public StorageLocationForm toLocationForm(StorageLocation location) {
        StorageLocationForm form = new StorageLocationForm();
        form.setWarehouseId(location.getWarehouse().getId());
        form.setCode(location.getCode());
        form.setName(location.getName());
        form.setActive(location.isActive());
        return form;
    }

    private Warehouse requireActiveWarehouse(Long id) {
        Warehouse warehouse = findWarehouse(id);
        if (!warehouse.isActive()) {
            throw new IllegalArgumentException("Select an active warehouse");
        }
        return warehouse;
    }

    private void validateWarehouseUniqueness(WarehouseForm form, Long currentId) {
        String code = form.getCode().trim();
        String name = form.getName().trim();
        boolean duplicateCode = currentId == null
                ? warehouseRepository.existsByCodeIgnoreCase(code)
                : warehouseRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        boolean duplicateName = currentId == null
                ? warehouseRepository.existsByNameIgnoreCase(name)
                : warehouseRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (duplicateCode) {
            throw new IllegalArgumentException("Warehouse code already exists");
        }
        if (duplicateName) {
            throw new IllegalArgumentException("Warehouse name already exists");
        }
    }

    private void validateLocationUniqueness(StorageLocationForm form, Long currentId) {
        String code = form.getCode().trim();
        boolean duplicate = currentId == null
                ? storageLocationRepository.existsByWarehouseIdAndCodeIgnoreCase(
                form.getWarehouseId(), code)
                : storageLocationRepository.existsByWarehouseIdAndCodeIgnoreCaseAndIdNot(
                form.getWarehouseId(), code, currentId);
        if (duplicate) {
            throw new IllegalArgumentException(
                    "This location code already exists in the selected warehouse"
            );
        }
    }

    private void applyWarehouseForm(Warehouse warehouse, WarehouseForm form) {
        warehouse.setCode(form.getCode());
        warehouse.setName(form.getName());
        warehouse.setAddress(form.getAddress());
        warehouse.setActive(form.isActive());
    }

    private void applyLocationForm(StorageLocation location,
                                   StorageLocationForm form,
                                   Warehouse warehouse) {
        location.setWarehouse(warehouse);
        location.setCode(form.getCode());
        location.setName(form.getName());
        location.setActive(form.isActive());
    }
}
