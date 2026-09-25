package com.cgv.identityservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.identityservice.dto.request.UpdateProfileRequest;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    private String getUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập trước khi thực hiện thao tác này!");
        }
        return jwt.getSubject();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('user:manage', 'SUPER_ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var response = userService.getAllUsers(keyword, sort, page, size);
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách người dùng")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .data(userService.getMyProfile(userId))
                .message("Thông tin tài khoản cá nhân")
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        String userId = getUserId(jwt);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .data(userService.updateProfile(userId, request))
                .message("Cập nhật thông tin tài khoản thành công")
                .build();
    }
}
