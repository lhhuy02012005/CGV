package com.cgv.catalogservice.dto.request.seat;

import com.cgv.catalogservice.enums.SeatTypeName;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SeatUpdateRequest(

        UUID roomId,

        @Size(max = 2, message = "Hàng ghế tối đa 2 ký tự")
        String rowChar,

        @Positive(message = "Số ghế phải lớn hơn 0")
        Integer seatNumber,

        SeatTypeName seatTypeName
) {
}