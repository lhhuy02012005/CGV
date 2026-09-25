package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.cinema.CinemaCreateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaFilterRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.CinemaResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import com.cgv.catalogservice.dto.response.NearbyCinemaResponse;

import java.util.List;
import java.util.UUID;

public interface CinemaService {

    CinemaResponse createCinema(CinemaCreateRequest request);

    CinemaResponse updateCinema(UUID cinemaId, CinemaUpdateRequest request);

    CinemaResponse updateCinemaStatus(UUID cinemaId, CinemaUpdateStatusRequest request);

    CinemaResponse getCinemaById(UUID cinemaId);

    PageResponse<CinemaResponse> getCinemasByRegionId(Integer regionId, Pageable pageable);

    PageResponse<CinemaResponse> getAllCinemas(
            CinemaFilterRequest filter,
            Pageable pageable
    );

    List<NearbyCinemaResponse> getNearbyCinemas(
            double latitude,
            double longitude,
            Double radiusKm
    );

    void deleteCinema(UUID cinemaId);
}
