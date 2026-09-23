package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.moviegenre.MovieGenreCreateRequest;
import com.cgv.catalogservice.dto.response.MovieGenreResponse;
import com.cgv.catalogservice.service.MovieGenreService;
import com.cgv.commondto.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Movie Genres", description = "API quản lý quan hệ giữa phim và thể loại.")
@RestController
@RequestMapping("/movie-genres")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieGenreController {

    MovieGenreService movieGenreService;

    @Operation(
            summary = "Thêm thể loại cho phim",
            description = "Gán một thể loại cho phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieGenreResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu gán thể loại cho phim", required = true) @RequestBody @Valid MovieGenreCreateRequest request
    ) {

        MovieGenreResponse response =
                movieGenreService.createMovieGenre(request);

        return ApiResponse.<MovieGenreResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Thêm thể loại cho phim thành công")
                .build();
    }

    @Operation(
            summary = "Xoá thể loại khỏi phim",
            description = "Xoá quan hệ giữa một phim và một thể loại."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{movieId}/{genreId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId,
            @Parameter(description = "ID của thể loại", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer genreId
    ) {

        movieGenreService.deleteMovieGenre(movieId, genreId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá thể loại khỏi phim thành công")
                .build();
    }

    @Operation(
            summary = "Lấy thể loại của phim",
            description = "Lấy toàn bộ thể loại đã được gán cho một phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/movie/{movieId}")
    public ApiResponse<List<MovieGenreResponse>> getGenresByMovieId(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId
    ) {

        List<MovieGenreResponse> response =
                movieGenreService.getGenresByMovieId(movieId);

        return ApiResponse.<List<MovieGenreResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách thể loại của phim")
                .build();
    }
}
