package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.article.ArticleCreateRequest;
import com.cgv.catalogservice.dto.request.article.ArticleFilterRequest;
import com.cgv.catalogservice.dto.request.article.ArticleUpdateRequest;
import com.cgv.catalogservice.dto.response.ArticleResponse;
import com.cgv.catalogservice.entity.Article;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.enums.ArticleCategory;
import com.cgv.catalogservice.mapper.ArticleMapper;
import com.cgv.catalogservice.repository.ArticleRepository;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.service.ArticleService;
import com.cgv.catalogservice.specification.ArticleSpecification;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.catalogservice.util.SlugUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArticleServiceImpl implements ArticleService {

    ArticleRepository articleRepository;
    MovieRepository movieRepository;

    ArticleMapper articleMapper;

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "articlesByCategory",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "featuredArticles",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "trendingArticles",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public ArticleResponse createArticle(
            ArticleCreateRequest request
    ) {
        String title = request.title();

        String slug = SlugUtils.generateUniqueSlug(
                title,
                articleRepository::existsBySlug
        );

        Article article = articleMapper.toEntity(request);

        article.setSlug(slug);

        if (request.movieId() != null) {
            Movie movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy movie với id: "
                                            + request.movieId()
                            )
                    );

            article.setMovie(movie);
        }

        Article savedArticle =
                articleRepository.save(article);

        return articleMapper.toResponse(savedArticle);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "articlesByCategory",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "featuredArticles",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "trendingArticles",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public ArticleResponse updateArticle(
            UUID articleId,
            ArticleUpdateRequest request
    ) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy bài viết với id: "
                                        + articleId
                        )
                );

        String newSlug = null;

        if (request.title() != null) {

            String newTitle = request.title();

            if (newTitle.isBlank()) {
                throw new IllegalArgumentException(
                        "Tiêu đề không được để trống"
                );
            }

            newSlug = SlugUtils.generateUniqueSlug(
                    newTitle,
                    slug -> articleRepository.existsBySlugAndIdNot(
                            slug,
                            articleId
                    )
            );
        }

        articleMapper.updateEntity(request, article);

        if (newSlug != null) {
            article.setSlug(newSlug);
        }

        if (request.movieId() != null) {
            Movie movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy movie với id: "
                                            + request.movieId()
                            )
                    );

            article.setMovie(movie);
        }

        articleRepository.saveAndFlush(article);

        return articleMapper.toResponse(article);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "articlesByCategory",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "featuredArticles",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "trendingArticles",
                            allEntries = true
                    )
            }
    )
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
    @Cacheable(
            value = "articlesByCategory",
            key = "#category"
                    + " + ':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getArticlesByCategory(ArticleCategory category, Pageable pageable) {

        Specification<Article> spec =
                Specification.allOf(
                        ArticleSpecification.hasCategory(category)
                );

        return PageResponseUtils.findAllAndMap(
                p  -> articleRepository.findAll(spec, p),
                pageable,
                articleMapper::toResponseList
        );
    }

    @Override
    @Cacheable(
            value = "featuredArticles",
            key = "':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getFeaturedArticles(Pageable pageable) {

        Specification<Article> spec =
                Specification.allOf(
                        ArticleSpecification.isFeatured(Boolean.TRUE)
                );

        return PageResponseUtils.findAllAndMap(
                p  -> articleRepository.findAll(spec, p),
                pageable,
                articleMapper::toResponseList
        );
    }

    @Override
    @Cacheable(
            value = "trendingArticles",
            key = "':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getTrendingArticles(Pageable pageable) {

        Specification<Article> spec =
                Specification.allOf(
                        ArticleSpecification.isTrending(Boolean.TRUE)
                );

        return PageResponseUtils.findAllAndMap(
                p  -> articleRepository.findAll(spec, p),
                pageable,
                articleMapper::toResponseList
        );
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

        return PageResponseUtils.findAllAndMap(
                p  -> articleRepository.findAll(spec, p),
                pageable,
                articleMapper::toResponseList
        );
    }
}
