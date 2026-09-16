package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.ArticleCategory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String title,
        String slug,
        String thumbnailUrl,
        String content,
        String excerpt,
        ArticleCategory category,
        String tags,
        String authorName,
        Integer views,
        Boolean featured,
        Boolean trending,
        UUID movieId,
        String movieTitle,
        LocalDateTime publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
