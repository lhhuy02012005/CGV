package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Integer> {

    Optional<Region> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Integer regionId
    );

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(
            String slug,
            Integer regionId
    );
}
