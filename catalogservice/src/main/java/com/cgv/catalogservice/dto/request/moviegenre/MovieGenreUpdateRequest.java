package com.cgv.catalogservice.dto.request.moviegenre;

import java.util.UUID;

public record MovieGenreUpdateRequest(
        UUID movieId,
        Integer genreId
) {
}