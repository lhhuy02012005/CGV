package com.cgv.catalogservice.specification;

import com.cgv.catalogservice.entity.Article;
import com.cgv.catalogservice.enums.ArticleCategory;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class ArticleSpecification {

    private ArticleSpecification() {
    }

    public static Specification<Article> containsKeyword(
            String keyword
    ) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String pattern =
                    "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(
                            cb.lower(root.get("title")),
                            pattern
                    ),
                    cb.like(
                            cb.lower(root.get("excerpt")),
                            pattern
                    ),
                    cb.like(
                            cb.lower(root.get("authorName")),
                            pattern
                    )
            );
        };
    }

    public static Specification<Article> hasCategory(
            ArticleCategory category
    ) {
        return (root, query, cb) ->
                category == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("category"),
                        category
                );
    }

    public static Specification<Article> isFeatured(
            Boolean featured
    ) {
        return (root, query, cb) ->
                featured == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("featured"),
                        featured
                );
    }

    public static Specification<Article> isTrending(
            Boolean trending
    ) {
        return (root, query, cb) ->
                trending == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("trending"),
                        trending
                );
    }

    public static Specification<Article> hasMovieId(
            UUID movieId
    ) {
        return (root, query, cb) ->
                movieId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("movie").get("id"),
                        movieId
                );
    }

    public static Specification<Article> isPublished(
            Boolean published
    ) {
        return (root, query, cb) -> {
            if (published == null) {
                return cb.conjunction();
            }

            return published
                    ? cb.isNotNull(root.get("publishedAt"))
                    : cb.isNull(root.get("publishedAt"));
        };
    }

    public static Specification<Article> publishedFrom(
            Instant from
    ) {
        return (root, query, cb) ->
                from == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("publishedAt"),
                        from
                );
    }

    public static Specification<Article> publishedTo(
            Instant to
    ) {
        return (root, query, cb) ->
                to == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("publishedAt"),
                        to
                );
    }
}