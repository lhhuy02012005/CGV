package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.room.RoomCreateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.RoomResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.RoomMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.RoomRepository;
import com.cgv.catalogservice.service.RoomService;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (roomRepository.existsByCinemaIdAndNameIgnoreCase(
                request.cinemaId(),
                request.name()
        )) {
            throw new ResourceConflictException(
                    "Phòng '" + request.name()
                            + "' đã tồn tại trong rạp " + request.cinemaId()
            );
        }

        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy rạp chiếu với id: " + request.cinemaId()
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

        if (request.name() != null
                && roomRepository.existsByCinemaIdAndNameIgnoreCaseAndIdNot(
                room.getCinema().getId(),
                request.name(),
                roomId
        )) {
            throw new ResourceConflictException(
                    "Phòng '" + request.name() + "' đã tồn tại trong rạp"
            );
        }

        roomMapper.updateEntity(request, room);

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
    public PageResponse<RoomResponse> getAllRoomsByCinemaId(UUID cinemaId, Pageable pageable) {

        if (!cinemaRepository.existsById(cinemaId)) {
            throw new EntityNotFoundException(
                    "Không tồn tại rạp chiếu với id: " + cinemaId
            );
        }

        return PageResponseUtils.findAllAndMap(
                p -> roomRepository.findAllByCinemaId(cinemaId, p),
                pageable,
                roomMapper::toResponseList
        );
    }
}
