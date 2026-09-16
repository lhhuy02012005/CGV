package com.cgv.catalogservice.dto.response;

import com.cgv.catalogservice.enums.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String thumbnailUrl,
        LocalDate eventDate,
        LocalTime eventTime,
        String locationName,
        UUID cinemaId,
        String cinemaName,
        String registrationUrl,
        Integer maxAttendees,
        Integer currentAttendees,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
