package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.catalogservice.enums.ViewingMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowtimeResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        RoomResponse roomResponse,
        Instant showDate,
        Instant startTime,
        Instant endTime,
        String language,
        String subtitleLanguage,
        ViewingMode viewingMode,
        Format format,
        BigDecimal basePrice,
        Integer availableSeats,
        ShowtimeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
