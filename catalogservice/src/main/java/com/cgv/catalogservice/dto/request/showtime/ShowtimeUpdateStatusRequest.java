package com.cgv.catalogservice.dto.request.showtime;

import com.cgv.catalogservice.enums.ShowtimeStatus;
import jakarta.validation.constraints.NotNull;

public record ShowtimeUpdateStatusRequest(

        @NotNull(message = "Trạng thái suất chiếu không được để trống")
        ShowtimeStatus status

) {
}
