package com.cgv.paymentservice;
import com.cgv.paymentservice.entity.*;
import com.cgv.paymentservice.enums.PaymentStatus;
import com.cgv.paymentservice.repository.*;
import com.cgv.paymentservice.service.impl.VnpayPayment;
import com.cgv.paymentservice.config.VnpayConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties={"spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect","spring.jpa.show-sql=false"})
@Import({VnpayPayment.class,PaymentCallbackIntegrationTest.Config.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class PaymentCallbackIntegrationTest {
    @TestConfiguration static class Config { @Bean ObjectMapper objectMapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); } }
    @Autowired PaymentRepository payments;
    @Autowired OutboxEventRepository outbox;
    @Autowired VnpayPayment service;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean VnpayConfig config;
    @MockitoBean com.cgv.paymentservice.service.BookingPaymentAccess access;
    @MockitoBean RedisTemplate<String,Object> redis;
    @MockitoSpyBean ObjectMapper mapper;
    @BeforeEach void clean() { new TransactionTemplate(manager).executeWithoutResult(s -> {outbox.deleteAll();payments.deleteAll();}); }
    Payment payment() { return new TransactionTemplate(manager).execute(s -> payments.saveAndFlush(Payment.builder().provider("VNPAY").bookingId(UUID.randomUUID()).userId("test-user")
            .transactionId(UUID.randomUUID().toString()).amount(BigDecimal.valueOf(100)).status(PaymentStatus.PENDING).build())); }
    HttpServletRequest request(Payment p) {
        HttpServletRequest req=mock(HttpServletRequest.class);
        Map<String,String[]> fields=new HashMap<>(); fields.put("vnp_TxnRef",new String[]{p.getTransactionId()}); fields.put("vnp_ResponseCode",new String[]{"00"}); fields.put("vnp_TransactionStatus",new String[]{"00"}); fields.put("vnp_TmnCode",new String[]{"merchant"}); fields.put("vnp_CurrCode",new String[]{"VND"}); fields.put("vnp_Amount",new String[]{"10000"}); fields.put("vnp_SecureHash",new String[]{"test-signature"});
        when(req.getParameterMap()).thenReturn(fields);
        when(req.getParameterNames()).thenAnswer(i -> java.util.Collections.enumeration(fields.keySet()));
        when(req.getParameter(anyString())).thenAnswer(i -> {var value=fields.get(i.getArgument(0)); return value==null?null:value[0];});
        when(config.hashAllFields(anyMap())).thenReturn("test-signature"); when(config.getVnpTmnCode()).thenReturn("merchant"); when(access.lockAndCheckBooking(eq(p.getBookingId()), any())).thenReturn(true); return req;
    }
    @Test void concurrentDuplicateCallbackPersistsOneOutbox() throws Exception {
        Payment p=payment(); HttpServletRequest req=request(p); ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch go=new CountDownLatch(1);
        try { Callable<Boolean> call=() -> {go.await(); return service.callback(req);}; var a=pool.submit(call);var b=pool.submit(call);go.countDown();
            assertTrue(a.get(10,TimeUnit.SECONDS));assertTrue(b.get(10,TimeUnit.SECONDS)); assertEquals(1,outbox.count()); assertEquals(PaymentStatus.SUCCESS,payments.findById(p.getId()).orElseThrow().getStatus());
        } finally {go.countDown();pool.shutdownNow();}
    }
    @Test void outboxSerializationFailureRollsBackPayment() throws Exception {
        Payment p=payment();HttpServletRequest req=request(p);
        doThrow(new com.fasterxml.jackson.core.JsonProcessingException("forced failure"){}).when(mapper).writeValueAsString(any(com.cgv.commondto.event.PaymentCompletedEvent.class));
        assertThrows(IllegalStateException.class,() -> service.callback(req));
        assertEquals(PaymentStatus.PENDING,payments.findById(p.getId()).orElseThrow().getStatus()); assertEquals(0,outbox.count());
    }
    @Test void paidAfterBookingWasCancelledRequiresRefundInsteadOfIssuingTicket() {
        Payment payment = payment(); HttpServletRequest req = request(payment);
        when(access.lockAndCheckBooking(eq(payment.getBookingId()), any())).thenReturn(false);
        assertFalse(service.callback(req));
        assertEquals(PaymentStatus.REFUND_PENDING, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(0, outbox.count());
    }
    @Test void mismatchedAmountNeverMarksPaymentSuccessful() {
        Payment payment = payment(); HttpServletRequest req = request(payment);
        when(req.getParameter("vnp_Amount")).thenReturn("1");
        assertThrows(com.cgv.commondto.exception.BusinessException.class, () -> service.callback(req));
        assertEquals(PaymentStatus.PENDING, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(0, outbox.count());
    }
    @Test void invalidSignatureCannotChangePayment() {
        Payment payment = payment(); HttpServletRequest req = request(payment);
        when(config.hashAllFields(anyMap())).thenReturn("wrong-signature");
        assertThrows(com.cgv.commondto.exception.BusinessException.class, () -> service.callback(req));
        assertEquals(PaymentStatus.PENDING, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(0, outbox.count());
    }
    @Test void wrongMerchantCurrencyAndSuccessFlagCannotChangePayment() {
        Payment payment = payment();
        for (String field : List.of("vnp_TmnCode", "vnp_CurrCode", "vnp_TransactionStatus")) {
            HttpServletRequest req = request(payment);
            when(req.getParameter(field)).thenReturn("wrong");
            assertThrows(com.cgv.commondto.exception.BusinessException.class, () -> service.callback(req), field);
            assertEquals(PaymentStatus.PENDING, payments.findById(payment.getId()).orElseThrow().getStatus());
            assertEquals(0, outbox.count());
        }
    }
    @Test void failedCallbackCreatesOneFailureEventAndCannotBecomeSuccessLater() {
        Payment payment = payment(); HttpServletRequest req = request(payment);
        when(req.getParameter("vnp_ResponseCode")).thenReturn("24");
        when(req.getParameter("vnp_TransactionStatus")).thenReturn("02");
        assertFalse(service.callback(req));
        assertEquals(PaymentStatus.FAILED, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals("PaymentFailedEvent", outbox.findAll().get(0).getEventType());
        when(req.getParameter("vnp_ResponseCode")).thenReturn("00");
        when(req.getParameter("vnp_TransactionStatus")).thenReturn("00");
        assertFalse(service.callback(req));
        assertEquals(1, outbox.count());
        assertEquals(PaymentStatus.FAILED, payments.findById(payment.getId()).orElseThrow().getStatus());
    }
    @Test void duplicateSuccessCallbackDoesNotPublishSecondEvent() {
        Payment payment = payment(); HttpServletRequest req = request(payment);
        assertTrue(service.callback(req));
        assertTrue(service.callback(req));
        assertEquals(PaymentStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(1, outbox.count());
    }
    @Test void missingOrExpiredDeadlineCannotCreatePayment() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        for (java.time.Instant deadline : new java.time.Instant[]{null, java.time.Instant.now().minusSeconds(1)}) {
            assertThrows(com.cgv.commondto.exception.BusinessException.class,
                    () -> service.add(req, UUID.randomUUID(), BigDecimal.TEN, "test-user", deadline));
        }
        assertEquals(0, payments.count());
    }
    @Test void repeatedCreateUrlReusesPendingPaymentInsteadOfCreatingChargeableSecondUrl() {
        Payment payment = payment();
        payment.setRaw("https://sandbox.vnpayment.vn/existing-payment");
        payments.saveAndFlush(payment);
        assertEquals(payment.getRaw(), service.add(request(payment), payment.getBookingId(), payment.getAmount(),
                payment.getUserId(), java.time.Instant.now().plusSeconds(300)));
        assertEquals(1, payments.count());
    }
}
