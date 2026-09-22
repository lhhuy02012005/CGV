package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.article.ArticleCreateRequest;
import com.cgv.catalogservice.dto.request.article.ArticleFilterRequest;
import com.cgv.catalogservice.dto.request.article.ArticleUpdateRequest;
import com.cgv.catalogservice.dto.response.ArticleResponse;
import com.cgv.catalogservice.enums.ArticleCategory;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ArticleService {

    ArticleResponse createArticle(
            ArticleCreateRequest request
    );

    ArticleResponse updateArticle(
            UUID articleId,
            ArticleUpdateRequest request
    );

    void deleteArticle(
            UUID articleId
    );

    ArticleResponse getArticleById(
            UUID articleId
    );

    PageResponse<ArticleResponse> getArticlesByCategory(ArticleCategory category, Pageable pageable);

    PageResponse<ArticleResponse> getFeaturedArticles(Pageable pageable);

    PageResponse<ArticleResponse> getTrendingArticles(Pageable pageable);

    PageResponse<ArticleResponse> getAllArticles(
            ArticleFilterRequest filter,
            Pageable pageable
    );
}
