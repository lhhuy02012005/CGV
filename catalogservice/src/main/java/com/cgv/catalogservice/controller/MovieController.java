package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.movie.*;
import com.cgv.catalogservice.dto.response.MovieResponse;
import com.cgv.catalogservice.service.MovieService;
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
@RequestMapping("/movies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieController {

    MovieService movieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieResponse> create(
            @RequestBody @Valid MovieCreateRequest request
    ) {

        MovieResponse movieResponse =
                movieService.createMovie(request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(movieResponse)
                .message("Tạo phim thành công")
                .build();
    }

    @PatchMapping("/{movieId}")
    public ApiResponse<MovieResponse> update(
            @PathVariable UUID movieId,
            @RequestBody @Valid MovieUpdateRequest request
    ) {

        MovieResponse movieResponse =
                movieService.updateMovie(movieId, request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Cập nhật phim thành công")
                .build();
    }

    @PatchMapping("/{movieId}/status")
    public ApiResponse<MovieResponse> updateStatus(
            @PathVariable UUID movieId,
            @RequestBody @Valid MovieUpdateStatusRequest request
    ) {

        MovieResponse movieResponse =
                movieService.updateMovieStatus(movieId, request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Cập nhật trạng thái phim thành công")
                .build();
    }

    @PatchMapping("/{movieId}/showing-status")
    public ApiResponse<MovieResponse> updateShowingStatus(
            @PathVariable UUID movieId,
            @RequestBody @Valid MovieUpdateShowingStatusRequest request
    ) {

        MovieResponse movieResponse =
                movieService.updateMovieShowingStatus(movieId, request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Cập nhật trạng thái chiếu thành công")
                .build();
    }

    @GetMapping("/{movieId}")
    public ApiResponse<MovieResponse> get(
            @PathVariable UUID movieId
    ) {

        MovieResponse movieResponse =
                movieService.getMovieById(movieId);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Xem chi tiết phim thành công")
                .build();
    }

    @GetMapping("/now-showing")
    public ApiResponse<PageResponse<MovieResponse>>
    getNowShowingMovies(
            @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieResponse> movieResponsePage = movieService.getNowShowingMovies(pageable);

        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách phim đang chiếu thành công")
                .data(movieResponsePage)
                .build();
    }

    @GetMapping("/coming-soon")
    public ApiResponse<PageResponse<MovieResponse>>
    getComingSoonMovies(
            @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieResponse> movieResponsePage = movieService.getComingSoonMovies(pageable);

        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách phim sắp chiếu thành công")
                .data(movieResponsePage)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<MovieResponse>> findAll(
            @Valid @ModelAttribute MovieFilterRequest request,
            @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieResponse> movieResponsePage =
                movieService.getAllMovies(request, pageable);

        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponsePage)
                .message("Danh sách phim")
                .build();
    }
}