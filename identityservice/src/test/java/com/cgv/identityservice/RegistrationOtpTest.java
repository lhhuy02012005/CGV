package com.cgv.identityservice;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.identityservice.dto.request.VerifyOtpRequest;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.entity.MemberShipTier;
import com.cgv.identityservice.mapper.UserMapper;
import com.cgv.identityservice.repository.MemberShipTierRepository;
import com.cgv.identityservice.repository.UserRepository;
import com.cgv.identityservice.repository.httpclient.KeycloakClient;
import com.cgv.identityservice.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationOtpTest {
    @Mock UserRepository users;
    @Mock MemberShipTierRepository tiers;
    @Mock JwtDecoder jwtDecoder;
    @Mock KeycloakClient keycloak;
    @Mock UserMapper mapper;
    @Mock RedisTemplate<String, Object> redis;
    @Mock ValueOperations<String, Object> values;
    @Mock KafkaTemplate<String, Object> kafka;
    @InjectMocks AuthenticationServiceImpl service;
    private static final String EMAIL = "otp-test@example.invalid";
    private static final String KEY = "OTP:" + EMAIL;

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(service, "realm", "test-realm");
        ReflectionTestUtils.setField(service, "clientId", "test-client");
        ReflectionTestUtils.setField(service, "clientSecret", "test-secret");
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test void correctOtpCreatesAccountAndIsConsumedAfterSuccess() {
        when(values.get(KEY)).thenReturn("654321");
        when(keycloak.exchangeToken(eq("test-realm"), anyMap())).thenReturn(JsonMapper.builder().build().readTree("{\"access_token\":\"test-admin\"}"));
        when(keycloak.createUser(eq("Bearer test-admin"), eq("test-realm"), any())).thenReturn(
                ResponseEntity.created(URI.create("http://localhost/users/test-user")).build());
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(tiers.findById("MEMBER")).thenReturn(Optional.of(MemberShipTier.builder().code("MEMBER").build()));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toUserResponse(any())).thenReturn(UserResponse.builder().id("test-user").email(EMAIL).build());
        var result = service.verifyAndRegister(request("654321"));
        assertEquals(EMAIL, result.getEmail());
        verify(users).save(argThat(user -> user.getId().equals("test-user") && user.getEmail().equals(EMAIL)));
        verify(redis).delete(KEY);
    }

    @Test void incorrectOtpDoesNotCallKeycloakOrConsumeTheStoredCode() {
        when(values.get(KEY)).thenReturn("654321");
        assertThrows(BusinessException.class, () -> service.verifyAndRegister(request("000000")));
        verifyNoInteractions(keycloak, users);
        verify(redis, never()).delete(anyString());
    }

    @Test void keycloakFailureKeepsOtpAvailableForRetry() {
        when(values.get(KEY)).thenReturn("654321");
        when(keycloak.exchangeToken(anyString(), anyMap())).thenThrow(new IllegalStateException("test upstream failure"));
        assertThrows(IllegalStateException.class, () -> service.verifyAndRegister(request("654321")));
        verify(users, never()).save(any());
        verify(redis, never()).delete(anyString());
    }

    private VerifyOtpRequest request(String otp) {
        return VerifyOtpRequest.builder().email(EMAIL).password("test-password").fullName("Test User").otp(otp).build();
    }
}
