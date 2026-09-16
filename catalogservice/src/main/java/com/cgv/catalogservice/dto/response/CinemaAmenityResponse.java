package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.Amenity;

import java.util.UUID;

public record CinemaAmenityResponse(
        UUID cinemaId,
        String cinemaName,
        Amenity amenity
) {
}
