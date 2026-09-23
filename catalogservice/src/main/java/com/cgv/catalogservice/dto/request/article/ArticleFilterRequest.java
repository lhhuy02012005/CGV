package com.cgv.catalogservice.dto.request.article;

import com.cgv.catalogservice.enums.ArticleCategory;

import java.time.Instant;
import java.util.UUID;

public record ArticleFilterRequest(

        String keyword,

        ArticleCategory category,

        Boolean featured,

        Boolean trending,

        UUID movieId,

        Boolean published,

        Instant publishedFrom,

        Instant publishedTo

) {
}
