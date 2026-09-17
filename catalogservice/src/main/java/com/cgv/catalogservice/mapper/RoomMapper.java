package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.room.RoomCreateRequest;
import com.cgv.catalogservice.dto.request.room.RoomUpdateRequest;
import com.cgv.catalogservice.dto.response.RoomResponse;
import com.cgv.catalogservice.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface RoomMapper {

    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "totalSeats", ignore = true)
    Room toEntity(RoomCreateRequest request);

    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "totalSeats", ignore = true)
    void updateEntity(RoomUpdateRequest request, @MappingTarget Room room);

    @Mapping(target = "cinemaId", source = "cinema.id")
    @Mapping(target = "cinemaName", source = "cinema.name")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);
}
