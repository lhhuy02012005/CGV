package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.request.region.RegionCreateRequest;
import com.cgv.catalogservice.dto.request.region.RegionUpdateRequest;
import com.cgv.catalogservice.dto.response.RegionResponse;
import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface RegionService {

    RegionResponse createRegion(RegionCreateRequest request);

    RegionResponse updateRegion(Integer regionId, RegionUpdateRequest request);

    void deleteRegion(Integer regionId);

    RegionResponse getRegionById(Integer regionId);

    PageResponse<RegionResponse> getAllRegions(Pageable pageable);
}
