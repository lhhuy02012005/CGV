package com.cgv.catalogservice.dto.request.seattype;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SeatTypeUpdateRequest(

        @PositiveOrZero(message = "Phụ thu không được âm")
        BigDecimal surcharge,

        String description
) {
}
