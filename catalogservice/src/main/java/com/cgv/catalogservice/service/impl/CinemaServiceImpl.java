package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.cinema.CinemaCreateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaFilterRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.CinemaResponse;
import com.cgv.catalogservice.dto.response.NearbyCinemaResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Region;
import com.cgv.catalogservice.enums.CinemaStatus;
import com.cgv.catalogservice.mapper.CinemaMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.RegionRepository;
import com.cgv.catalogservice.service.CinemaService;
import com.cgv.catalogservice.specification.CinemaSpecification;
import com.cgv.catalogservice.util.GeoUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaServiceImpl implements CinemaService {

    CinemaRepository cinemaRepository;
    RegionRepository regionRepository;
    CinemaMapper cinemaMapper;

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaCreateRequest request) {
        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy region với id: " + request.regionId()
                ));

        Cinema cinema = cinemaMapper.toEntity(request);
        cinema.setRegion(region);

        Cinema savedCinema = cinemaRepository.save(cinema);

        return cinemaMapper.toResponse(savedCinema);
    }

    @Override
    @Transactional
    public CinemaResponse updateCinema(UUID cinemaId, CinemaUpdateRequest request) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cinema với id: " + cinemaId
                ));

        cinemaMapper.updateEntity(request, cinema);

        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy region với id: " + request.regionId()
                    ));
            cinema.setRegion(region);
        }

        cinemaRepository.saveAndFlush(cinema);

        return cinemaMapper.toResponse(cinema);
    }

    @Override
    @Transactional
    public CinemaResponse updateCinemaStatus(UUID cinemaId, CinemaUpdateStatusRequest request) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cinema với id: " + cinemaId
                ));

        cinema.setStatus(request.status());

        cinemaRepository.saveAndFlush(cinema);

        return cinemaMapper.toResponse(cinema);
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(UUID cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cinema với id: " + cinemaId
                ));

        return cinemaMapper.toResponse(cinema);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CinemaResponse> getAllCinemas(
            CinemaFilterRequest filter,
            Pageable pageable
    ) {

        Specification<Cinema> specification =
                Specification.allOf(
                        CinemaSpecification.containsKeyword(
                                filter.keyword()
                        ),
                        CinemaSpecification.hasRegionId(
                                filter.regionId()
                        ),
                        CinemaSpecification.hasRegionSlug(
                                filter.regionSlug()
                        ),
                        CinemaSpecification.hasStatus(
                                filter.status()
                        ),
                        CinemaSpecification.hasAmenity(
                                filter.amenity()
                        )
                );

        Page<Cinema> cinemaPage =
                cinemaRepository.findAll(
                        specification,
                        pageable
                );

        List<CinemaResponse> cinemaResponses =
                cinemaMapper.toResponseList(
                        cinemaPage.getContent()
                );

        return PageResponse.<CinemaResponse>builder()
                .data(cinemaResponses)
                .pageNumber(cinemaPage.getNumber() + 1)
                .pageSize(cinemaPage.getSize())
                .totalPages(cinemaPage.getTotalPages())
                .totalElements(cinemaPage.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyCinemaResponse> getNearbyCinemas(double latitude, double longitude, Double radiusKm) {
        List<Cinema> activeCinemas = cinemaRepository.findByStatusWithRegion(CinemaStatus.ACTIVE);

        return activeCinemas.stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .map(c -> {
                    double dist = GeoUtils.calculateDistanceInKm(latitude, longitude, c.getLatitude(), c.getLongitude());
                    return NearbyCinemaResponse.builder()
                            .id(c.getId())
                            .regionId(c.getRegion() != null ? c.getRegion().getId() : null)
                            .regionName(c.getRegion() != null ? c.getRegion().getName() : null)
                            .name(c.getName())
                            .address(c.getAddress())
                            .phone(c.getPhone())
                            .openingHours(c.getOpeningHours())
                            .latitude(c.getLatitude())
                            .longitude(c.getLongitude())
                            .distanceInKm(dist)
                            .status(c.getStatus())
                            .build();
                })
                .filter(res -> radiusKm == null || res.distanceInKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyCinemaResponse::distanceInKm))
                .toList();
    }
}
