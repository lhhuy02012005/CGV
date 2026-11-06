package com.cgv.bookingservice.repository;
import com.cgv.bookingservice.entity.SeatClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface SeatClaimRepository extends JpaRepository<SeatClaim, UUID> {
    @Modifying
    @Query("delete from SeatClaim c where c.bookingId = :bookingId")
    void releaseByBookingId(@Param("bookingId") UUID bookingId);
}
