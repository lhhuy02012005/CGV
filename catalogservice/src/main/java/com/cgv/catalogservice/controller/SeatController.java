package com.cgv.catalogservice.controller;

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

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatController {

    SeatService seatService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatResponse> create(
            @RequestBody @Valid SeatCreateRequest request
    ) {
        SeatResponse response = seatService.createSeat(request);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo ghế thành công")
                .build();
    }

    @PatchMapping("/{seatId}")
    public ApiResponse<SeatResponse> update(
            @PathVariable UUID seatId,
            @RequestBody @Valid SeatUpdateRequest request
    ) {
        SeatResponse response = seatService.updateSeat(seatId, request);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật ghế thành công")
                .build();
    }

    @PatchMapping("/{seatId}/status")
    public ApiResponse<SeatResponse> updateStatus(
            @PathVariable UUID seatId,
            @RequestBody @Valid SeatUpdateStatusRequest request
    ) {
        SeatResponse response = seatService.updateSeatStatus(seatId, request);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật trạng thái ghế thành công")
                .build();
    }

    @DeleteMapping("/{seatId}")
    public ApiResponse<Void> delete(@PathVariable UUID seatId) {
        seatService.deleteSeat(seatId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá ghế thành công")
                .build();
    }

    @GetMapping("/{seatId}")
    public ApiResponse<SeatResponse> get(@PathVariable UUID seatId) {
        SeatResponse response = seatService.getSeatById(seatId);

        return ApiResponse.<SeatResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết ghế thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<SeatResponse>> findAll(
            @PageableDefault Pageable pageable
    ) {
        PageResponse<SeatResponse> response = seatService.getAllSeats(pageable);

        return ApiResponse.<PageResponse<SeatResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách ghế")
                .build();
    }
}
