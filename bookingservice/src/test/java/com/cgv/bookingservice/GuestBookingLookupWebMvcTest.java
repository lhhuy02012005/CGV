package com.cgv.bookingservice;

import com.cgv.bookingservice.config.SecurityConfig;
import com.cgv.bookingservice.controller.BookingController;
import com.cgv.bookingservice.dto.response.BookingResponse;
import com.cgv.bookingservice.exception.GlobalExceptionHandler;
import com.cgv.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GuestBookingLookupWebMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean BookingService service;
    @MockitoBean JwtDecoder decoder;

    @Test void guestLookupIsPublicAndAcceptsPrefixedBookingCode() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(service.lookupGuestBooking(eq(bookingId), eq("guest@example.com"), isNull()))
                .thenReturn(List.of(BookingResponse.builder().bookingId(bookingId).build()));

        mvc.perform(post("/bookings/guest-lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"BK-" + bookingId + "\",\"email\":\"guest@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bookingId").value(bookingId.toString()));
    }

    @Test void guestLookupRequiresEmailOrPhone() throws Exception {
        mvc.perform(post("/bookings/guest-lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void guestLookupRequiresBookingId() throws Exception {
        mvc.perform(post("/bookings/guest-lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"guest@example.com\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void guestLookupRejectsMalformedBookingId() throws Exception {
        mvc.perform(post("/bookings/guest-lookup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"not-a-uuid\",\"phone\":\"0912345678\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
