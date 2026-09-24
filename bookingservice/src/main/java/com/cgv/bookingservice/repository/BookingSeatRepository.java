package com.cgv.bookingservice.repository;

import com.cgv.bookingservice.entity.BookingSeat;
import com.cgv.bookingservice.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {

    @Query("SELECT bs.seatId FROM BookingSeat bs WHERE bs.showtimeId = :showtimeId AND bs.booking.status IN :statuses")
    List<UUID> findBookedSeatIdsByShowtimeId(
            @Param("showtimeId") UUID showtimeId,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}
