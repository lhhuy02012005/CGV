package com.cgv.bookingservice.specification;

import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.entity.Booking;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {
    }

    public static Specification<Booking> filterMyBookings(String userId, BookingFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Bắt buộc theo userId
            predicates.add(cb.equal(root.get("userId"), userId));

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
                }

                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
