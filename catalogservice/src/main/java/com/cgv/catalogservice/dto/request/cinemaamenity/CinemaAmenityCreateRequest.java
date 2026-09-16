package com.cgv.catalogservice.dto.request.cinemaamenity;

import com.cgv.catalogservice.enums.Amenity;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CinemaAmenityCreateRequest(

        @NotNull(message = "Cinema ID không được để trống")
        UUID cinemaId,

        @NotNull(message = "Tiện ích không được để trống")
        Amenity amenity
) {
}