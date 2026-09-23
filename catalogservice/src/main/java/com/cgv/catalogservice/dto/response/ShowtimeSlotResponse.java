package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.catalogservice.enums.ViewingMode;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record ShowtimeSlotResponse(
        UUID showtimeId,
        UUID roomId,
        String roomName,
        Instant startTime,
        Instant endTime,
        Format format,
        ViewingMode viewingMode,
        String language,
        String subtitleLanguage,
        BigDecimal basePrice,
        Integer availableSeats,
        ShowtimeStatus status
) {
}
