package com.cgv.catalogservice.controller;

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

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomResponse> create(
            @RequestBody @Valid RoomCreateRequest request
    ) {
        RoomResponse response = roomService.createRoom(request);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo phòng chiếu thành công")
                .build();
    }

    @PatchMapping("/{roomId}")
    public ApiResponse<RoomResponse> update(
            @PathVariable UUID roomId,
            @RequestBody @Valid RoomUpdateRequest request
    ) {
        RoomResponse response = roomService.updateRoom(roomId, request);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật phòng chiếu thành công")
                .build();
    }

    @PatchMapping("/{roomId}/status")
    public ApiResponse<RoomResponse> updateStatus(
            @PathVariable UUID roomId,
            @RequestBody @Valid RoomUpdateStatusRequest request
    ) {
        RoomResponse response = roomService.updateRoomStatus(roomId, request);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái phòng chiếu thành công")
                .build();
    }

    @GetMapping("/{roomId}")
    public ApiResponse<RoomResponse> get(@PathVariable UUID roomId) {
        RoomResponse response = roomService.getRoomById(roomId);

        return ApiResponse.<RoomResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết phòng chiếu thành công")
                .build();
    }

    @GetMapping("/cinema/{cinemaId}")
    public ApiResponse<PageResponse<RoomResponse>> findAllByCinemaId(
            @PathVariable UUID cinemaId,
            @PageableDefault Pageable pageable
    ) {
        PageResponse<RoomResponse> response = roomService.getAllRoomsByCinemaId(cinemaId, pageable);

        return ApiResponse.<PageResponse<RoomResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách phòng chiếu")
                .build();
    }
}
