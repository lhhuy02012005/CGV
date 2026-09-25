package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.cinema.CinemaCreateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaFilterRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.CinemaResponse;
import com.cgv.catalogservice.dto.response.CinemaScheduleResponse;
import com.cgv.catalogservice.dto.response.NearbyCinemaResponse;
import com.cgv.catalogservice.service.CinemaService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Cinemas", description = "API quản lý rạp chiếu phim và truy vấn danh sách rạp theo bộ lọc.")
@RestController
@RequestMapping("/cinemas")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaController {

    CinemaService cinemaService;
    ShowtimeService showtimeService;

    @Operation(
            summary = "Tạo rạp chiếu phim",
            description = "Tạo rạp mới và liên kết rạp với khu vực tương ứng."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('cinema:manage', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<CinemaResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo rạp chiếu phim", required = true) @RequestBody @Valid CinemaCreateRequest request
    ) {
        CinemaResponse response = cinemaService.createCinema(request);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo rạp chiếu phim thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật rạp chiếu phim",
            description = "Cập nhật một phần thông tin của rạp theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{cinemaId}")
    @PreAuthorize("hasAnyAuthority('cinema:manage', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<CinemaResponse> update(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật rạp chiếu phim", required = true) @RequestBody @Valid CinemaUpdateRequest request
    ) {
        CinemaResponse response = cinemaService.updateCinema(cinemaId, request);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật rạp chiếu phim thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái rạp",
            description = "Cập nhật trạng thái hoạt động của rạp chiếu phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{cinemaId}/status")
    @PreAuthorize("hasAnyAuthority('cinema:manage', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<CinemaResponse> updateStatus(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái rạp chiếu phim", required = true) @RequestBody @Valid CinemaUpdateStatusRequest request
    ) {
        CinemaResponse response = cinemaService.updateCinemaStatus(cinemaId, request);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái rạp chiếu phim thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết rạp",
            description = "Lấy thông tin chi tiết của một rạp chiếu phim theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{cinemaId}")
    public ApiResponse<CinemaResponse> get(@Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId) {
        CinemaResponse response = cinemaService.getCinemaById(cinemaId);

        return ApiResponse.<CinemaResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết rạp chiếu phim thành công")
                .build();
    }

    @Operation(
            summary = "Tìm kiếm và lọc rạp",
            description = "Lấy danh sách rạp theo từ khoá, khu vực, trạng thái, tiện ích và các điều kiện lọc khác, kèm phân trang."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<CinemaResponse>> findAll(
            @ParameterObject @Valid @ModelAttribute CinemaFilterRequest filter,
            @ParameterObject @PageableDefault Pageable pageable
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

    @GetMapping("/nearby")
    public ApiResponse<List<NearbyCinemaResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) Double radiusKm
    ) {
        List<NearbyCinemaResponse> response = cinemaService.getNearbyCinemas(lat, lon, radiusKm);

        return ApiResponse.<List<NearbyCinemaResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách rạp gần nhất")
                .build();
    }

    @GetMapping("/{cinemaId}/schedule")
    public ApiResponse<CinemaScheduleResponse> getSchedule(
            @PathVariable UUID cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        CinemaScheduleResponse response = showtimeService.getCinemaSchedule(cinemaId, date);

        return ApiResponse.<CinemaScheduleResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Lịch chiếu theo rạp")
                .build();
    }

    @Operation(
            summary = "Xóa rạp chiếu phim",
            description = "Xóa rạp chiếu phim theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa rạp chiếu phim thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy rạp chiếu phim"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Rạp chiếu phim đang có suất chiếu hoặc không thể xóa")
    })
    @DeleteMapping("/{cinemaId}")
    @PreAuthorize("hasAnyAuthority('cinema:manage', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<Void> delete(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId
    ) {
        cinemaService.deleteCinema(cinemaId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa rạp chiếu phim thành công")
                .build();
    }
}
