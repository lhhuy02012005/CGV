package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.showtime.ShowtimeCreateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeFilterRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateRequest;
import com.cgv.catalogservice.dto.request.showtime.ShowtimeUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.ShowtimeResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ShowtimeService {

    ShowtimeResponse createShowtime(
            ShowtimeCreateRequest request
    );

    ShowtimeResponse updateShowtime(
            UUID showtimeId,
            ShowtimeUpdateRequest request
    );

    ShowtimeResponse updateShowtimeStatus(
            UUID showtimeId,
            ShowtimeUpdateStatusRequest request
    );

    ShowtimeResponse getShowtimeById(
            UUID showtimeId
    );

    PageResponse<ShowtimeResponse> getAllShowtimes(
            ShowtimeFilterRequest filter,
            Pageable pageable
    );
}
