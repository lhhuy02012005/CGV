package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.room.RoomCreateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateRequest;
import com.cgv.catalogservice.dto.response.RoomResponse;
import com.cgv.catalogservice.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class, uses = {CinemaMapper.class})
public interface RoomMapper {

    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "totalSeats", ignore = true)
    @Mapping(target = "format", defaultValue = "TWO_D")
    @Mapping(target = "status", defaultValue = "ACTIVE")
    Room toEntity(RoomCreateRequest request);

    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "totalSeats", ignore = true)
    void updateEntity(RoomUpdateRequest request, @MappingTarget Room room);

    @Mapping(target = "cinemaResponse", source = "cinema")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);
}
