package com.cgv.catalogservice.dto.request.movie;

import com.cgv.catalogservice.enums.MovieStatus;
import jakarta.validation.constraints.NotNull;

public record MovieUpdateStatusRequest(

        @NotNull(message = "Trạng thái phim không được để trống")
        MovieStatus status

) {
}