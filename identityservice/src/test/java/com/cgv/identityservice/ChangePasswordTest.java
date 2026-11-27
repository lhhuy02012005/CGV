package com.cgv.identityservice;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.identityservice.dto.request.ChangePasswordRequest;
import com.cgv.identityservice.entity.User;
import com.cgv.identityservice.mapper.UserMapper;
import com.cgv.identityservice.repository.MemberShipTierRepository;
import com.cgv.identityservice.repository.UserRepository;
import com.cgv.identityservice.repository.httpclient.KeycloakClient;
import com.cgv.identityservice.service.impl.UserServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChangePasswordTest {
    UserRepository userRepository;
    KeycloakClient keycloak;
    UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        keycloak = mock(KeycloakClient.class);
        service = new UserServiceImpl(userRepository, mock(UserMapper.class), keycloak, mock(MemberShipTierRepository.class));
        ReflectionTestUtils.setField(service, "realm", "cgv-realm");
        ReflectionTestUtils.setField(service, "clientId", "CGV_App");
        ReflectionTestUtils.setField(service, "clientSecret", "secret");
        when(userRepository.findById("u1")).thenReturn(Optional.of(User.builder().id("u1").email("member@example.com").build()));
    }

    @Test void verifiesCurrentPasswordThenResetsInKeycloak() {
        var token = JsonMapper.builder().build().readTree("{\"access_token\":\"admin-token\"}");
        when(keycloak.exchangeToken(eq("cgv-realm"), anyMap())).thenReturn(token);

        service.changePassword("u1", new ChangePasswordRequest("old-pass", "new-pass-123"));

        verify(keycloak).exchangeToken(eq("cgv-realm"), argThat((Map<String, ?> m) ->
                "password".equals(m.get("grant_type")) && "member@example.com".equals(m.get("username"))
                        && "old-pass".equals(m.get("password"))));
        verify(keycloak).resetPassword(eq("Bearer admin-token"), eq("cgv-realm"), eq("u1"),
                argThat((Map<String, ?> m) -> "new-pass-123".equals(m.get("value")) && Boolean.FALSE.equals(m.get("temporary"))));
    }

    @Test void wrongCurrentPasswordDoesNotReset() {
        when(keycloak.exchangeToken(eq("cgv-realm"), anyMap())).thenThrow(mock(FeignException.class));

        assertThrows(BusinessException.class,
                () -> service.changePassword("u1", new ChangePasswordRequest("wrong", "new-pass-123")));
        verify(keycloak, never()).resetPassword(any(), any(), any(), any());
    }

    @Test void newPasswordMustDifferFromCurrent() {
        assertThrows(BusinessException.class,
                () -> service.changePassword("u1", new ChangePasswordRequest("same-pass", "same-pass")));
        verifyNoInteractions(keycloak);
    }
}
