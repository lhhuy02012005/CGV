package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.seattype.SeatTypeCreateRequest;
import com.cgv.catalogservice.dto.request.seattype.SeatTypeUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatTypeResponse;
import com.cgv.catalogservice.entity.SeatType;
import com.cgv.catalogservice.enums.SeatTypeName;
import com.cgv.catalogservice.mapper.SeatTypeMapper;
import com.cgv.catalogservice.repository.SeatTypeRepository;
import com.cgv.catalogservice.service.SeatTypeService;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatTypeServiceImpl implements SeatTypeService {

    SeatTypeRepository seatTypeRepository;
    SeatTypeMapper seatTypeMapper;

    @Override
    @Transactional
    public SeatTypeResponse createSeatType(SeatTypeCreateRequest request) {
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
        SeatType seatType = seatTypeRepository.findById(seatTypeName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat type với name: " + seatTypeName
                ));

        seatTypeRepository.delete(seatType);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatTypeResponse getSeatTypeByName(SeatTypeName seatTypeName) {
        SeatType seatType = seatTypeRepository.findById(seatTypeName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy seat type với name: " + seatTypeName
                ));

        return seatTypeMapper.toResponse(seatType);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SeatTypeResponse> getAllSeatTypes(Pageable pageable) {
        Page<SeatType> seatTypePage = seatTypeRepository.findAll(pageable);
        List<SeatTypeResponse> seatTypeResponses =
                seatTypeMapper.toResponseList(seatTypePage.getContent());

        return PageResponse.<SeatTypeResponse>builder()
                .data(seatTypeResponses)
                .pageNumber(seatTypePage.getNumber() + 1)
                .pageSize(seatTypePage.getSize())
                .totalPages(seatTypePage.getTotalPages())
                .totalElements(seatTypePage.getTotalElements())
                .build();
    }
}
