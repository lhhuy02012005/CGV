package com.cgv.bookingservice.service;

import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.dto.response.BookingResponse;

public interface BookingService {
    BookingResponse createBooking(String userId, BookingCreateRequest request);
}
