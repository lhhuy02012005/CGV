package com.cgv.bookingservice;

import com.cgv.bookingservice.config.SecurityConfig;
import com.cgv.bookingservice.controller.BookingController;
import com.cgv.bookingservice.dto.request.BookingFilterRequest;
import com.cgv.bookingservice.enums.BookingStatus;
import com.cgv.bookingservice.exception.GlobalExceptionHandler;
import com.cgv.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminBookingFilterWebMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean BookingService service;
    @MockitoBean JwtDecoder decoder;

    void staffToken() {
        when(decoder.decode("staff-token")).thenReturn(Jwt.withTokenValue("staff-token").header("alg", "RS256")
                .subject("staff").claim("realm_access", Map.of("roles", List.of("TICKET_STAFF"))).build());
    }

    @Test void statusAndVietnamDateRangeArePassedToService() throws Exception {
        staffToken();
        mvc.perform(get("/bookings/admin/search").header("Authorization", "Bearer staff-token")
                        .param("status", "CONFIRMED").param("fromDate", "2026-11-01").param("toDate", "2026-11-02"))
                .andExpect(status().isOk());

        verify(service).adminSearchBookings(isNull(), argThat((BookingFilterRequest f) ->
                f.getStatus() == BookingStatus.CONFIRMED
                        // 00:00 ngày 01/11 giờ VN = 17:00 ngày 31/10 UTC; toDate tính hết ngày 02/11
                        && Instant.parse("2026-10-31T17:00:00Z").equals(f.getFromDate())
                        && Instant.parse("2026-11-02T17:00:00Z").equals(f.getToDate())), any());
    }

    @Test void invertedDateRangeIsRejected() throws Exception {
        staffToken();
        mvc.perform(get("/bookings/admin/search").header("Authorization", "Bearer staff-token")
                        .param("fromDate", "2026-11-05").param("toDate", "2026-11-01"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
