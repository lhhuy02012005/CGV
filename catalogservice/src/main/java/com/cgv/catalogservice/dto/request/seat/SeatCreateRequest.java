package com.cgv.catalogservice.dto.request.seat;

import com.cgv.catalogservice.enums.SeatTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SeatCreateRequest(

        @NotNull(message = "Room ID không được để trống")
        UUID roomId,

        @NotBlank(message = "Hàng ghế không được để trống")
        @Size(max = 2, message = "Hàng ghế tối đa 2 ký tự")
        String rowChar,

        @NotNull(message = "Số ghế không được để trống")
        @Positive(message = "Số ghế phải lớn hơn 0")
        Integer seatNumber,

        SeatTypeName seatTypeName,

        Boolean isActive
) {
}