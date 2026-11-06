package com.cgv.paymentservice;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.paymentservice.service.BookingPaymentAccess;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookingPaymentAccessTest {
    @Test void onlyOwnerCanPayUnexpiredBookingAtDatabaseAmount() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:member_payment;DB_CLOSE_DELAY=-1"));
        jdbc.execute("CREATE TABLE bookings (id UUID PRIMARY KEY, user_id VARCHAR(100), final_amount DECIMAL(12,2), status VARCHAR(30), payment_deadline TIMESTAMP)");
        String owner = "member-id";
        UUID bookingId = UUID.randomUUID();
        jdbc.update("INSERT INTO bookings VALUES (?, ?, ?, ?, ?)", bookingId, owner, new BigDecimal("110000"),
                "PAYMENT_PENDING", Timestamp.from(Instant.now().plusSeconds(600)));
        BookingPaymentAccess access = new BookingPaymentAccess(jdbc);

        assertEquals(owner, access.verify(bookingId, new BigDecimal("110000"), owner).owner());
        assertThrows(BusinessException.class, () -> access.verify(bookingId, new BigDecimal("1"), owner));
        assertThrows(BusinessException.class, () -> access.verify(bookingId, new BigDecimal("110000"), "other-user"));
        assertThrows(BusinessException.class, () -> access.verify(bookingId, new BigDecimal("110000"), null));
        jdbc.update("UPDATE bookings SET status = 'CANCELLED' WHERE id = ?", bookingId);
        assertThrows(BusinessException.class, () -> access.verify(bookingId, new BigDecimal("110000"), owner));
        jdbc.update("UPDATE bookings SET status = 'PAYMENT_PENDING', payment_deadline = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), bookingId);
        assertThrows(BusinessException.class, () -> access.verify(bookingId, new BigDecimal("110000"), owner));
    }
}
