package com.cgv.catalogservice.dto.request.showtime;

import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeCreateRequest(

        @NotNull(message = "Movie ID không được để trống")
        UUID movieId,

        @NotNull(message = "Room ID không được để trống")
        UUID roomId,

        @NotNull(message = "Ngày chiếu không được để trống")
        LocalDate showDate,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        LocalDateTime startTime,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        LocalDateTime endTime,

        String language,

        String subtitleLanguage,

        Format format,

        @NotNull(message = "Giá cơ bản không được để trống")
        @PositiveOrZero(message = "Giá cơ bản không được âm")
        BigDecimal basePrice,

        ShowtimeStatus status
) {
}