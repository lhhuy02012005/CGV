package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.cinemaamenity.CinemaAmenityCreateRequest;
import com.cgv.catalogservice.dto.response.CinemaAmenityResponse;
import com.cgv.catalogservice.entity.Cinema;
import com.cgv.catalogservice.entity.CinemaAmenity;
import com.cgv.catalogservice.entity.CinemaAmenityId;
import com.cgv.catalogservice.enums.Amenity;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.CinemaAmenityMapper;
import com.cgv.catalogservice.repository.CinemaAmenityRepository;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.service.CinemaAmenityService;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaAmenityServiceImpl
        implements CinemaAmenityService {

    CinemaAmenityRepository cinemaAmenityRepository;
    CinemaRepository cinemaRepository;

    CinemaAmenityMapper cinemaAmenityMapper;

    @Override
    @Transactional
    public CinemaAmenityResponse createCinemaAmenity(
            CinemaAmenityCreateRequest request
    ) {

        if (cinemaAmenityRepository.existsByIdCinemaIdAndIdAmenity(
                request.cinemaId(),
                request.amenity()
        )) {
            throw new ResourceConflictException(
                    "Rạp chiếu đã có tiện ích " + request.amenity()
            );
        }

        Cinema cinema = cinemaRepository
                .findById(request.cinemaId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy rạp chiếu phim với id: "
                                        + request.cinemaId()
                        )
                );

        CinemaAmenity cinemaAmenity =
                cinemaAmenityMapper.toEntity(request, cinema);

        CinemaAmenity savedCinemaAmenity =
                cinemaAmenityRepository.save(cinemaAmenity);

        return cinemaAmenityMapper.toResponse(
                savedCinemaAmenity
        );
    }

    @Override
    @Transactional
    public void deleteCinemaAmenity(
            UUID cinemaId,
            Amenity amenity
    ) {

        CinemaAmenityId cinemaAmenityId =
                new CinemaAmenityId(cinemaId, amenity);

        if (!cinemaAmenityRepository.existsById(cinemaAmenityId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy tiện ích "
                            + amenity
                            + " của rạp "
                            + cinemaId
            );
        }

        cinemaAmenityRepository.deleteById(
                cinemaAmenityId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaAmenityResponse> getAmenitiesByCinemaId(
            UUID cinemaId
    ) {

        if (!cinemaRepository.existsById(cinemaId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy rạp chiếu phim với id: "
                            + cinemaId
            );
        }

        List<CinemaAmenity> cinemaAmenityList =
                cinemaAmenityRepository
                        .findByIdCinemaId(cinemaId);

        return cinemaAmenityMapper
                .toResponseList(cinemaAmenityList);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CinemaAmenityResponse> getCinemasByAmenity(
            Amenity amenity,
            Pageable pageable
    ) {

        Page<CinemaAmenity> cinemaAmenityPage =
                cinemaAmenityRepository.findByIdAmenity(
                        amenity,
                        pageable
                );

        List<CinemaAmenityResponse> responses =
                cinemaAmenityMapper.toResponseList(
                        cinemaAmenityPage.getContent()
                );

        return PageResponse.<CinemaAmenityResponse>builder()
                .data(responses)
                .pageNumber(cinemaAmenityPage.getNumber() + 1)
                .pageSize(cinemaAmenityPage.getSize())
                .totalPages(cinemaAmenityPage.getTotalPages())
                .totalElements(cinemaAmenityPage.getTotalElements())
                .build();
    }
}
