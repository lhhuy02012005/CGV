package com.cgv.catalogservice.dto.response;

import java.util.UUID;

public record MovieGenreResponse(
        UUID movieId,
        String movieTitle,
        Integer genreId,
        String genreName,
        String genreSlug
) {
}
