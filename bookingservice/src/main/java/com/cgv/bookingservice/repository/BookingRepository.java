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

import java.math.BigDecimal;
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

    @EntityGraph(attributePaths = {"bookingSeats"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = {"bookingSeats"})
    Page<Booking> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @EntityGraph(attributePaths = {"bookingSeats"})
    List<Booking> findByGuestPhoneOrderByCreatedAtDesc(String guestPhone);

    @EntityGraph(attributePaths = {"bookingSeats"})
    List<Booking> findByGuestEmailOrderByCreatedAtDesc(String guestEmail);

    @EntityGraph(attributePaths = {"bookingSeats"})
    List<Booking> findByGuestPhoneAndGuestEmailOrderByCreatedAtDesc(String guestPhone, String guestEmail);

    @Query("""
        SELECT b FROM Booking b 
        WHERE (:keyword IS NULL OR :keyword = ''
           OR LOWER(b.guestPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(b.guestEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(b.guestName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(b.userId) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY b.createdAt DESC
    """)
    Page<Booking> searchBookingsByKeyword(@Param("keyword") String keyword, Pageable pageable);

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

    List<Booking> findByStatusAndPaymentDeadlineBefore(BookingStatus status, Instant deadline);

    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumTotalRevenueByStatuses(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Booking b WHERE b.status IN :statuses AND b.createdAt >= :since")
    BigDecimal sumRevenueByStatusesAndCreatedAtAfter(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("since") Instant since
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN :statuses AND b.createdAt >= :since")
    long countBookingsByStatusesAndCreatedAtAfter(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("since") Instant since
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN :statuses")
    long countBookingsByStatuses(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b.status, COUNT(b) FROM Booking b GROUP BY b.status")
    List<Object[]> countBookingsByStatus();

    @Query("""
        SELECT b.showtimeId, COUNT(b), COALESCE(SUM(b.finalAmount), 0)
        FROM Booking b
        WHERE b.status IN :statuses
        GROUP BY b.showtimeId
        ORDER BY COUNT(b) DESC
    """)
    List<Object[]> findTopShowtimesByBookings(@Param("statuses") List<BookingStatus> statuses, Pageable pageable);
}

