package com.cgv.bookingservice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatLockResponse {
    UUID showtimeId;
    List<UUID> seatIds;
    Instant expiresAt;
}
