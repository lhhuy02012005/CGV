package com.cgv.catalogservice.dto.request.cinemaamenity;

import com.cgv.catalogservice.enums.Amenity;

import java.util.UUID;

public record CinemaAmenityUpdateRequest(
        UUID cinemaId,
        Amenity amenity
) {
}