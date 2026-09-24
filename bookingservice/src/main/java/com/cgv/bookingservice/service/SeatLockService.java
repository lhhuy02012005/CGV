package com.cgv.bookingservice.service;

import com.cgv.bookingservice.dto.request.SeatLockRequest;
import com.cgv.bookingservice.dto.response.SeatLockResponse;

import java.util.List;
import java.util.UUID;

public interface SeatLockService {
    SeatLockResponse lockSeats(String userId , SeatLockRequest seatLockRequest);
    void releaseSeats(String userId , SeatLockRequest seatLockRequest);
    List<UUID> getActiveLockedSeatIds(UUID showtimeId);
    List<UUID> getBookedSeatIds(UUID showtimeId);
    void reportExpiredLock(String userId, UUID showtimeId);
}
