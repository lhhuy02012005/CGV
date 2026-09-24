package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

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

@Tag(name = "Movie Casts", description = "API quản lý thông tin diễn viên và vai diễn liên kết với phim.")
@RestController
@RequestMapping("/movie-casts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieCastController {

    MovieCastService movieCastService;

    @Operation(
            summary = "Thêm diễn viên cho phim",
            description = "Tạo thông tin diễn viên hoặc vai diễn gắn với một phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieCastResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu thêm diễn viên cho phim", required = true) @RequestBody @Valid MovieCastCreateRequest request
    ) {
        MovieCastResponse response = movieCastService.createMovieCast(request);

        return ApiResponse.<MovieCastResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo diễn viên phim thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật diễn viên phim",
            description = "Cập nhật một phần thông tin diễn viên hoặc vai diễn theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{movieCastId}")
    public ApiResponse<MovieCastResponse> update(
            @Parameter(description = "ID của thông tin diễn viên phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieCastId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật diễn viên phim", required = true) @RequestBody @Valid MovieCastUpdateRequest request
    ) {
        MovieCastResponse response = movieCastService.updateMovieCast(movieCastId, request);

        return ApiResponse.<MovieCastResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật diễn viên phim thành công")
                .build();
    }

    @Operation(
            summary = "Xoá diễn viên phim",
            description = "Xoá thông tin diễn viên hoặc vai diễn theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{movieCastId}")
    public ApiResponse<Void> delete(@Parameter(description = "ID của thông tin diễn viên phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieCastId) {
        movieCastService.deleteMovieCast(movieCastId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá diễn viên phim thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết diễn viên phim",
            description = "Lấy thông tin chi tiết diễn viên hoặc vai diễn theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{movieCastId}")
    public ApiResponse<MovieCastResponse> get(@Parameter(description = "ID của thông tin diễn viên phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieCastId) {
        MovieCastResponse response = movieCastService.getMovieCastById(movieCastId);

        return ApiResponse.<MovieCastResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết diễn viên phim thành công")
                .build();
    }

    @Operation(
            summary = "Lấy danh sách diễn viên phim",
            description = "Lấy danh sách thông tin diễn viên phim có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<MovieCastResponse>> findAll(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<MovieCastResponse> response = movieCastService.getAllMovieCasts(pageable);

        return ApiResponse.<PageResponse<MovieCastResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách diễn viên phim")
                .build();
    }

    @GetMapping("/movie/{movieId}")
    public ApiResponse<java.util.List<MovieCastResponse>> getByMovieId(@PathVariable UUID movieId) {
        return ApiResponse.<java.util.List<MovieCastResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(movieCastService.getMovieCastsByMovieId(movieId))
                .message("Danh sách diễn viên của phim thành công")
                .build();
    }
}
