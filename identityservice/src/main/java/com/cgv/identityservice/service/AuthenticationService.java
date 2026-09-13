package com.cgv.identityservice.service;

import com.cgv.identityservice.dto.request.AuthenticationRequest;
import com.cgv.identityservice.dto.request.RefreshTokenRequest;
import com.cgv.identityservice.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    AuthenticationResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}
