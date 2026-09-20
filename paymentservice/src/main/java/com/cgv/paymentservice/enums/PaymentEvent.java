package com.cgv.paymentservice.enums;

public enum PaymentEvent {
    INITIATED,
    REDIRECT,
    CALLBACK_RECEIVED,
    SUCCESS,
    FAILED,
    REFUND_REQUESTED,
    REFUNDED
}
