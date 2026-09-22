package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.dto.request.seattype.SeatTypeCreateRequest;
import com.cgv.catalogservice.dto.request.seattype.SeatTypeUpdateRequest;
import com.cgv.catalogservice.dto.response.SeatTypeResponse;
import com.cgv.catalogservice.enums.SeatTypeName;
import com.cgv.catalogservice.service.SeatTypeService;
import com.cgv.commondto.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seat-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatTypeController {

    SeatTypeService seatTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatTypeResponse> create(
            @RequestBody @Valid SeatTypeCreateRequest request
    ) {
        SeatTypeResponse response = seatTypeService.createSeatType(request);

        return ApiResponse.<SeatTypeResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo loại ghế thành công")
                .build();
    }

    @PatchMapping("/{seatTypeName}")
    public ApiResponse<SeatTypeResponse> update(
            @PathVariable SeatTypeName seatTypeName,
            @RequestBody @Valid SeatTypeUpdateRequest request
    ) {
        SeatTypeResponse response = seatTypeService.updateSeatType(seatTypeName, request);

        return ApiResponse.<SeatTypeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật loại ghế thành công")
                .build();
    }

    @DeleteMapping("/{seatTypeName}")
    public ApiResponse<Void> delete(@PathVariable SeatTypeName seatTypeName) {
        seatTypeService.deleteSeatType(seatTypeName);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá loại ghế thành công")
                .build();
    }

    @GetMapping("/{seatTypeName}")
    public ApiResponse<SeatTypeResponse> get(@PathVariable SeatTypeName seatTypeName) {
        SeatTypeResponse response = seatTypeService.getSeatTypeByName(seatTypeName);

        return ApiResponse.<SeatTypeResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết loại ghế thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<SeatTypeResponse>> findAll() {

        List<SeatTypeResponse> response = seatTypeService.getAllSeatTypes();

        return ApiResponse.<List<SeatTypeResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách loại ghế")
                .build();
    }
}
