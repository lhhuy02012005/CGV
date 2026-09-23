package com.cgv.identityservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.identityservice.dto.request.AuthenticationRequest;
import com.cgv.identityservice.dto.request.RefreshTokenRequest;
import com.cgv.identityservice.dto.request.SocialSyncRequest;
import com.cgv.identityservice.dto.request.UserRegistrationRequest;
import com.cgv.identityservice.dto.request.VerifyOtpRequest;
import com.cgv.identityservice.dto.response.AuthenticationResponse;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .data(authenticationService.authenticate(request))
                .build();
    }

    @PostMapping("/register/init")
    public ApiResponse<Void> registerInit(@RequestBody @Valid UserRegistrationRequest request) {
        authenticationService.initiateRegistration(request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Mã OTP đã được gửi tới email của bạn.")
                .build();
    }

    @PostMapping("/register/verify")
    public ApiResponse<UserResponse> registerVerify(@RequestBody @Valid VerifyOtpRequest request) {
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .data(authenticationService.verifyAndRegister(request))
                .message("Đăng ký tài khoản thành công!")
                .build();
    }

    @PostMapping(value = "/social-sync")
    public ApiResponse<UserResponse> socialSync(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) SocialSyncRequest request
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String customFullName = request != null ? request.getFullName() : null;
            UserResponse userResponse = authenticationService.syncUserFromAccessToken(token, customFullName);
            return ApiResponse.<UserResponse>builder()
                    .status(HttpStatus.OK.value())
                    .data(userResponse)
                    .message("Đồng bộ user social thành công!")
                    .build();
        }
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Token không hợp lệ hoặc thiếu Authorization header!")
                .build();
    }

    @PostMapping("/exchange-code")
    public ApiResponse<AuthenticationResponse> exchangeCode(@RequestBody @Valid com.cgv.identityservice.dto.request.ExchangeCodeRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .data(authenticationService.exchangeCode(request))
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .data(authenticationService.refreshToken(request))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .build();
    }
}
