package com.astrotech.transport.repositories;

import com.astrotech.transport.entities.Region;
import com.astrotech.transport.enums.RegionName;
import com.astrotech.transport.projection.ExistingRegionFields;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    Slice<Region> findAllBy(Pageable pageable);
    @EntityGraph(attributePaths = {
            "terminals",
            "regionalSupervisor"
    })
    Optional<Region> findWithTerminalsById(UUID id);
    boolean existsByNameAndIdNot(RegionName name, UUID id);

    boolean existsByRegionalSupervisorIdAndIdNot(UUID supervisorId, UUID regionId);
    @Query("""
            SELECT
                r.name AS name,
                r.regionalSupervisor.id AS supervisorId
            FROM Region r
            WHERE
                   r.name = :name
                OR r.regionalSupervisor.id = :supervisorId
            """)
    List<ExistingRegionFields> findExistingFields(
            RegionName name,
            UUID supervisorId
    );
}
