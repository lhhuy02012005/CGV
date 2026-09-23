package com.cgv.bookingservice.dto.event;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record SeatRealtimeEvent(
        String type, // "LOCK", "RELEASE", "BOOKED"
        UUID showtimeId,
        List<UUID> seatIds,
        String userId,
        Instant timestamp
) {
}
