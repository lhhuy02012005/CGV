package com.cgv.bookingservice;

import com.cgv.bookingservice.entity.*;
import com.cgv.bookingservice.dto.request.BookingCreateRequest;
import com.cgv.bookingservice.enums.BookingStatus;
import com.cgv.bookingservice.repository.*;
import com.cgv.bookingservice.service.impl.BookingServiceImpl;
import com.cgv.bookingservice.service.*;
import com.cgv.bookingservice.grpc.*;
import com.cgv.commondto.event.PaymentCompletedEvent;
import com.cgv.commondto.grpc.ShowtimePricingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties={"spring.flyway.enabled=false","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false"})
@Import({BookingServiceImpl.class,AfterCommit.class,BookingIntegrityIntegrationTest.Config.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class BookingIntegrityIntegrationTest {
    @TestConfiguration static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }
    }
    @Autowired BookingRepository bookings;
    @Autowired SeatClaimRepository claims;
    @Autowired OutboxEventRepository outbox;
    @Autowired BookingServiceImpl service;
    @Autowired PlatformTransactionManager manager;
    @MockitoSpyBean ObjectMapper mapper;
    @MockitoBean CatalogGrpcClient catalog;
    @MockitoBean MarketingGrpcClient marketing;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean SeatLockOwnership ownership;
    @MockitoBean SimpMessagingTemplate messaging;
    @MockitoBean BookingRealtimeService realtime;
    @MockitoBean org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    TransactionTemplate tx() { return new TransactionTemplate(manager); }
    Booking booking() { return Booking.builder().userId("test-owner").showtimeId(UUID.randomUUID())
            .status(BookingStatus.PAYMENT_PENDING).paymentDeadline(Instant.now().plusSeconds(600))
            .totalBaseAmount(BigDecimal.valueOf(100)).finalAmount(BigDecimal.valueOf(100)).build(); }
    @BeforeEach void clean() {
        tx().executeWithoutResult(status -> { claims.deleteAll(); outbox.deleteAll(); bookings.deleteAll(); });
        when(catalog.getShowtimePricing(any())).thenReturn(ShowtimePricingResponse.newBuilder()
                .setMovieTitle("Test film").setStartTime(Instant.now().toString()).build());
    }
    PaymentCompletedEvent event(Booking b) { return PaymentCompletedEvent.builder().bookingId(b.getId()).paymentId(UUID.randomUUID()).amount(b.getFinalAmount()).build(); }
    @Test void duplicateConcurrentPaymentCreatesOneConfirmationOutbox() throws Exception {
        Booking b=tx().execute(s -> bookings.saveAndFlush(booking())); PaymentCompletedEvent e=event(b);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch go=new CountDownLatch(1);
        long started=System.nanoTime();
        try {
            Future<?> a=pool.submit(() -> { await(go); service.confirmBooking(e); });
            Future<?> c=pool.submit(() -> { await(go); service.confirmBooking(e); }); go.countDown();
            a.get(15,TimeUnit.SECONDS); c.get(15,TimeUnit.SECONDS);
            assertEquals(BookingStatus.CONFIRMED,bookings.findById(b.getId()).orElseThrow().getStatus());
            assertEquals(1,outbox.count());
            verify(realtime,times(1)).broadcastBooking(eq(b.getId()),eq("PAYMENT_CONFIRMED"),anyMap());
            System.out.printf("PAYMENT_DUPLICATE threads=2 confirmations=1 outbox=1 elapsed_ms=%.3f%n",(System.nanoTime()-started)/1_000_000.0);
        } finally { pool.shutdownNow(); }
    }
    @Test void serializationFailureRollsBackStatusAndSuppressesRealtime() throws Exception {
        Booking b=tx().execute(s -> bookings.saveAndFlush(booking()));
        doThrow(new com.fasterxml.jackson.core.JsonProcessingException("forced serialization failure"){}).when(mapper).writeValueAsString(any(com.cgv.commondto.event.BookingConfirmedEvent.class));
        assertThrows(IllegalStateException.class,() -> service.confirmBooking(event(b)));
        assertEquals(BookingStatus.PAYMENT_PENDING,bookings.findById(b.getId()).orElseThrow().getStatus());
        assertEquals(0,outbox.count()); verifyNoInteractions(realtime);
    }
    @Test void uniqueActiveClaimArbitratesTwoConcurrentBookings() throws Exception {
        UUID showtime=UUID.randomUUID(),seat=UUID.randomUUID(); ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        Callable<Boolean> attempt=() -> { ready.countDown(); await(go); try {
            tx().executeWithoutResult(status -> { Booking b=booking(); b.setShowtimeId(showtime); bookings.saveAndFlush(b);
                claims.saveAndFlush(SeatClaim.builder().bookingId(b.getId()).showtimeId(showtime).seatId(seat).build()); }); return true;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) { return false; } };
        try { Future<Boolean> a=pool.submit(attempt),c=pool.submit(attempt); assertTrue(ready.await(5,TimeUnit.SECONDS)); go.countDown();
            int successes=(a.get(10,TimeUnit.SECONDS)?1:0)+(c.get(10,TimeUnit.SECONDS)?1:0);
            assertEquals(1,successes); assertEquals(1,claims.count()); assertEquals(1,bookings.count());
        } finally { go.countDown(); pool.shutdownNow(); }
    }
    @Test void cancellingPendingBookingReleasesActiveClaimAndKeepsHistory() {
        Booking b=tx().execute(s -> bookings.saveAndFlush(booking()));
        tx().executeWithoutResult(s -> claims.saveAndFlush(SeatClaim.builder().bookingId(b.getId()).showtimeId(b.getShowtimeId()).seatId(UUID.randomUUID()).build()));
        service.cancelBooking(b.getId(),"test-owner");
        assertEquals(0,claims.count()); assertEquals(BookingStatus.CANCELLED,bookings.findById(b.getId()).orElseThrow().getStatus());
    }
    @Test void cancellationLetsAnotherAccountClaimSameSeatButHistoryRemains() {
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID();
        Booking first = booking(); first.setShowtimeId(showtime);
        Booking savedFirst = tx().execute(s -> bookings.saveAndFlush(first));
        UUID firstId = savedFirst.getId();
        tx().executeWithoutResult(s -> claims.saveAndFlush(SeatClaim.builder()
                .bookingId(firstId).showtimeId(showtime).seatId(seat).build()));
        service.cancelBooking(firstId, "test-owner");
        Booking second = booking(); second.setUserId("second-owner"); second.setShowtimeId(showtime);
        Booking savedSecond = tx().execute(s -> bookings.saveAndFlush(second));
        UUID secondId = savedSecond.getId();
        tx().executeWithoutResult(s -> claims.saveAndFlush(SeatClaim.builder()
                .bookingId(secondId).showtimeId(showtime).seatId(seat).build()));
        assertEquals(BookingStatus.CANCELLED, bookings.findById(firstId).orElseThrow().getStatus());
        assertEquals(2, bookings.count());
        assertEquals(1, claims.count());
        assertEquals(secondId, claims.findAll().get(0).getBookingId());
    }
    @Test void anotherAccountCannotCancelOrConfirmAlreadyCancelledBooking() {
        Booking booking = tx().execute(s -> bookings.saveAndFlush(booking()));
        assertThrows(com.cgv.commondto.exception.BusinessException.class,
                () -> service.cancelBooking(booking.getId(), "other-owner"));
        assertEquals(BookingStatus.PAYMENT_PENDING, bookings.findById(booking.getId()).orElseThrow().getStatus());
        service.cancelBooking(booking.getId(), "test-owner");
        assertThrows(com.cgv.commondto.exception.BusinessException.class,
                () -> service.confirmBooking(event(booking)));
        assertEquals(BookingStatus.CANCELLED, bookings.findById(booking.getId()).orElseThrow().getStatus());
        assertEquals(0, outbox.count());
    }
    @Test void rollbackWithoutUnsettledPaymentReleasesClaim() {
        Booking booking = tx().execute(s -> bookings.saveAndFlush(booking()));
        tx().executeWithoutResult(s -> claims.saveAndFlush(SeatClaim.builder()
                .bookingId(booking.getId()).showtimeId(booking.getShowtimeId()).seatId(UUID.randomUUID()).build()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(booking.getId()))).thenReturn(false);
        service.rollbackBooking(booking.getId(), "HET_HAN_THANH_TOAN_10_PHUT");
        assertEquals(BookingStatus.CANCELLED, bookings.findById(booking.getId()).orElseThrow().getStatus());
        assertEquals(0, claims.count());
    }
    @Test void pendingLookupUsesOwnerAndShowtimeAndIgnoresCancelledBookings() {
        Booking pending = tx().execute(status -> bookings.saveAndFlush(booking()));
        Booking cancelled = booking();
        cancelled.setStatus(BookingStatus.CANCELLED);
        tx().executeWithoutResult(status -> bookings.saveAndFlush(cancelled));
        assertEquals(pending.getId(), service.getPendingBooking("test-owner", pending.getShowtimeId()).getBookingId());
        assertNull(service.getPendingBooking("other-owner", pending.getShowtimeId()));
        assertNull(service.getPendingBooking("test-owner", cancelled.getShowtimeId()));
    }
    @Test void expiredBookingWithUnsettledVnpayPaymentKeepsSeatClaim() {
        Booking booking = tx().execute(status -> bookings.saveAndFlush(booking()));
        tx().executeWithoutResult(status -> claims.saveAndFlush(SeatClaim.builder().bookingId(booking.getId())
                .showtimeId(booking.getShowtimeId()).seatId(UUID.randomUUID()).build()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(booking.getId()))).thenReturn(true);
        service.rollbackBooking(booking.getId(), "HET_HAN_THANH_TOAN_10_PHUT");
        assertEquals(BookingStatus.PAYMENT_PENDING, bookings.findById(booking.getId()).orElseThrow().getStatus());
        assertEquals(1, claims.count());
    }
    @Test void cancelledShowtimeCannotClaimSeat() {
        BookingCreateRequest request=new BookingCreateRequest();
        request.setShowtimeId(UUID.randomUUID()); request.setSeatIds(List.of(UUID.randomUUID()));
        when(catalog.getShowtimePricing(request.getShowtimeId())).thenReturn(ShowtimePricingResponse.newBuilder()
                .setShowtimeStatus("CANCELLED").build());
        assertThrows(com.cgv.commondto.exception.BusinessException.class,()->service.createBooking("test-owner",null,request));
        verifyNoInteractions(ownership); assertEquals(0,bookings.count());
    }
    @Test void invalidSeatCannotRenewRedisHoldDuringCheckout() {
        BookingCreateRequest request=new BookingCreateRequest();
        request.setShowtimeId(UUID.randomUUID()); request.setSeatIds(List.of(UUID.randomUUID()));
        when(catalog.getShowtimePricing(request.getShowtimeId())).thenReturn(ShowtimePricingResponse.newBuilder()
                .setShowtimeStatus("SCHEDULED").build());
        assertThrows(com.cgv.commondto.exception.BusinessException.class,()->service.createBooking("test-owner",null,request));
        verifyNoInteractions(ownership); assertEquals(0,bookings.count());
    }
    static void await(CountDownLatch latch) { try { if (!latch.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("barrier timed out"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); } }
}
