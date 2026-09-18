package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.seat.SeatCreateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.SeatResponse;
import com.cgv.catalogservice.entity.Room;
import com.cgv.catalogservice.entity.Seat;
import com.cgv.catalogservice.entity.SeatType;
import com.cgv.catalogservice.enums.SeatTypeName;
import com.cgv.catalogservice.mapper.SeatMapper;
import com.cgv.catalogservice.repository.RoomRepository;
import com.cgv.catalogservice.repository.SeatRepository;
import com.cgv.catalogservice.repository.SeatTypeRepository;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatServiceImpl implements SeatService {

    SeatRepository seatRepository;
    RoomRepository roomRepository;
    SeatTypeRepository seatTypeRepository;
    SeatMapper seatMapper;

    @Override
    @Transactional
    public SeatResponse createSeat(SeatCreateRequest request) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy room với id: " + request.roomId()
                ));

        SeatTypeName seatTypeName = request.seatTypeName() != null
                ? request.seatTypeName()
                : SeatTypeName.NORMAL;

        SeatType seatType = seatTypeRepository.findById(seatTypeName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat type với name: " + seatTypeName
                ));

        Seat seat = seatMapper.toEntity(request);
        seat.setRoom(room);
        seat.setSeatType(seatType);

        Seat savedSeat = seatRepository.save(seat);

        return seatMapper.toResponse(savedSeat);
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(UUID seatId, SeatUpdateRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat với id: " + seatId
                ));

        seatMapper.updateEntity(request, seat);

        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy room với id: " + request.roomId()
                    ));
            seat.setRoom(room);
        }

        if (request.seatTypeName() != null) {
            SeatType seatType = seatTypeRepository.findById(request.seatTypeName())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy seat type với name: " + request.seatTypeName()
                    ));
            seat.setSeatType(seatType);
        }

        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional
    public SeatResponse updateSeatStatus(UUID seatId, SeatUpdateStatusRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat với id: " + seatId
                ));

        seat.setIsActive(request.isActive());

        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional
    public void deleteSeat(UUID seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat với id: " + seatId
                ));

        seatRepository.delete(seat);
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
    public PageResponse<SeatResponse> getAllSeats(Pageable pageable) {
        Page<Seat> seatPage = seatRepository.findAll(pageable);
        List<SeatResponse> seatResponses = seatMapper.toResponseList(seatPage.getContent());

        return PageResponse.<SeatResponse>builder()
                .data(seatResponses)
                .pageNumber(seatPage.getNumber() + 1)
                .pageSize(seatPage.getSize())
                .totalPages(seatPage.getTotalPages())
                .totalElements(seatPage.getTotalElements())
                .build();
    }
}
