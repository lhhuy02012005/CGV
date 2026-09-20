package com.cgv.bookingservice.repository;

import com.cgv.bookingservice.entity.SeatLock;
import com.cgv.bookingservice.enums.SeatLockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, UUID> {
    List<SeatLock> findByShowtimeIdAndStatus(UUID showtimeId , SeatLockStatus status);
}
