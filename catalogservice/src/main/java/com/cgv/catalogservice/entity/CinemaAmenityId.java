package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.converter.AmenityConverter;
import com.cgv.catalogservice.enums.Amenity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaAmenityId implements Serializable {

    @Column(name = "cinema_id", nullable = false)
    UUID cinemaId;

    @Convert(converter = AmenityConverter.class)
    @Column(name = "amenity", nullable = false)
    Amenity amenity;
}
