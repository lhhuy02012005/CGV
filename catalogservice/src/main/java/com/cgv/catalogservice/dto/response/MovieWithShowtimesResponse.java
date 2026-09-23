package com.cgv.catalogservice.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record MovieWithShowtimesResponse(
        UUID movieId,
        String movieTitle,
        String posterUrl,
        String ageRating,
        Integer durationMinutes,
        String language,
        String supportedModes,
        List<ShowtimeSlotResponse> showtimes
) {
}
