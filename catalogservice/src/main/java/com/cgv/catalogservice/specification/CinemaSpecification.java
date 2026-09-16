package com.cgv.catalogservice.specification;

import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.CinemaAmenity;
import com.cgv.catalogservice.enums.Amenity;
import com.cgv.catalogservice.enums.CinemaStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class CinemaSpecification {

    private CinemaSpecification() {
    }

    public static Specification<Cinema> containsKeyword(
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
                            cb.lower(root.get("name")),
                            pattern
                    ),
                    cb.like(
                            cb.lower(root.get("address")),
                            pattern
                    )
            );
        };
    }

    public static Specification<Cinema> hasRegionId(
            Integer regionId
    ) {
        return (root, query, cb) ->
                regionId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("region").get("id"),
                        regionId
                );
    }

    public static Specification<Cinema> hasRegionSlug(
            String regionSlug
    ) {
        return (root, query, cb) -> {
            if (regionSlug == null || regionSlug.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("region").get("slug"),
                    regionSlug
            );
        };
    }

    public static Specification<Cinema> hasStatus(
            CinemaStatus status
    ) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Cinema> hasAmenity(
            Amenity amenity
    ) {
        return (root, query, cb) -> {
            if (amenity == null) {
                return cb.conjunction();
            }

            Subquery<Integer> subquery =
                    query.subquery(Integer.class);

            Root<CinemaAmenity> cinemaAmenity =
                    subquery.from(CinemaAmenity.class);

            subquery.select(cb.literal(1));

            subquery.where(
                    cb.equal(
                            cinemaAmenity
                                    .get("id")
                                    .get("cinemaId"),
                            root.get("id")
                    ),
                    cb.equal(
                            cinemaAmenity
                                    .get("id")
                                    .get("amenity"),
                            amenity
                    )
            );

            return cb.exists(subquery);
        };
    }
}