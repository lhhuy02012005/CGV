package com.cgv.bookingservice.controller;

import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.dto.response.BookingResponse;
import com.cgv.bookingservice.service.BookingService;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class BookingController {
    BookingService bookingService;

    private String getUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập trước khi thực hiện thao tác này!");
        }
        return jwt.getSubject();
    }

    @PostMapping
    public ApiResponse<BookingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BookingCreateRequest request
    ){
        String userId = getUserId(jwt);
        var result  = bookingService.createBooking(userId , request);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(result)
                .message("Tạo đơn đặt vé thành công, vui lòng tiến hành thanh toán trong 10 phút!")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        String userId = getUserId(jwt);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(bookingService.getBookingById(id, userId))
                .message("Lấy thông tin vé thành công")
                .build();
    }

    @GetMapping("/my-bookings")
    public ApiResponse<PageResponse<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt,
            @ModelAttribute BookingFilterRequest filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String userId = getUserId(jwt);
        return ApiResponse.<PageResponse<BookingResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(bookingService.getMyBookings(userId, filter, pageable))
                .message("Lấy lịch sử đặt vé thành công")
                .build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        String userId = getUserId(jwt);
        bookingService.cancelBooking(id, userId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Đã hủy đơn đặt vé và giải phóng ghế thành công!")
                .build();
    }
}
