package com.cgv.catalogservice.dto.request.cinema;

import com.cgv.catalogservice.enums.Amenity;
import com.cgv.catalogservice.enums.CinemaStatus;

public record CinemaFilterRequest(

        String keyword,

        Integer regionId,

        String regionSlug,

        CinemaStatus status,

        Amenity amenity

) {
}
