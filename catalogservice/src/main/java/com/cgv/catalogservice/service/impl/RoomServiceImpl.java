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
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgv.catalogservice.entity.Seat;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.catalogservice.repository.SeatRepository;
import com.cgv.catalogservice.repository.ShowtimeRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j(topic = "ROOM-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomServiceImpl implements RoomService {

    RoomRepository roomRepository;
    CinemaRepository cinemaRepository;
    SeatRepository seatRepository;
    ShowtimeRepository showtimeRepository;
    RoomMapper roomMapper;
    com.cgv.catalogservice.service.CatalogRealtimeService catalogRealtimeService;

    @Override
    @CacheEvict(
            value = "roomsByCinema",
            allEntries = true
    )
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {

        log.info("Creating room: cinemaId={}, name={}", request.cinemaId(), request.name());

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
    @Caching(evict = {
            @CacheEvict(value = "roomsByCinema", allEntries = true),
            @CacheEvict(value = "cinemaSchedule", allEntries = true),
            @CacheEvict(value = "showtime", allEntries = true),
            @CacheEvict(value = "showtimes", allEntries = true)
    })
    @Transactional
    public RoomResponse updateRoom(UUID roomId, RoomUpdateRequest request) {

        log.info("Updating room: roomId={}", roomId);

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
    @Caching(evict = {
            @CacheEvict(value = "roomsByCinema", allEntries = true),
            @CacheEvict(value = "cinemaSchedule", allEntries = true),
            @CacheEvict(value = "showtime", allEntries = true),
            @CacheEvict(value = "showtimes", allEntries = true)
    })
    @Transactional
    public RoomResponse updateRoomStatus(UUID roomId, RoomUpdateStatusRequest request) {

        log.info("Updating room status: roomId={}, status={}", roomId, request.status());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + roomId
                ));

        room.setStatus(request.status());
        roomRepository.saveAndFlush(room);

        try {
            catalogRealtimeService.broadcast("ROOM_STATUS_CHANGED", java.util.Map.of(
                    "roomId", roomId.toString(),
                    "cinemaId", room.getCinema() != null ? room.getCinema().getId().toString() : "",
                    "status", room.getStatus() != null ? room.getStatus().name() : "",
                    "name", room.getName() != null ? room.getName() : ""
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast ROOM_STATUS_CHANGED: {}", e.getMessage());
        }

        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId) {

        log.debug("Getting room by id: roomId={}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + roomId
                ));

        return roomMapper.toResponse(room);
    }

    @Override
    @Cacheable(
            value = "roomsByCinema",
            key = "#cinemaId"
                    + " + ':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getAllRoomsByCinemaId(UUID cinemaId, Pageable pageable) {

        log.debug("Getting rooms by cinema: cinemaId={}, page={}, size={}", cinemaId, pageable.getPageNumber(), pageable.getPageSize());

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

    @Override
    @CacheEvict(
            value = "roomsByCinema",
            allEntries = true
    )
    @Transactional
    public void deleteRoom(UUID roomId) {
        log.info("Deleting room: roomId={}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phòng chiếu với id: " + roomId
                ));

        if (showtimeRepository.existsByRoomIdAndStatusAndEndTimeAfter(roomId, ShowtimeStatus.SCHEDULED, Instant.now())) {
            throw new ResourceConflictException("Không thể xóa phòng chiếu đang có lịch chiếu sắp diễn ra.");
        }

        if (showtimeRepository.existsByRoomId(roomId)) {
            throw new ResourceConflictException("Không thể xóa phòng chiếu đã có lịch sử suất chiếu. Vui lòng chuyển trạng thái sang BẢO TRÌ.");
        }

        List<Seat> seats = seatRepository.findByRoomId(roomId);
        if (!seats.isEmpty()) {
            seatRepository.deleteAllInBatch(seats);
        }

        roomRepository.delete(room);

        try {
            catalogRealtimeService.broadcast("ROOM_STATUS_CHANGED", java.util.Map.of(
                    "roomId", roomId.toString(),
                    "action", "DELETED"
            ));
        } catch (Exception e) {
            log.warn("Failed to broadcast ROOM_STATUS_CHANGED on delete: {}", e.getMessage());
        }
    }
}
