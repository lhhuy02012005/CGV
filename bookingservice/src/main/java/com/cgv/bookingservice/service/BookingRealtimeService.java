package com.cgv.bookingservice.service;

import com.cgv.bookingservice.dto.realtime.BookingRealtimeEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Slf4j(topic = "BOOKING-REALTIME-SERVICE")
public class BookingRealtimeService {

    private static final Long BOOKING_EMITTER_TIMEOUT = 900_000L;  // 15 mins (chờ thanh toán)
    private static final Long GENERAL_EMITTER_TIMEOUT = 1800_000L; // 30 mins (soát vé tại rạp)

    private final ConcurrentMap<UUID, List<SseEmitter>> bookingEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> generalEmitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public BookingRealtimeService() {
        // Gửi heartbeat định kỳ 25s chống gateway/proxy timeout
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 15, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribeBooking(UUID bookingId) {
        SseEmitter emitter = new SseEmitter(BOOKING_EMITTER_TIMEOUT);
        bookingEmitters.computeIfAbsent(bookingId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = bookingEmitters.get(bookingId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    bookingEmitters.remove(bookingId);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(BookingRealtimeEvent.builder()
                            .type("CONNECTED")
                            .bookingId(bookingId)
                            .payload("Waiting for booking payment confirmation")
                            .timestamp(Instant.now())
                            .build(), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE connect event for booking {}: {}", bookingId, e.getMessage());
            cleanup.run();
        }

        log.info("Client subscribed to payment stream for bookingId: {}", bookingId);
        return emitter;
    }

    public SseEmitter subscribeGeneral() {
        SseEmitter emitter = new SseEmitter(GENERAL_EMITTER_TIMEOUT);
        generalEmitters.add(emitter);

        Runnable cleanup = () -> generalEmitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(BookingRealtimeEvent.builder()
                            .type("CONNECTED")
                            .payload("Connected to Booking & Gate Check-in Stream")
                            .timestamp(Instant.now())
                            .build(), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE connect for general stream: {}", e.getMessage());
            cleanup.run();
        }

        log.info("New client subscribed to General Booking/Check-in stream. Total: {}", generalEmitters.size());
        return emitter;
    }

    public void broadcastBooking(UUID bookingId, String type, Object payload) {
        BookingRealtimeEvent event = BookingRealtimeEvent.builder()
                .type(type)
                .bookingId(bookingId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        log.info("Broadcasting Booking Realtime Event: type={}, bookingId={}", type, bookingId);

        // 1. Gửi cho client đang chờ thanh toán đơn vé này
        List<SseEmitter> emitters = bookingEmitters.get(bookingId);
        if (emitters != null && !emitters.isEmpty()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(type)
                            .data(event, MediaType.APPLICATION_JSON));
                    // Nếu là thanh toán hoàn tất hoặc hủy thì kết thúc stream của booking đó
                    if ("PAYMENT_CONFIRMED".equals(type) || "PAYMENT_FAILED".equals(type)) {
                        emitter.complete();
                    }
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty() || "PAYMENT_CONFIRMED".equals(type) || "PAYMENT_FAILED".equals(type)) {
                bookingEmitters.remove(bookingId);
            }
        }

        // 2. Gửi luôn cho general stream (admin dashboard & máy quét vé)
        broadcastGeneralEvent(event);
    }

    public void broadcastGeneral(String type, Object payload) {
        BookingRealtimeEvent event = BookingRealtimeEvent.builder()
                .type(type)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        broadcastGeneralEvent(event);
    }

    private void broadcastGeneralEvent(BookingRealtimeEvent event) {
        if (generalEmitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : generalEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            generalEmitters.removeAll(deadEmitters);
        }
    }

    private void sendHeartbeat() {
        // Heartbeat booking emitters
        bookingEmitters.forEach((bookingId, list) -> {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("PING").comment("keep-alive").data("{\"ping\": true}"));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            list.removeAll(dead);
        });

        // Heartbeat general emitters
        if (!generalEmitters.isEmpty()) {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : generalEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("PING").comment("keep-alive").data("{\"ping\": true}"));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            generalEmitters.removeAll(dead);
        }
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdown();
        bookingEmitters.values().forEach(list -> list.forEach(e -> {
            try { e.complete(); } catch (Exception ignored) {}
        }));
        bookingEmitters.clear();
        generalEmitters.forEach(e -> {
            try { e.complete(); } catch (Exception ignored) {}
        });
        generalEmitters.clear();
    }
}
