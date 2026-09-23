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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthenticationServiceImpl implements AuthenticationService {
    UserRepository userRepository;
    MemberShipTierRepository memberShipTierRepository;
    JwtDecoder jwtDecoder;
    KeycloakClient keycloakClient;
    UserMapper userMapper;
    RedisTemplate<String, Object> redisTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;

    // Constants
    static String OTP_KEY_PREFIX = "OTP:";
    static String USER_TIER_KEY_PREFIX = "USER_TIER:";
    static int USER_TIER_KEY_DURATION = 24;
    static String NOTIFICATION_SEND_TOPIC = "notification.send";

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
        Map<String, String> body = createTokenRequestBody("password", Map.of(
                "username", request.getUsername(),
                "password", request.getPassword()
        ));

        try {
            JsonNode responseNode = keycloakClient.exchangeToken(realm, body);
            assert responseNode != null;

            String accessToken = responseNode.get("access_token").asText();
            String refreshToken = responseNode.get("refresh_token").asText();
            long expiresIn = responseNode.get("expires_in").asLong();

            syncUserToLocalDatabase(accessToken, null);

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .authenticated(true)
                    .build();

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Đăng nhập thất bại: Sai tài khoản hoặc mật khẩu!");
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        Map<String, String> body = createTokenRequestBody("refresh_token", Map.of(
                "refresh_token", request.getRefreshToken()
        ));

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
    public AuthenticationResponse exchangeCode(ExchangeCodeRequest request) {
        Map<String, String> body = createTokenRequestBody("authorization_code", Map.of(
                "code", request.getCode(),
                "redirect_uri", request.getRedirectUri()
        ));

        try {
            JsonNode responseNode = keycloakClient.exchangeToken(realm, body);
            if (responseNode == null || !responseNode.has("access_token")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể đổi mã xác thực lấy token từ Keycloak!");
            }

            String accessToken = responseNode.get("access_token").asText();
            String refreshToken = responseNode.has("refresh_token") ? responseNode.get("refresh_token").asText() : null;
            long expiresIn = responseNode.has("expires_in") ? responseNode.get("expires_in").asLong() : 300;

            syncUserToLocalDatabase(accessToken, null);

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .authenticated(true)
                    .build();

        } catch (Exception e) {
            log.error("Exchange code failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Xác thực mã đăng nhập thất bại: " + e.getMessage());
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
    public void initiateRegistration(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email đã tồn tại trong hệ thống!");
        }

        String otp = generateOTP();
        redisTemplate.opsForValue().set(OTP_KEY_PREFIX + request.getEmail(), otp, Duration.ofMinutes(5));

        NotificationEvent event = NotificationEvent.builder()
                .email(request.getEmail())
                .otp(otp)
                .build();

        kafkaTemplate.send(NOTIFICATION_SEND_TOPIC, event);
        log.info("Đã khởi tạo đăng ký và gửi OTP tới Kafka cho email: {}", request.getEmail());
    }

    @Override
    public UserResponse verifyAndRegister(VerifyOtpRequest request) {
        String redisKey = OTP_KEY_PREFIX + request.getEmail();
        String storedOtp = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã OTP đã hết hạn hoặc không tồn tại!");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mã OTP không chính xác!");
        }

        UserResponse response = registerOnKeycloakAndLocal(request.getEmail(), request.getPassword(), request.getFullName());
        redisTemplate.delete(redisKey);
        return response;
    }

    @Override
    public UserResponse syncUserFromAccessToken(String accessToken, String customFullName) {
        return syncUserToLocalDatabase(accessToken, customFullName);
    }

    private UserResponse registerOnKeycloakAndLocal(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản đã tồn tại !");
        }

        Map<String, String> tokenBody = createTokenRequestBody("client_credentials", Collections.emptyMap());
        JsonNode tokenResponse = keycloakClient.exchangeToken(realm, tokenBody);
        String adminToken = "Bearer " + tokenResponse.get("access_token").asText();

        String trimmedName = fullName != null ? removeVietnameseDiacritics(fullName.trim()) : email;
        String firstName = trimmedName;
        String lastName = trimmedName;

        int lastSpaceIndex = trimmedName.lastIndexOf(" ");
        if (lastSpaceIndex > 0) {
            firstName = trimmedName.substring(lastSpaceIndex + 1);
            lastName = trimmedName.substring(0, lastSpaceIndex);
        }

        UserCreationParam creationParam = UserCreationParam.builder()
                .username(email)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .emailVerified(true)
                .credentials(List.of(UserCreationParam.Credential.builder()
                        .type("password")
                        .value(password)
                        .temporary(false)
                        .build()))
                .build();

        try {
            var response = keycloakClient.createUser(adminToken, realm, creationParam);
            String userId = extractUserIdKeyCloak(response);

            User newUser = saveOrGetUser(userId, email, trimmedName);
            return userMapper.toUserResponse(newUser);

        } catch (Exception e) {
            log.error("Tạo tài khoản thất bại: {}", e.getMessage());
            throw new RuntimeException("Tạo tài khoản thất bại!", e);
        }
    }

    private UserResponse syncUserToLocalDatabase(String accessToken, String customFullName) {
        Jwt jwt = jwtDecoder.decode(accessToken);
        String email = jwt.getClaimAsString("email");
        log.info("email: {}", email);
        String sub = jwt.getSubject();

        if (email == null || sub == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Token không hợp lệ!");
        }

        User user = saveOrGetUser(sub, email, customFullName);
        String tierCode = (user.getMembershipTier() != null) ? user.getMembershipTier().getCode() : "MEMBER";
        redisTemplate.opsForValue().set(USER_TIER_KEY_PREFIX + user.getId() , tierCode , Duration.ofHours(USER_TIER_KEY_DURATION));
        return userMapper.toUserResponse(user);
    }

    private User saveOrGetUser(String id, String email, String fullName) {
        String normalizedName = (fullName != null && !fullName.trim().isEmpty())
                ? removeVietnameseDiacritics(fullName.trim())
                : null;

        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (normalizedName != null && (existingUser.getFullName() == null || existingUser.getFullName().isEmpty())) {
                        existingUser.setFullName(normalizedName);
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    log.info("not exist email: {}", email);
                    MemberShipTier defaultTier = memberShipTierRepository.findById("MEMBER")
                            .orElseGet(() -> memberShipTierRepository.save(MemberShipTier.builder()
                                    .code("MEMBER")
                                    .name("Member")
                                    .minSpend(BigDecimal.ZERO)
                                    .description("Hạng thành viên tiêu chuẩn")
                                    .build()));

                    User newUser = User.builder()
                            .id(id)
                            .email(email)
                            .fullName(normalizedName)
                            .membershipTier(defaultTier)
                            .total_spend_ytd(BigDecimal.ZERO)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private String removeVietnameseDiacritics(String str) {
        if (str == null) return null;
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'D').trim();
    }

    private Map<String, String> createTokenRequestBody(String grantType, Map<String, String> extraParams) {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", grantType);
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("scope", "openid");
        body.putAll(extraParams);
        return body;
    }

    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private String extractUserIdKeyCloak(ResponseEntity<?> response) {
        String location = response.getHeaders().getFirst("Location");
        String[] splittedStr = location.split("/");
        return splittedStr[splittedStr.length - 1];
    }
}