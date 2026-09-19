package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.event.EventCreateRequest;
import com.cgv.catalogservice.dto.request.event.EventFilterRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateRequest;
import com.cgv.catalogservice.dto.request.event.EventUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.EventResponse;
import com.cgv.catalogservice.service.EventService;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventController {

    EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EventResponse> create(
            @RequestBody @Valid EventCreateRequest request
    ) {
        EventResponse eventResponse = eventService.createEvent(request);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(eventResponse)
                .message("Tạo sự kiện thành công")
                .build();
    }

    @PatchMapping("/{eventId}")
    public ApiResponse<EventResponse> update(
            @PathVariable UUID eventId,
            @RequestBody @Valid EventUpdateRequest request
    ) {
        EventResponse eventResponse = eventService.updateEvent(eventId, request);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponse)
                .message("Cập nhật sự kiện thành công")
                .build();
    }

    @PatchMapping("/{eventId}/status")
    public ApiResponse<EventResponse> updateStatus(
            @PathVariable UUID eventId,
            @RequestBody @Valid EventUpdateStatusRequest request
    ) {
        EventResponse eventResponse = eventService.updateEventStatus(eventId, request);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponse)
                .message("Cập nhật trạng thái sự kiện thành công")
                .build();
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> get(
            @PathVariable UUID eventId
    ) {
        EventResponse eventResponse = eventService.getEventById(eventId);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponse)
                .message("Xem chi tiết sự kiện thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<EventResponse>> findAll(
            @ModelAttribute EventFilterRequest request,
            @PageableDefault Pageable pageable
    ) {
        PageResponse<EventResponse> eventResponsePage =
                eventService.getAllEvents(request, pageable);

        return ApiResponse.<PageResponse<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponsePage)
                .message("Danh sách sự kiện")
                .build();
    }
}
