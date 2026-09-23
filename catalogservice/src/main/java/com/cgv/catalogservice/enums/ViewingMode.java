package com.cgv.catalogservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ViewingMode {

    SUBTITLED("Phụ đề"),
    DUBBED("Lồng tiếng"),
    VOICEOVER("Thuyết minh");

    private final String description;

    ViewingMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String getCode() {
        return name();
    }

    @JsonCreator
    public static ViewingMode fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SUBTITLED;
        }
        for (ViewingMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value) || mode.description.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        String lower = value.toLowerCase();
        if (lower.contains("lồng tiếng")) return DUBBED;
        if (lower.contains("thuyết minh")) return VOICEOVER;
        return SUBTITLED;
    }
}
