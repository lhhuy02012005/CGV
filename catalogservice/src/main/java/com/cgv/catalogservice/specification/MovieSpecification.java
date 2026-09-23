package com.cgv.catalogservice.specification;

import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.MovieGenre;
import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class MovieSpecification {

    private MovieSpecification() {
    }

    public static Specification<Movie> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("originalTitle")), pattern),
                    cb.like(cb.lower(root.get("director")), pattern)
            );
        };
    }

    public static Specification<Movie> hasShowingStatus(
            ShowingStatus showingStatus
    ) {
        return (root, query, cb) ->
                showingStatus == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("showingStatus"),
                        showingStatus
                );
    }

    public static Specification<Movie> hasStatus(
            MovieStatus status
    ) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Movie> hasGenreId(
            Integer genreId
    ) {
        return (root, query, cb) -> {
            if (genreId == null) {
                return cb.conjunction();
            }

            Subquery<Integer> subquery =
                    query.subquery(Integer.class);

            Root<MovieGenre> movieGenre =
                    subquery.from(MovieGenre.class);

            subquery.select(cb.literal(1));

            subquery.where(
                    cb.equal(
                            movieGenre.get("id").get("movieId"),
                            root.get("id")
                    ),
                    cb.equal(
                            movieGenre.get("id").get("genreId"),
                            genreId
                    )
            );

            return cb.exists(subquery);
        };
    }

    public static Specification<Movie> hasGenreSlug(
            String genreSlug
    ) {
        return (root, query, cb) -> {
            if (genreSlug == null || genreSlug.isBlank()) {
                return cb.conjunction();
            }

            Subquery<Integer> subquery =
                    query.subquery(Integer.class);

            Root<MovieGenre> movieGenre =
                    subquery.from(MovieGenre.class);

            subquery.select(cb.literal(1));

            subquery.where(
                    cb.equal(
                            movieGenre.get("id").get("movieId"),
                            root.get("id")
                    ),
                    cb.equal(
                            movieGenre.get("genre").get("slug"),
                            genreSlug
                    )
            );

            return cb.exists(subquery);
        };
    }

    public static Specification<Movie> releaseDateFrom(
            LocalDate from
    ) {
        return (root, query, cb) ->
                from == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("releaseDate"),
                        from
                );
    }

    public static Specification<Movie> releaseDateTo(
            LocalDate to
    ) {
        return (root, query, cb) ->
                to == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("releaseDate"),
                        to
                );
    }

    public static Specification<Movie> durationGreaterThanOrEqual(
            Integer minDuration
    ) {
        return (root, query, cb) ->
                minDuration == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("durationMinutes"),
                        minDuration
                );
    }

    public static Specification<Movie> durationLessThanOrEqual(
            Integer maxDuration
    ) {
        return (root, query, cb) ->
                maxDuration == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("durationMinutes"),
                        maxDuration
                );
    }

    public static Specification<Movie> hasLanguage(
            String language
    ) {
        return (root, query, cb) -> {
            if (language == null || language.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.get("language")),
                    language.trim().toLowerCase()
            );
        };
    }

    public static Specification<Movie> hasAgeRating(
            String ageRating
    ) {
        return (root, query, cb) -> {
            if (ageRating == null || ageRating.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.get("ageRating")),
                    ageRating.trim().toLowerCase()
            );
        };
    }

    public static Specification<Movie> isFeatured(
            Boolean isFeatured
    ) {
        return (root, query, cb) ->
                isFeatured == null
                        ? cb.conjunction()
                        : cb.equal(root.get("isFeatured"), isFeatured);
    }
}