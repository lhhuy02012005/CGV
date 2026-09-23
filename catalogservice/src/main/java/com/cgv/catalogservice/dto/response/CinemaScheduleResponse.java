package com.cgv.catalogservice.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record CinemaScheduleResponse(
        UUID cinemaId,
        String cinemaName,
        String address,
        Double latitude,
        Double longitude,
        LocalDate date,
        List<MovieWithShowtimesResponse> movies
) {
}
