package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.region.RegionCreateRequest;
import com.cgv.catalogservice.dto.request.region.RegionUpdateRequest;
import com.cgv.catalogservice.dto.response.RegionResponse;
import com.cgv.catalogservice.entity.Region;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface RegionMapper {

    Region toEntity(RegionCreateRequest request);

    void updateEntity(RegionUpdateRequest request, @MappingTarget Region region);

    RegionResponse toResponse(Region region);

    List<RegionResponse> toResponseList(List<Region> regions);
}
