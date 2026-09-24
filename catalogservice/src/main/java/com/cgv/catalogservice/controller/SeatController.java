package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.seat.SeatCreateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.SeatResponse;
import com.cgv.catalogservice.service.SeatService;
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

@Tag(name = "Seats", description = "API quản lý ghế và truy vấn sơ đồ ghế theo phòng chiếu.")
@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatController {

    SeatService seatService;

    @Operation(
            summary = "Tạo ghế",
            description = "Tạo ghế mới trong phòng chiếu khi quy tắc lịch chiếu cho phép."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo ghế", required = true) @RequestBody @Valid SeatCreateRequest request
    ) {
        SeatResponse response = seatService.createSeat(request);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo ghế thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật ghế",
            description = "Cập nhật một phần thông tin ghế theo ID khi quy tắc lịch chiếu cho phép."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{seatId}")
    public ApiResponse<SeatResponse> update(
            @Parameter(description = "ID của ghế", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID seatId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật ghế", required = true) @RequestBody @Valid SeatUpdateRequest request
    ) {
        SeatResponse response = seatService.updateSeat(seatId, request);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật ghế thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái ghế",
            description = "Bật hoặc tắt trạng thái hoạt động của ghế."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{seatId}/status")
    public ApiResponse<SeatResponse> updateStatus(
            @Parameter(description = "ID của ghế", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID seatId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái ghế", required = true) @RequestBody @Valid SeatUpdateStatusRequest request
    ) {
        SeatResponse response = seatService.updateSeatStatus(seatId, request);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái ghế thành công")
                .build();
    }

    @Operation(
            summary = "Lấy ghế theo phòng",
            description = "Lấy danh sách ghế thuộc một phòng chiếu, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/room/{roomId}")
    public ApiResponse<PageResponse<SeatResponse>> findAllByRoomId(
            @Parameter(description = "ID của phòng chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID roomId,
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<SeatResponse> response = seatService.getAllSeatsByRoomId(roomId, pageable);

        return ApiResponse.<PageResponse<SeatResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách ghế theo phòng chiếu")
                .build();
    }
}
