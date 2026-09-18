package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.seat.SeatCreateRequest;
import com.cgv.catalogservice.dto.request.seat.SeatUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatResponse;
import com.cgv.catalogservice.entity.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface SeatMapper {

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "seatType", ignore = true)
    @Mapping(target = "isActive", defaultValue = "true")
    Seat toEntity(SeatCreateRequest request);

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "seatType", ignore = true)
    void updateEntity(SeatUpdateRequest request, @MappingTarget Seat seat);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "seatTypeName", source = "seatType.name")
    SeatResponse toResponse(Seat seat);

    List<SeatResponse> toResponseList(List<Seat> seats);
}
