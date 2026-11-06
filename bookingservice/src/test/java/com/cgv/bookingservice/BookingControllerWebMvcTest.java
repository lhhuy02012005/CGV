package com.cgv.bookingservice;
import com.cgv.bookingservice.controller.BookingController;
import com.cgv.bookingservice.exception.GlobalExceptionHandler;
import com.cgv.bookingservice.service.BookingService;
import com.cgv.commondto.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class,com.cgv.bookingservice.config.SecurityConfig.class})
class BookingControllerWebMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean BookingService service;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    void customerToken() {
        when(jwtDecoder.decode("customer-token")).thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("customer-token")
                .header("alg","RS256").subject("test-user").claim("email", "member@example.com").build());
    }
    String validJson() { return "{\"showtimeId\":\""+UUID.randomUUID()+"\",\"seatIds\":[\""+UUID.randomUUID()+"\"]}"; }
    @Test void invalidInputReturns400WithoutCallingService() throws Exception {
        customerToken();
        var result = mvc.perform(post("/bookings").header("Authorization","Bearer customer-token").contentType(MediaType.APPLICATION_JSON).content("{\"seatIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray()).andReturn();
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/http-error-400.json"), result.getResponse().getContentAsString());
        verifyNoInteractions(service);
    }
    @Test void successfulCreationUsesActualHttp201() throws Exception {
        customerToken();
        mvc.perform(post("/bookings").header("Authorization","Bearer customer-token").header("X-User-Id","victim")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201));
        verify(service).createBooking(eq("test-user"), eq("member@example.com"), any());
    }
    @Test void clientHeaderCannotCreateBookingWithoutJwt() throws Exception {
        mvc.perform(post("/bookings").header("X-User-Id","victim").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void pendingBookingLookupRequiresJwtAndUsesItsSubject() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/bookings/my-pending")
                .param("showtimeId", showtimeId.toString()).header("X-User-Id", "victim"))
                .andExpect(status().isUnauthorized());
        customerToken();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/bookings/my-pending")
                .param("showtimeId", showtimeId.toString()).header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk());
        verify(service).getPendingBooking("test-user", showtimeId);
    }
    @Test void guestTokenCannotCreateBooking() throws Exception {
        mvc.perform(post("/bookings/guest-session")).andExpect(status().isUnauthorized());
        mvc.perform(post("/bookings").header("X-Guest-Session-Id", "server-issued-token")
                .contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isUnauthorized());
        UUID bookingId = UUID.randomUUID();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/bookings/" + bookingId)
                .header("X-Guest-Session-Id", "server-issued-token")).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/bookings/" + bookingId + "/cancel")
                .header("X-Guest-Session-Id", "server-issued-token")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void seatConflictReturns409WithMatchingBodyStatus() throws Exception {
        customerToken();
        when(service.createBooking(any(),any(),any())).thenThrow(new BusinessException(ErrorCode.DUPLICATE,"Seat already claimed"));
        mvc.perform(post("/bookings").header("Authorization","Bearer customer-token")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
    @Test void protectedAdminEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/bookings/admin/search"))
                .andExpect(status().isUnauthorized()); verifyNoInteractions(service);
    }
    @Test void authenticatedCustomerCannotAccessAdminEndpoint() throws Exception {
        when(jwtDecoder.decode("customer-token")).thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("customer-token")
                .header("alg","RS256").subject("customer").claim("realm_access",java.util.Map.of("roles",java.util.List.of("CUSTOMER"))).build());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/bookings/admin/search").header("Authorization","Bearer customer-token"))
                .andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    @Test void ticketStaffCanAccessAdminEndpoint() throws Exception {
        when(jwtDecoder.decode("staff-token")).thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("staff-token")
                .header("alg","RS256").subject("staff").claim("realm_access",java.util.Map.of("roles",java.util.List.of("TICKET_STAFF"))).build());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/bookings/admin/search").header("Authorization","Bearer staff-token"))
                .andExpect(status().isOk()); verify(service).adminSearchBookings(any(),any());
    }

}
