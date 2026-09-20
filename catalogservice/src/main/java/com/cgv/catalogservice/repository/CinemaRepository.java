package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CinemaRepository
        extends JpaRepository<Cinema, UUID>,
        JpaSpecificationExecutor<Cinema> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsById(UUID cinemaId);

    boolean existsByRegionId(Integer regionId);
}
