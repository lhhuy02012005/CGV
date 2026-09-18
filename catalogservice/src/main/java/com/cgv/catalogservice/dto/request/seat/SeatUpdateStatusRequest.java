package com.cgv.catalogservice.dto.request.seat;

import jakarta.validation.constraints.NotNull;

public record SeatUpdateStatusRequest(

        @NotNull(message = "Trạng thái ghế không được để trống")
        Boolean isActive

) {
}
