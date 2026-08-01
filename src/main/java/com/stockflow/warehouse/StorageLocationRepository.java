package com.stockflow.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorageLocationRepository
        extends JpaRepository<StorageLocation, Long> {

    @Query("""
            select location
            from StorageLocation location
            join fetch location.warehouse warehouse
            where location.active = true
              and warehouse.active = true
            order by warehouse.name, location.name
            """)
    List<StorageLocation> findAllActiveWithWarehouse();

    @Query("""
            select location
            from StorageLocation location
            join fetch location.warehouse
            where location.id = :id
            """)
    Optional<StorageLocation> findByIdWithWarehouse(
            @Param("id") Long id
    );
}