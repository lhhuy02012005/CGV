package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.room.RoomCreateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.RoomResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.mapper.RoomMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.RoomRepository;
import com.cgv.catalogservice.service.RoomService;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomServiceImpl implements RoomService {

    RoomRepository roomRepository;
    CinemaRepository cinemaRepository;
    RoomMapper roomMapper;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {
        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cinema với id: " + request.cinemaId()
                ));

        Room room = roomMapper.toEntity(request);
        room.setCinema(cinema);

        Room savedRoom = roomRepository.save(room);

        return roomMapper.toResponse(savedRoom);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(UUID roomId, RoomUpdateRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + roomId
                ));

        roomMapper.updateEntity(request, room);

        if (request.cinemaId() != null) {
            Cinema cinema = cinemaRepository.findById(request.cinemaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy cinema với id: " + request.cinemaId()
                    ));
            room.setCinema(cinema);
        }

        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse updateRoomStatus(UUID roomId, RoomUpdateStatusRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + roomId
                ));

        room.setStatus(request.status());

        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + roomId
                ));

        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + roomId
                ));

        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getAllRooms(Pageable pageable) {
        Page<Room> roomPage = roomRepository.findAll(pageable);

        List<RoomResponse> roomResponses = roomMapper.toResponseList(roomPage.getContent());

        return PageResponse.<RoomResponse>builder()
                .data(roomResponses)
                .pageNumber(roomPage.getNumber() + 1)
                .pageSize(roomPage.getSize())
                .totalPages(roomPage.getTotalPages())
                .totalElements(roomPage.getTotalElements())
                .build();
    }
}
