package com.cgv.catalogservice.specification;

import com.cgv.catalogservice.entity.Showtime;
import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ShowtimeSpecification {

    private ShowtimeSpecification() {
    }

    public static Specification<Showtime> hasMovieId(
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

    public static Specification<Showtime> hasRoomId(
            UUID roomId
    ) {
        return (root, query, cb) ->
                roomId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("room").get("id"),
                        roomId
                );
    }

    public static Specification<Showtime> hasCinemaId(
            UUID cinemaId
    ) {
        return (root, query, cb) ->
                cinemaId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root
                                .get("room")
                                .get("cinema")
                                .get("id"),
                        cinemaId
                );
    }

    public static Specification<Showtime> hasShowDate(
            Instant showDate
    ) {
        return (root, query, cb) ->
                showDate == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("showDate"),
                        showDate
                );
    }

    public static Specification<Showtime> showDateFrom(
            Instant from
    ) {
        return (root, query, cb) ->
                from == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("showDate"),
                        from
                );
    }

    public static Specification<Showtime> showDateTo(
            Instant to
    ) {
        return (root, query, cb) ->
                to == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("showDate"),
                        to
                );
    }

    public static Specification<Showtime> startsAtOrAfter(
            Instant startTime
    ) {
        return (root, query, cb) ->
                startTime == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("startTime"),
                        startTime
                );
    }

    public static Specification<Showtime> startsAtOrBefore(
            Instant startTime
    ) {
        return (root, query, cb) ->
                startTime == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("startTime"),
                        startTime
                );
    }

    public static Specification<Showtime> hasStatus(
            ShowtimeStatus status
    ) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Showtime> hasFormat(
            Format format
    ) {
        return (root, query, cb) ->
                format == null
                        ? cb.conjunction()
                        : cb.equal(root.get("format"), format);
    }

    public static Specification<Showtime> hasLanguage(
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

    public static Specification<Showtime> hasSubtitleLanguage(
            String subtitleLanguage
    ) {
        return (root, query, cb) -> {
            if (subtitleLanguage == null
                    || subtitleLanguage.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.get("subtitleLanguage")),
                    subtitleLanguage.trim().toLowerCase()
            );
        };
    }

    public static Specification<Showtime> priceGreaterThanOrEqual(
            BigDecimal minPrice
    ) {
        return (root, query, cb) ->
                minPrice == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("basePrice"),
                        minPrice
                );
    }

    public static Specification<Showtime> priceLessThanOrEqual(
            BigDecimal maxPrice
    ) {
        return (root, query, cb) ->
                maxPrice == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("basePrice"),
                        maxPrice
                );
    }

    public static Specification<Showtime> hasAvailableSeats(
            Boolean available
    ) {
        return (root, query, cb) -> {
            if (available == null) {
                return cb.conjunction();
            }

            if (available) {
                return cb.greaterThan(
                        root.get("availableSeats"),
                        0
                );
            }

            return cb.lessThanOrEqualTo(
                    root.get("availableSeats"),
                    0
            );
        };
    }
}