package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.region.RegionCreateRequest;
import com.cgv.catalogservice.dto.request.region.RegionUpdateRequest;
import com.cgv.catalogservice.dto.response.RegionResponse;
import com.cgv.catalogservice.service.RegionService;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegionController {

    RegionService regionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegionResponse> create(
            @RequestBody @Valid RegionCreateRequest request
    ) {
        RegionResponse response = regionService.createRegion(request);

        return ApiResponse.<RegionResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo khu vực thành công")
                .build();
    }

    @PutMapping("/{regionId}")
    public ApiResponse<RegionResponse> update(
            @PathVariable Integer regionId,
            @RequestBody @Valid RegionUpdateRequest request
    ) {
        RegionResponse response = regionService.updateRegion(regionId, request);

        return ApiResponse.<RegionResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật khu vực thành công")
                .build();
    }

    @DeleteMapping("/{regionId}")
    public ApiResponse<Void> delete(@PathVariable Integer regionId) {
        regionService.deleteRegion(regionId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá khu vực thành công")
                .build();
    }

    @GetMapping("/{regionId}")
    public ApiResponse<RegionResponse> get(@PathVariable Integer regionId) {
        RegionResponse response = regionService.getRegionById(regionId);

        return ApiResponse.<RegionResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết khu vực thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<RegionResponse>> findAll(
            @PageableDefault Pageable pageable
    ) {
        PageResponse<RegionResponse> response = regionService.getAllRegions(pageable);

        return ApiResponse.<PageResponse<RegionResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách khu vực")
                .build();
    }
}
