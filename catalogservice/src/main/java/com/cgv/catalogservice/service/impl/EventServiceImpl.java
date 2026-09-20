package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.event.EventCreateRequest;
import com.cgv.catalogservice.dto.request.event.EventFilterRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.EventResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Event;
import com.cgv.catalogservice.mapper.EventMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.EventRepository;
import com.cgv.catalogservice.service.EventService;
import com.cgv.catalogservice.specification.EventSpecification;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    CinemaRepository cinemaRepository;
    EventMapper eventMapper;

    @Override
    @Transactional
    public EventResponse createEvent(EventCreateRequest request) {
        Event event = eventMapper.toEntity(request);

        if (request.cinemaId() != null) {
            Cinema cinema = cinemaRepository.findById(request.cinemaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy cinema với id: " + request.cinemaId()
                    ));

            event.setCinema(cinema);
        }

        Event savedEvent = eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(UUID eventId, EventUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy event với id: " + eventId
                ));

        eventMapper.updateEntity(request, event);

        if (request.cinemaId() != null) {
            Cinema cinema = cinemaRepository.findById(request.cinemaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy cinema với id: " + request.cinemaId()
                    ));

            event.setCinema(cinema);
        }

        eventRepository.saveAndFlush(event);

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public EventResponse updateEventStatus(
            UUID eventId,
            EventUpdateStatusRequest request
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy event với id: " + eventId
                ));

        event.setStatus(request.status());

        eventRepository.saveAndFlush(event);

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy event với id: " + eventId
                ));

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getAllEvents(
            EventFilterRequest filter,
            Pageable pageable
    ) {
        Specification<Event> specification = Specification.allOf(
                EventSpecification.containsKeyword(filter.keyword()),
                EventSpecification.hasStatus(filter.status()),
                EventSpecification.hasCinemaId(filter.cinemaId()),
                EventSpecification.hasEventDate(filter.eventDate()),
                EventSpecification.eventDateFrom(filter.eventDateFrom()),
                EventSpecification.eventDateTo(filter.eventDateTo())
        );

        Page<Event> eventPage = eventRepository.findAll(specification, pageable);

        List<EventResponse> eventResponses = eventMapper.toResponseList(eventPage.getContent());

        return PageResponse.<EventResponse>builder()
                .data(eventResponses)
                .pageNumber(eventPage.getNumber() + 1)
                .pageSize(eventPage.getSize())
                .totalPages(eventPage.getTotalPages())
                .totalElements(eventPage.getTotalElements())
                .build();
    }
}
