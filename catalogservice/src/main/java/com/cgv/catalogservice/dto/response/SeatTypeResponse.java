package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.SeatTypeName;

import java.math.BigDecimal;

public record SeatTypeResponse(
        SeatTypeName name,
        BigDecimal surcharge,
        String description
) {
}
