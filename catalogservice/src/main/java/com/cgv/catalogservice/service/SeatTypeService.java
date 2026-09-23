package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.seattype.SeatTypeCreateRequest;
import com.cgv.catalogservice.dto.request.seattype.SeatTypeUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatTypeResponse;
import com.cgv.catalogservice.enums.SeatTypeName;

import java.util.List;

public interface SeatTypeService {

    SeatTypeResponse createSeatType(SeatTypeCreateRequest request);

    SeatTypeResponse updateSeatType(SeatTypeName seatTypeName, SeatTypeUpdateRequest request);

    void deleteSeatType(SeatTypeName seatTypeName);

    SeatTypeResponse getSeatTypeByName(SeatTypeName seatTypeName);

    List<SeatTypeResponse> getAllSeatTypes();
}
