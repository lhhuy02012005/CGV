package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.seattype.SeatTypeCreateRequest;
import com.cgv.catalogservice.dto.request.seattype.SeatTypeUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatTypeResponse;
import com.cgv.catalogservice.entity.SeatType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface SeatTypeMapper {

    SeatType toEntity(SeatTypeCreateRequest request);

    void updateEntity(SeatTypeUpdateRequest request, @MappingTarget SeatType seatType);

    SeatTypeResponse toResponse(SeatType seatType);

    List<SeatTypeResponse> toResponseList(List<SeatType> seatTypes);
}
