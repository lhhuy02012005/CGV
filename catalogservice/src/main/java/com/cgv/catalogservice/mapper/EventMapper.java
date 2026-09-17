package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.event.EventCreateRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateRequest;
import com.cgv.catalogservice.dto.response.EventResponse;
import com.cgv.catalogservice.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface EventMapper {

    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "currentAttendees", ignore = true)
    Event toEntity(EventCreateRequest request);

    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "currentAttendees", ignore = true)
    void updateEntity(EventUpdateRequest request, @MappingTarget Event event);

    @Mapping(target = "cinemaId", source = "cinema.id")
    @Mapping(target = "cinemaName", source = "cinema.name")
    EventResponse toResponse(Event event);

    List<EventResponse> toResponseList(List<Event> events);
}
