package com.cgv.identityservice.service;

import com.cgv.identityservice.dto.request.AuthenticationRequest;
import com.cgv.identityservice.dto.request.RefreshTokenRequest;
import com.cgv.identityservice.dto.request.UserRegistrationRequest;
import com.cgv.identityservice.dto.request.VerifyOtpRequest;
import com.cgv.identityservice.dto.response.AuthenticationResponse;
import com.cgv.identityservice.dto.response.UserResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    AuthenticationResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
    void initiateRegistration(UserRegistrationRequest request);
    UserResponse verifyAndRegister(VerifyOtpRequest request);
    UserResponse syncUserFromAccessToken(String accessToken, String customFullName);
}
