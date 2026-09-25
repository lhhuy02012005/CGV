package com.cgv.bookingservice.service;

import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.dto.response.BookingResponse;
import com.cgv.bookingservice.dto.response.DashboardStatisticsResponse;
import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.event.PaymentCompletedEvent;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(String userId, BookingCreateRequest request);
    void confirmBooking(PaymentCompletedEvent event);
    BookingResponse getBookingById(UUID bookingId, String userId);
    PageResponse<BookingResponse> getMyBookings(String userId, BookingFilterRequest filter, Pageable pageable);
    void cancelBooking(UUID bookingId, String userId);
    void rollbackBooking(UUID bookingId, String reason);

    PageResponse<BookingResponse> adminSearchBookings(String keyword, Pageable pageable);
    BookingResponse adminCheckIn(UUID bookingId);
    BookingResponse adminRefund(UUID bookingId);
    DashboardStatisticsResponse getDashboardStatistics();
}
