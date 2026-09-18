package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.moviegenre.MovieGenreCreateRequest;
import com.cgv.catalogservice.dto.response.MovieGenreResponse;
import com.cgv.catalogservice.service.MovieGenreService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/movie-genres")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieGenreController {

    MovieGenreService movieGenreService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieGenreResponse> create(
            @RequestBody @Valid MovieGenreCreateRequest request
    ) {

        MovieGenreResponse response =
                movieGenreService.createMovieGenre(request);

        return ApiResponse.<MovieGenreResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Thêm thể loại cho phim thành công")
                .build();
    }

    @DeleteMapping("/{movieId}/{genreId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID movieId,
            @PathVariable Integer genreId
    ) {

        movieGenreService.deleteMovieGenre(movieId, genreId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá thể loại khỏi phim thành công")
                .build();
    }

    @GetMapping("/movie/{movieId}")
    public ApiResponse<List<MovieGenreResponse>> getGenresByMovieId(
            @PathVariable UUID movieId
    ) {

        List<MovieGenreResponse> response =
                movieGenreService.getGenresByMovieId(movieId);

        return ApiResponse.<List<MovieGenreResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách thể loại của phim")
                .build();
    }

    @GetMapping("/genre/{genreId}")
    public ApiResponse<PageResponse<MovieGenreResponse>> getMoviesByGenreId(
            @PathVariable Integer genreId,
            @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieGenreResponse> response =
                movieGenreService.getMoviesByGenreId(
                        genreId,
                        pageable
                );

        return ApiResponse.<PageResponse<MovieGenreResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách phim theo thể loại")
                .build();
    }
}
