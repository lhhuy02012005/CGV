package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;

import java.time.Instant;
import java.util.UUID;

public record MovieResponse(
        UUID id,
        String title,
        String originalTitle,
        String synopsis,
        String director,
        String language,
        String subtitle,
        String supportedModes,
        String ageRating,
        Integer durationMinutes,
        Instant releaseDate,
        Instant endDate,
        ShowingStatus showingStatus,
        String posterUrl,
        String backdropUrl,
        String trailerYoutubeUrl,
        Boolean isFeatured,
        MovieStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
