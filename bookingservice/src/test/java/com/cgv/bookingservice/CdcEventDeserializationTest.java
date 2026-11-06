package com.cgv.bookingservice;

import com.cgv.bookingservice.config.KafkaConsumerConfig;
import com.cgv.commondto.event.PaymentCompletedEvent;
import com.cgv.commondto.event.PaymentFailedEvent;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CdcEventDeserializationTest {
    private JacksonJsonDeserializer<Object> deserializer() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "cdc-contract-test");
        var factory = (DefaultKafkaConsumerFactory<String, Object>) config.consumerFactory();
        var deserializer = new JacksonJsonDeserializer<Object>();
        deserializer.configure(factory.getConfigurationProperties(), false);
        return deserializer;
    }

    private org.apache.kafka.common.header.Headers headers(String type) {
        return new RecordHeaders().add("__TypeId__", type.getBytes(StandardCharsets.UTF_8));
    }

    @Test void cdcPaymentCompletedPreservesDtoIdAmountAndTimestamp() {
        UUID bookingId = UUID.randomUUID();
        byte[] payload = ("{\"bookingId\":\"" + bookingId + "\",\"amount\":340000,\"paidAt\":\"2026-10-13T00:00:00Z\"}").getBytes(StandardCharsets.UTF_8);
        try (var decoder = deserializer()) {
            var event = assertInstanceOf(PaymentCompletedEvent.class,
                    decoder.deserialize("payment.completed", headers("PaymentCompletedEvent"), payload));
            assertEquals(bookingId, event.getBookingId());
            assertEquals(0, event.getAmount().compareTo(new java.math.BigDecimal("340000")));
            assertEquals(Instant.parse("2026-10-13T00:00:00Z"), event.getPaidAt());
        }
    }

    @Test void cdcPaymentFailedRoutesToDifferentDto() {
        try (var decoder = deserializer()) {
            var event = assertInstanceOf(PaymentFailedEvent.class,
                    decoder.deserialize("payment.failed", headers("PaymentFailedEvent"),
                            "{\"failureReason\":\"declined\"}".getBytes(StandardCharsets.UTF_8)));
            assertEquals("declined", event.getFailureReason());
        }
    }

    @Test void missingTypeHeaderFailsInsteadOfAcceptingAnUntypedEvent() {
        try (var decoder = deserializer()) {
            assertThrows(IllegalStateException.class, () -> decoder.deserialize("payment.completed",
                    new RecordHeaders(), "{}".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test void previousSpringProducerTypeHeaderRemainsReadable() {
        try (var decoder = deserializer()) {
            assertInstanceOf(PaymentCompletedEvent.class, decoder.deserialize("payment.completed",
                    headers(PaymentCompletedEvent.class.getName()), "{}".getBytes(StandardCharsets.UTF_8)));
        }
    }
}
