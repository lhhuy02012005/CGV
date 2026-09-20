package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.event.EventCreateRequest;
import com.cgv.catalogservice.dto.request.event.EventFilterRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.EventResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {

    EventResponse createEvent(EventCreateRequest request);

    EventResponse updateEvent(UUID eventId, EventUpdateRequest request);

    EventResponse updateEventStatus(UUID eventId, EventUpdateStatusRequest request);

    EventResponse getEventById(UUID eventId);

    PageResponse<EventResponse> getAllEvents(
            EventFilterRequest filter,
            Pageable pageable
    );
}
