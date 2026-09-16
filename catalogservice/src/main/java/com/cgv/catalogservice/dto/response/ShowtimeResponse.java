package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        UUID roomId,
        String roomName,
        UUID cinemaId,
        String cinemaName,
        LocalDate showDate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String language,
        String subtitleLanguage,
        Format format,
        BigDecimal basePrice,
        Integer availableSeats,
        ShowtimeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
