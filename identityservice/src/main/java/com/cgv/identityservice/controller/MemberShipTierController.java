package com.cgv.identityservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.identityservice.dto.request.MemberShipTierRequest;
import com.cgv.identityservice.dto.response.MemberShipTierResponse;
import com.cgv.identityservice.service.MemberShipTierService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/membership-tiers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MemberShipTierController {

    MemberShipTierService memberShipTierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberShipTierResponse> create(@Valid @RequestBody MemberShipTierRequest request) {
        MemberShipTierResponse response = memberShipTierService.createTier(request);
        return ApiResponse.<MemberShipTierResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo hạng thành viên thành công")
                .build();
    }

    @PutMapping("/{code}")
    public ApiResponse<MemberShipTierResponse> update(
            @PathVariable String code,
            @Valid @RequestBody MemberShipTierRequest request
    ) {
        MemberShipTierResponse response = memberShipTierService.updateTier(code, request);
        return ApiResponse.<MemberShipTierResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật hạng thành viên thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<MemberShipTierResponse>> getAll() {
        List<MemberShipTierResponse> response = memberShipTierService.getAllTiers();
        return ApiResponse.<List<MemberShipTierResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách hạng thành viên")
                .build();
    }

    @GetMapping("/{code}")
    public ApiResponse<MemberShipTierResponse> getByCode(@PathVariable String code) {
        MemberShipTierResponse response = memberShipTierService.getTierByCode(code);
        return ApiResponse.<MemberShipTierResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Chi tiết hạng thành viên")
                .build();
    }

    @DeleteMapping("/{code}")
    public ApiResponse<Void> delete(@PathVariable String code) {
        memberShipTierService.deleteTier(code);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa hạng thành viên thành công")
                .build();
    }
}
