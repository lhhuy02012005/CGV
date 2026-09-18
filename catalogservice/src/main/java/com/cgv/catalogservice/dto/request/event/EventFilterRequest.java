package com.cgv.catalogservice.dto.request.event;

import com.cgv.catalogservice.enums.EventStatus;

import java.time.LocalDate;
import java.util.UUID;

public record EventFilterRequest(
        String keyword,
        EventStatus status,
        UUID cinemaId,
        LocalDate eventDate,
        LocalDate eventDateFrom,
        LocalDate eventDateTo
) {
}
