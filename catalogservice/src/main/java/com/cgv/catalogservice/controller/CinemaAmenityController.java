package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgv.catalogservice.dto.request.cinemaamenity.CinemaAmenityCreateRequest;
import com.cgv.catalogservice.dto.response.CinemaAmenityResponse;
import com.cgv.catalogservice.enums.Amenity;
import com.cgv.catalogservice.service.CinemaAmenityService;
import com.cgv.commondto.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cinema Amenities", description = "API quản lý quan hệ giữa rạp chiếu phim và các tiện ích của rạp.")
@RestController
@RequestMapping("/cinema-amenities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaAmenityController {

    CinemaAmenityService cinemaAmenityService;

    @Operation(
            summary = "Thêm tiện ích cho rạp",
            description = "Gán một tiện ích cho rạp chiếu phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CinemaAmenityResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu thêm tiện ích cho rạp", required = true) @RequestBody @Valid CinemaAmenityCreateRequest request
    ) {

        CinemaAmenityResponse response =
                cinemaAmenityService.createCinemaAmenity(request);

        return ApiResponse.<CinemaAmenityResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Thêm tiện ích cho rạp thành công")
                .build();
    }

    @Operation(
            summary = "Xoá tiện ích khỏi rạp",
            description = "Xoá một tiện ích cụ thể khỏi rạp chiếu phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{cinemaId}/{amenity}")
    public ApiResponse<Void> delete(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId,
            @Parameter(description = "Tiện ích của rạp", required = true, schema = @Schema(implementation = Amenity.class)) @PathVariable Amenity amenity
    ) {

        cinemaAmenityService.deleteCinemaAmenity(
                cinemaId,
                amenity
        );

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá tiện ích khỏi rạp thành công")
                .build();
    }

    @Operation(
            summary = "Lấy tiện ích của rạp",
            description = "Lấy toàn bộ tiện ích hiện có của một rạp chiếu phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/cinema/{cinemaId}")
    public ApiResponse<List<CinemaAmenityResponse>> getAmenitiesByCinemaId(
            @Parameter(description = "ID của rạp chiếu phim", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID cinemaId
    ) {

        List<CinemaAmenityResponse> response =
                cinemaAmenityService.getAmenitiesByCinemaId(cinemaId);

        return ApiResponse.<List<CinemaAmenityResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách tiện ích của rạp")
                .build();
    }
}
