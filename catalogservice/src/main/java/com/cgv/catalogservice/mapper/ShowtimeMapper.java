package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.showtime.ShowtimeCreateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateRequest;
import com.cgv.catalogservice.dto.response.ShowtimeResponse;
import com.cgv.catalogservice.entity.Showtime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class, uses = {RoomMapper.class})
public interface ShowtimeMapper {

    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "availableSeats", ignore = true)
    @Mapping(target = "status", defaultValue = "SCHEDULED")
    Showtime toEntity(ShowtimeCreateRequest request);

    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "availableSeats", ignore = true)
    void updateEntity(ShowtimeUpdateRequest request, @MappingTarget Showtime showtime);

    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "movieTitle", source = "movie.title")
    @Mapping(target = "roomResponse", source = "room")
    ShowtimeResponse toResponse(Showtime showtime);

    List<ShowtimeResponse> toResponseList(List<Showtime> showtimes);
}
