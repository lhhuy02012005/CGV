package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.seat.SeatCreateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.SeatResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SeatService {

    SeatResponse createSeat(SeatCreateRequest request);

    SeatResponse updateSeat(UUID seatId, SeatUpdateRequest request);

    SeatResponse updateSeatStatus(UUID seatId, SeatUpdateStatusRequest request);

    SeatResponse getSeatById(UUID seatId);

    PageResponse<SeatResponse> getAllSeats(Pageable pageable);
}
