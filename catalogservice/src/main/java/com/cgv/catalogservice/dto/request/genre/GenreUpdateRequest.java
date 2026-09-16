package com.cgv.catalogservice.dto.request.genre;

public record GenreUpdateRequest(
        String name,
        String slug
) {
}