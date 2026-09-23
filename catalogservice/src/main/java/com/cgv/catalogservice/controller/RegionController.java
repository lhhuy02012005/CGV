package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

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

@Tag(name = "Regions", description = "API quản lý khu vực dùng để phân nhóm rạp chiếu phim.")
@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegionController {

    RegionService regionService;

    @Operation(
            summary = "Tạo khu vực",
            description = "Tạo khu vực mới. Slug được sinh tự động từ tên ở tầng service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegionResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo khu vực", required = true) @RequestBody @Valid RegionCreateRequest request
    ) {
        RegionResponse response = regionService.createRegion(request);

        return ApiResponse.<RegionResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo khu vực thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật khu vực",
            description = "Thay thế thông tin tên của khu vực theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PutMapping("/{regionId}")
    public ApiResponse<RegionResponse> update(
            @Parameter(description = "ID của khu vực", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer regionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật khu vực", required = true) @RequestBody @Valid RegionUpdateRequest request
    ) {
        RegionResponse response = regionService.updateRegion(regionId, request);

        return ApiResponse.<RegionResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật khu vực thành công")
                .build();
    }

    @Operation(
            summary = "Xoá khu vực",
            description = "Xoá khu vực nếu không còn rạp chiếu phim tham chiếu."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{regionId}")
    public ApiResponse<Void> delete(@Parameter(description = "ID của khu vực", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer regionId) {
        regionService.deleteRegion(regionId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá khu vực thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết khu vực",
            description = "Lấy thông tin khu vực theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{regionId}")
    public ApiResponse<RegionResponse> get(@Parameter(description = "ID của khu vực", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer regionId) {
        RegionResponse response = regionService.getRegionById(regionId);

        return ApiResponse.<RegionResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết khu vực thành công")
                .build();
    }

    @Operation(
            summary = "Lấy danh sách khu vực",
            description = "Lấy danh sách khu vực có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<RegionResponse>> findAll(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<RegionResponse> response = regionService.getAllRegions(pageable);

        return ApiResponse.<PageResponse<RegionResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách khu vực")
                .build();
    }
}
