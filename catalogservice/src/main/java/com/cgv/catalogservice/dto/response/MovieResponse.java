package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MovieResponse(
        UUID id,
        String title,
        String originalTitle,
        String synopsis,
        String director,
        String language,
        String subtitle,
        String ageRating,
        Integer durationMinutes,
        LocalDate releaseDate,
        LocalDate endDate,
        ShowingStatus showingStatus,
        String posterUrl,
        String backdropUrl,
        String trailerYoutubeUrl,
        MovieStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
