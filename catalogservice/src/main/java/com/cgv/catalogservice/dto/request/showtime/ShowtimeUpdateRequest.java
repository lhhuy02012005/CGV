package com.cgv.catalogservice.dto.request.showtime;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ViewingMode;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShowtimeUpdateRequest(

        UUID movieId,

        UUID roomId,

        LocalDate showDate,

        Instant startTime,

        Instant endTime,

        String language,

        String subtitleLanguage,

        ViewingMode viewingMode,

        Format format,

        @PositiveOrZero(message = "Giá cơ bản không được âm")
        BigDecimal basePrice
) {
}