package com.matheushenrique.nexum.repositories;

import com.matheushenrique.nexum.entities.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByNameAndActiveTrueAndOwnerId(String name, UUID ownerId);

    boolean existsByNameAndActiveTrueAndIdNotAndOwnerId(String name, UUID id, UUID ownerId);

    @Query("""
        SELECT p FROM Plan p
        WHERE p.owner.id = :ownerId
        AND (:active IS NULL OR p.active = :active)
        AND (:search IS NULL
             OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    """)
    Page<Plan> findAllByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}