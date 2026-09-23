package com.cgv.catalogservice.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CinemaWithShowtimesResponse(
        UUID cinemaId,
        String cinemaName,
        String address,
        Integer regionId,
        String regionName,
        Double latitude,
        Double longitude,
        Double distanceInKm,
        List<ShowtimeSlotResponse> showtimes
) {
}
