package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CinemaRepository
        extends JpaRepository<Cinema, UUID>,
        JpaSpecificationExecutor<Cinema> {

    Optional<Cinema> findByName(String name);

    boolean existsByName(String name);
}
