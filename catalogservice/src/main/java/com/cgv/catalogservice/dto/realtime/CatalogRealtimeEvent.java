package com.cgv.catalogservice.dto.realtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogRealtimeEvent implements Serializable {
    private String type;
    private Object payload;
    private Instant timestamp;
}
