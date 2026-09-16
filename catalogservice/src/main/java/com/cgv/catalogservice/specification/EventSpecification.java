package com.cgv.catalogservice.specification;

import com.cgv.catalogservice.entity.Event;
import com.cgv.catalogservice.enums.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> containsKeyword(
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
                            cb.lower(root.get("locationName")),
                            pattern
                    ),
                    cb.like(
                            cb.lower(root.get("description")),
                            pattern
                    )
            );
        };
    }

    public static Specification<Event> hasStatus(
            EventStatus status
    ) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasCinemaId(
            UUID cinemaId
    ) {
        return (root, query, cb) ->
                cinemaId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("cinema").get("id"),
                        cinemaId
                );
    }

    public static Specification<Event> eventDateFrom(
            LocalDate from
    ) {
        return (root, query, cb) ->
                from == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("eventDate"),
                        from
                );
    }

    public static Specification<Event> eventDateTo(
            LocalDate to
    ) {
        return (root, query, cb) ->
                to == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("eventDate"),
                        to
                );
    }

    public static Specification<Event> hasEventDate(
            LocalDate eventDate
    ) {
        return (root, query, cb) ->
                eventDate == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("eventDate"),
                        eventDate
                );
    }
}