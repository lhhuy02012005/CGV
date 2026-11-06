package com.cgv.bookingservice;

import com.cgv.bookingservice.config.SecurityConfig;
import com.cgv.bookingservice.controller.SeatLockController;
import com.cgv.bookingservice.exception.GlobalExceptionHandler;
import com.cgv.bookingservice.service.SeatLockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatLockController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SeatLockIdentityWebMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean SeatLockService service;
    @MockitoBean JwtDecoder decoder;

    String body() {
        return "{\"showtimeId\":\"" + UUID.randomUUID() + "\",\"seatIds\":[\"" + UUID.randomUUID() + "\"]}";
    }

    @Test void clientHeaderCannotImpersonateOrTransferGuest() throws Exception {
        mvc.perform(post("/bookings/seat-locks").header("X-User-Id", "victim")
                .contentType("application/json").content(body())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/bookings/seat-locks").header("X-User-Id", "victim")
                .contentType("application/json").content(body())).andExpect(status().isUnauthorized());
        mvc.perform(post("/bookings/seat-locks/transfer").header("X-User-Id", "victim")
                .contentType("application/json").content(body())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void verifiedJwtSubjectWinsOverSpoofedHeaderAndGuestClaim() throws Exception {
        when(decoder.decode("good-token")).thenReturn(Jwt.withTokenValue("good-token")
                .header("alg", "RS256").subject("real-owner").build());
        mvc.perform(delete("/bookings/seat-locks").header("Authorization", "Bearer good-token")
                .header("X-User-Id", "victim").header("X-Guest-Session-Id", "victim")
                .contentType("application/json").content(body())).andExpect(status().isOk());
        verify(service).releaseSeats(eq("real-owner"), any());
        verifyNoMoreInteractions(service);
    }

}
