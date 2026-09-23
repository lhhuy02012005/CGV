package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Cinema;
import jakarta.validation.constraints.NotBlank;
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

    Optional<Cinema> findByName(String name);

    boolean existsById(UUID id);

    @Query("SELECT c FROM Cinema c JOIN FETCH c.region WHERE c.status = :status")
    List<Cinema> findByStatusWithRegion(CinemaStatus status);

    boolean existsByName(@NotBlank(message = "Tên rạp không được để trống") String name);

    boolean existsByNameAndIdNot(String name, UUID cinemaId);
}
