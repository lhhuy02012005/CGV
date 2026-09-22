package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.genre.GenreCreateRequest;
import com.cgv.catalogservice.dto.request.genre.GenreUpdateRequest;
import com.cgv.catalogservice.dto.response.GenreResponse;
import com.cgv.catalogservice.service.GenreService;
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

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenreController {

    GenreService genreService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GenreResponse> create(
            @RequestBody @Valid GenreCreateRequest request
    ) {
        GenreResponse response = genreService.createGenre(request);

        return ApiResponse.<GenreResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo thể loại thành công")
                .build();
    }

    @PutMapping("/{genreId}")
    public ApiResponse<GenreResponse> update(
            @PathVariable Integer genreId,
            @RequestBody @Valid GenreUpdateRequest request
    ) {
        GenreResponse response = genreService.updateGenre(genreId, request);

        return ApiResponse.<GenreResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật thể loại thành công")
                .build();
    }

    @DeleteMapping("/{genreId}")
    public ApiResponse<Void> delete(@PathVariable Integer genreId) {
        genreService.deleteGenre(genreId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá thể loại thành công")
                .build();
    }

    @GetMapping("/{genreId}")
    public ApiResponse<GenreResponse> get(@PathVariable Integer genreId) {
        GenreResponse response = genreService.getGenreById(genreId);

        return ApiResponse.<GenreResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết thể loại thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<GenreResponse>> findAll(
            @PageableDefault Pageable pageable
    ) {
        PageResponse<GenreResponse> response = genreService.getAllGenres(pageable);

        return ApiResponse.<PageResponse<GenreResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách thể loại")
                .build();
    }
}
