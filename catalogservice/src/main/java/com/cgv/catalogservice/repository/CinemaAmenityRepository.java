package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.CinemaAmenity;
import com.cgv.catalogservice.entity.CinemaAmenityId;
import com.cgv.catalogservice.enums.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CinemaAmenityRepository
        extends JpaRepository<CinemaAmenity, CinemaAmenityId> {

    List<CinemaAmenity> findByIdCinemaId(UUID cinemaId);

    List<CinemaAmenity> findByIdAmenity(Amenity amenity);

    boolean existsByIdCinemaIdAndIdAmenity(
            UUID cinemaId,
            Amenity amenity
    );
}
