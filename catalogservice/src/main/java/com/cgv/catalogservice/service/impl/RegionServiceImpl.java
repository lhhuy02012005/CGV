package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.region.RegionCreateRequest;
import com.cgv.catalogservice.dto.request.region.RegionUpdateRequest;
import com.cgv.catalogservice.dto.response.RegionResponse;
import com.cgv.catalogservice.entity.Region;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.RegionMapper;
import com.cgv.catalogservice.repository.CinemaRepository;
import com.cgv.catalogservice.repository.RegionRepository;
import com.cgv.catalogservice.service.RegionService;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegionServiceImpl implements RegionService {

    RegionRepository regionRepository;
    CinemaRepository cinemaRepository;

    RegionMapper regionMapper;

    @Override
    @Transactional
    public RegionResponse createRegion(RegionCreateRequest request) {
        Region region = regionMapper.toEntity(request);
        Region savedRegion = regionRepository.save(region);

        return regionMapper.toResponse(savedRegion);
    }

    @Override
    @Transactional
    public RegionResponse updateRegion(Integer regionId, RegionUpdateRequest request) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy region với id: " + regionId
                ));

        regionMapper.updateEntity(request, region);

        return regionMapper.toResponse(region);
    }

    @Override
    @Transactional
    public void deleteRegion(Integer regionId) {

        if (cinemaRepository.existsByRegionId(regionId)) {
            throw new ResourceConflictException(
                    "Không thể xoá region vì đang có cinema thuộc region này"
            );
        }

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy region với id: " + regionId
                ));

        regionRepository.delete(region);
    }

    @Override
    @Transactional(readOnly = true)
    public RegionResponse getRegionById(Integer regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy region với id: " + regionId
                ));

        return regionMapper.toResponse(region);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RegionResponse> getAllRegions(Pageable pageable) {
        Page<Region> regionPage = regionRepository.findAll(pageable);
        List<RegionResponse> regionResponses = regionMapper.toResponseList(regionPage.getContent());

        return PageResponse.<RegionResponse>builder()
                .data(regionResponses)
                .pageNumber(regionPage.getNumber() + 1)
                .pageSize(regionPage.getSize())
                .totalPages(regionPage.getTotalPages())
                .totalElements(regionPage.getTotalElements())
                .build();
    }
}
