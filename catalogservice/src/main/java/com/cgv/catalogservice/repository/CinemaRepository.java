package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cgv.catalogservice.enums.CinemaStatus;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CinemaRepository
        extends JpaRepository<Cinema, UUID>,
        JpaSpecificationExecutor<Cinema> {

    @Override
    @EntityGraph(attributePaths = {"region"})
    Optional<Cinema> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"region"})
    Page<Cinema> findAll(Specification<Cinema> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"region"})
    Optional<Cinema> findByName(String name);

    boolean existsById(UUID id);

    boolean existsByRegionId(Integer regionId);

    boolean existsByRegion_Id(Integer regionId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("SELECT c FROM Cinema c JOIN FETCH c.region WHERE c.status = :status")
    List<Cinema> findByStatusWithRegion(CinemaStatus status);
}
