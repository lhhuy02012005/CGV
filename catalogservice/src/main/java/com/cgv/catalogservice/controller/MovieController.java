package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

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

import java.util.List;
import java.util.UUID;

@Tag(name = "Movies", description = "API quản lý phim, trạng thái phim, trạng thái chiếu và các danh sách phim phổ biến.")
@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieController {

    MovieService movieService;

    @Operation(
            summary = "Tạo phim",
            description = "Tạo phim mới với thông tin phát hành và nội dung hiển thị."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo phim", required = true) @RequestBody @Valid MovieCreateRequest request
    ) {

        MovieResponse movieResponse =
                movieService.createMovie(request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(movieResponse)
                .message("Tạo phim thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật phim",
            description = "Cập nhật một phần thông tin phim theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{movieId}")
    public ApiResponse<MovieResponse> update(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật phim", required = true) @RequestBody @Valid MovieUpdateRequest request
    ) {

        MovieResponse movieResponse =
                movieService.updateMovie(movieId, request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Cập nhật phim thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái phim",
            description = "Cập nhật trạng thái quản trị của phim, ví dụ ACTIVE, DRAFT hoặc HIDDEN."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{movieId}/status")
    public ApiResponse<MovieResponse> updateStatus(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái phim", required = true) @RequestBody @Valid MovieUpdateStatusRequest request
    ) {

        MovieResponse movieResponse =
                movieService.updateMovieStatus(movieId, request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Cập nhật trạng thái phim thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái chiếu",
            description = "Cập nhật trạng thái chiếu của phim, ví dụ NOW_SHOWING, COMING_SOON hoặc ENDED."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{movieId}/showing-status")
    public ApiResponse<MovieResponse> updateShowingStatus(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái chiếu của phim", required = true) @RequestBody @Valid MovieUpdateShowingStatusRequest request
    ) {

        MovieResponse movieResponse =
                movieService.updateMovieShowingStatus(movieId, request);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Cập nhật trạng thái chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết phim",
            description = "Lấy thông tin chi tiết phim theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{movieId}")
    public ApiResponse<MovieResponse> get(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId
    ) {

        MovieResponse movieResponse =
                movieService.getMovieById(movieId);

        return ApiResponse.<MovieResponse>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponse)
                .message("Xem chi tiết phim thành công")
                .build();
    }

    @Operation(
            summary = "Lấy phim đang chiếu",
            description = "Lấy danh sách phim ACTIVE có trạng thái NOW_SHOWING, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping("/now-showing")
    public ApiResponse<PageResponse<MovieResponse>>
    getNowShowingMovies(
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieResponse> movieResponsePage = movieService.getNowShowingMovies(pageable);

        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách phim đang chiếu thành công")
                .data(movieResponsePage)
                .build();
    }

    @Operation(
            summary = "Lấy phim sắp chiếu",
            description = "Lấy danh sách phim ACTIVE có trạng thái COMING_SOON, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping("/coming-soon")
    public ApiResponse<PageResponse<MovieResponse>>
    getComingSoonMovies(
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieResponse> movieResponsePage = movieService.getComingSoonMovies(pageable);

        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách phim sắp chiếu thành công")
                .data(movieResponsePage)
                .build();
    }

    @Operation(
            summary = "Tìm kiếm và lọc phim",
            description = "Lấy danh sách phim theo MovieFilterRequest, kèm phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<MovieResponse>> findAll(
            @ParameterObject @Valid @ModelAttribute MovieFilterRequest request,
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<MovieResponse> movieResponsePage =
                movieService.getAllMovies(request, pageable);

        return ApiResponse.<PageResponse<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(movieResponsePage)
                .message("Danh sách phim")
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<List<MovieResponse>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        List<MovieResponse> responses = movieService.searchMovies(keyword, pageable);
        return ApiResponse.<List<MovieResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(responses)
                .message("Kết quả tìm kiếm phim")
                .build();
    }
}