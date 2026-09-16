package com.cgv.catalogservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Amenity {

    IMAX("IMAX"),
    FOUR_D("4D"),
    DOLBY_ATMOS("DOLBY_ATMOS"),
    SWEETBOX("SWEETBOX"),
    PARKING("PARKING"),
    ONLINE_BOOKING("ONLINE_BOOKING"),
    FOOD_COURT("FOOD_COURT");

    private final String dbValue;

    Amenity(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static Amenity fromDbValue(String dbValue) {
        for (Amenity amenity : values()) {
            if (amenity.dbValue.equals(dbValue)) {
                return amenity;
            }
        }

        throw new IllegalArgumentException(
                "Unknown Amenity value: " + dbValue
        );
    }
}
