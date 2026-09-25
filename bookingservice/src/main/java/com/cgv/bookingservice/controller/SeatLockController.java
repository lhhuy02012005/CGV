package com.cgv.bookingservice.controller;

import com.cgv.bookingservice.dto.request.SeatLockRequest;
import com.cgv.bookingservice.dto.response.SeatLockResponse;
import com.cgv.bookingservice.service.SeatLockService;
import com.cgv.commondto.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookings/seat-locks")
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    private String resolveUserId(Jwt jwt, String headerUserId) {
        if (jwt != null && jwt.getSubject() != null) {
            return jwt.getSubject();
        }
        if (headerUserId != null && !headerUserId.isBlank() && !headerUserId.equalsIgnoreCase("anonymous")) {
            return headerUserId;
        }
        return "guest_" + UUID.randomUUID().toString();
    }

    @PostMapping
    public ApiResponse<SeatLockResponse> lockSeats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestBody @Valid SeatLockRequest request
    ) {
        String userId = resolveUserId(jwt, headerUserId);
        SeatLockResponse response = seatLockService.lockSeats(userId, request);
        return ApiResponse.<SeatLockResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Giữ ghế thành công")
                .data(response)
                .build();
    }

    @DeleteMapping
    public ApiResponse<Void> releaseSeats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestBody @Valid SeatLockRequest request
    ) {
        String userId = resolveUserId(jwt, headerUserId);
        seatLockService.releaseSeats(userId, request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Giải phóng ghế thành công")
                .build();
    }

    @PostMapping("/report-expired")
    public ApiResponse<Void> reportExpired(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam UUID showtimeId
    ) {
        String userId = resolveUserId(jwt, headerUserId);
        seatLockService.reportExpiredLock(userId, showtimeId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Ghi nhận phiên giữ ghế hết hạn thành công")
                .build();
    }

    @GetMapping("/{showtimeId}")
    public ApiResponse<java.util.List<UUID>> getLockedSeats(
            @PathVariable UUID showtimeId
    ) {
        return ApiResponse.<java.util.List<UUID>>builder()
                .status(HttpStatus.OK.value())
                .message("Danh sách ghế đang được giữ")
                .data(seatLockService.getActiveLockedSeatIds(showtimeId))
                .build();
    }

    @GetMapping("/booked/{showtimeId}")
    public ApiResponse<java.util.List<UUID>> getBookedSeats(
            @PathVariable UUID showtimeId
    ) {
        return ApiResponse.<java.util.List<UUID>>builder()
                .status(HttpStatus.OK.value())
                .message("Danh sách ghế đã được đặt")
                .data(seatLockService.getBookedSeatIds(showtimeId))
                .build();
    }
}