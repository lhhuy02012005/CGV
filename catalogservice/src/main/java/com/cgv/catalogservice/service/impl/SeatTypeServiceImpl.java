package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.seattype.SeatTypeCreateRequest;
import com.cgv.catalogservice.dto.request.seattype.SeatTypeUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatTypeResponse;
import com.cgv.catalogservice.entity.SeatType;
import com.cgv.catalogservice.enums.SeatTypeName;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.SeatTypeMapper;
import com.cgv.catalogservice.repository.SeatRepository;
import com.cgv.catalogservice.repository.SeatTypeRepository;
import com.cgv.catalogservice.service.SeatTypeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j(topic = "SEAT-TYPE-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatTypeServiceImpl implements SeatTypeService {

    SeatTypeRepository seatTypeRepository;
    SeatRepository seatRepository;

    SeatTypeMapper seatTypeMapper;

    @Override
    @Transactional
    public SeatTypeResponse createSeatType(SeatTypeCreateRequest request) {

        log.info("Creating seat type");

        if (seatTypeRepository.existsByName(request.name())) {
            throw new ResourceConflictException(
                    "Seat type " + request.name() + " đã tồn tại"
            );
        }

        SeatType seatType = seatTypeMapper.toEntity(request);
        SeatType savedSeatType = seatTypeRepository.save(seatType);

        return seatTypeMapper.toResponse(savedSeatType);
    }

    @Override
    @Transactional
    public SeatTypeResponse updateSeatType(
            SeatTypeName seatTypeName,
            SeatTypeUpdateRequest request
    ) {

        log.info("Updating seat type: seatTypeName={}", seatTypeName);

        SeatType seatType = seatTypeRepository.findById(seatTypeName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat type với name: " + seatTypeName
                ));

        seatTypeMapper.updateEntity(request, seatType);

        return seatTypeMapper.toResponse(seatType);
    }

    @Override
    @Transactional
    public void deleteSeatType(SeatTypeName seatTypeName) {

        log.info("Deleting seat type: seatTypeName={}", seatTypeName);
        SeatType seatType = seatTypeRepository.findById(seatTypeName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat type với name: " + seatTypeName
                ));

        boolean isUsed =
                seatRepository.existsBySeatTypeName(seatTypeName);

        if (isUsed) {
            throw new ResourceConflictException(
                    "Không thể xoá seat type "
                            + seatTypeName
                            + " vì đang có ghế sử dụng loại ghế này"
            );
        }

        seatTypeRepository.delete(seatType);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatTypeResponse getSeatTypeByName(SeatTypeName seatTypeName) {

        log.debug("Getting seat type by name: seatTypeName={}", seatTypeName);
        SeatType seatType = seatTypeRepository.findById(seatTypeName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat type với name: " + seatTypeName
                ));

        return seatTypeMapper.toResponse(seatType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatTypeResponse> getAllSeatTypes() {

        log.debug("Getting all seat types");

        List<SeatType> seatTypeList = seatTypeRepository.findAll();

        return seatTypeMapper.toResponseList(seatTypeList);
    }
}
