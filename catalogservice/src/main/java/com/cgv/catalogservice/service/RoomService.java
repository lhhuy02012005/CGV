package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.room.RoomCreateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.RoomResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoomService {

    RoomResponse createRoom(RoomCreateRequest request);

    RoomResponse updateRoom(UUID roomId, RoomUpdateRequest request);

    RoomResponse updateRoomStatus(UUID roomId, RoomUpdateStatusRequest request);

    RoomResponse getRoomById(UUID roomId);

    PageResponse<RoomResponse> getAllRoomsByCinemaId(UUID cinemaId, Pageable pageable);
}
