package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.CinemaStatus;

import java.time.Instant;
import java.util.UUID;

public record CinemaResponse(
        UUID id,
        Integer regionId,
        String regionName,
        String name,
        String address,
        String phone,
        String openingHours,
        Double latitude,
        Double longitude,
        CinemaStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
