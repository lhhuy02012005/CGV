package com.cgv.catalogservice.dto.request.moviegenre;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MovieGenreCreateRequest(

        @NotNull(message = "Movie ID không được để trống")
        UUID movieId,

        @NotNull(message = "Genre ID không được để trống")
        Integer genreId
) {
}