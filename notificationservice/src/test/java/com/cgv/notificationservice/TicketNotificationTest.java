package com.cgv.notificationservice;

import com.cgv.commondto.event.BookingConfirmedEvent;
import com.cgv.notificationservice.consumer.NotificationConsumer;
import com.cgv.notificationservice.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.mockito.Mockito.*;

class TicketNotificationTest {
    @Test
    void acknowledgesOnlyAfterTicketEmailIsSent() {
        EmailService email = mock(EmailService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        BookingConfirmedEvent event = BookingConfirmedEvent.builder().bookingId(UUID.randomUUID())
                .userEmail("guest@example.com").build();
        new NotificationConsumer(email).handleConfirmedBooking(event, acknowledgment);
        var order = inOrder(email, acknowledgment);
        order.verify(email).sendTicket(event);
        order.verify(acknowledgment).acknowledge();
    }
}
