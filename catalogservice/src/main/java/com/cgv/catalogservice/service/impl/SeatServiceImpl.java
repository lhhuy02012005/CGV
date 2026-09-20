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
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    @Transactional
    public SeatResponse createSeat(SeatCreateRequest request) {

        validateRoomHasNoScheduledShowtime(request.roomId());

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
                seatRepository.countByRoom_Id(room.getId())
        );

        return seatMapper.toResponse(savedSeat);
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(
            UUID seatId,
            SeatUpdateRequest request
    ) {

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy seat với id: " + seatId
                        )
                );

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
                    seatRepository.countByRoom_Id(
                            oldRoom.getId()
                    )
            );

            newRoom.setTotalSeats(
                    seatRepository.countByRoom_Id(
                            newRoom.getId()
                    )
            );

        }

        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional
    public SeatResponse updateSeatStatus(
            UUID seatId,
            SeatUpdateStatusRequest request
    ) {

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
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(UUID seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat với id: " + seatId
                ));

        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> getAllSeatsByRoomId(UUID roomId, Pageable pageable) {

        if (roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Không tồn tại phòng chiếu với id: " + roomId);
        }

        Page<Seat> seatPage = seatRepository.findAllByRoom_Id(roomId, pageable);
        List<SeatResponse> seatResponses = seatMapper.toResponseList(seatPage.getContent());

        return PageResponse.<SeatResponse>builder()
                .data(seatResponses)
                .pageNumber(seatPage.getNumber() + 1)
                .pageSize(seatPage.getSize())
                .totalPages(seatPage.getTotalPages())
                .totalElements(seatPage.getTotalElements())
                .build();
    }

    private void validateRoomHasNoScheduledShowtime(UUID roomId) {

        boolean hasScheduledShowtime =
                showtimeRepository
                        .existsByRoom_IdAndStatusAndEndTimeAfter(
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
