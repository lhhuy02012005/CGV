package com.cgv.bookingservice.controller;

import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.dto.response.BookingResponse;
import com.cgv.bookingservice.dto.response.DashboardStatisticsResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {
    BookingService bookingService;

    private String resolveUserId(Jwt jwt, String headerUserId) {
        if (jwt != null && jwt.getSubject() != null) {
            return jwt.getSubject();
        }
        if (headerUserId != null && !headerUserId.isBlank() && !headerUserId.equalsIgnoreCase("anonymous")) {
            return headerUserId;
        }
        return null;
    }

    private String requireAuthenticatedUserId(Jwt jwt, String headerUserId) {
        String userId = resolveUserId(jwt, headerUserId);
        if (userId == null || userId.startsWith("guest_") || userId.equalsIgnoreCase("guest")) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập trước khi thực hiện thao tác này!");
        }
        return userId;
    }

    @PostMapping
    public ApiResponse<BookingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestBody BookingCreateRequest request
    ) {
        String userId = resolveUserId(jwt, headerUserId);
        var result = bookingService.createBooking(userId, request);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(result)
                .message("Tạo đơn đặt vé thành công, vui lòng tiến hành thanh toán trong 10 phút!")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable UUID id
    ) {
        String userId = resolveUserId(jwt, headerUserId);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(bookingService.getBookingById(id, userId))
                .message("Lấy thông tin vé thành công")
                .build();
    }

    @GetMapping("/my-bookings")
    public ApiResponse<PageResponse<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @ModelAttribute BookingFilterRequest filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String userId = requireAuthenticatedUserId(jwt, headerUserId);
        return ApiResponse.<PageResponse<BookingResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(bookingService.getMyBookings(userId, filter, pageable))
                .message("Lấy lịch sử đặt vé thành công")
                .build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @PathVariable UUID id
    ) {
        String userId = resolveUserId(jwt, headerUserId);
        bookingService.cancelBooking(id, userId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Đã hủy đơn đặt vé và giải phóng ghế thành công!")
                .build();
    }

    // ─── ADMIN & STAFF ENDPOINTS ───

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyAuthority('ticket:view', 'TICKET_STAFF', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<PageResponse<BookingResponse>> adminSearch(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var results = bookingService.adminSearchBookings(keyword, pageable);
        return ApiResponse.<PageResponse<BookingResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(results)
                .message("Tìm kiếm danh sách vé rạp thành công")
                .build();
    }

    @PostMapping("/admin/check-in/{id}")
    @PreAuthorize("hasAnyAuthority('ticket:view', 'TICKET_STAFF', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<BookingResponse> adminCheckIn(@PathVariable UUID id) {
        var result = bookingService.adminCheckIn(id);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(result)
                .message("Soát vé QR thành công! Vé đã được sử dụng.")
                .build();
    }

    @PostMapping("/admin/refund/{id}")
    @PreAuthorize("hasAnyAuthority('ticket:refund', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<BookingResponse> adminRefund(@PathVariable UUID id) {
        var result = bookingService.adminRefund(id);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(result)
                .message("Hoàn vé và hủy chỗ ngồi thành công!")
                .build();
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAnyAuthority('dashboard:view', 'ticket:view', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<DashboardStatisticsResponse> getDashboardStatistics() {
        var result = bookingService.getDashboardStatistics();
        return ApiResponse.<DashboardStatisticsResponse>builder()
                .status(HttpStatus.OK.value())
                .data(result)
                .message("Lấy dữ liệu thống kê dashboard thành công")
                .build();
    }
}
