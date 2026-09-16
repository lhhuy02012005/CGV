package com.cgv.catalogservice.dto.request.event;

import com.cgv.catalogservice.enums.EventStatus;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventUpdateRequest(

        String title,

        String description,

        String thumbnailUrl,

        LocalDate eventDate,

        LocalTime eventTime,

        String locationName,

        UUID cinemaId,

        String registrationUrl,

        @Positive(message = "Số người tối đa phải lớn hơn 0")
        Integer maxAttendees,

        EventStatus status
) {
}