package com.cgv.catalogservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Format {

    TWO_D("2D"),
    THREE_D("3D"),
    IMAX("IMAX"),
    FOUR_D("4D"),
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
        for (Format format : values()) {
            if (format.dbValue.equals(dbValue)) {
                return format;
            }
        }

        throw new IllegalArgumentException(
                "Unknown Format value: " + dbValue
        );
    }
}
