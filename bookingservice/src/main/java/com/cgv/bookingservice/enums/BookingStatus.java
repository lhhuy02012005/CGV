package com.cgv.bookingservice.enums;

public enum BookingStatus {
    SEAT_RESERVED,
    PAYMENT_PENDING,
    CONFIRMED,
    USED,
    CANCELLED,
    CANCELLED_DUE_TO_MAINTENANCE, //Khi ghế/lịch bị Admin hủy trong lúc chờ thanh toán
    REFUNDED
}
