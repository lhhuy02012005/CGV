package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.seattype.SeatTypeCreateRequest;
import com.cgv.catalogservice.dto.request.seattype.SeatTypeUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatTypeResponse;
import com.cgv.catalogservice.enums.SeatTypeName;
import com.cgv.catalogservice.service.SeatTypeService;
import com.cgv.commondto.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Seat Types", description = "API quản lý các loại ghế và phụ phí tương ứng.")
@RestController
@RequestMapping("/seat-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatTypeController {

    SeatTypeService seatTypeService;

    @Operation(
            summary = "Tạo loại ghế",
            description = "Tạo cấu hình loại ghế mới."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatTypeResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo loại ghế", required = true) @RequestBody @Valid SeatTypeCreateRequest request
    ) {
        SeatTypeResponse response = seatTypeService.createSeatType(request);

        return ApiResponse.<SeatTypeResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo loại ghế thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật loại ghế",
            description = "Cập nhật thông tin loại ghế theo tên enum."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{seatTypeName}")
    public ApiResponse<SeatTypeResponse> update(
            @Parameter(description = "Tên loại ghế", required = true, schema = @Schema(implementation = SeatTypeName.class)) @PathVariable SeatTypeName seatTypeName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật loại ghế", required = true) @RequestBody @Valid SeatTypeUpdateRequest request
    ) {
        SeatTypeResponse response = seatTypeService.updateSeatType(seatTypeName, request);

        return ApiResponse.<SeatTypeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật loại ghế thành công")
                .build();
    }

    @Operation(
            summary = "Xoá loại ghế",
            description = "Xoá loại ghế nếu không có ghế nào đang tham chiếu."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{seatTypeName}")
    public ApiResponse<Void> delete(@Parameter(description = "Tên loại ghế", required = true, schema = @Schema(implementation = SeatTypeName.class)) @PathVariable SeatTypeName seatTypeName) {
        seatTypeService.deleteSeatType(seatTypeName);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá loại ghế thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết loại ghế",
            description = "Lấy thông tin loại ghế theo tên."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{seatTypeName}")
    public ApiResponse<SeatTypeResponse> get(@Parameter(description = "Tên loại ghế", required = true, schema = @Schema(implementation = SeatTypeName.class)) @PathVariable SeatTypeName seatTypeName) {
        SeatTypeResponse response = seatTypeService.getSeatTypeByName(seatTypeName);

        return ApiResponse.<SeatTypeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết loại ghế thành công")
                .build();
    }

    @Operation(
            summary = "Lấy danh sách loại ghế",
            description = "Lấy toàn bộ cấu hình loại ghế."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<List<SeatTypeResponse>> findAll() {

        List<SeatTypeResponse> response = seatTypeService.getAllSeatTypes();

        return ApiResponse.<List<SeatTypeResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách loại ghế")
                .build();
    }
}
