package com.cgv.catalogservice.dto.request.article;

import com.cgv.catalogservice.enums.ArticleCategory;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleUpdateRequest(

        String title,

        String thumbnailUrl,

        String content,

        @Size(max = 500, message = "Excerpt tối đa 500 ký tự")
        String excerpt,

        ArticleCategory category,

        String tags,

        String authorName,

        Boolean featured,

        Boolean trending,

        UUID movieId,

        LocalDateTime publishedAt
) {
}