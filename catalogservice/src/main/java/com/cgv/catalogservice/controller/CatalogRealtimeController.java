package com.cgv.catalogservice.controller;

import com.cgv.catalogservice.service.CatalogRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Realtime", description = "Server-Sent Events (SSE) cho hệ thống realtime trạng thái rạp, phòng, phim, suất chiếu")
@RestController
@RequestMapping("/realtime")
@RequiredArgsConstructor
public class CatalogRealtimeController {

    private final CatalogRealtimeService catalogRealtimeService;

    @Operation(
            summary = "Đăng ký nhận luồng sự kiện realtime",
            description = "Luồng Server-Sent Events (SSE) đẩy thông báo trực tiếp khi có thay đổi về trạng thái rạp, phòng chiếu, phim, hoặc lịch chiếu."
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return catalogRealtimeService.subscribe();
    }
}
