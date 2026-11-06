package com.cgv.bookingservice;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingSeatClaimPostgresTest {
    @Test void twoTransactionsCannotClaimSameSeat() throws Exception {
        String url = System.getenv("CGV_TEST_PG_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "Set CGV_TEST_PG_URL to run real PostgreSQL race");
        String user = System.getenv("CGV_TEST_PG_USER");
        String password = System.getenv("CGV_TEST_PG_PASSWORD");
        UUID showtime = UUID.randomUUID(), seat = UUID.randomUUID();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        try (Connection setup = DriverManager.getConnection(url, user, password)) {
            for (UUID booking : new UUID[]{first, second}) {
                try (PreparedStatement insert = setup.prepareStatement(
                        "INSERT INTO bookings(id,user_id,showtime_id,total_base_amount,final_amount,status,payment_deadline) " +
                        "VALUES (?, 'qa-postgres-race', ?, 100, 100, 'PAYMENT_PENDING', now() + interval '10 minutes')")) {
                    insert.setObject(1, booking);
                    insert.setObject(2, showtime);
                    assertEquals(1, insert.executeUpdate());
                }
            }
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
            try {
                Future<Boolean> one = pool.submit(() -> claim(url, user, password, first, showtime, seat, ready, go));
                Future<Boolean> two = pool.submit(() -> claim(url, user, password, second, showtime, seat, ready, go));
                assertTrue(ready.await(10, TimeUnit.SECONDS));
                go.countDown();
                assertEquals(1, (one.get(15, TimeUnit.SECONDS) ? 1 : 0) + (two.get(15, TimeUnit.SECONDS) ? 1 : 0));
                try (PreparedStatement query = setup.prepareStatement(
                        "SELECT count(*) FROM booking_seat_claims WHERE showtime_id=? AND seat_id=?")) {
                    query.setObject(1, showtime);
                    query.setObject(2, seat);
                    try (ResultSet rows = query.executeQuery()) {
                        assertTrue(rows.next());
                        assertEquals(1, rows.getInt(1));
                    }
                }
            } finally {
                go.countDown();
                pool.shutdownNow();
            }
        } finally {
            try (Connection cleanup = DriverManager.getConnection(url, user, password)) {
                for (String sql : new String[]{
                        "DELETE FROM booking_seat_claims WHERE booking_id IN (?, ?)",
                        "DELETE FROM bookings WHERE id IN (?, ?)"}) {
                    try (PreparedStatement delete = cleanup.prepareStatement(sql)) {
                        delete.setObject(1, first);
                        delete.setObject(2, second);
                        delete.executeUpdate();
                    }
                }
            }
        }
    }

    private boolean claim(String url, String user, String password, UUID booking, UUID showtime,
                          UUID seat, CountDownLatch ready, CountDownLatch go) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            ready.countDown();
            assertTrue(go.await(10, TimeUnit.SECONDS));
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO booking_seat_claims(id,booking_id,showtime_id,seat_id) VALUES (?, ?, ?, ?)")) {
                insert.setObject(1, UUID.randomUUID());
                insert.setObject(2, booking);
                insert.setObject(3, showtime);
                insert.setObject(4, seat);
                insert.executeUpdate();
                Thread.sleep(100);
                connection.commit();
                return true;
            } catch (SQLException error) {
                connection.rollback();
                if ("23505".equals(error.getSQLState())) return false;
                throw error;
            }
        }
    }
}
