package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
        LocalDate releaseDate,
        LocalDate endDate,
        ShowingStatus showingStatus,
        String posterUrl,
        String backdropUrl,
        String trailerYoutubeUrl,
        Boolean isFeatured,
        MovieStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<MovieCastResponse> casts
) {
    public MovieResponse withCasts(List<MovieCastResponse> newCasts) {
        return new MovieResponse(
                id, title, originalTitle, synopsis, director, language, subtitle,
                supportedModes, ageRating, durationMinutes, releaseDate, endDate,
                showingStatus, posterUrl, backdropUrl, trailerYoutubeUrl,
                isFeatured, status, createdAt, updatedAt, newCasts
        );
    }
}
