package com.cgv.paymentservice.service;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.paymentservice.dto.PaymentAdminResponse;
import com.cgv.paymentservice.dto.ReconcileResponse;
import com.cgv.paymentservice.entity.Payment;
import com.cgv.paymentservice.enums.PaymentStatus;
import com.cgv.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentAdminServiceTest {
    PaymentRepository repository;
    VnpayMerchantClient client;
    BookingPaymentAccess bookingAccess;
    PaymentAdminService service;
    UUID bookingId;
    Payment payment;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentRepository.class);
        client = mock(VnpayMerchantClient.class);
        bookingAccess = mock(BookingPaymentAccess.class);
        service = new PaymentAdminService(repository, client, bookingAccess);
        bookingId = UUID.randomUUID();
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .userId("member")
                .provider("VNPAY")
                .amount(new BigDecimal("110000"))
                .status(PaymentStatus.SUCCESS)
                .transactionId("CGV_" + bookingId + "_1795000000000")
                .raw("VNPAY_TRANS_NO:14123456")
                .build();
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)).thenReturn(Optional.of(payment));
        when(repository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(repository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingAccess.bookingStatus(bookingId)).thenReturn("CONFIRMED");
    }

    static VnpayMerchantClient.GatewayResult gateway(Map<String, String> fields, boolean signatureValid) {
        return new VnpayMerchantClient.GatewayResult(fields, "{}", "{}", signatureValid);
    }

    @Test void refundSuccessMarksPaymentRefunded() {
        when(client.refund(eq(payment), eq("admin1"), any()))
                .thenReturn(gateway(Map.of("vnp_ResponseCode", "00", "vnp_TransactionNo", "999"), true));

        PaymentAdminResponse result = service.refundByBooking(bookingId, "admin1", "127.0.0.1");

        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        assertEquals(0, new BigDecimal("110000").compareTo(payment.getRefundAmount()));
        assertNotNull(payment.getRefundedAt());
        assertEquals(1, payment.getPaymentLogs().size());
    }

    @Test void refundRejectedByVnpayKeepsSuccessAndLogsFailure() {
        when(client.refund(any(), any(), any()))
                .thenReturn(gateway(Map.of("vnp_ResponseCode", "94", "vnp_Message", "Duplicate"), true));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refundByBooking(bookingId, "admin1", "127.0.0.1"));

        assertTrue(ex.getMessage().contains("94"));
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(1, payment.getPaymentLogs().size());
    }

    @Test void unverifiableRefundResponseLocksPaymentAsRefundPending() {
        when(client.refund(any(), any(), any()))
                .thenReturn(gateway(Map.of("vnp_ResponseCode", "00"), false));

        assertThrows(BusinessException.class, () -> service.refundByBooking(bookingId, "admin1", "127.0.0.1"));
        assertEquals(PaymentStatus.REFUND_PENDING, payment.getStatus());
    }

    @Test void alreadyRefundedIsIdempotentAndDoesNotCallVnpay() {
        payment.setStatus(PaymentStatus.REFUNDED);
        assertEquals(PaymentStatus.REFUNDED, service.refundByBooking(bookingId, "admin1", null).getStatus());
        verifyNoInteractions(client);
    }

    @Test void checkedInTicketCannotBeRefunded() {
        when(bookingAccess.bookingStatus(bookingId)).thenReturn("USED");
        assertThrows(BusinessException.class, () -> service.refundByBooking(bookingId, "admin1", null));
        verifyNoInteractions(client);
    }

    @Test void bookingWithoutOnlinePaymentReturnsNull() {
        UUID freeBooking = UUID.randomUUID();
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(freeBooking)).thenReturn(Optional.empty());
        assertNull(service.refundByBooking(freeBooking, "admin1", null));
        verifyNoInteractions(client);
    }

    @Test void reconcileDetectsPendingPaymentThatVnpayMarkedPaid() {
        payment.setStatus(PaymentStatus.PENDING);
        when(client.queryTransaction(eq(payment), any())).thenReturn(gateway(Map.of(
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_Amount", "11000000"), true));

        ReconcileResponse result = service.reconcile(payment.getId(), "127.0.0.1");

        assertFalse(result.isMatched());
        assertEquals(0, new BigDecimal("110000").compareTo(result.getGatewayAmount()));
        assertEquals(1, payment.getPaymentLogs().size());
    }

    @Test void reconcileMatchesSuccessfulPayment() {
        when(client.queryTransaction(any(), any())).thenReturn(gateway(Map.of(
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_Amount", "11000000"), true));
        assertTrue(service.reconcile(payment.getId(), null).isMatched());
    }

    @Test void reconcileDetectsAmountMismatch() {
        when(client.queryTransaction(any(), any())).thenReturn(gateway(Map.of(
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_Amount", "100"), true));
        assertFalse(service.reconcile(payment.getId(), null).isMatched());
    }

    @Test void transactionDateComesFromTxnRefTimestampInVietnamTime() {
        // 1795000000000 ms = 2026-11-18T11:06:40Z = 18:06:40 giờ VN
        assertEquals("20261118180640", VnpayMerchantClient.transactionDate(payment));
        payment.setTransactionId("legacy");
        payment.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        assertEquals("20260101070000", VnpayMerchantClient.transactionDate(payment));
        assertEquals("14123456", VnpayMerchantClient.gatewayTransactionNo(
                Payment.builder().raw("VNPAY_TRANS_NO:14123456").build()));
        assertEquals("", VnpayMerchantClient.gatewayTransactionNo(Payment.builder().raw("https://pay-url").build()));
    }
}
