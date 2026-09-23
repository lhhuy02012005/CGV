package com.cgv.catalogservice.dto.request.showtime;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowtimeFilterRequest(

        UUID movieId,

        UUID roomId,

        UUID cinemaId,

        Instant showDate,

        Instant showDateFrom,

        Instant showDateTo,

        Instant startTimeFrom,

        Instant startTimeTo,

        ShowtimeStatus status,

        Format format,

        String language,

        String subtitleLanguage,

        @PositiveOrZero(message = "Giá tối thiểu không được âm")
        BigDecimal minPrice,

        @PositiveOrZero(message = "Giá tối đa không được âm")
        BigDecimal maxPrice,

        Boolean available

) {
}
