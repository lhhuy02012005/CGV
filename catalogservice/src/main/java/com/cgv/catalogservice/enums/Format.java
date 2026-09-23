package com.cgv.catalogservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Format {

    TWO_D("2D"),
    THREE_D("3D"),
    IMAX("IMAX"),
    FOUR_D("4DX"),
    SCREENX("SCREENX");

    private final String dbValue;

    Format(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static Format fromDbValue(String dbValue) {
        if (dbValue == null) return TWO_D;
        if ("4D".equalsIgnoreCase(dbValue) || "4DX".equalsIgnoreCase(dbValue)) {
            return FOUR_D;
        }
        for (Format format : values()) {
            if (format.dbValue.equalsIgnoreCase(dbValue) || format.name().equalsIgnoreCase(dbValue)) {
                return format;
            }
        }

        return TWO_D;
    }
}
