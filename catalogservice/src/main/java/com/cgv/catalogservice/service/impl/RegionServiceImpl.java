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
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.catalogservice.util.SlugUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j(topic = "REGION-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegionServiceImpl implements RegionService {

    RegionRepository regionRepository;
    CinemaRepository cinemaRepository;

    RegionMapper regionMapper;

    @Override
    @Transactional
    public RegionResponse createRegion(
            RegionCreateRequest request
    ) {

        log.info("Creating region: name={}", request.name());
        String name = request.name();

        if (regionRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException(
                    "Khu vực với tên '" + name + "' đã tồn tại"
            );
        }

        String slug = SlugUtils.toSlug(name);

        if (regionRepository.existsBySlug(slug)) {
            throw new ResourceConflictException(
                    "Slug '" + slug + "' đã tồn tại"
            );
        }

        Region region = regionMapper.toEntity(request);

        region.setSlug(slug);

        Region savedRegion = regionRepository.save(region);

        return regionMapper.toResponse(savedRegion);
    }

    @Override
    @Transactional
    public RegionResponse updateRegion(
            Integer regionId,
            RegionUpdateRequest request
    ) {

        log.info("Updating region: regionId={}", regionId);
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy region với id: " + regionId
                        )
                );

        String name = request.name();

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên khu vực không được để trống"
            );
        }

        if (regionRepository.existsByNameIgnoreCaseAndIdNot(
                name,
                regionId
        )) {
            throw new ResourceConflictException(
                    "Khu vực với tên '" + name + "' đã tồn tại"
            );
        }

        String slug = SlugUtils.toSlug(name);

        if (regionRepository.existsBySlugAndIdNot(
                slug,
                regionId
        )) {
            throw new ResourceConflictException(
                    "Slug '" + slug + "' đã tồn tại"
            );
        }

        regionMapper.updateEntity(request, region);

        region.setSlug(slug);

        return regionMapper.toResponse(region);
    }

    @Override
    @Transactional
    public void deleteRegion(Integer regionId) {

        log.info("Deleting region: regionId={}", regionId);

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

        log.debug("Getting region by id: regionId={}", regionId);
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy region với id: " + regionId
                ));

        return regionMapper.toResponse(region);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RegionResponse> getAllRegions(Pageable pageable) {

        log.debug("Getting all regions: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        return PageResponseUtils.findAllAndMap(
                regionRepository::findAll,
                pageable,
                regionMapper::toResponseList
        );
    }
}
