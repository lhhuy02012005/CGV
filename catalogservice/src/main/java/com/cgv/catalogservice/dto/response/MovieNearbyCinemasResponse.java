package com.cgv.catalogservice.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record MovieNearbyCinemasResponse(
        UUID movieId,
        String movieTitle,
        String posterUrl,
        LocalDate date,
        List<RegionResponse> availableRegions,
        List<CinemaWithShowtimesResponse> cinemas
) {
}
