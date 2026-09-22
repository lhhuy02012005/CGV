package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.event.EventCreateRequest;
import com.cgv.catalogservice.dto.request.event.EventFilterRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.EventResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Event;
import com.cgv.catalogservice.enums.EventStatus;
import com.cgv.catalogservice.mapper.EventMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.EventRepository;
import com.cgv.catalogservice.service.EventService;
import com.cgv.catalogservice.specification.EventSpecification;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    CinemaRepository cinemaRepository;
    EventMapper eventMapper;

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "upcomingEvents",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "ongoingEvents",
                            allEntries = true
                    )
            }
    )
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
    @Caching(
            put = {
                    @CachePut(
                            value = "event",
                            key = "#eventId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "upcomingEvents",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "ongoingEvents",
                            allEntries = true
                    )
            }
    )
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
    @Caching(
            put = {
                    @CachePut(
                            value = "event",
                            key = "#eventId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "upcomingEvents",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "ongoingEvents",
                            allEntries = true
                    )
            }
    )
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
    @Cacheable(
            value = "event",
            key = "#eventId"
    )
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy event với id: " + eventId
                ));

        return eventMapper.toResponse(event);
    }

    @Override
    @Cacheable(
            value = "upcomingEvents",
            key = "'page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getUpcomingEvents(Pageable pageable) {

        Specification<Event> spec =
                Specification.allOf(
                        EventSpecification.hasStatus(EventStatus.UPCOMING)
                );

        return PageResponseUtils.findAllAndMap(
                p -> eventRepository.findAll(spec, p),
                pageable,
                eventMapper::toResponseList
        );
    }

    @Override
    @Cacheable(
            value = "ongoingEvents",
            key = "'page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getOngoingEvents(Pageable pageable) {

        Specification<Event> spec =
                Specification.allOf(
                        EventSpecification.hasStatus(EventStatus.ONGOING)
                );

        return PageResponseUtils.findAllAndMap(
                p -> eventRepository.findAll(spec, p),
                pageable,
                eventMapper::toResponseList
        );
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

        return PageResponseUtils.findAllAndMap(
                p -> eventRepository.findAll(specification, p),
                pageable,
                eventMapper::toResponseList
        );
    }
}
