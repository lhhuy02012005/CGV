package com.cgv.bookingservice.repository;

import com.cgv.bookingservice.entity.Booking;
import com.cgv.bookingservice.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {
    @Override
    @EntityGraph(attributePaths = {"bookingSeats"})
    Optional<Booking> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"bookingSeats"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);

    @Modifying
    @Query("""
        UPDATE Booking b
        SET b.status = :cancelledStatus,
            b.cancelledAt = :now
        WHERE b.status = :pendingStatus 
          AND b.paymentDeadline < :now
    """)
    int cancelExpiredBookings(
            @Param("cancelledStatus") BookingStatus cancelledStatus,
            @Param("pendingStatus") BookingStatus pendingStatus,
            @Param("now") Instant now
    );

    Page<Booking> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Booking> findByStatusAndPaymentDeadlineBefore(BookingStatus status, Instant deadline);
}
