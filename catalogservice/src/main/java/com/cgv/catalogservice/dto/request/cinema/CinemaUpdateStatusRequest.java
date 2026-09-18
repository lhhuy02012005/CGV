package com.cgv.catalogservice.dto.request.cinema;

import com.cgv.catalogservice.enums.CinemaStatus;
import jakarta.validation.constraints.NotNull;

public record CinemaUpdateStatusRequest(

        @NotNull(message = "Trạng thái rạp chiếu phim không được để trống")
        CinemaStatus status

) {
}
