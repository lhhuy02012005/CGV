package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.article.ArticleCreateRequest;
import com.cgv.catalogservice.dto.request.article.ArticleFilterRequest;
import com.cgv.catalogservice.dto.request.article.ArticleUpdateRequest;
import com.cgv.catalogservice.dto.response.ArticleResponse;
import com.cgv.catalogservice.entity.Article;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.mapper.ArticleMapper;
import com.cgv.catalogservice.repository.ArticleRepository;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.service.ArticleService;
import com.cgv.catalogservice.specification.ArticleSpecification;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArticleServiceImpl implements ArticleService {

    ArticleRepository articleRepository;
    MovieRepository movieRepository;

    ArticleMapper articleMapper;

    @Override
    @Transactional
    public ArticleResponse createArticle(ArticleCreateRequest request) {

        Article article = articleMapper.toEntity(request);

        if (request.movieId() != null) {
            Movie movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy movie với id: " + request.movieId()
                            )
                    );

            article.setMovie(movie);
        }

        Article savedArticle = articleRepository.save(article);

        return articleMapper.toResponse(savedArticle);
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(
            UUID articleId,
            ArticleUpdateRequest request
    ) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy bài viết với id: " + articleId
                        )
                );

        articleMapper.updateEntity(request, article);

        if (request.movieId() != null) {
            Movie movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy movie với id: " + request.movieId()
                            )
                    );

            article.setMovie(movie);
        }

        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional
    public void deleteArticle(UUID articleId) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy bài viết với id: " + articleId
                        )
                );

        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public ArticleResponse getArticleById(UUID articleId) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy bài viết với id: " + articleId
                        )
                );

        int currentViews =
                article.getViews() != null
                        ? article.getViews()
                        : 0;

        article.setViews(currentViews + 1);

        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getAllArticles(
            ArticleFilterRequest filter,
            Pageable pageable
    ) {

        Specification<Article> spec =
                Specification.allOf(
                        ArticleSpecification.containsKeyword(
                                filter.keyword()
                        ),
                        ArticleSpecification.hasCategory(
                                filter.category()
                        ),
                        ArticleSpecification.isFeatured(
                                filter.featured()
                        ),
                        ArticleSpecification.isTrending(
                                filter.trending()
                        ),
                        ArticleSpecification.hasMovieId(
                                filter.movieId()
                        ),
                        ArticleSpecification.isPublished(
                                filter.published()
                        ),
                        ArticleSpecification.publishedFrom(
                                filter.publishedFrom()
                        ),
                        ArticleSpecification.publishedTo(
                                filter.publishedTo()
                        )
                );

        Page<Article> articlePage =
                articleRepository.findAll(spec, pageable);

        List<ArticleResponse> articleResponses =
                articleMapper.toResponseList(
                        articlePage.getContent()
                );

        return PageResponse.<ArticleResponse>builder()
                .data(articleResponses)
                .pageNumber(articlePage.getNumber() + 1)
                .pageSize(articlePage.getSize())
                .totalPages(articlePage.getTotalPages())
                .totalElements(articlePage.getTotalElements())
                .build();
    }
}
