package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.showtime.ShowtimeCreateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeFilterRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.MovieNearbyCinemasResponse;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Showtimes", description = "API quản lý suất chiếu và truy vấn lịch chiếu theo phim, rạp và ngày.")
@RestController
@RequestMapping("/showtimes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShowtimeController {

    ShowtimeService showtimeService;

    @Operation(
            summary = "Tạo suất chiếu",
            description = "Tạo suất chiếu mới sau khi kiểm tra phim, phòng, ngày chiếu và xung đột thời gian."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShowtimeResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo suất chiếu", required = true) @RequestBody @Valid ShowtimeCreateRequest request
    ) {

        ShowtimeResponse response =
                showtimeService.createShowtime(request);

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo suất chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật suất chiếu",
            description = "Cập nhật một phần thông tin suất chiếu và kiểm tra lại các ràng buộc lịch chiếu."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{showtimeId}")
    public ApiResponse<ShowtimeResponse> update(
            @Parameter(description = "ID của suất chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID showtimeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật suất chiếu", required = true) @RequestBody @Valid ShowtimeUpdateRequest request
    ) {

        ShowtimeResponse response =
                showtimeService.updateShowtime(showtimeId, request);

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật suất chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái suất chiếu",
            description = "Cập nhật trạng thái suất chiếu và kiểm tra xung đột khi kích hoạt lại."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{showtimeId}/status")
    public ApiResponse<ShowtimeResponse> updateStatus(
            @Parameter(description = "ID của suất chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID showtimeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái suất chiếu", required = true) @RequestBody @Valid ShowtimeUpdateStatusRequest request
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

    @Operation(
            summary = "Xem chi tiết suất chiếu",
            description = "Lấy thông tin chi tiết suất chiếu theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{showtimeId}")
    public ApiResponse<ShowtimeResponse> get(
            @Parameter(description = "ID của suất chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID showtimeId
    ) {

        ShowtimeResponse response =
                showtimeService.getShowtimeById(showtimeId);

        return ApiResponse.<ShowtimeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết suất chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Lấy suất chiếu theo phim và ngày",
            description = "Lấy danh sách suất chiếu của một phim trong ngày cụ thể, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/movie/{movieId}/date/{showDate}")
    public ApiResponse<PageResponse<ShowtimeResponse>>
    getShowtimesByMovieAndDate(
            @Parameter(description = "ID của phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID movieId,
            @Parameter(description = "Ngày chiếu theo định dạng yyyy-MM-dd", required = true, schema = @Schema(type = "string", format = "date", example = "2026-09-23")) @PathVariable LocalDate showDate,
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ShowtimeResponse> response =
                showtimeService.getShowtimesByMovieAndDate(
                        movieId,
                        showDate,
                        pageable
                );

        return ApiResponse.<PageResponse<ShowtimeResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách suất chiếu theo phim và ngày")
                .build();
    }

    @Operation(
            summary = "Lấy suất chiếu theo rạp và ngày",
            description = "Lấy danh sách suất chiếu tại một rạp trong ngày cụ thể, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/cinema/{cinemaId}/date/{showDate}")
    public ApiResponse<PageResponse<ShowtimeResponse>>
    getShowtimesByCinemaAndDate(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId,
            @Parameter(description = "Ngày chiếu theo định dạng yyyy-MM-dd", required = true, schema = @Schema(type = "string", format = "date", example = "2026-09-23")) @PathVariable LocalDate showDate,
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ShowtimeResponse> response =
                showtimeService.getShowtimesByCinemaAndDate(
                        cinemaId,
                        showDate,
                        pageable
                );

        return ApiResponse.<PageResponse<ShowtimeResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách suất chiếu theo rạp và ngày")
                .build();
    }

    @Operation(
            summary = "Tìm kiếm và lọc suất chiếu",
            description = "Lấy danh sách suất chiếu theo ShowtimeFilterRequest, kèm phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<ShowtimeResponse>> findAll(
            @ParameterObject @Valid @ModelAttribute ShowtimeFilterRequest filter,
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ShowtimeResponse> response =
                showtimeService.getAllShowtimes(filter, pageable);

        return ApiResponse.<PageResponse<ShowtimeResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách suất chiếu")
                .build();
    }

    @GetMapping("/nearby")
    public ApiResponse<MovieNearbyCinemasResponse> getNearby(
            @RequestParam UUID movieId,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Double radiusKm
    ) {
        MovieNearbyCinemasResponse response =
                showtimeService.getNearbyMovieShowtimes(movieId, lat, lon, date, radiusKm);

        return ApiResponse.<MovieNearbyCinemasResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách rạp và suất chiếu gần bạn cho phim")
                .build();
    }

    @GetMapping("/movie-schedule")
    public ApiResponse<MovieNearbyCinemasResponse> getMovieSchedule(
            @RequestParam UUID movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer regionId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm
    ) {
        MovieNearbyCinemasResponse response =
                showtimeService.getMovieSchedule(movieId, date, regionId, lat, lon, radiusKm);

        return ApiResponse.<MovieNearbyCinemasResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Lịch chiếu phim theo cụm rạp")
                .build();
    }
}