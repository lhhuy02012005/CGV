package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.cinema.CinemaCreateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaFilterRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.CinemaResponse;
import com.cgv.catalogservice.service.CinemaService;
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
@RequestMapping("/cinemas")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaController {

    CinemaService cinemaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CinemaResponse> create(
            @RequestBody @Valid CinemaCreateRequest request
    ) {
        CinemaResponse response = cinemaService.createCinema(request);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo rạp chiếu phim thành công")
                .build();
    }

    @PatchMapping("/{cinemaId}")
    public ApiResponse<CinemaResponse> update(
            @PathVariable UUID cinemaId,
            @RequestBody @Valid CinemaUpdateRequest request
    ) {
        CinemaResponse response = cinemaService.updateCinema(cinemaId, request);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật rạp chiếu phim thành công")
                .build();
    }

    @PatchMapping("/{cinemaId}/status")
    public ApiResponse<CinemaResponse> updateStatus(
            @PathVariable UUID cinemaId,
            @RequestBody @Valid CinemaUpdateStatusRequest request
    ) {
        CinemaResponse response = cinemaService.updateCinemaStatus(cinemaId, request);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái rạp chiếu phim thành công")
                .build();
    }

    @DeleteMapping("/{cinemaId}")
    public ApiResponse<Void> delete(@PathVariable UUID cinemaId) {
        cinemaService.deleteCinema(cinemaId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá rạp chiếu phim thành công")
                .build();
    }

    @GetMapping("/{cinemaId}")
    public ApiResponse<CinemaResponse> get(@PathVariable UUID cinemaId) {
        CinemaResponse response = cinemaService.getCinemaById(cinemaId);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết rạp chiếu phim thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<CinemaResponse>> findAll(
            @Valid @ModelAttribute CinemaFilterRequest filter,
            @PageableDefault Pageable pageable
    ) {

        PageResponse<CinemaResponse> response =
                cinemaService.getAllCinemas(
                        filter,
                        pageable
                );

        return ApiResponse
                .<PageResponse<CinemaResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách rạp chiếu phim")
                .build();
    }
}
