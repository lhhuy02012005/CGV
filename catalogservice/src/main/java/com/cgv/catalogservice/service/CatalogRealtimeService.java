package com.cgv.catalogservice.service;

import com.cgv.catalogservice.dto.realtime.CatalogRealtimeEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "CATALOG-REALTIME-SERVICE")
public class CatalogRealtimeService {

    private static final Long EMITTER_TIMEOUT = 1800_000L; // 30 minutes
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public CatalogRealtimeService() {
        // Heartbeat every 25 seconds to keep HTTP connections alive through proxies/gateways
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 15, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitters.add(emitter);

        Runnable cleanup = () -> emitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send initial connection acknowledgement
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(CatalogRealtimeEvent.builder()
                            .type("CONNECTED")
                            .payload("CGV Catalog Realtime Stream established")
                            .timestamp(Instant.now())
                            .build(), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE connect event: {}", e.getMessage());
            emitters.remove(emitter);
        }

        log.info("New SSE client subscribed to Catalog Realtime. Total active: {}", emitters.size());
        return emitter;
    }

    public void broadcast(String type, Object payload) {
        if (emitters.isEmpty()) {
            return;
        }

        CatalogRealtimeEvent event = CatalogRealtimeEvent.builder()
                .type(type)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        log.info("Broadcasting Catalog Realtime Event: {} to {} clients", type, emitters.size());

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Removed {} dead SSE emitters. Remaining active: {}", deadEmitters.size(), emitters.size());
        }
    }

    private void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("PING")
                        .comment("keep-alive")
                        .data("{\"ping\": true}"));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
        }
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdown();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }
        emitters.clear();
    }
}
