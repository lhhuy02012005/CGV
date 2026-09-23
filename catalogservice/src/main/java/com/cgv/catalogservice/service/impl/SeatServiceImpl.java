package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.seat.SeatCreateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.SeatResponse;
import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.entity.Seat;
import com.cgv.catalogservice.entity.SeatType;
import com.cgv.catalogservice.enums.SeatTypeName;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.SeatMapper;
import com.cgv.catalogservice.repository.RoomRepository;
import com.cgv.catalogservice.repository.SeatRepository;
import com.cgv.catalogservice.repository.SeatTypeRepository;
import com.cgv.catalogservice.repository.ShowtimeRepository;
import com.cgv.catalogservice.service.SeatService;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j(topic = "SEAT-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatServiceImpl implements SeatService {

    SeatRepository seatRepository;
    RoomRepository roomRepository;
    SeatTypeRepository seatTypeRepository;
    ShowtimeRepository showtimeRepository;

    SeatMapper seatMapper;

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "seatsByRoom",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "roomsByCinema",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public SeatResponse createSeat(SeatCreateRequest request) {

        log.info("Creating seat: roomId={}, row={}, number={}", request.roomId(), request.rowChar(), request.seatNumber());

        validateRoomHasNoScheduledShowtime(request.roomId());

        if (seatRepository.existsByRoomIdAndRowCharAndSeatNumber(
                request.roomId(),
                request.rowChar(),
                request.seatNumber()
        )) {
            throw new ResourceConflictException(
                    "Ghế " + request.rowChar() + request.seatNumber()
                            + " đã tồn tại trong phòng"
            );
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy room với id: "
                                        + request.roomId()
                        )
                );

        SeatTypeName seatTypeName =
                request.seatTypeName() != null
                        ? request.seatTypeName()
                        : SeatTypeName.NORMAL;

        SeatType seatType =
                seatTypeRepository.findById(seatTypeName)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Không tìm thấy seat type với name: "
                                                + seatTypeName
                                )
                        );

        Seat seat = seatMapper.toEntity(request);

        seat.setRoom(room);
        seat.setSeatType(seatType);

        Seat savedSeat =
                seatRepository.saveAndFlush(seat);

        room.setTotalSeats(
                seatRepository.countByRoomId(room.getId())
        );

        return seatMapper.toResponse(savedSeat);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "seatsByRoom",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "roomsByCinema",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public SeatResponse updateSeat(
            UUID seatId,
            SeatUpdateRequest request
    ) {

        log.info("Updating seat: seatId={}", seatId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy seat với id: " + seatId
                        )
                );

        UUID targetRoomId =
                request.roomId() != null
                        ? request.roomId()
                        : seat.getRoom().getId();

        String targetRowChar =
                request.rowChar() != null
                        ? request.rowChar()
                        : seat.getRowChar();

        Integer targetSeatNumber =
                request.seatNumber() != null
                        ? request.seatNumber()
                        : seat.getSeatNumber();

        if (seatRepository.existsByRoomIdAndRowCharAndSeatNumberAndIdNot(
                targetRoomId,
                targetRowChar,
                targetSeatNumber,
                seatId
        )) {
            throw new ResourceConflictException(
                    "Ghế " + targetRowChar + targetSeatNumber
                            + " đã tồn tại trong phòng"
            );
        }

        validateRoomHasNoScheduledShowtime(seat.getRoom().getId());

        if (request.roomId() != null
                && !request.roomId().equals(seat.getRoom().getId())) {

            validateRoomHasNoScheduledShowtime(
                    request.roomId()
            );
        }

        Room oldRoom = seat.getRoom();
        Room newRoom = oldRoom;

        boolean roomChanged = false;

        if (request.roomId() != null) {

            newRoom = roomRepository.findById(request.roomId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy room với id: "
                                            + request.roomId()
                            )
                    );

            roomChanged =
                    !oldRoom.getId().equals(newRoom.getId());
        }

        SeatType seatType = seat.getSeatType();

        if (request.seatTypeName() != null) {

            seatType = seatTypeRepository
                    .findById(request.seatTypeName())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Không tìm thấy seat type với name: "
                                            + request.seatTypeName()
                            )
                    );
        }

        seatMapper.updateEntity(request, seat);

        seat.setRoom(newRoom);
        seat.setSeatType(seatType);

        seatRepository.flush();

        if (roomChanged) {

            oldRoom.setTotalSeats(
                    seatRepository.countByRoomId(
                            oldRoom.getId()
                    )
            );

            newRoom.setTotalSeats(
                    seatRepository.countByRoomId(
                            newRoom.getId()
                    )
            );

        }

        return seatMapper.toResponse(seat);
    }

    @Override
    @CacheEvict(
            value = "seatsByRoom",
            allEntries = true
    )
    @Transactional
    public SeatResponse updateSeatStatus(
            UUID seatId,
            SeatUpdateStatusRequest request
    ) {

        log.info("Updating seat status: seatId={}, active={}", seatId, request.isActive());

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy seat với id: " + seatId
                        )
                );

        validateRoomHasNoScheduledShowtime(seat.getRoom().getId());

        seat.setIsActive(request.isActive());

        return seatMapper.toResponse(seat);
    }

    @Override
    @Cacheable(
            value = "seatsByRoom",
            key = "#roomId"
                    + " + ':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> getAllSeatsByRoomId(UUID roomId, Pageable pageable) {

        log.debug("Getting seats by room: roomId={}, page={}, size={}", roomId, pageable.getPageNumber(), pageable.getPageSize());

        if (!roomRepository.existsById(roomId)) {
            throw new EntityNotFoundException(
                    "Không tồn tại phòng chiếu với id: " + roomId
            );
        }

        return PageResponseUtils.findAllAndMap(
                p -> seatRepository.findAllByRoomId(roomId, p),
                pageable,
                seatMapper::toResponseList
        );
    }

    private void validateRoomHasNoScheduledShowtime(UUID roomId) {

        boolean hasScheduledShowtime =
                showtimeRepository
                        .existsByRoomIdAndStatusAndEndTimeAfter(
                                roomId,
                                ShowtimeStatus.SCHEDULED,
                                LocalDateTime.now()
                        );

        if (hasScheduledShowtime) {
            throw new ResourceConflictException(
                    "Không thể thay đổi ghế vì phòng đang có suất chiếu đang diễn ra hoặc đã được lên lịch"
            );
        }
    }
}
