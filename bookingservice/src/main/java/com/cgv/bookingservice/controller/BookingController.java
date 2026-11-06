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

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {
    BookingService bookingService;
    private String requireAuthenticatedUserId(Jwt jwt) {
        String userId = jwt == null ? null : jwt.getSubject();
        if (userId == null || userId.isBlank() || userId.startsWith("guest_") || userId.equalsIgnoreCase("guest")) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập trước khi thực hiện thao tác này!");
        }
        return userId;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        String userId = requireAuthenticatedUserId(jwt);
        var result = bookingService.createBooking(userId, jwt.getClaimAsString("email"), request);
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
        String userId = requireAuthenticatedUserId(jwt);
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
        String userId = requireAuthenticatedUserId(jwt);
        return ApiResponse.<PageResponse<BookingResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(bookingService.getMyBookings(userId, filter, pageable))
                .message("Lấy lịch sử đặt vé thành công")
                .build();
    }

    @GetMapping("/my-pending")
    public ApiResponse<BookingResponse> getMyPending(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID showtimeId
    ) {
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(bookingService.getPendingBooking(requireAuthenticatedUserId(jwt), showtimeId))
                .build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        String userId = requireAuthenticatedUserId(jwt);
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
    public ApiResponse<BookingResponse> adminCheckIn(@PathVariable String id) {
        UUID bookingId = parseBookingUuid(id);
        var result = bookingService.adminCheckIn(bookingId);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(result)
                .message("Soát vé QR thành công! Vé đã được sử dụng.")
                .build();
    }

    @PostMapping("/admin/refund/{id}")
    @PreAuthorize("hasAnyAuthority('ticket:refund', 'CINEMA_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<BookingResponse> adminRefund(@PathVariable String id) {
        UUID bookingId = parseBookingUuid(id);
        var result = bookingService.adminRefund(bookingId);
        return ApiResponse.<BookingResponse>builder()
                .status(HttpStatus.OK.value())
                .data(result)
                .message("Hoàn vé và hủy chỗ ngồi thành công!")
                .build();
    }

    private UUID parseBookingUuid(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("Mã đơn đặt vé không được để trống");
        }
        String trimmed = rawId.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                .matcher(trimmed);
        if (matcher.find()) {
            return UUID.fromString(matcher.group());
        }
        return UUID.fromString(trimmed.replaceAll("(?i)^(CGV_TICKET_|TICKET_|BK-|BOOKING_)", ""));
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
