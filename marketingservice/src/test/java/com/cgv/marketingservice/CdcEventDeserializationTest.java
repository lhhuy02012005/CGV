package com.cgv.marketingservice;

import com.cgv.marketingservice.config.KafkaConsumerConfig;
import com.cgv.commondto.event.BookingConfirmedEvent;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CdcEventDeserializationTest {
    @Test void cdcBookingConfirmedIsDecodedByActualConsumerConfiguration() {
        var config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "cdc-contract-test");
        var factory = (DefaultKafkaConsumerFactory<String, Object>) config.consumerFactory();
        UUID bookingId = UUID.randomUUID();
        byte[] payload = ("{\"bookingId\":\"" + bookingId + "\",\"seatLabels\":[\"A1\"]}").getBytes(StandardCharsets.UTF_8);
        var headers = new RecordHeaders().add("__TypeId__", "BookingConfirmedEvent".getBytes(StandardCharsets.UTF_8));
        try (var decoder = new JacksonJsonDeserializer<Object>()) {
            decoder.configure(factory.getConfigurationProperties(), false);
            var event = assertInstanceOf(BookingConfirmedEvent.class,
                    decoder.deserialize("booking.confirmed", headers, payload));
            assertEquals(bookingId, event.getBookingId());
            assertEquals(java.util.List.of("A1"), event.getSeatLabels());
        }
    }
}
