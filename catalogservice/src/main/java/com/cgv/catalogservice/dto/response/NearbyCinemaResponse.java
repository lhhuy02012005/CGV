package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.CinemaStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record NearbyCinemaResponse(
        UUID id,
        Integer regionId,
        String regionName,
        String name,
        String address,
        String phone,
        String openingHours,
        Double latitude,
        Double longitude,
        Double distanceInKm,
        CinemaStatus status
) {
}
