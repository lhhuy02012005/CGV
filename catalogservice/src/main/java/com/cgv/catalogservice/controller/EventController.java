package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

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

@Tag(name = "Events", description = "API quản lý sự kiện và truy vấn các sự kiện sắp diễn ra hoặc đang diễn ra.")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventController {

    EventService eventService;

    @Operation(
            summary = "Tạo sự kiện",
            description = "Tạo một sự kiện mới, có thể liên kết với một rạp chiếu phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EventResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo sự kiện", required = true) @RequestBody @Valid EventCreateRequest request
    ) {
        EventResponse eventResponse = eventService.createEvent(request);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(eventResponse)
                .message("Tạo sự kiện thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật sự kiện",
            description = "Cập nhật một phần thông tin sự kiện theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{eventId}")
    public ApiResponse<EventResponse> update(
            @Parameter(description = "ID của sự kiện", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID eventId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật sự kiện", required = true) @RequestBody @Valid EventUpdateRequest request
    ) {
        EventResponse eventResponse = eventService.updateEvent(eventId, request);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponse)
                .message("Cập nhật sự kiện thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái sự kiện",
            description = "Cập nhật trạng thái của sự kiện."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{eventId}/status")
    public ApiResponse<EventResponse> updateStatus(
            @Parameter(description = "ID của sự kiện", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID eventId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái sự kiện", required = true) @RequestBody @Valid EventUpdateStatusRequest request
    ) {
        EventResponse eventResponse = eventService.updateEventStatus(eventId, request);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponse)
                .message("Cập nhật trạng thái sự kiện thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết sự kiện",
            description = "Lấy thông tin chi tiết sự kiện theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> get(
            @Parameter(description = "ID của sự kiện", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID eventId
    ) {
        EventResponse eventResponse = eventService.getEventById(eventId);

        return ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponse)
                .message("Xem chi tiết sự kiện thành công")
                .build();
    }

    @Operation(
            summary = "Lấy sự kiện sắp diễn ra",
            description = "Lấy danh sách sự kiện có trạng thái UPCOMING, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping("/status/upcoming")
    public ApiResponse<PageResponse<EventResponse>> getUpcomingEvents(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<EventResponse> eventResponsePage =
                eventService.getUpcomingEvents(pageable);

        return ApiResponse.<PageResponse<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponsePage)
                .message("Danh sách sự kiện sắp diễn ra")
                .build();
    }

    @Operation(
            summary = "Lấy sự kiện đang diễn ra",
            description = "Lấy danh sách sự kiện có trạng thái ONGOING, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping("/status/ongoing")
    public ApiResponse<PageResponse<EventResponse>> getOngoingEvents(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<EventResponse> eventResponsePage =
                eventService.getOngoingEvents(pageable);

        return ApiResponse.<PageResponse<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(eventResponsePage)
                .message("Danh sách sự kiện đang diễn ra")
                .build();
    }

    @Operation(
            summary = "Tìm kiếm và lọc sự kiện",
            description = "Lấy danh sách sự kiện theo EventFilterRequest, kèm phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<EventResponse>> findAll(
            @ParameterObject @ModelAttribute EventFilterRequest request,
            @ParameterObject @PageableDefault Pageable pageable
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
