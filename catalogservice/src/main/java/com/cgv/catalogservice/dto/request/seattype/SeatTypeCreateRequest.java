package com.cgv.catalogservice.dto.request.seattype;

import com.cgv.catalogservice.enums.SeatTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SeatTypeCreateRequest(

        @NotNull(message = "Tên loại ghế không được để trống")
        SeatTypeName name,

        @PositiveOrZero(message = "Phụ thu không được âm")
        BigDecimal surcharge,

        String description

) {
}
