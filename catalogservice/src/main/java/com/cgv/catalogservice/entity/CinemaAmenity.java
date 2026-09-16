package com.cgv.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "cinema_amenities",
        indexes = {
                @Index(name = "idx_cinema_amenity", columnList = "amenity")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaAmenity {

    @EmbeddedId
    CinemaAmenityId id;

    @MapsId("cinemaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    Cinema cinema;
}
