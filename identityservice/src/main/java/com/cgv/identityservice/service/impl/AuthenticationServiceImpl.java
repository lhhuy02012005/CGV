package com.cgv.identityservice.service.impl;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.identityservice.dto.request.AuthenticationRequest;
import com.cgv.identityservice.dto.request.RefreshTokenRequest;
import com.cgv.identityservice.dto.response.AuthenticationResponse;
import com.cgv.identityservice.entity.User;
import com.cgv.identityservice.repository.UserRepository;
import com.cgv.identityservice.repository.httpclient.KeycloakClient;
import com.cgv.identityservice.service.AuthenticationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.cgv.identityservice.entity.MemberShipTier;
import com.cgv.identityservice.repository.MemberShipTierRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthenticationServiceImpl implements AuthenticationService {
    UserRepository userRepository;
    MemberShipTierRepository memberShipTierRepository;
    JwtDecoder jwtDecoder;
    KeycloakClient keycloakClient;

    @NonFinal
    @Value("${keycloak.realm:cgv-realm}")
    String realm;

    @NonFinal
    @Value("${keycloak.client-id:CGV_App}")
    String clientId;

    @NonFinal
    @Value("${keycloak.client-secret}")
    String clientSecret;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "password");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("username", request.getUsername());
        body.put("password", request.getPassword());
        body.put("scope" , "openid");

        try {
            JsonNode responseNode = keycloakClient.exchangeToken(realm, body);
            assert responseNode != null;

            String accessToken = responseNode.get("access_token").asText();
            String refreshToken = responseNode.get("refresh_token").asText();
            long expiresIn = responseNode.get("expires_in").asLong();

            syncUserToLocalDatabase(accessToken);

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .authenticated(true)
                    .build();

        }catch (Exception e) {
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST,"Đăng nhập thất bại: Sai tài khoản hoặc mật khẩu!");
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "refresh_token");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("refresh_token", request.getRefreshToken());

        try {
            JsonNode responseNode = keycloakClient.exchangeToken(realm, body);

            assert responseNode != null;
            return AuthenticationResponse.builder()
                    .accessToken(responseNode.get("access_token").asText())
                    .refreshToken(responseNode.get("refresh_token").asText())
                    .expiresIn(responseNode.get("expires_in").asLong())
                    .authenticated(true)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn!", e);
        }
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("refresh_token", request.getRefreshToken());

        keycloakClient.logout(realm, body);
    }

    private void syncUserToLocalDatabase(String accessToken) {
        Jwt jwt = jwtDecoder.decode(accessToken);
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String sub = jwt.getSubject();

        if (email != null && userRepository.findByEmail(email).isEmpty()) {
            MemberShipTier defaultTier = memberShipTierRepository.findById("MEMBER")
                    .orElseGet(() -> memberShipTierRepository.save(MemberShipTier.builder()
                            .code("MEMBER")
                            .name("Member")
                            .minSpend(BigDecimal.ZERO)
                            .description("Hạng thành viên tiêu chuẩn")
                            .build()));

            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .keycloakId(sub)
                    .membershipTier(defaultTier)
                    .total_spend_ytd(BigDecimal.ZERO)
                    .build();
            userRepository.save(newUser);
        }
    }
}