package com.cgv.bookingservice.controller;

import com.cgv.bookingservice.service.BookingRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Tag(name = "Booking Realtime", description = "Server-Sent Events (SSE) cho thanh toán thời gian thực và soát vé tại cổng")
@RestController
@RequestMapping("/bookings/realtime")
@RequiredArgsConstructor
public class BookingRealtimeController {

    private final BookingRealtimeService bookingRealtimeService;

    @Operation(
            summary = "Đăng ký nhận luồng xác nhận thanh toán cho đơn vé",
            description = "Client (User) lắng nghe luồng SSE khi đang ở màn hình thanh toán. Khi Webhook từ cổng thanh toán báo thành công, event PAYMENT_CONFIRMED sẽ được đẩy về tức thì để mở vé."
    )
    @GetMapping(value = "/booking/{bookingId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookingEvents(@PathVariable UUID bookingId) {
        return bookingRealtimeService.subscribeBooking(bookingId);
    }

    @Operation(
            summary = "Đăng ký nhận luồng sự kiện vé toàn hệ thống (Soát vé & Thống kê)",
            description = "Admin / Thiết bị quét vé tại quầy/cổng rạp kết nối để nhận trạng thái TICKET_CHECKED_IN, chống gian lận dùng vé trùng (anti-fraud double entry)."
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGeneralEvents() {
        return bookingRealtimeService.subscribeGeneral();
    }
}
