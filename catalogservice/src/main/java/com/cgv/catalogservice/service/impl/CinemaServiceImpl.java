package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.cinema.CinemaCreateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaFilterRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateRequest;
import com.cgv.catalogservice.dto.request.cinema.CinemaUpdateStatusRequest;
import com.cgv.catalogservice.dto.response.CinemaResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.Region;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.CinemaMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.RegionRepository;
import com.cgv.catalogservice.service.CinemaService;
import com.cgv.catalogservice.specification.CinemaSpecification;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaServiceImpl implements CinemaService {

    CinemaRepository cinemaRepository;
    RegionRepository regionRepository;
    CinemaMapper cinemaMapper;

    @Override
    @CacheEvict(
            value = "cinemasByRegion",
            allEntries = true
    )
    @Transactional
    public CinemaResponse createCinema(CinemaCreateRequest request) {

        if (cinemaRepository.existsByName(request.name())) {
            throw new ResourceConflictException(
                    "Rạp chiếu với tên '" + request.name() + "' đã tồn tại"
            );
        }

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
    @Caching(
            put = {
                    @CachePut(
                            value = "cinema",
                            key = "#cinemaId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "cinemasByRegion",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public CinemaResponse updateCinema(UUID cinemaId, CinemaUpdateRequest request) {

        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cinema với id: " + cinemaId
                ));

        if (request.name() != null
                && cinemaRepository.existsByNameAndIdNot(
                request.name(),
                cinemaId
        )) {

            throw new ResourceConflictException(
                    "Rạp chiếu với tên '"
                            + request.name()
                            + "' đã tồn tại"
            );
        }

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
    @Caching(
            put = {
                    @CachePut(
                            value = "cinema",
                            key = "#cinemaId"
                    )
            },
            evict = {
                    @CacheEvict(
                            value = "cinemasByRegion",
                            allEntries = true
                    )
            }
    )
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
    @Cacheable(
            value = "cinema",
            key = "#cinemaId"
    )
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(UUID cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy cinema với id: " + cinemaId
                ));

        return cinemaMapper.toResponse(cinema);
    }

    @Override
    @Cacheable(
            value = "cinemasByRegion",
            key = "#regionId"
                    + " + ':page=' + #pageable.pageNumber"
                    + " + ':size=' + #pageable.pageSize"
                    + " + ':sort=' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public PageResponse<CinemaResponse> getCinemasByRegionId(
            Integer regionId,
            Pageable pageable
    ) {

        if (!regionRepository.existsById(regionId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy region với id: " + regionId
            );
        }

        Specification<Cinema> spec =
                Specification.allOf(
                        CinemaSpecification.hasRegionId(
                                regionId
                        )
                );

        return PageResponseUtils.findAllAndMap(
                p -> cinemaRepository.findAll(spec, p),
                pageable,
                cinemaMapper::toResponseList
        );
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

        return PageResponseUtils.findAllAndMap(
                p -> cinemaRepository.findAll(specification, p),
                pageable,
                cinemaMapper::toResponseList
        );
    }
}
