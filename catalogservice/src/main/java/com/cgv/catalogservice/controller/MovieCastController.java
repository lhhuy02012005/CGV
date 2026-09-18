package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.moviecast.MovieCastCreateRequest;
import com.cgv.catalogservice.dto.request.moviecast.MovieCastUpdateRequest;
import com.cgv.catalogservice.dto.response.MovieCastResponse;
import com.cgv.catalogservice.service.MovieCastService;
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
@RequestMapping("/movie-casts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieCastController {

    MovieCastService movieCastService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieCastResponse> create(
            @RequestBody @Valid MovieCastCreateRequest request
    ) {
        MovieCastResponse response = movieCastService.createMovieCast(request);

        return ApiResponse.<MovieCastResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo diễn viên phim thành công")
                .build();
    }

    @PatchMapping("/{movieCastId}")
    public ApiResponse<MovieCastResponse> update(
            @PathVariable UUID movieCastId,
            @RequestBody @Valid MovieCastUpdateRequest request
    ) {
        MovieCastResponse response = movieCastService.updateMovieCast(movieCastId, request);

        return ApiResponse.<MovieCastResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật diễn viên phim thành công")
                .build();
    }

    @DeleteMapping("/{movieCastId}")
    public ApiResponse<Void> delete(@PathVariable UUID movieCastId) {
        movieCastService.deleteMovieCast(movieCastId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá diễn viên phim thành công")
                .build();
    }

    @GetMapping("/{movieCastId}")
    public ApiResponse<MovieCastResponse> get(@PathVariable UUID movieCastId) {
        MovieCastResponse response = movieCastService.getMovieCastById(movieCastId);

        return ApiResponse.<MovieCastResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết diễn viên phim thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<MovieCastResponse>> findAll(
            @PageableDefault Pageable pageable
    ) {
        PageResponse<MovieCastResponse> response = movieCastService.getAllMovieCasts(pageable);

        return ApiResponse.<PageResponse<MovieCastResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách diễn viên phim")
                .build();
    }
}
