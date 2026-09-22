package com.cgv.catalogservice.controller;

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

@RestController
@RequestMapping("/cinema-amenities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaAmenityController {

    CinemaAmenityService cinemaAmenityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CinemaAmenityResponse> create(
            @RequestBody @Valid CinemaAmenityCreateRequest request
    ) {

        CinemaAmenityResponse response =
                cinemaAmenityService.createCinemaAmenity(request);

        return ApiResponse.<CinemaAmenityResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Thêm tiện ích cho rạp thành công")
                .build();
    }

    @DeleteMapping("/{cinemaId}/{amenity}")
    public ApiResponse<Void> delete(
            @PathVariable UUID cinemaId,
            @PathVariable Amenity amenity
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

    @GetMapping("/cinema/{cinemaId}")
    public ApiResponse<List<CinemaAmenityResponse>> getAmenitiesByCinemaId(
            @PathVariable UUID cinemaId
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
