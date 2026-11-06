package com.cgv.paymentservice;

import com.cgv.paymentservice.config.SecurityConfig;
import com.cgv.paymentservice.controller.PaymentController;
import com.cgv.paymentservice.service.BookingPaymentAccess;
import com.cgv.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, com.cgv.paymentservice.exception.GlobalExceptionHandler.class})
class PaymentControllerWebMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean PaymentService payment;
    @MockitoBean BookingPaymentAccess access;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void onlyAuthenticatedOwnerCanCreateVnpayUrl() throws Exception {
        UUID bookingId = UUID.randomUUID();
        String path = "/payments/vnpay/create-url?bookingId=" + bookingId + "&amount=110000";
        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(payment);

        when(jwtDecoder.decode("good-token")).thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("good-token")
                .header("alg", "RS256").subject("member-id").build());
        when(access.verify(eq(bookingId), eq(new BigDecimal("110000")), eq("member-id")))
                .thenReturn(new BookingPaymentAccess.Access("member-id", java.time.Instant.now().plusSeconds(300)));
        when(payment.add(any(), eq(bookingId), eq(new BigDecimal("110000")), eq("member-id"), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        mvc.perform(post(path).header("X-Guest-Session-Id", "issued-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
        verify(payment).add(any(), eq(bookingId), eq(new BigDecimal("110000")), eq("member-id"), any());
    }
}
