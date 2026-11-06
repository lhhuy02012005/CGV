package com.cgv.paymentservice.service;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class BookingPaymentAccess {
    private final JdbcTemplate jdbc;
    public BookingPaymentAccess(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Access(String owner, Instant deadline) {}

    public Access verify(UUID bookingId, BigDecimal amount, String jwtSubject) {
        if (bookingId == null || amount == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Thiếu thông tin thanh toán");
        }
        if (jwtSubject == null || jwtSubject.isBlank() || jwtSubject.startsWith("guest_")) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập để thanh toán");
        }
        String owner;
        BigDecimal actualAmount;
        String status;
        Instant deadline;
        try {
            Object[] row = jdbc.queryForObject(
                    "SELECT user_id, final_amount, status, payment_deadline FROM bookings WHERE id = ? FOR UPDATE",
                    (result, number) -> new Object[]{result.getString(1), result.getBigDecimal(2),
                            result.getString(3), result.getTimestamp(4) == null ? null : result.getTimestamp(4).toInstant()}, bookingId);
            owner = (String) row[0];
            actualAmount = (BigDecimal) row[1];
            status = (String) row[2];
            deadline = (Instant) row[3];
        } catch (EmptyResultDataAccessException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy đơn vé");
        }
        if (!jwtSubject.equals(owner)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Tài khoản không sở hữu đơn vé");
        }
        if (!"PAYMENT_PENDING".equals(status) || deadline == null || !deadline.isAfter(Instant.now())
                || actualAmount.compareTo(amount) != 0 || actualAmount.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Đơn hết hạn hoặc số tiền không hợp lệ");
        }
        return new Access(owner, deadline);
    }

    public boolean lockAndCheckBooking(UUID bookingId, BigDecimal amount) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT status = 'PAYMENT_PENDING' AND final_amount = ? FROM bookings WHERE id = ? FOR UPDATE",
                Boolean.class, amount, bookingId));
        } catch (EmptyResultDataAccessException exception) {
            return false;
        }
    }

}
