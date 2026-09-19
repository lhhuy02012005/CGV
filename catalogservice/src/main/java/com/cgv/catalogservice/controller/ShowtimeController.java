package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.showtime.ShowtimeCreateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeFilterRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.ShowtimeResponse;
import com.cgv.catalogservice.service.ShowtimeService;
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
@RequestMapping("/showtimes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShowtimeController {

    ShowtimeService showtimeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShowtimeResponse> create(
            @RequestBody @Valid ShowtimeCreateRequest request
    ) {

        ShowtimeResponse response =
                showtimeService.createShowtime(request);

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo suất chiếu thành công")
                .build();
    }

    @PatchMapping("/{showtimeId}")
    public ApiResponse<ShowtimeResponse> update(
            @PathVariable UUID showtimeId,
            @RequestBody @Valid ShowtimeUpdateRequest request
    ) {

        ShowtimeResponse response =
                showtimeService.updateShowtime(showtimeId, request);

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật suất chiếu thành công")
                .build();
    }

    @PatchMapping("/{showtimeId}/status")
    public ApiResponse<ShowtimeResponse> updateStatus(
            @PathVariable UUID showtimeId,
            @RequestBody @Valid ShowtimeUpdateStatusRequest request
    ) {

        ShowtimeResponse response =
                showtimeService.updateShowtimeStatus(
                        showtimeId,
                        request
                );

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái suất chiếu thành công")
                .build();
    }

    @GetMapping("/{showtimeId}")
    public ApiResponse<ShowtimeResponse> get(
            @PathVariable UUID showtimeId
    ) {

        ShowtimeResponse response =
                showtimeService.getShowtimeById(showtimeId);

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết suất chiếu thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<ShowtimeResponse>> findAll(
            @Valid @ModelAttribute ShowtimeFilterRequest filter,
            @PageableDefault Pageable pageable
    ) {

        PageResponse<ShowtimeResponse> response =
                showtimeService.getAllShowtimes(filter, pageable);

        return ApiResponse.<PageResponse<ShowtimeResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách suất chiếu")
                .build();
    }
}