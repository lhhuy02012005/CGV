package com.cgv.bookingservice.service;

import com.cgv.bookingservice.dto.request.SeatLockRequest;
import com.cgv.bookingservice.dto.response.SeatLockResponse;

public interface SeatLockService {
    SeatLockResponse lockSeats(String userId , SeatLockRequest seatLockRequest);
    void releaseSeats(String userId , SeatLockRequest seatLockRequest);
}
