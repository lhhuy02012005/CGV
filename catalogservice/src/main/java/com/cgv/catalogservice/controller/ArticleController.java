package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.article.ArticleCreateRequest;
import com.cgv.catalogservice.dto.request.article.ArticleFilterRequest;
import com.cgv.catalogservice.dto.request.article.ArticleUpdateRequest;
import com.cgv.catalogservice.dto.response.ArticleResponse;
import com.cgv.catalogservice.enums.ArticleCategory;
import com.cgv.catalogservice.service.ArticleService;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArticleController {

    ArticleService articleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArticleResponse> create(
            @RequestBody @Valid ArticleCreateRequest request
    ) {

        ArticleResponse response =
                articleService.createArticle(request);

        return ApiResponse.<ArticleResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo bài viết thành công")
                .build();
    }

    @PatchMapping("/{articleId}")
    public ApiResponse<ArticleResponse> update(
            @PathVariable UUID articleId,
            @RequestBody @Valid ArticleUpdateRequest request
    ) {

        ArticleResponse response =
                articleService.updateArticle(articleId, request);

        return ApiResponse.<ArticleResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật bài viết thành công")
                .build();
    }

    @DeleteMapping("/{articleId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID articleId
    ) {

        articleService.deleteArticle(articleId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá bài viết thành công")
                .build();
    }

    @GetMapping("/{articleId}")
    public ApiResponse<ArticleResponse> get(
            @PathVariable UUID articleId
    ) {

        ArticleResponse response =
                articleService.getArticleById(articleId);

        return ApiResponse.<ArticleResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết bài viết thành công")
                .build();
    }

    @GetMapping("/category/{category}")
    public ApiResponse<PageResponse<ArticleResponse>> getArticlesByCategory(
            @PathVariable ArticleCategory category,
            @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getArticlesByCategory(category, pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết theo loại")
                .build();
    }

    @GetMapping("/featured")
    public ApiResponse<PageResponse<ArticleResponse>> getFeaturedArticles(
            @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getFeaturedArticles(pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết nổi bật")
                .build();
    }

    @GetMapping("/trending")
    public ApiResponse<PageResponse<ArticleResponse>> getTrendingArticles(
            @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getTrendingArticles(pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết thịnh hành")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<ArticleResponse>> findAll(
            @Valid @ModelAttribute ArticleFilterRequest filter,
            @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getAllArticles(filter, pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết")
                .build();
    }
}