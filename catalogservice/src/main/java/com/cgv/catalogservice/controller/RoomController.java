package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.room.RoomCreateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.RoomResponse;
import com.cgv.catalogservice.service.RoomService;
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

@Tag(name = "Rooms", description = "API quản lý phòng chiếu và truy vấn phòng theo rạp.")
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;

    @Operation(
            summary = "Tạo phòng chiếu",
            description = "Tạo phòng chiếu mới trong một rạp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo phòng chiếu", required = true) @RequestBody @Valid RoomCreateRequest request
    ) {
        RoomResponse response = roomService.createRoom(request);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo phòng chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật phòng chiếu",
            description = "Cập nhật một phần thông tin phòng chiếu theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{roomId}")
    public ApiResponse<RoomResponse> update(
            @Parameter(description = "ID của phòng chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật phòng chiếu", required = true) @RequestBody @Valid RoomUpdateRequest request
    ) {
        RoomResponse response = roomService.updateRoom(roomId, request);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật phòng chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật trạng thái phòng",
            description = "Cập nhật trạng thái hoạt động của phòng chiếu."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{roomId}/status")
    public ApiResponse<RoomResponse> updateStatus(
            @Parameter(description = "ID của phòng chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật trạng thái phòng chiếu", required = true) @RequestBody @Valid RoomUpdateStatusRequest request
    ) {
        RoomResponse response = roomService.updateRoomStatus(roomId, request);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái phòng chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết phòng chiếu",
            description = "Lấy thông tin chi tiết phòng chiếu theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{roomId}")
    public ApiResponse<RoomResponse> get(@Parameter(description = "ID của phòng chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID roomId) {
        RoomResponse response = roomService.getRoomById(roomId);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết phòng chiếu thành công")
                .build();
    }

    @Operation(
            summary = "Lấy phòng theo rạp",
            description = "Lấy danh sách phòng thuộc một rạp, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/cinema/{cinemaId}")
    public ApiResponse<PageResponse<RoomResponse>> findAllByCinemaId(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId,
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<RoomResponse> response = roomService.getAllRoomsByCinemaId(cinemaId, pageable);

        return ApiResponse.<PageResponse<RoomResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách phòng chiếu theo rạp")
                .build();
    }

    @Operation(
            summary = "Xóa phòng chiếu",
            description = "Xóa phòng chiếu theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa phòng chiếu thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng chiếu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phòng chiếu đang có suất chiếu hoặc không thể xóa")
    })
    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "ID của phòng chiếu", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID roomId
    ) {
        roomService.deleteRoom(roomId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa phòng chiếu thành công")
                .build();
    }
}
