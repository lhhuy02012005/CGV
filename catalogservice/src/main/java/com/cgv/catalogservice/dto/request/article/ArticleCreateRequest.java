package com.cgv.catalogservice.dto.request.article;

import com.cgv.catalogservice.enums.ArticleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record ArticleCreateRequest(

        @NotBlank(message = "Tiêu đề không được để trống")
        String title,

        String thumbnailUrl,

        @NotBlank(message = "Nội dung không được để trống")
        String content,

        @Size(max = 500, message = "Excerpt tối đa 500 ký tự")
        String excerpt,

        @NotNull(message = "Danh mục không được để trống")
        ArticleCategory category,

        String tags,

        String authorName,

        Boolean featured,

        Boolean trending,

        UUID movieId,

        Instant publishedAt
) {
}