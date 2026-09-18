package com.cgv.catalogservice.dto.request.showtime;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeUpdateRequest(

        UUID movieId,

        UUID roomId,

        LocalDate showDate,

        LocalDateTime startTime,

        LocalDateTime endTime,

        String language,

        String subtitleLanguage,

        Format format,

        @PositiveOrZero(message = "Giá cơ bản không được âm")
        BigDecimal basePrice
) {
}