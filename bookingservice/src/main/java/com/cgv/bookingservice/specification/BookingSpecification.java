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

    /** Tìm kiếm đơn cho admin: từ khóa (mã đơn, email, SĐT, tên, userId) + trạng thái + khoảng thời gian đặt. */
    public static Specification<Booking> adminSearch(String keyword, BookingFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String trimmed = keyword.trim();
                String pattern = "%" + trimmed.toLowerCase() + "%";
                List<Predicate> keywordPredicates = new ArrayList<>(List.of(
                        cb.like(cb.lower(root.get("guestPhone")), pattern),
                        cb.like(cb.lower(root.get("guestEmail")), pattern),
                        cb.like(cb.lower(root.get("guestName")), pattern),
                        cb.like(cb.lower(root.get("userId")), pattern)
                ));
                java.util.regex.Matcher uuid = java.util.regex.Pattern
                        .compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                        .matcher(trimmed);
                if (uuid.find()) {
                    keywordPredicates.add(cb.equal(root.get("id"), java.util.UUID.fromString(uuid.group())));
                }
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThan(root.get("createdAt"), filter.getToDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
