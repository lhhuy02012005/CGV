package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Integer> {

    Optional<Region> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
