package com.cgv.identityservice.service.impl;

import com.cgv.commondto.event.NotificationEvent;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.identityservice.dto.request.*;
import com.cgv.identityservice.dto.response.AuthenticationResponse;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.entity.User;
import com.cgv.identityservice.mapper.UserMapper;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthenticationServiceImpl implements AuthenticationService {
    UserRepository userRepository;
    MemberShipTierRepository memberShipTierRepository;
    JwtDecoder jwtDecoder;
    KeycloakClient keycloakClient;
    UserMapper userMapper;
    RedisTemplate<String, Object> redisTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;

    //Redis
    String OTP_KEY = "OTP:";

    //Kafka
    String NOTIFICATIONSENDTOPIC = "notification.send";

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

    @Override
    public void initiateRegistration(UserRegistrationRequest request){
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email đã tồn tại trong hệ thống!");
        }
        String otp = generateOTP();
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("email", request.getEmail());
        registrationData.put("otp", otp);

        redisTemplate.opsForValue().set(OTP_KEY+request.getEmail(), registrationData, Duration.ofMinutes(5));
        NotificationEvent event = NotificationEvent.builder()
                .email(request.getEmail())
                .otp(otp)
                .build();
        kafkaTemplate.send(NOTIFICATIONSENDTOPIC, event);
        log.info("Đã khởi tạo đăng ký và gửi OTP tới Kafka cho email: {}", request.getEmail());
    }

    @Override
    public UserResponse verifyAndRegister(VerifyOtpRequest request) {
        String redisKey = OTP_KEY+request.getEmail();
        Map<String,Object> registrationData = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
        if (registrationData == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã OTP đã hết hạn hoặc không tồn tại!");
        }

        String storedOtp = (String) registrationData.get("otp");
        if (!storedOtp.equals(request.getOtp())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã OTP không chính xác!");
        }
        return register(request);
    }

    private String generateOTP(){
        return String.format("%06d", new Random().nextInt(999999));
    }


    private UserResponse register(VerifyOtpRequest request){
       if(userRepository.existsByEmail(request.getEmail())){
           throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản đã tồn tại !");
       }
        MemberShipTier defaultTier = memberShipTierRepository.findById("MEMBER")
                .orElseGet(() -> memberShipTierRepository.save(MemberShipTier.builder()
                        .code("MEMBER")
                        .name("Member")
                        .minSpend(BigDecimal.ZERO)
                        .description("Hạng thành viên tiêu chuẩn")
                        .build()));

        Map<String, String> tokenBody = new HashMap<>();
        tokenBody.put("grant_type", "client_credentials");
        tokenBody.put("client_id", clientId);
        tokenBody.put("client_secret", clientSecret);
        tokenBody.put("scope" , "openid");

        JsonNode tokenResponse = keycloakClient.exchangeToken(realm, tokenBody);
        String adminToken = "Bearer " + tokenResponse.get("access_token").asText();

        String fullName = request.getFullName().trim();
        String firstName = fullName;
        String lastName = fullName;

        int lastSpaceIndex = fullName.lastIndexOf(" ");
        if (lastSpaceIndex > 0) {
            firstName = fullName.substring(lastSpaceIndex + 1);
            lastName = fullName.substring(0, lastSpaceIndex);
        }

        UserCreationParam creationParam = UserCreationParam.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .emailVerified(true)
                .credentials(List.of(UserCreationParam.Credential.builder()
                        .type("password")
                        .value(request.getPassword())
                        .temporary(false)
                        .build()))
                .build();

        try {
            var response = keycloakClient.createUser(adminToken,realm, creationParam);

            String userId = extractUserIdKeyCloak(response);
            User newUser = User.builder()
                    .id(userId)
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .membershipTier(defaultTier)
                    .build();
            userRepository.save(newUser);
            redisTemplate.delete(OTP_KEY+request.getEmail());
            return userMapper.toUserResponse(newUser);

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new RuntimeException("Tạo tài khoản thất bại!", e);
        }
    }

    private String extractUserIdKeyCloak(ResponseEntity<?> response) {
        String location = response.getHeaders().getFirst("Location");
        String[] sliptedStr =  location.split("/");
        return sliptedStr[sliptedStr.length - 1];
    }

    private void syncUserToLocalDatabase(String accessToken) {
        Jwt jwt = jwtDecoder.decode(accessToken);
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String sub = jwt.getSubject();

        if (email != null && !userRepository.existsById(sub)) {
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
                    .id(sub)
                    .membershipTier(defaultTier)
                    .total_spend_ytd(BigDecimal.ZERO)
                    .build();
            userRepository.save(newUser);
        }
    }
}