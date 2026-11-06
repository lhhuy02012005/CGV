package com.cgv.bookingservice.controller;

import com.cgv.bookingservice.dto.request.SeatLockRequest;
import com.cgv.bookingservice.dto.response.SeatLockResponse;
import com.cgv.bookingservice.service.SeatLockService;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
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
    private String requireAuthenticatedUserId(Jwt jwt) {
        String userId = jwt == null ? null : jwt.getSubject();
        if (userId == null || userId.isBlank() || userId.startsWith("guest_") || userId.equalsIgnoreCase("guest")) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập trước khi giữ ghế");
        }
        return userId;
    }

    @PostMapping
    public ApiResponse<SeatLockResponse> lockSeats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SeatLockRequest request
    ) {
        String userId = requireAuthenticatedUserId(jwt);
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
            @RequestBody @Valid SeatLockRequest request
    ) {
        String userId = requireAuthenticatedUserId(jwt);
        seatLockService.releaseSeats(userId, request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Giải phóng ghế thành công")
                .build();
    }

    @PostMapping("/report-expired")
    public ApiResponse<Void> reportExpired(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID showtimeId
    ) {
        String userId = requireAuthenticatedUserId(jwt);
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
