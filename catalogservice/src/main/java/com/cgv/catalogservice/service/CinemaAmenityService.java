package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.cinemaamenity.CinemaAmenityCreateRequest;
import com.cgv.catalogservice.dto.response.CinemaAmenityResponse;
import com.cgv.catalogservice.enums.Amenity;

import java.util.List;
import java.util.UUID;

public interface CinemaAmenityService {

    CinemaAmenityResponse createCinemaAmenity(
            CinemaAmenityCreateRequest request
    );

    void deleteCinemaAmenity(
            UUID cinemaId,
            Amenity amenity
    );

    List<CinemaAmenityResponse> getAmenitiesByCinemaId(
            UUID cinemaId
    );
}
