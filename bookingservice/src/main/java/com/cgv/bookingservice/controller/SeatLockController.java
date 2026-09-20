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

@RestController
@RequestMapping("/bookings/seat-locks")
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    @PostMapping
    public ApiResponse<SeatLockResponse> lockSeats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SeatLockRequest request
    ) {
        String userId = jwt.getSubject();
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
        String userId = jwt.getSubject();
        seatLockService.releaseSeats(userId, request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Giải phóng ghế thành công")
                .build();
    }
}